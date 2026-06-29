package ru.fpvladder.laps.trainer.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.util.Log

private const val TAG = "JoystickButtonReportProcessor"

/**
 * Parses HID reports from a joystick/gamepad and emits [UsbHidEvent] for button
 * presses/releases and for axes at their extreme positions.
 *
 * Buttons are emitted with modifiers = 0x00 and keyCode = button index.
 * Axes at +100% (logical maximum) are emitted with modifiers = 0x01.
 * Axes at -100% (logical minimum) are emitted with modifiers = 0x02.
 */
class JoystickButtonReportProcessor(
    connection: UsbDeviceConnection,
    iface: UsbInterface,
) : ReportProcessor {

    private val descriptor = fetchReportDescriptor(connection, iface)
    private val buttonFields: List<ButtonField> = parseButtonFields(descriptor)
    private val axisFields: List<AxisField> = parseAxisFields(descriptor)
    private var previousReport: ByteArray? = null
    private var previousAxisStates: IntArray? = null

    override fun processReport(report: ByteArray): List<UsbHidEvent> {
        Log.d(TAG, "raw report (${report.size}): ${report.toHex()}")

        val prev = previousReport
        previousReport = report.copyOf()
        if (prev == null || prev.size != report.size) {
            Log.d(TAG, "first report or size mismatch, storing as baseline")
            return emptyList()
        }

        val events = mutableListOf<UsbHidEvent>()
        events.addAll(processButtons(report, prev))
        events.addAll(processAxes(report, prev))

        if (events.isEmpty()) {
            Log.d(TAG, "no button/axis changes")
        }
        return events
    }

    private fun processButtons(report: ByteArray, prev: ByteArray): List<UsbHidEvent> {
        if (buttonFields.isEmpty()) return emptyList()

        val events = mutableListOf<UsbHidEvent>()
        var buttonIndex = 0

        for (field in buttonFields) {
            for (i in 0 until field.count) {
                val bit = field.offsetBits + i * field.size
                val current = readBit(report, bit)
                val previous = readBit(prev, bit)
                if (current != previous) {
                    val event = UsbHidEvent(
                        keyCode = buttonIndex,
                        modifiers = 0,
                        state = if (current) UsbHidEvent.STATE_DOWN else UsbHidEvent.STATE_UP,
                    )
                    Log.d(TAG, "button event: index=$buttonIndex state=${event.state} bit=$bit")
                    events.add(event)
                }
                buttonIndex++
            }
        }

        return events
    }

    private fun processAxes(report: ByteArray, prev: ByteArray): List<UsbHidEvent> {
        if (axisFields.isEmpty()) return emptyList()

        val previousStates = previousAxisStates ?: IntArray(axisFields.size)
        val currentStates = IntArray(axisFields.size)
        for ((index, field) in axisFields.withIndex()) {
            val signed = field.logicalMin < 0
            val value = readBits(report, field.offsetBits, field.size)
                .signExtend(field.size, signed)
            val range = field.logicalMax - field.logicalMin
            if (range <= 0) {
                currentStates[index] = 0
                continue
            }
            val center = (field.logicalMin + field.logicalMax) / 2
            val onMax = field.logicalMax - range / 10
            val offMax = field.logicalMax - range * 15 / 100
            val onMin = field.logicalMin + range / 10
            val offMin = field.logicalMin + range * 15 / 100
            val onCenter = range / 10
            val offCenter = range * 15 / 100

            currentStates[index] = when (previousStates[index]) {
                1 -> if (value <= offMax) {
                    when {
                        kotlin.math.abs(value - center) <= onCenter -> 3
                        value > onMax -> 1
                        value < onMin -> 2
                        else -> 0
                    }
                } else 1
                2 -> if (value >= offMin) {
                    when {
                        kotlin.math.abs(value - center) <= onCenter -> 3
                        value > onMax -> 1
                        value < onMin -> 2
                        else -> 0
                    }
                } else 2
                3 -> if (kotlin.math.abs(value - center) <= offCenter) 3 else {
                    when {
                        value > onMax -> 1
                        value < onMin -> 2
                        else -> 0
                    }
                }
                else -> when {
                    kotlin.math.abs(value - center) <= onCenter -> 3
                    value > onMax -> 1
                    value < onMin -> 2
                    else -> 0
                }
            }
        }
        previousAxisStates = currentStates

        val events = mutableListOf<UsbHidEvent>()
        for (index in currentStates.indices) {
            val current = currentStates[index]
            val previous = previousStates[index]
            if (current == previous) continue

            if (previous != 0) {
                events.add(
                    UsbHidEvent(
                        keyCode = index,
                        modifiers = previous.toAxisModifier(),
                        state = UsbHidEvent.STATE_UP,
                    )
                )
            }
            if (current != 0) {
                val event = UsbHidEvent(
                    keyCode = index,
                    modifiers = current.toAxisModifier(),
                    state = UsbHidEvent.STATE_DOWN,
                )
                Log.d(TAG, "axis event: index=$index state=${event.state} modifiers=${event.modifiers.toHex()}")
                events.add(event)
            }
        }

        return events
    }

    private fun Int.toAxisModifier(): Int = when (this) {
        1 -> 0x01
        2 -> 0x02
        3 -> 0x03
        else -> 0x00
    }

    private data class ButtonField(
        val offsetBits: Int,
        val count: Int,
        val size: Int,
    )

    private data class AxisField(
        val offsetBits: Int,
        val size: Int,
        val logicalMin: Int,
        val logicalMax: Int,
    )

    companion object {
        private const val USAGE_PAGE_GENERIC_DESKTOP = 0x01
        private const val USAGE_PAGE_BUTTON = 0x09
        private const val ITEM_TYPE_MAIN = 0
        private const val ITEM_TYPE_GLOBAL = 1
        private const val ITEM_TYPE_LOCAL = 2

        private fun fetchReportDescriptor(connection: UsbDeviceConnection, iface: UsbInterface): ByteArray {
            val buffer = ByteArray(512)
            val len = connection.controlTransfer(
                0x81, // USB_DIR_IN | USB_TYPE_STANDARD | USB_RECIP_INTERFACE
                0x06, // USB_REQ_GET_DESCRIPTOR
                0x2200, // HID report descriptor
                iface.id,
                buffer,
                buffer.size,
                5000
            )
            return if (len > 0) buffer.copyOf(len) else ByteArray(0)
        }

        private fun parseButtonFields(descriptor: ByteArray): List<ButtonField> {
            return parseFields(descriptor).first
        }

        private fun parseAxisFields(descriptor: ByteArray): List<AxisField> {
            return parseFields(descriptor).second
        }

        private fun parseFields(descriptor: ByteArray): Pair<List<ButtonField>, List<AxisField>> {
            Log.d(TAG, "report descriptor (${descriptor.size}): ${descriptor.toHex()}")
            var index = 0
            var usagePage = 0
            var reportSize = 0
            var reportCount = 0
            var reportId = 0
            var logicalMin = 0
            var logicalMax = 0
            var bitOffset = 0
            val buttonFields = mutableListOf<ButtonField>()
            val axisFields = mutableListOf<AxisField>()

            while (index < descriptor.size) {
                val startIndex = index
                val prefix = descriptor[index].toInt() and 0xFF
                if (prefix == 0x00) {
                    index++
                    continue
                }

                val size = when (prefix and 0x03) {
                    0 -> 0
                    1 -> 1
                    2 -> 2
                    else -> 4
                }
                val type = (prefix shr 2) and 0x03
                val tag = (prefix shr 4) and 0x0F
                index++
                if (index + size > descriptor.size) break

                val value = readUnsigned(descriptor, index, size)
                val signedValue = readSigned(descriptor, index, size)
                index += size

                val typeName = when (type) { ITEM_TYPE_MAIN -> "MAIN"; ITEM_TYPE_GLOBAL -> "GLOBAL"; ITEM_TYPE_LOCAL -> "LOCAL"; else -> "RESERVED" }
                Log.d(TAG, "item pos=$startIndex type=$typeName tag=$tag size=$size value=$value")

                when (type) {
                    ITEM_TYPE_MAIN -> {
                        when (tag) {
                            8 -> { // Input
                                val isConstant = (value and 0x01) != 0
                                val isVariable = (value and 0x02) != 0
                                Log.d(TAG, "Input usagePage=$usagePage reportCount=$reportCount reportSize=$reportSize bitOffset=$bitOffset constant=$isConstant variable=$isVariable")
                                if (!isConstant && reportCount > 0 && reportSize > 0) {
                                    when (usagePage) {
                                        USAGE_PAGE_BUTTON -> {
                                            buttonFields.add(ButtonField(bitOffset, reportCount, reportSize))
                                            Log.d(TAG, "added button field: offset=$bitOffset count=$reportCount size=$reportSize")
                                        }
                                        USAGE_PAGE_GENERIC_DESKTOP -> {
                                            if (isVariable) {
                                                for (i in 0 until reportCount) {
                                                    axisFields.add(
                                                        AxisField(
                                                            offsetBits = bitOffset + i * reportSize,
                                                            size = reportSize,
                                                            logicalMin = logicalMin,
                                                            logicalMax = logicalMax,
                                                        )
                                                    )
                                                }
                                                Log.d(TAG, "added $reportCount axis field(s): offset=$bitOffset size=$reportSize min=$logicalMin max=$logicalMax")
                                            }
                                        }
                                    }
                                }
                                bitOffset += reportCount * reportSize
                            }
                            9 -> { // Output
                                bitOffset += reportCount * reportSize
                            }
                            10 -> { /* Collection start */ }
                            11 -> { /* Feature */ }
                            12 -> { /* Collection end */ }
                            else -> {
                                bitOffset += reportCount * reportSize
                            }
                        }
                    }
                    ITEM_TYPE_GLOBAL -> {
                        when (tag) {
                            0 -> { usagePage = value; Log.d(TAG, "Usage Page = $usagePage") }
                            1 -> { logicalMin = signedValue; Log.d(TAG, "Logical Minimum = $logicalMin") }
                            2 -> { logicalMax = signedValue; Log.d(TAG, "Logical Maximum = $logicalMax") }
                            7 -> { reportSize = value; Log.d(TAG, "Report Size = $reportSize") }
                            8 -> { reportId = value; Log.d(TAG, "Report ID = $reportId") }
                            9 -> { reportCount = value; Log.d(TAG, "Report Count = $reportCount") }
                        }
                    }
                    ITEM_TYPE_LOCAL -> {
                        when (tag) {
                            0 -> Log.d(TAG, "Usage = $value")
                            1 -> Log.d(TAG, "Usage Minimum = $value")
                            2 -> Log.d(TAG, "Usage Maximum = $value")
                            else -> Log.d(TAG, "Local tag=$tag value=$value")
                        }
                    }
                }
            }

            val buttons = buttonFields.sumOf { it.count }
            val axes = axisFields.size
            Log.d(TAG, "Parsed $buttons button(s) and $axes axis(es)")
            return buttonFields to axisFields
        }

        private fun readUnsigned(data: ByteArray, offset: Int, size: Int): Int {
            return when (size) {
                1 -> data[offset].toInt() and 0xFF
                2 -> (data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8)
                4 -> (data[offset].toInt() and 0xFF) or
                        ((data[offset + 1].toInt() and 0xFF) shl 8) or
                        ((data[offset + 2].toInt() and 0xFF) shl 16) or
                        ((data[offset + 3].toInt() and 0xFF) shl 24)
                else -> 0
            }
        }

        private fun readSigned(data: ByteArray, offset: Int, size: Int): Int {
            val unsigned = readUnsigned(data, offset, size)
            return if (size == 0) 0 else unsigned.signExtend(size * 8, signed = true)
        }
    }
}

private fun readBit(data: ByteArray, bit: Int): Boolean {
    val byteIndex = bit / 8
    val bitIndex = bit % 8
    return byteIndex < data.size && (data[byteIndex].toInt() and (1 shl bitIndex)) != 0
}

private fun readBits(data: ByteArray, offsetBits: Int, size: Int): Int {
    var value = 0
    for (i in 0 until size) {
        val bit = offsetBits + i
        val byteIndex = bit / 8
        val bitIndex = bit % 8
        if (byteIndex < data.size && (data[byteIndex].toInt() and (1 shl bitIndex)) != 0) {
            value = value or (1 shl i)
        }
    }
    return value
}

private fun Int.signExtend(size: Int, signed: Boolean): Int {
    if (!signed || size <= 0) return this
    val signBit = 1 shl (size - 1)
    return if ((this and signBit) != 0) {
        this or ((-1) shl size)
    } else {
        this
    }
}

private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

private fun Int.toHex(): String = "0x%02X".format(this)
