package ru.fpvladder.laps.lite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import ru.fpvladder.laps.lite.usb.HidKeyboardManager

class KeyboardViewModel(application: Application) : AndroidViewModel(application) {

    private val hidManager = HidKeyboardManager(application.applicationContext)

    val connectionState: StateFlow<HidKeyboardManager.ConnectionState> = hidManager.connectionState
    val lastKeyEvent: StateFlow<HidKeyboardManager.KeyEvent?> = hidManager.lastKeyEvent
    val availableDevices: StateFlow<List<HidKeyboardManager.UsbDeviceInfo>> = hidManager.availableDevices

    init {
        hidManager.start()
    }

    fun connect(deviceInfo: HidKeyboardManager.UsbDeviceInfo) {
        hidManager.connect(deviceInfo)
    }

    fun disconnect() {
        hidManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        hidManager.stop()
    }
}
