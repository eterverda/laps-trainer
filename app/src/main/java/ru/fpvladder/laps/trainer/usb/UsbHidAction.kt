package ru.fpvladder.laps.trainer.usb

import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.usb.serialization.UsbHidActionSerializer

@Serializable(with = UsbHidActionSerializer::class)
enum class UsbHidAction {
    START,
    LAP,
    ERROR,
    FIX,
    PITSTOP,
    UNDO,
}