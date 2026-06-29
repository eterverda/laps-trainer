package ru.fpvladder.laps.trainer.usb

import android.app.PendingIntent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * Per-device state machine for a USB HID keyboard.
 *
 * This class owns the real USB flow for one keyboard: permission request, exclusive interface
 * capture, interrupt endpoint read loop and release.
 *
 * UI-visible aggregate state is derived from multiple sessions by [UsbHidManager].
 */
enum class UsbHidSessionState {
    RequestingPermission,
    SetupPending,
    Active,
    Released,
}

class UsbHidSession(
    val deviceName: String,
    private val device: UsbDevice,
    private val usbManager: UsbManager,
    private val scope: CoroutineScope,
    private val isKnownIdentity: (String) -> Boolean,
    private val reportProcessorFactory: (UsbDeviceConnection, UsbInterface) -> ReportProcessor,
    private val onStateChanged: (UsbHidSessionState) -> Unit,
    private val onKeyEvent: (UsbHidEvent) -> Unit,
) {
    companion object {
        private const val TAG = "UsbHidSession"
        private const val READ_TIMEOUT_MS = 1000
    }

    private val _state = MutableStateFlow(UsbHidSessionState.RequestingPermission)
    val state: StateFlow<UsbHidSessionState> = _state.asStateFlow()

    private var _info: UsbHidInfo? = null
    val info: UsbHidInfo
        get() = _info
            ?: throw IllegalStateException("info not available until permission is granted")

    private val lock = Any()

    private var connection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpoint: UsbEndpoint? = null
    @Volatile
    private var readJob: Job? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var reportProcessor: ReportProcessor? = null

    /** Requests USB permission for this device. */
    fun requestPermission(pendingIntent: PendingIntent) {
        if (_state.value != UsbHidSessionState.RequestingPermission) return
        log("requesting permission")
        usbManager.requestPermission(device, pendingIntent)
    }

    /**
     * Called when the system permission dialog result is delivered. On success the device is captured
     * immediately in the background.
     */
    fun onPermissionResult(granted: Boolean) {
        if (_state.value != UsbHidSessionState.RequestingPermission) return

        if (!granted) {
            log("permission denied")
            release()
            return
        }

        val currentInfo =
            try {
                UsbHidInfo.from(device)
            } catch (e: SecurityException) {
                log("permission granted but cannot read device info")
                release()
                return
            }
        _info = currentInfo

        log("permission granted, starting capture")
        val known = isKnownIdentity(currentInfo.identity)
        if (!known) {
            transitionTo(UsbHidSessionState.SetupPending)
        }
        captureInBackground(activateOnCapture = known)
    }

    /** Called when the user confirmed the keyboard setup dialog (OK). */
    fun confirmSetup() {
        if (!_state.compareAndSet(UsbHidSessionState.SetupPending, UsbHidSessionState.Active)) {
            return
        }
        log("setup confirmed")
        onStateChanged(UsbHidSessionState.Active)
    }

    /** Called when the user cancelled the keyboard setup dialog. Releases this device only. */
    fun cancelSetup() {
        val current = _state.value
        if (current != UsbHidSessionState.SetupPending) return
        log("setup cancelled")
        release()
    }

    /** Called when the device is physically detached. */
    fun detach() {
        log("detached")
        release()
    }

    /** Forces release of USB resources and moves the session to [Released]. */
    fun release() {
        val oldState: UsbHidSessionState
        while (true) {
            val current = _state.value
            if (current == UsbHidSessionState.Released) return
            if (_state.compareAndSet(current, UsbHidSessionState.Released)) {
                oldState = current
                break
            }
        }

        log("state: $oldState → Released")
        onStateChanged(UsbHidSessionState.Released)

        synchronized(lock) {
            readJob?.cancel()
            readJob = null

            val conn = connection
            val iface = usbInterface
            if (conn != null && iface != null) {
                try {
                    conn.releaseInterface(iface)
                } catch (e: Exception) {
                    log("releaseInterface failed: ${e.message}")
                }
            }
            try {
                conn?.close()
            } catch (e: Exception) {
                log("connection close failed: ${e.message}")
            }
            connection = null
            usbInterface = null
            endpoint = null
        }
    }

    private fun captureInBackground(activateOnCapture: Boolean) {
        readJob = scope.launch {
            val captured =
                try {
                    capture()
                } catch (e: Exception) {
                    log("capture exception: ${e.message}")
                    false
                }

            if (!captured) {
                log("capture failed")
                release()
                return@launch
            }

            val proc = reportProcessorFactory(connection ?: return@launch, usbInterface ?: return@launch)
            reportProcessor = proc

            if (activateOnCapture) {
                transitionTo(UsbHidSessionState.Active)
            }

            // Read the mutable connection and endpoint under the same lock that guards writes,
            // otherwise release() could null/close them between the null-check and use.
            val conn: UsbDeviceConnection
            val ep: UsbEndpoint
            synchronized(lock) {
                conn = connection ?: return@launch
                ep = endpoint ?: return@launch
            }
            readLoop(conn, ep)
        }
    }

    private fun capture(): Boolean {
        synchronized(lock) {
            if (_state.value == UsbHidSessionState.Released) return false
        }

        val conn = usbManager.openDevice(device)
        if (conn == null) {
            log("openDevice failed")
            return false
        }

        val configuration = device.getConfiguration(0)
        if (!conn.setConfiguration(configuration)) {
            log("setConfiguration returned false, continuing")
        }

        val productCategory = classifyHidDevice(device)
        if (productCategory == null) {
            log("not a supported HID device")
            conn.close()
            return false
        }
        val predicate: (UsbInterface) -> Boolean = when (productCategory) {
            UsbHidCategory.KEYBOARD -> { iface -> iface.isHidKeyboard }
            UsbHidCategory.JOYSTICK -> { iface -> iface.interfaceClass == UsbConstants.USB_CLASS_HID && !iface.isHidKeyboard }
        }
        val (iface, ep) = findHidInterruptInEndpoint(device, predicate)
        if (iface == null || ep == null) {
            log("no HID interrupt IN endpoint found")
            conn.close()
            return false
        }

        synchronized(lock) {
            if (_state.value == UsbHidSessionState.Released) {
                conn.close()
                return false
            }

            val claimed = conn.claimInterface(iface, true)
            if (!claimed) {
                log("claimInterface failed")
                conn.close()
                return false
            }

            connection = conn
            usbInterface = iface
            endpoint = ep
        }

        if (!conn.setInterface(iface)) {
            log("setInterface returned false, continuing")
        }

        initializeHid(conn, iface)

        synchronized(lock) {
            if (_state.value == UsbHidSessionState.Released) {
                log("released during capture")
                return false
            }
        }

        log("interface claimed, endpoint=${ep.address}")
        return true
    }

    private fun initializeHid(conn: UsbDeviceConnection, iface: UsbInterface) {
        val setProtocol = conn.controlTransfer(
            0x21, // HOST_TO_DEVICE | CLASS | INTERFACE
            0x0B, // SET_PROTOCOL
            0, // boot protocol
            iface.id,
            null,
            0,
            1000
        )
        if (setProtocol < 0) {
            log("SET_PROTOCOL returned $setProtocol, continuing")
        }

        val setIdle = conn.controlTransfer(
            0x21,
            0x0A, // SET_IDLE
            0, // indefinite
            iface.id,
            null,
            0,
            1000
        )
        if (setIdle < 0) {
            log("SET_IDLE returned $setIdle, continuing")
        }
    }

    private suspend fun readLoop(conn: UsbDeviceConnection, ep: UsbEndpoint) {
        val buffer = ByteArray(ep.maxPacketSize.coerceAtLeast(8))
        while (currentCoroutineContext().isActive) {
            val bytesRead = try {
                conn.bulkTransfer(ep, buffer, buffer.size, READ_TIMEOUT_MS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("bulkTransfer error: ${e.message}")
                break
            }

            when {
                bytesRead > 0 -> {
                    processReport(buffer.copyOf(bytesRead))
                }
                bytesRead == 0 || bytesRead == -1 -> {
                    // Yield so cancellation can propagate and we don't spin the CPU.
                    yield()
                }
                else -> {
                    log("bulkTransfer returned $bytesRead, stopping")
                    break
                }
            }
        }
        log("read loop ended")
        release()
    }

    private fun transitionTo(newState: UsbHidSessionState) {
        while (true) {
            val oldState = _state.value
            if (oldState == newState || oldState == UsbHidSessionState.Released) return
            if (_state.compareAndSet(oldState, newState)) {
                onStateChanged(newState)
                log("state: $oldState → $newState")
                return
            }
        }
    }

    private fun log(message: String) {
        Log.d(TAG, "[$deviceName] $message")
    }

    private fun processReport(report: ByteArray) {
        val processor = reportProcessor ?: return
        val events = processor.processReport(report)
        for (event in events) {
            mainHandler.post { onKeyEvent(event) }
        }
    }

}


