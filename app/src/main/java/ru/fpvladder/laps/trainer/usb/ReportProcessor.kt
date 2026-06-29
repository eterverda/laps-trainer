package ru.fpvladder.laps.trainer.usb

interface ReportProcessor {
    fun processReport(report: ByteArray): List<UsbHidEvent>
}
