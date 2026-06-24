package ru.fpvladder.laps.trainer.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.fpvladder.laps.trainer.settings.USB_ENABLED

/**
 * Singleton manager responsible for the USB HID keyboard feature state.
 *
 * The manager owns a finite state machine with these states:
 * Disabled → Disconnected → Permission → Setup → Connected(devices).
 *
 * Multiple keyboards may be connected simultaneously. However, only one
 * keyboard at a time may be in the permission/setup flow.
 *
 * Actual USB HID open/claim/read logic is intentionally left as TODO.
 */
class UsbHidManager private constructor(context: Context) {

    companion object {
        private const val TAG = "UsbHidManager"
        private const val ACTION_USB_PERMISSION = "ru.fpvladder.laps.trainer.USB_PERMISSION"

        @Volatile
        private var instance: UsbHidManager? = null

        fun getInstance(context: Context): UsbHidManager {
            return instance ?: synchronized(this) {
                instance ?: UsbHidManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    private val appContext: Context = context.applicationContext
    private val usbManager: UsbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _state = MutableStateFlow<UsbHidState>(UsbHidState.Disabled)
    val state: StateFlow<UsbHidState> = _state.asStateFlow()

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _isDiscoveryAllowed = MutableStateFlow(false)
    val isDiscoveryAllowed: StateFlow<Boolean> = _isDiscoveryAllowed.asStateFlow()

    private val knownKeyboards: MutableSet<UsbHidInfo> = mutableSetOf()

    private val _knownDevices = MutableStateFlow<Set<UsbHidInfo>>(emptySet())
    val knownDevices: StateFlow<Set<UsbHidInfo>> = _knownDevices.asStateFlow()
    private val blacklistedDeviceNames: MutableSet<String> = mutableSetOf()
    private val connectedDevices: MutableMap<String, UsbHidInfo> = mutableMapOf()

    private var receiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device = extractDevice(intent)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (device == null) {
                        logEvent("permission result", "granted=$granted, device=null")
                        return
                    }
                    if (granted) {
                        val info = UsbHidInfo.from(device)
                        logEvent("permission result", "granted=true, info=$info")
                        onPermissionResultInternal(device.deviceName, granted = true)
                    } else {
                        logEvent("permission result", "granted=false, deviceName=${device.deviceName}")
                        onPermissionResultInternal(device.deviceName, granted = false)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = extractDevice(intent)
                    if (device == null) {
                        logEvent("device attached", "device=null")
                        return
                    }
                    val identity = UsbHidInfo.formatIdentity(device)
                    logEvent("device attached", "deviceName=${device.deviceName}, identity=$identity")
                    handleDeviceAttached(device)
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = extractDevice(intent)
                    if (device == null) {
                        logEvent("device detached", "device=null")
                        return
                    }
                    val identity = UsbHidInfo.formatIdentity(device)
                    logEvent("device detached", "deviceName=${device.deviceName}, identity=$identity")
                    handleDeviceDetached(device.deviceName)
                }
            }
        }
    }

    /**
     * Reflects the user setting (e.g. from the Settings screen).
     * The actual [isEnabled] state also depends on [USB_ENABLED].
     */
    fun setUserEnabled(enabled: Boolean) {
        val newEnabled = enabled && USB_ENABLED
        logEvent("setUserEnabled", "requested=$enabled, effective=$newEnabled")
        if (_isEnabled.value == newEnabled) return

        _isEnabled.value = newEnabled
        if (newEnabled) {
            registerUsbReceiver()
            transitionTo(UsbHidState.Disconnected, "enabled")
        } else {
            unregisterUsbReceiver()
            clearConnectedDevices("disabled")
            transitionTo(UsbHidState.Disabled, "disabled")
        }
    }

    /**
     * Tells the manager whether the current application state permits
     * discovering and connecting new keyboards.
     */
    fun setDiscoveryAllowed(allowed: Boolean) {
        logEvent("setDiscoveryAllowed", "allowed=$allowed")
        if (_isDiscoveryAllowed.value == allowed) return
        _isDiscoveryAllowed.value = allowed

        if (!_isEnabled.value) return

        when (_state.value) {
            is UsbHidState.Permission,
            is UsbHidState.Setup -> {
                if (!allowed) {
                    transitionTo(afterFlowBrokenState(), "discovery disallowed")
                }
            }

            else -> {
                // Disconnected and Connected are not affected by the discovery flag.
            }
        }
    }

