package ru.fpvladder.laps.trainer.usb

class UsbHidConfigRepository {
    private val configs = mutableMapOf<String, UsbHidConfig>()

    fun all(): List<UsbHidConfig> = configs.values.toList()

    operator fun get(id: String): UsbHidConfig? = configs[id]

    fun save(config: UsbHidConfig) {
        configs[config.id] = config
    }

    fun remove(id: String): UsbHidConfig? =
        configs.remove(id)

    fun bindingFor(identity: String, keyCode: Int, modifiers: Int): UsbHidBinding? =
        configs.values.find { it.identity == identity }
            ?.bindings?.find { it.keyCode == keyCode && it.modifiers == modifiers }

    fun mappedAction(identity: String, keyCode: Int, modifiers: Int): UsbHidAction? =
        bindingFor(identity, keyCode, modifiers)?.action
}
