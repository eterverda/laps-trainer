package ru.fpvladder.laps.trainer.usb

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.fpvladder.laps.trainer.usb.serialization.UsbHidYamlFormat

class UsbHidConfigRepository(context: Context) {

    private val appContext = context.applicationContext
    private val storageDir = File(appContext.filesDir, "keyboard")
    private val configs = mutableMapOf<String, UsbHidConfig>()

    fun all(): List<UsbHidConfig> =
        configs.values.sortedByDescending { config -> configFile(config.uuid).lastModified() }

    fun findByIdentity(identity: String): UsbHidConfig? =
        configs.values.find { it.identity == identity }

    suspend fun save(config: UsbHidConfig) = withContext(Dispatchers.IO) {
        configs[config.uuid] = config
        writeConfigFile(config)
    }

    suspend fun remove(identity: String) = withContext(Dispatchers.IO) {
        val config = findByIdentity(identity) ?: return@withContext
        configs.remove(config.uuid)
        configFile(config.uuid).delete()
    }

    /**
     * Updates the file modification time for the config with [identity] so that it
     * floats to the top of the most-recently-used order.
     */
    suspend fun touch(identity: String) = withContext(Dispatchers.IO) {
        val config = findByIdentity(identity) ?: return@withContext
        configFile(config.uuid).takeIf { it.exists() }?.setLastModified(System.currentTimeMillis())
    }

    /**
     * Loads all configs from [storageDir]. Existing in-memory configs are
     * replaced. Callers should publish [all] after loading.
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        if (!storageDir.exists() && !storageDir.mkdirs()) return@withContext

        configs.clear()
        storageDir.listFiles { file -> file.isFile && file.extension == "yaml" }?.forEach { file ->
            runCatching {
                val text = file.readText()
                val config = UsbHidYamlFormat.decodeFromString(UsbHidConfig.serializer(), text)
                configs[config.uuid] = config
            }.onFailure { e ->
                Log.w(TAG, "Failed to load keyboard config from ${file.name}, deleting", e)
                file.delete()
            }
        }
    }

    fun bindingFor(identity: String, keyCode: Int, modifiers: Int): UsbHidBinding? =
        findByIdentity(identity)
            ?.bindings?.find { it.keyCode == keyCode && it.modifiers == modifiers }

    fun mappedAction(identity: String, keyCode: Int, modifiers: Int): UsbHidAction? =
        bindingFor(identity, keyCode, modifiers)?.action

    private fun configFile(uuid: String): File = File(storageDir, "$uuid.yaml")

    private fun writeConfigFile(config: UsbHidConfig) {
        if (!storageDir.exists()) storageDir.mkdirs()
        val file = configFile(config.uuid)
        file.writeText(UsbHidYamlFormat.encodeToString(UsbHidConfig.serializer(), config))
    }

    companion object {
        private const val TAG = "UsbHidConfigRepository"
    }
}