    /**
     * Called by UI when the system permission dialog is dismissed.
     */
    fun onPermissionResult(deviceName: String, granted: Boolean) {
        logEvent("onPermissionResult", "deviceName=$deviceName, granted=$granted")
        onPermissionResultInternal(deviceName, granted)
    }

    /**
     * Called by UI when the user confirmed keyboard setup (OK).
     */
    fun onSetupConfirmed(deviceName: String) {
        logEvent("onSetupConfirmed", "deviceName=$deviceName")
        val current = _state.value
        if (current is UsbHidState.Setup && current.deviceName == deviceName) {
            addKnownKeyboard(current.info)
            addConnectedDevice(deviceName, current.info)
        } else {
            logEvent("onSetupConfirmed ignored", "state=$current")
        }
    }

    /**
     * Called by UI when the user cancelled keyboard setup.
     */
    fun onSetupCancelled(deviceName: String) {
        logEvent("onSetupCancelled", "deviceName=$deviceName")
        val current = _state.value
        if (current is UsbHidState.Setup && current.deviceName == deviceName) {
            addToBlacklist(deviceName)
            transitionTo(afterFlowBrokenState(), "setup cancelled")
        } else {
            logEvent("onSetupCancelled ignored", "state=$current")
        }
    }

    private fun onPermissionResultInternal(deviceName: String, granted: Boolean) {
        val current = _state.value
        if (current !is UsbHidState.Permission || current.deviceName != deviceName) {
            logEvent("permission result ignored", "state=$current, deviceName=$deviceName")
            return
        }

        if (!granted) {
            addToBlacklist(deviceName)
            transitionTo(afterFlowBrokenState(), "permission denied")
            return
        }

        val device = findUsbDevice(deviceName)
        if (device == null) {
            logEvent("permission result ignored", "device not found, deviceName=$deviceName")
            transitionTo(afterFlowBrokenState(), "device missing after grant")
            return
        }

        val actualInfo = UsbHidInfo.from(device)
        logEvent("permission granted", "info=$actualInfo")

        if (isKnown(actualInfo.identity)) {
            addConnectedDevice(deviceName, actualInfo)
        } else {
            transitionTo(UsbHidState.Setup(deviceName, actualInfo), "permission granted, unknown device")
        }
    }

    private fun handleDeviceAttached(device: UsbDevice) {
        val identity = UsbHidInfo.formatIdentity(device)
        val deviceName = device.deviceName

        if (!_isEnabled.value) {
            logEvent("device attached ignored", "not enabled, deviceName=$deviceName, identity=$identity")
            return
        }
        if (!_isDiscoveryAllowed.value) {
            logEvent("device attached ignored", "discovery not allowed, deviceName=$deviceName, identity=$identity")
            return
        }
        if (blacklistedDeviceNames.contains(deviceName)) {
            logEvent("device attached ignored", "blacklisted, deviceName=$deviceName, identity=$identity")
            return
        }
        if (!isHidKeyboard(device)) {
            logEvent("device attached ignored", "not a HID keyboard, deviceName=$deviceName, identity=$identity")
            return
        }
        if (isConnected(deviceName)) {
            logEvent("device attached ignored", "already connected, deviceName=$deviceName, identity=$identity")
            return
        }

        when (_state.value) {
            is UsbHidState.Disconnected,
            is UsbHidState.Connected -> {
                if (usbManager.hasPermission(device)) {
                    val actualInfo = UsbHidInfo.from(device)
                    if (isKnown(actualInfo.identity)) {
                        addConnectedDevice(deviceName, actualInfo)
                    } else {
                        transitionTo(UsbHidState.Setup(deviceName, actualInfo), "unknown device, permission already granted")
                    }
                } else {
                    transitionTo(UsbHidState.Permission(deviceName), "requesting permission")
                    requestPermission(device)
                }
            }

            is UsbHidState.Permission,
            is UsbHidState.Setup -> {
                logEvent("device attached ignored", "permission/setup flow in progress, deviceName=$deviceName, identity=$identity")
            }

            is UsbHidState.Disabled -> {
                logEvent("device attached ignored", "disabled, deviceName=$deviceName, identity=$identity")
            }
        }
    }

