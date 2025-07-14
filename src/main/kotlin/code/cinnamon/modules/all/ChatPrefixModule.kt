package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.util.MinecraftColorCodes
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

import code.cinnamon.modules.ModeSetting

class ChatPrefixModule : Module("ChatPrefix", "Adds a color prefix to chat messages.") {

    private val colorSetting = ModeSetting(
        "Prefix Color",
        MinecraftColorCodes.WHITE.code,
        MinecraftColorCodes.entries.map { it.code }
    )

    init {
        settings.add(colorSetting)
    }

    override fun onEnable() {
        println("ChatPrefixModule enabled.")
    }

    override fun onDisable() {
        println("ChatPrefixModule disabled.")
    }

    fun getPrefixedMessage(message: String): String {
        return if (this.isEnabled) {
            colorSetting.value + message
        } else {
            message
        }
    }
}
