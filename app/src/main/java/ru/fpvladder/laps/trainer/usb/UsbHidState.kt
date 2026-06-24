package ru.fpvladder.laps.trainer.usb

sealed class UsbHidState {
    data object Disabled : UsbHidState()
    data object Disconnected : UsbHidState()
    data class Permission(val deviceName: String) : UsbHidState()
    data class Setup(val deviceName: String, val info: UsbHidInfo) : UsbHidState()
    data class Connected(val devices: Set<UsbHidInfo>) : UsbHidState()
}
