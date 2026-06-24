package ru.fpvladder.laps.trainer.usb

import android.hardware.usb.UsbDevice

/**
 * Persistable information about a USB HID keyboard.
 *
 * [identity] is used to match known keyboards. It consists of vendorId,
 * productId and serialNumber (when available). productName is display-only.
 *
 * [deviceName] is intentionally not part of this class because it is a
 * volatile Android system path. When the system path is needed, it is stored
 * alongside this info (e.g. inside the state object or the manager maps).
 */
data class UsbHidInfo(
    val productName: String,
    val vendorId: Int,
    val productId: Int,
    val serialNumber: String?
) {
    val identity: String
        get() = formatIdentity(vendorId, productId, serialNumber)

    val displayIdentity: String
        get() = formatIdentity(vendorId, productId, serialNumber?.hashCode()?.toUInt()?.toString(16)?.lowercase()?.padStart(16, '0'))

    override fun toString(): String {
        return "UsbHidInfo($displayIdentity)"
    }

    companion object {
        /**
         * Creates a full info object. Requires USB permission to read the
         * serial number; throws otherwise.
         */
        fun from(device: UsbDevice): UsbHidInfo {
            return UsbHidInfo(
                productName = device.productName ?: "Unknown",
                vendorId = device.vendorId,
                productId = device.productId,
                serialNumber = device.serialNumber
            )
        }

        fun formatIdentity(device: UsbDevice): String {
            val serialNumber = try {
                device.serialNumber
            } catch (_: SecurityException) {
                "<REDACTED>"
            }
            return formatIdentity(device.vendorId, device.productId, serialNumber)
        }

        private fun formatIdentity(vendorId: Int, productId: Int, serialNumber: String?): String {
            val vid = vendorId.toString(16).uppercase().padStart(4, '0')
            val pid = productId.toString(16).uppercase().padStart(4, '0')
            return if (serialNumber != null) "$vid:$pid:$serialNumber" else "$vid:$pid"
        }
    }
}
