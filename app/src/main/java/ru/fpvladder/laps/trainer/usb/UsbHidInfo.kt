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
    val serialNumber: String?,
) {
    val identity: String
        get() = formatIdentity(vendorId, productId, serialNumber)

    val displayIdentity: String
        get() = formatIdentity(
            vendorId,
            productId,
            when (serialNumber) {
                null -> null
                else -> "%08x".format(serialNumber.hashCode())
            },
        )

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
                serialNumber = device.serialNumber,
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
            return when (serialNumber) {
                null -> "%04X:%04X".format(vendorId, productId)
                else -> "%04X:%04X:%s".format(vendorId, productId, serialNumber)
            }
        }
    }
}
