package code.cinnamon.keybindings

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import org.lwjgl.glfw.GLFW
import java.io.File
import java.nio.file.Paths

/**
 * Manages the registration and state of keybindings for the Cinnamon mod.
 */
object KeybindingManager {
    private val keybindings = mutableMapOf<String, KeyBinding>()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val configDir = Paths.get("config", "cinnamon").toFile()
    private val configFile = File(configDir, "keybindings.json")

    @Serializable
    data class KeybindingConfig(val name: String, val key: Int)

    fun registerKeybinding(name: String, key: Int, category: String = "CinnamonClient"): KeyBinding {
        val keyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                name,
                InputUtil.Type.KEYSYM,
                key,
                category
            )
        )
        keybindings[name] = keyBinding
        return keyBinding
    }

    fun getKeybinding(name: String): KeyBinding? {
        return keybindings[name]
    }

    fun getAllKeybindings(): Map<String, KeyBinding> = keybindings.toMap()

    fun isPressed(name: String): Boolean {
        return keybindings[name]?.isPressed ?: false
    }

    fun wasPressed(name: String): Boolean {
        return keybindings[name]?.wasPressed() ?: false
    }

    fun initialize() {
        registerKeybinding("cinnamon.toggle_autoclicker", GLFW.GLFW_KEY_X)
        registerKeybinding("cinnamon.open_saved_gui", GLFW.GLFW_KEY_V)
        loadKeybindings()
    }

    fun updateKeybinding(name: String, newKey: Int) {
        keybindings[name]?.let {
            it.setBoundKey(InputUtil.fromKeyCode(newKey, 0))
            KeyBinding.updateKeysByCode()
            saveKeybindings()
        }
    }

    fun saveKeybindings() {
        try {
            configDir.mkdirs()
            val configs = keybindings.map { (name, keyBinding) ->
                KeybindingConfig(name, KeyBindingHelper.getBoundKeyOf(keyBinding).code)
            }
            val jsonString = json.encodeToString(configs)
            configFile.writeText(jsonString)
        } catch (e: Exception) {
            println("[KeybindingManager] Failed to save keybindings: ${e.message}")
        }
    }

    private fun loadKeybindings() {
        if (!configFile.exists()) return

        try {
            val jsonString = configFile.readText()
            val configs = json.decodeFromString<List<KeybindingConfig>>(jsonString)
            configs.forEach { config ->
                updateKeybinding(config.name, config.key)
            }
        } catch (e: Exception) {
            println("[KeybindingManager] Failed to load keybindings: ${e.message}")
        }
    }
}