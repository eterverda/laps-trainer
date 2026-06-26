package ru.fpvladder.laps.trainer.usb.serialization

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.fpvladder.laps.trainer.usb.UsbHidAction
import ru.fpvladder.laps.trainer.usb.UsbHidConfig

class UsbHidConfigSerializationTest {

    @Test
    fun `deserializes real keyboard config from device`() {
        val text = javaClass.classLoader!!
            .getResource("keyboard/f7db0130-0795-4eea-9a81-0529736c3887.yaml")!!
            .readText()

        val config = UsbHidYamlFormat.decodeFromString(UsbHidConfig.serializer(), text)

        assertEquals("f7db0130-0795-4eea-9a81-0529736c3887", config.uuid)
        assertEquals("SayoDevice 1_3P", config.info.productName)
        assertEquals(0x8089, config.info.vendorId)
        assertEquals(0x000C, config.info.productId)
        assertEquals("03CDABE24071BC20A9FFFFFFFF00", config.info.serialNumber)
        assertEquals("8089:000C:03CDABE24071BC20A9FFFFFFFF00", config.identity)

        val bindingsByAction = config.bindings.associateBy { it.action }
        assertEquals(4, bindingsByAction.size)
        assertEquals(0x52, bindingsByAction[UsbHidAction.START]?.keyCode)
        assertEquals(0x4f, bindingsByAction[UsbHidAction.LAP]?.keyCode)
        assertEquals(0x51, bindingsByAction[UsbHidAction.ERROR]?.keyCode)
        assertEquals(0x50, bindingsByAction[UsbHidAction.FIX]?.keyCode)
        bindingsByAction.values.forEach { assertEquals(0x00, it.modifiers) }
    }
}
