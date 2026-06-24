package ru.fpvladder.laps.trainer.usb

/**
 * A single key press or release event produced from a HID boot keyboard report.
 *
 * @param keyCode HID usage ID of the key. Regular keys use 0x00-0xFF; modifier keys use 0xE0-0xE7.
 * @param modifiers Current modifier bitmask from the report byte 0.
 * @param action [UsbHidEvent.ACTION_DOWN] or [UsbHidEvent.ACTION_UP].
 */
data class UsbHidEvent(
    val keyCode: Int,
    val modifiers: Int,
    val action: Int,
) {
    companion object {
        const val ACTION_DOWN = 0x01
        const val ACTION_UP = 0x02
    }
}
