package ru.fpvladder.laps.trainer.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HidKeyboardManager(private val context: Context) {

    companion object {
        private const val TAG = "HidKeyboardManager"
        const val ACTION_USB_PERMISSION = "ru.fpvladder.laps.trainer.USB_PERMISSION"
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lastKeyEvent = MutableStateFlow<KeyEvent?>(null)
    val lastKeyEvent: StateFlow<KeyEvent?> = _lastKeyEvent.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<UsbDeviceInfo>> = _availableDevices.asStateFlow()

    private var usbConnection: UsbDeviceConnection? = null
    private var readEndpoint: UsbEndpoint? = null
    private var usbInterface: UsbInterface? = null
    private var readJob: Job? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    @Suppress("DEPRECATION") val device: UsbDevice? =
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (device != null) {
                        Handler(Looper.getMainLooper()).post {
                            if (usbManager.hasPermission(device)) {
                                connectToDevice(device)
                            } else {
                                _connectionState.value = ConnectionState.Disconnected
                            }
                        }
                    } else {
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    refreshDeviceList()
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE, UsbDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                    if (device != null && isHidKeyboard(device)) {
                        disconnect()
                    }
                    refreshDeviceList()
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }

        refreshDeviceList()
    }

    fun stop() {
        disconnect()
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
    }

    fun refreshDeviceList() {
        val devices = usbManager.deviceList.values.filter { isHidKeyboard(it) }.map { it.toInfo() }
            .sortedBy { it.deviceName }
        _availableDevices.value = devices
    }

    fun connect(deviceInfo: UsbDeviceInfo) {
        val device = usbManager.deviceList.values.find {
            it.deviceName == deviceInfo.deviceName
        } ?: return

        if (usbManager.hasPermission(device)) {
            connectToDevice(device)
        } else {
            _connectionState.value = ConnectionState.Connecting(
                deviceInfo = deviceInfo, waitingPermission = true
            )
            val pi = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                PendingIntent.FLAG_MUTABLE
            )
            usbManager.requestPermission(device, pi)
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionWithRetry(device: UsbDevice, retryCount: Int) {
        if (usbManager.hasPermission(device)) {
            connectToDevice(device)
            return
        }
        if (retryCount <= 0) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            checkPermissionWithRetry(device, retryCount - 1)
        }, 200)
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null

        usbConnection?.let { conn ->
            usbInterface?.let { conn.releaseInterface(it) }
            conn.close()
        }
        usbConnection = null
        usbInterface = null
        readEndpoint = null
        _lastKeyEvent.value = null
        _connectionState.value = ConnectionState.Disconnected
        refreshDeviceList()
    }

    private fun isHidKeyboard(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT && ep.direction == UsbConstants.USB_DIR_IN) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun UsbDevice.toInfo(): UsbDeviceInfo {
        return UsbDeviceInfo(
            deviceName = this.deviceName,
            productName = this.productName ?: "Unknown",
            vid = this.vendorId,
            pid = this.productId,
            serialNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    this.serialNumber
                } catch (_: SecurityException) {
                    null
                }
            } else {
                null
            },
            hasPermission = usbManager.hasPermission(this)
        )
    }

    private fun connectToDevice(device: UsbDevice) {
        Log.d(TAG, "connectToDevice: ${device.deviceName}")

        readJob?.cancel()
        readJob = null
        usbConnection?.let { conn ->
            usbInterface?.let { conn.releaseInterface(it) }
            conn.close()
        }
        usbConnection = null
        usbInterface = null
        readEndpoint = null
        _lastKeyEvent.value = null

        val iface = findHidInterface(device) ?: run {
            Log.e(TAG, "HID interface not found on device")
            _connectionState.value = ConnectionState.Error("HID interface not found")
            return
        }

        val endpoint = findInterruptInEndpoint(iface) ?: run {
            Log.e(TAG, "Interrupt IN endpoint not found")
            _connectionState.value = ConnectionState.Error("Interrupt IN endpoint not found")
            return
        }

        val connection = usbManager.openDevice(device) ?: run {
            Log.e(TAG, "Failed to open USB device")
            _connectionState.value = ConnectionState.Error("Failed to open device")
            return
        }

        val claimed = connection.claimInterface(iface, true)
        if (!claimed) {
            Log.e(TAG, "Failed to claim interface")
            connection.close()
            _connectionState.value = ConnectionState.Error("Failed to claim interface")
            return
        }

        usbConnection = connection
        usbInterface = iface
        readEndpoint = endpoint
        _connectionState.value = ConnectionState.Connected(
            deviceName = device.productName ?: "Unknown",
            vid = device.vendorId,
            pid = device.productId,
            serialNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    device.serialNumber
                } catch (_: SecurityException) {
                    null
                }
            } else {
                null
            }
        )

        startReading()
    }

    private fun findHidInterface(device: UsbDevice): UsbInterface? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_HID) {
                return iface
            }
        }
        return null
    }

    private fun findInterruptInEndpoint(iface: UsbInterface): UsbEndpoint? {
        for (i in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT && ep.direction == UsbConstants.USB_DIR_IN) {
                return ep
            }
        }
        return null
    }

    private fun startReading() {
        readJob?.cancel()
        readJob = CoroutineScope(Dispatchers.IO).launch {
            val connection = usbConnection ?: return@launch
            val endpoint = readEndpoint ?: return@launch
            val buffer = ByteArray(endpoint.maxPacketSize)

            while (isActive) {
                try {
                    val bytesRead = connection.bulkTransfer(
                        endpoint, buffer, buffer.size, 100
                    )
                    if (bytesRead > 0) {
                        parseHidReport(buffer.copyOf(bytesRead))
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Read error", e)
                        delay(100)
                    }
                }
            }
        }
    }

    private fun parseHidReport(report: ByteArray) {
        if (report.size < 2) return

        val modifier = report[0].toInt() and 0xFF

        for (i in 2 until report.size) {
            val keycode = report[i].toInt() and 0xFF
            if (keycode == 0) continue

            val event = KeyEvent(
                keycode = keycode, modifier = modifier
            )
            _lastKeyEvent.value = event
            Log.d(
                TAG, "Key event: keycode=0x${
                    keycode.toString(16).uppercase().padStart(2, '0')
                }, modifier=0x${modifier.toString(16).uppercase().padStart(2, '0')}"
            )
        }
    }

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data class Connecting(
            val deviceInfo: UsbDeviceInfo, val waitingPermission: Boolean
        ) : ConnectionState()

        data class Connected(
            val deviceName: String, val vid: Int, val pid: Int, val serialNumber: String?
        ) : ConnectionState()

        data class Error(val message: String) : ConnectionState()
    }

    data class UsbDeviceInfo(
        val deviceName: String,
        val productName: String,
        val vid: Int,
        val pid: Int,
        val serialNumber: String?,
        val hasPermission: Boolean
    ) {
        val vidHex: String
            get() = "0x${vid.toString(16).uppercase().padStart(4, '0')}"

        val pidHex: String
            get() = "0x${pid.toString(16).uppercase().padStart(4, '0')}"
    }

    data class KeyEvent(
        val keycode: Int, val modifier: Int
    ) {
        val keycodeHex: String
            get() = "0x${keycode.toString(16).uppercase().padStart(2, '0')}"

        val modifierHex: String
            get() = "0x${modifier.toString(16).uppercase().padStart(2, '0')}"
    }
}
