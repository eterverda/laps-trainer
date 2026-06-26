package ru.fpvladder.laps.trainer.usb

/**
 * A single key press or release event produced from a HID boot keyboard report.
 *
 * @param keyCode HID usage ID of the key. Regular keys use 0x00-0xFF; modifier keys use 0xE0-0xE7.
 * @param modifiers Current modifier bitmask from the report byte 0.
 * @param state [UsbHidEvent.STATE_DOWN] or [UsbHidEvent.STATE_UP].
 * @param action The application action mapped to this key, if any.
 */
data class UsbHidEvent(
    val keyCode: Int,
    val modifiers: Int,
    val state: Int,
    val action: UsbHidAction? = null,
) {
    companion object {
        const val STATE_DOWN = 0x01
        const val STATE_UP = 0x02
    }
}
