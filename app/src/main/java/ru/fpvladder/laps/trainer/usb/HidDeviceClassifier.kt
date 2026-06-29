package ru.fpvladder.laps.trainer.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface

fun classifyHidDevice(device: UsbDevice): UsbHidCategory? {
    for (i in 0 until device.interfaceCount) {
        val iface = device.getInterface(i)
        if (iface.isHidKeyboard) return UsbHidCategory.KEYBOARD
    }
    for (i in 0 until device.interfaceCount) {
        val iface = device.getInterface(i)
        if (iface.interfaceClass == UsbConstants.USB_CLASS_HID) return UsbHidCategory.JOYSTICK
    }
    return null
}

fun findHidInterruptInEndpoint(
    device: UsbDevice,
    predicate: (UsbInterface) -> Boolean = { true }
): Pair<UsbInterface?, UsbEndpoint?> {
    for (i in 0 until device.interfaceCount) {
        val iface = device.getInterface(i)
        if (iface.interfaceClass != UsbConstants.USB_CLASS_HID || !predicate(iface)) continue
        for (j in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(j)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_INT && ep.direction == UsbConstants.USB_DIR_IN) {
                return iface to ep
            }
        }
    }
    return null to null
}

internal val UsbInterface.isHidKeyboard: Boolean
    get() = interfaceClass == UsbConstants.USB_CLASS_HID &&
            interfaceSubclass == 1 &&
            interfaceProtocol == 1
