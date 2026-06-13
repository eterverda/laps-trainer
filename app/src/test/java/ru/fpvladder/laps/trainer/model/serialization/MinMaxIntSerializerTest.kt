package ru.fpvladder.laps.trainer.model.serialization

import org.junit.Assert.assertEquals
import org.junit.Test

class MinMaxIntSerializerTest {

    @Test
    fun `serialize writes max for Int_MAX_VALUE`() {
        val yaml = YamlFormat.encodeToString(MinMaxIntSerializer, Int.MAX_VALUE)
        assertEquals("max", yaml)
    }

    @Test
    fun `serialize writes min for Int_MIN_VALUE`() {
        val yaml = YamlFormat.encodeToString(MinMaxIntSerializer, Int.MIN_VALUE)
        assertEquals("min", yaml)
    }

    @Test
    fun `serialize writes plain int for regular values`() {
        val yaml = YamlFormat.encodeToString(MinMaxIntSerializer, 42)
        assertEquals("42", yaml)
    }

    @Test
    fun `deserialize reads max as Int_MAX_VALUE`() {
        val value = YamlFormat.decodeFromString(MinMaxIntSerializer, "max")
        assertEquals(Int.MAX_VALUE, value)
    }

    @Test
    fun `deserialize reads min as Int_MIN_VALUE`() {
        val value = YamlFormat.decodeFromString(MinMaxIntSerializer, "min")
        assertEquals(Int.MIN_VALUE, value)
    }

    @Test
    fun `deserialize reads plain int`() {
        val value = YamlFormat.decodeFromString(MinMaxIntSerializer, "123")
        assertEquals(123, value)
    }
}
