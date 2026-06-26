package ru.fpvladder.laps.trainer.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.fpvladder.laps.trainer.settings.USB_ENABLED

/**
 * Singleton manager responsible for the USB HID keyboard feature state.
 *
 * The manager owns an aggregate UI state machine ([UsbHidState]) while the real USB work is
 * delegated to per-device [UsbHidSession] instances.
 *
 * UI-visible states: Disabled → Disconnected → Permission → Setup → Connected(devices)
 *
 * Multiple keyboards may be captured simultaneously; each has its own session.
 */
class UsbHidManager private constructor(context: Context) {

    companion object {
        private const val TAG = "UsbHidManager"
        private const val ACTION_USB_PERMISSION = "ru.fpvladder.laps.trainer.USB_PERMISSION"

        @Volatile
        private var instance: UsbHidManager? = null

        fun getInstance(context: Context): UsbHidManager {
            return instance ?: synchronized(this) {
                instance ?: UsbHidManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val appContext: Context = context.applicationContext
    private val usbManager: UsbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<UsbHidState>(UsbHidState.Disabled)
    val state: StateFlow<UsbHidState> = _state.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    private val _isDiscoveryAllowed = MutableStateFlow(false)

    private val configRepository = UsbHidConfigRepository(appContext)
    private val _configs = MutableStateFlow<List<UsbHidConfig>>(emptyList())
    val configs: StateFlow<List<UsbHidConfig>> = _configs.asStateFlow()

    init {
        runBlocking(Dispatchers.IO) { configRepository.load() }
        _configs.value = configRepository.all()
    }

    fun saveConfig(config: UsbHidConfig) {
        managerScope.launch {
            configRepository.save(config)
            _configs.value = configRepository.all()
        }
    }

    fun removeConfig(identity: String) {
        // Release any active session for this keyboard identity.
        sessions.values.forEach { session ->
            if (session.info.identity == identity) {
                session.release()
            }
        }

        managerScope.launch {
            configRepository.remove(identity)
            _configs.value = configRepository.all()
        }
    }

    private val _lastKeyEvent = MutableStateFlow<UsbHidEvent?>(null)
    val lastKeyEvent: StateFlow<UsbHidEvent?> = _lastKeyEvent.asStateFlow()

    private val _keyEvents = MutableSharedFlow<UsbHidEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val keyEvents: SharedFlow<UsbHidEvent> = _keyEvents.asSharedFlow()

    // Accessed only on the main thread.
    private val blacklistedDeviceNames = mutableSetOf<String>()
    private val sessions = ConcurrentHashMap<String, UsbHidSession>()

    private var receiverRegistered = false

    private val usbReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_USB_PERMISSION -> {
                        val device = extractDevice(intent)
                        val granted =
                            intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (device == null) {
                            logEvent("permission result", "granted=$granted, device=null")
                            return
                        }
                        onPermissionResult(device.deviceName, granted)
                    }

                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        val device = extractDevice(intent)
                        if (device == null) {
                            logEvent("device attached", "device=null")
                            return
                        }
                        handleDeviceAttached(device)
                    }

                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        val device = extractDevice(intent)
                        if (device == null) {
                            logEvent("device detached", "device=null")
                            return
                        }
                        handleDeviceDetached(device.deviceName)
                    }
                }
            }
        }

    /**
     * Reflects the user setting (e.g. from the Settings screen). The actual [isEnabled] state also
     * depends on [USB_ENABLED].
     */
    fun setUserEnabled(enabled: Boolean) {
        val newEnabled = enabled && USB_ENABLED
        logEvent("setUserEnabled", "requested=$enabled, effective=$newEnabled")
        if (_isEnabled.value == newEnabled) return

        _isEnabled.value = newEnabled
        if (newEnabled) {
            registerUsbReceiver()
            transitionTo(UsbHidState.Disconnected)
            scanAttachedDevices()
        } else {
            unregisterUsbReceiver()
            releaseAllSessions()
            transitionTo(UsbHidState.Disabled)
        }
    }

    /**
     * Tells the manager whether the current application state permits discovering and connecting new
     * keyboards.
     */
    fun setDiscoveryAllowed(allowed: Boolean) {
        logEvent("setDiscoveryAllowed", "allowed=$allowed")
        if (_isDiscoveryAllowed.value == allowed) return
        _isDiscoveryAllowed.value = allowed

        if (!_isEnabled.value) return

        if (!allowed) {
            sessions.values.toList().forEach { session ->
                when (session.state.value) {
                    UsbHidSessionState.RequestingPermission,
                    UsbHidSessionState.SetupPending -> {
                        session.release()
                    }

                    else -> {}
                }
            }
            cleanupReleasedSessions()
            recomputeAggregateState()
        } else {
            scanAttachedDevices()
        }
    }

    /** Called by UI when the system permission dialog is dismissed. */
    fun onPermissionResult(deviceName: String, granted: Boolean) {
        if (!granted) {
            postToMain { addToBlacklist(deviceName) }
        }
        sessions[deviceName]?.onPermissionResult(granted)
    }

    /** Called by UI when the user confirmed keyboard setup (OK). */
    fun onSetupConfirmed(deviceName: String) {
        val session = sessions[deviceName]
        if (session != null && session.state.value == UsbHidSessionState.SetupPending) {
            session.confirmSetup()
        }
    }

    /** Called by UI when the user cancelled keyboard setup. */
    fun onSetupCancelled(deviceName: String) {
        val session = sessions[deviceName]
        if (session != null && session.state.value == UsbHidSessionState.SetupPending) {
            addToBlacklist(deviceName)
            session.cancelSetup()
        }
    }

    private fun handleDeviceAttached(device: UsbDevice) {
        val identity = UsbHidInfo.formatIdentity(device)
        val deviceName = device.deviceName

        if (!_isEnabled.value) {
            logEvent(
                "device attached ignored",
                "not enabled, deviceName=$deviceName, identity=$identity"
            )
            return
        }
        if (!_isDiscoveryAllowed.value) return
        if (blacklistedDeviceNames.contains(deviceName)) {
            logEvent(
                "device attached ignored",
                "blacklisted, deviceName=$deviceName, identity=$identity"
            )
            return
        }
        if (!isHidKeyboard(device)) {
            logEvent(
                "device attached ignored",
                "not a HID keyboard, deviceName=$deviceName, identity=$identity",
            )
            return
        }

        configRepository.findByIdentity(identity)?.let {
            managerScope.launch {
                configRepository.touch(identity)
                _configs.value = configRepository.all()
            }
        }

        sessions[deviceName]?.let { existing ->
            if (existing.state.value == UsbHidSessionState.Released) {
                sessions.remove(deviceName)
            } else {
                return
            }
        }

        val session = UsbHidSession(
            deviceName = deviceName,
            device = device,
            usbManager = usbManager,
            scope = managerScope,
            isKnownIdentity = ::isKnown,
            onStateChanged = { state ->
                postToMain {
                    if (state == UsbHidSessionState.Released) {
                        cleanupReleasedSessions()
                    }
                    recomputeAggregateState()
                }
            },
            onKeyEvent = { event ->
                val info = sessions[deviceName]?.info
                if (info != null) {
                    val mappedAction = configRepository.mappedAction(info.identity, event.keyCode, event.modifiers)
                    val mappedEvent = event.copy(action = mappedAction)
                    _lastKeyEvent.value = mappedEvent
                    _keyEvents.tryEmit(mappedEvent)
                    val keyHex = event.keyCode.toString(16).uppercase().padStart(2, '0')
                    val modsHex = event.modifiers.toString(16).uppercase().padStart(2, '0')
                    val state = if (event.state == UsbHidEvent.STATE_DOWN) "down" else "up"
                    logEvent("key", "0x$keyHex 0x$modsHex $state mapped=$mappedAction")
                }
            },
        )

        sessions[deviceName] = session

        if (usbManager.hasPermission(device)) {
            logEvent(
                "device attached",
                "permission already granted, deviceName=$deviceName, identity=$identity",
            )
            session.onPermissionResult(true)
        } else {
            logEvent(
                "device attached",
                "requesting permission, deviceName=$deviceName, identity=$identity",
            )
            session.requestPermission(createPermissionPendingIntent(deviceName))
        }
    }

    private fun handleDeviceDetached(deviceName: String) {
        removeFromBlacklist(deviceName)
        sessions[deviceName]?.detach()
    }

    private fun scanAttachedDevices() {
        if (!_isDiscoveryAllowed.value) return
        usbManager.deviceList.values.forEach { device -> handleDeviceAttached(device) }
    }

    private fun releaseAllSessions() {
        sessions.values.toList().forEach { it.release() }
        cleanupReleasedSessions()
    }

    private fun cleanupReleasedSessions() {
        sessions.entries
            .filter { it.value.state.value == UsbHidSessionState.Released }
            .forEach { sessions.remove(it.key) }
    }

    private fun recomputeAggregateState() {
        if (!_isEnabled.value) {
            if (_state.value != UsbHidState.Disabled) {
                transitionTo(UsbHidState.Disabled)
            }
            return
        }

        cleanupReleasedSessions()

        val activeSessions =
            sessions.values.filter { it.state.value != UsbHidSessionState.Released }

        val setupSession = activeSessions.find { it.state.value == UsbHidSessionState.SetupPending }
        if (setupSession != null) {
            transitionTo(UsbHidState.Setup(setupSession.deviceName, setupSession.info))
            return
        }

        val permissionSession =
            activeSessions.find { it.state.value == UsbHidSessionState.RequestingPermission }
        if (permissionSession != null) {
            transitionTo(UsbHidState.Permission(permissionSession.deviceName))
            return
        }

        val activeDevices =
            activeSessions.filter { it.state.value == UsbHidSessionState.Active }.map { it.info }
                .toSet()

        if (activeDevices.isNotEmpty()) {
            transitionTo(UsbHidState.Connected(activeDevices))
        } else {
            transitionTo(UsbHidState.Disconnected)
        }
    }

    private fun postToMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    private fun registerUsbReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            appContext,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterUsbReceiver() {
        if (!receiverRegistered) return
        try {
            appContext.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
        receiverRegistered = false
    }

    private fun createPermissionPendingIntent(deviceName: String): PendingIntent {
        return PendingIntent.getBroadcast(
            appContext,
            deviceName.hashCode(),
            Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName),
            PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun isKnown(identity: String): Boolean {
        return configRepository.findByIdentity(identity) != null
    }

    private fun addToBlacklist(deviceName: String) {
        blacklistedDeviceNames.add(deviceName)
    }

    private fun removeFromBlacklist(deviceName: String) {
        blacklistedDeviceNames.remove(deviceName)
    }

    private fun isHidKeyboard(device: UsbDevice): Boolean {
        val (iface, endpoint) = findHidInterruptInEndpoint(device)
        return iface != null && endpoint != null
    }

    private fun extractDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun transitionTo(newState: UsbHidState) {
        val oldState = _state.value
        if (oldState == newState) return
        if (newState is UsbHidState.Setup) {
            _lastKeyEvent.value = null
        }
        _state.value = newState
        Log.d(TAG, "state: $oldState → $newState")
    }

    private fun logEvent(event: String, details: String = "") {
        if (details.isEmpty()) {
            Log.d(TAG, "event: $event")
        } else {
            Log.d(TAG, "event: $event, $details")
        }
    }
}
