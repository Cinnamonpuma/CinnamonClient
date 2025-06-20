package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.util.MinecraftColorCodes
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class ChatPrefixModule : Module("ChatPrefix", "Adds a color prefix to chat messages.") {

    companion object {
        private const val CONFIG_FILE_NAME = "chat_prefix_settings.json"
        private val CONFIG_DIRECTORY = File(FabricLoader.getInstance().configDir.toFile(), "cinnamon_client")
        private val CONFIG_FILE = File(CONFIG_DIRECTORY, CONFIG_FILE_NAME)
    }
    private val gson = Gson()

    var selectedColorCode: String = MinecraftColorCodes.WHITE.code; private set

    init {
        loadConfig()
    }

    override fun onEnable() {
        println("ChatPrefixModule enabled.")
    }

    override fun onDisable() {
        println("ChatPrefixModule disabled.")
    }

    fun getPrefixedMessage(message: String): String {
        return if (this.isEnabled) {
            selectedColorCode + message
        } else {
            message
        }
    }

    fun loadConfig() {
        if (!CONFIG_DIRECTORY.exists()) {
            CONFIG_DIRECTORY.mkdirs()
        }
        if (CONFIG_FILE.exists()) {
            try {
                FileReader(CONFIG_FILE).use { reader ->
                    val jsonObject = gson.fromJson(reader, JsonObject::class.java)
                    if (jsonObject.has("selectedColorCode")) {
                        selectedColorCode = jsonObject.get("selectedColorCode").asString
                    }

                    println("ChatPrefixModule: Config loaded from ${CONFIG_FILE.absolutePath}")
                }
            } catch (e: Exception) {
                System.err.println("ChatPrefixModule: Error loading config: ${e.message}")
            }
        } else {
            println("ChatPrefixModule: No config file found, using default settings.")
        }
    }

    fun saveConfig() {
        if (!CONFIG_DIRECTORY.exists()) {
            CONFIG_DIRECTORY.mkdirs()
        }
        try {
            FileWriter(CONFIG_FILE).use { writer ->
                val jsonObject = JsonObject()
                jsonObject.addProperty("selectedColorCode", selectedColorCode)

                gson.toJson(jsonObject, writer)
                println("ChatPrefixModule: Config saved to ${CONFIG_FILE.absolutePath}")
            }
        } catch (e: Exception) {
            System.err.println("ChatPrefixModule: Error saving config: ${e.message}")
        }
    }

    fun setSelectedColorCode(newColorCode: String) {
        val color = MinecraftColorCodes.getByCode(newColorCode)
        if (color != null && color.isColor) {
            selectedColorCode = newColorCode
            saveConfig()
        } else if (newColorCode == MinecraftColorCodes.RESET.code) {
            selectedColorCode = newColorCode
            saveConfig()
        }
    }
}
