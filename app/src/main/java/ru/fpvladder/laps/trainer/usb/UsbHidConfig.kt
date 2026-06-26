package ru.fpvladder.laps.trainer.usb

import android.hardware.usb.UsbDevice
import java.util.UUID
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.fpvladder.laps.trainer.usb.serialization.IntegerAsLowerHexByteSerializer
import ru.fpvladder.laps.trainer.usb.serialization.IntegerAsUpperHexShortSerializer
import ru.fpvladder.laps.trainer.usb.serialization.UsbHidConfigSerializer

@Serializable(with = UsbHidConfigSerializer::class)
class UsbHidConfig private constructor(
    val id: String,
    val info: UsbHidInfo,
    val bindings: Set<UsbHidBinding>,
) {
    constructor(info: UsbHidInfo, bindings: Set<UsbHidBinding>) : this(
        id = UUID.randomUUID().toString(),
        info = info,
        bindings = bindings,
    )

    val identity: String
        get() = info.identity

    fun copy(
        info: UsbHidInfo = this.info,
        bindings: Set<UsbHidBinding> = this.bindings,
    ): UsbHidConfig = UsbHidConfig(id = id, info = info, bindings = bindings)

    override fun toString(): String = "UsbHidConfig($id, $info, $bindings)"

    companion object {
        fun create(
            id: String,
            info: UsbHidInfo,
            bindings: Set<UsbHidBinding>,
        ): UsbHidConfig = UsbHidConfig(id = id, info = info, bindings = bindings)
    }
}

/**
 * Persistable information about a USB HID keyboard.
 *
 * [identity] is used to match known keyboards. It consists of vendorId,
 * productId and serialNumber (when available). productName is display-only.
 *
 * [deviceName] is intentionally not part of this class because it is a
 * volatile Android system path. When the system path is needed, it is stored
 * alongside this info (e.g. inside the state object or the manager maps).
 */
@Serializable
data class UsbHidInfo(
    @SerialName("product_name")
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val productName: String?,
    @SerialName("vendor_id")
    @Serializable(with = IntegerAsUpperHexShortSerializer::class)
    val vendorId: Int,
    @SerialName("product_id")
    @Serializable(with = IntegerAsUpperHexShortSerializer::class)
    val productId: Int,
    @SerialName("serial_number")
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val serialNumber: String?,
) {
    val identity: String by lazy(LazyThreadSafetyMode.NONE) {
        formatIdentity(vendorId, productId, serialNumber)
    }

    val displayIdentity: String
        get() = formatIdentity(
            vendorId,
            productId,
            when (serialNumber) {
                null -> null
                else -> "%08x".format(serialNumber.hashCode())
            },
        )

    val displayProductName: String
        get() = productName ?: "Unknown"

    override fun toString(): String {
        return "UsbHidInfo($displayIdentity)"
    }

    companion object {
        /**
         * Creates a full info object. Requires USB permission to read the
         * serial number; throws otherwise.
         */
        fun from(device: UsbDevice): UsbHidInfo {
            return UsbHidInfo(
                productName = device.productName?.takeIf { it.isNotBlank() },
                vendorId = device.vendorId,
                productId = device.productId,
                serialNumber = device.serialNumber,
            )
        }

        fun formatIdentity(device: UsbDevice): String {
            val serialNumber = try {
                device.serialNumber
            } catch (_: SecurityException) {
                "<REDACTED>"
            }
            return formatIdentity(device.vendorId, device.productId, serialNumber)
        }

        private fun formatIdentity(vendorId: Int, productId: Int, serialNumber: String?): String {
            return when (serialNumber) {
                null -> "%04X:%04X".format(vendorId, productId)
                else -> "%04X:%04X:%s".format(vendorId, productId, serialNumber)
            }
        }
    }
}

@Serializable
data class UsbHidBinding(
    val action: UsbHidAction,
    @SerialName("key_code")
    @Serializable(with = IntegerAsLowerHexByteSerializer::class)
    val keyCode: Int,
    @Serializable(with = IntegerAsLowerHexByteSerializer::class)
    val modifiers: Int,
)

@Serializable
enum class UsbHidAction {
    START,
    LAP,
    ERROR,
    FIX,
}