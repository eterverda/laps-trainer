package ru.fpvladder.laps.trainer.usb

class KeyboardReportProcessor : ReportProcessor {

    private var previousReport: ByteArray? = ByteArray(8) { 0 }

    override fun processReport(report: ByteArray): List<UsbHidEvent> {
        if (report.size < 8) return emptyList()

        val prev = previousReport
        previousReport = report.copyOf()
        if (prev == null || prev.size < 8) return emptyList()

        val events = mutableListOf<UsbHidEvent>()

        val currentModifiers = report[0].toInt() and 0xFF
        val previousModifiers = prev[0].toInt() and 0xFF

        val changedModifiers = currentModifiers xor previousModifiers
        for (i in 0..7) {
            if (changedModifiers and (1 shl i) != 0) {
                val pressed = (currentModifiers and (1 shl i)) != 0
                events.add(
                    UsbHidEvent(
                        keyCode = 0xE0 + i,
                        modifiers = currentModifiers,
                        state = if (pressed) UsbHidEvent.STATE_DOWN else UsbHidEvent.STATE_UP,
                    )
                )
            }
        }

        val previousKeys = prev.sliceArray(2..7).filter { it != 0.toByte() }
        val currentKeys = report.sliceArray(2..7).filter { it != 0.toByte() }

        for (key in currentKeys) {
            if (key !in previousKeys) {
                events.add(
                    UsbHidEvent(
                        keyCode = key.toInt() and 0xFF,
                        modifiers = currentModifiers,
                        state = UsbHidEvent.STATE_DOWN,
                    )
                )
            }
        }
        for (key in previousKeys) {
            if (key !in currentKeys) {
                events.add(
                    UsbHidEvent(
                        keyCode = key.toInt() and 0xFF,
                        modifiers = currentModifiers,
                        state = UsbHidEvent.STATE_UP,
                    )
                )
            }
        }

        return events
    }
}
