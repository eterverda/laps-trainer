package ru.fpvladder.laps.trainer.usb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.usb.serialization.IntegerAsLowerHexByteSerializer

@Serializable
data class UsbHidBinding(
    val action: UsbHidAction,
    @SerialName("key_code")
    @Serializable(with = IntegerAsLowerHexByteSerializer::class)
    val keyCode: Int,
    @Serializable(with = IntegerAsLowerHexByteSerializer::class)
    val modifiers: Int,
)