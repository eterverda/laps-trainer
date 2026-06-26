package ru.fpvladder.laps.trainer.usb

import java.util.UUID
import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.usb.serialization.UsbHidConfigSerializer

@Serializable(with = UsbHidConfigSerializer::class)
class UsbHidConfig private constructor(
    val uuid: String,
    val info: UsbHidInfo,
    val bindings: Set<UsbHidBinding>,
) {
    constructor(info: UsbHidInfo, bindings: Set<UsbHidBinding>) : this(
        uuid = UUID.randomUUID().toString(),
        info = info,
        bindings = bindings,
    )

    val identity: String
        get() = info.identity

    fun copy(
        info: UsbHidInfo = this.info,
        bindings: Set<UsbHidBinding> = this.bindings,
    ): UsbHidConfig = UsbHidConfig(uuid = uuid, info = info, bindings = bindings)

    override fun toString(): String = "UsbHidConfig($uuid, $info, $bindings)"

    companion object {
        fun create(
            uuid: String,
            info: UsbHidInfo,
            bindings: Set<UsbHidBinding>,
        ): UsbHidConfig = UsbHidConfig(uuid = uuid, info = info, bindings = bindings)
    }
}