    private fun handleDeviceDetached(deviceName: String) {
        removeFromBlacklist(deviceName)

        val current = _state.value
        when (current) {
            is UsbHidState.Permission -> if (current.deviceName == deviceName) {
                transitionTo(afterFlowBrokenState(), "permission device detached")
                return
            }

            is UsbHidState.Setup -> if (current.deviceName == deviceName) {
                transitionTo(afterFlowBrokenState(), "setup device detached")
                return
            }

            else -> {}
        }

        if (isConnected(deviceName)) {
            removeConnectedDevice(deviceName)
        } else {
            logEvent("device detached ignored", "not connected, deviceName=$deviceName")
        }
    }

    private fun afterFlowBrokenState(): UsbHidState {
        return if (connectedDevices.isEmpty()) UsbHidState.Disconnected else connectedState()
    }

    private fun connectedState(): UsbHidState.Connected {
        return UsbHidState.Connected(connectedDevices.values.sortedBy { it.identity }.toSet())
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
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
        logEvent("usb receiver", "registered")
    }

    private fun unregisterUsbReceiver() {
        if (!receiverRegistered) return
        try {
            appContext.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
        receiverRegistered = false
        logEvent("usb receiver", "unregistered")
    }

    private fun requestPermission(device: UsbDevice) {
        val pi = PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName),
            PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(device, pi)
        logEvent("requestPermission", "deviceName=${device.deviceName}")
    }

    private fun findUsbDevice(deviceName: String): UsbDevice? {
        return usbManager.deviceList.values.find { it.deviceName == deviceName }
    }

    private fun isHidKeyboard(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT &&
                        ep.direction == UsbConstants.USB_DIR_IN
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun extractDevice(intent: Intent): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }

    private fun isKnown(identity: String): Boolean {
        return knownKeyboards.any { it.identity == identity }
    }

    private fun addKnownKeyboard(info: UsbHidInfo) {
        if (knownKeyboards.add(info)) {
            _knownDevices.value = knownKeyboards.toSet()
            logEvent("known keyboards", "added info=$info")
        }
    }

    private fun isConnected(deviceName: String): Boolean {
        return connectedDevices.containsKey(deviceName)
    }

    private fun addConnectedDevice(deviceName: String, info: UsbHidInfo) {
        if (connectedDevices.put(deviceName, info) != null) return
        logEvent("connected devices", "added deviceName=$deviceName, info=$info, count=${connectedDevices.size}")
        transitionTo(connectedState(), "device connected")
    }

    private fun removeConnectedDevice(deviceName: String) {
        if (connectedDevices.remove(deviceName) == null) return
        val reason = "device detached, count=${connectedDevices.size}"
        if (connectedDevices.isEmpty()) {
            transitionTo(UsbHidState.Disconnected, reason)
        } else {
            transitionTo(connectedState(), reason)
        }
    }

    private fun clearConnectedDevices(reason: String) {
        if (connectedDevices.isEmpty()) return
        connectedDevices.clear()
        logEvent("connected devices", "cleared, reason=$reason")
    }

    private fun addToBlacklist(deviceName: String) {
        if (blacklistedDeviceNames.add(deviceName)) {
            logEvent("blacklist", "added deviceName=$deviceName")
        }
    }

    private fun removeFromBlacklist(deviceName: String) {
        if (blacklistedDeviceNames.remove(deviceName)) {
            logEvent("blacklist", "removed deviceName=$deviceName")
        }
    }

    private fun transitionTo(newState: UsbHidState, reason: String) {
        val oldState = _state.value
        if (oldState == newState) return
        _state.value = newState
        Log.d(TAG, "state: $oldState → $newState, reason=$reason")
    }

    private fun logEvent(event: String, details: String = "") {
        if (details.isEmpty()) {
            Log.d(TAG, "event: $event")
        } else {
            Log.d(TAG, "event: $event, $details")
        }
    }
}
