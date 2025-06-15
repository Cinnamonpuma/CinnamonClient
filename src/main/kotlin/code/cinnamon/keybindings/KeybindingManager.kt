package code.cinnamon.keybindings

import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import org.lwjgl.glfw.GLFW

/**
 * Manages the registration and state of keybindings for the Cinnamon mod.
 */
object KeybindingManager {
    private val keybindings = mutableMapOf<String, KeyBinding>()

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
    }

    fun updateKeybinding(name: String, newKey: Int) {
        keybindings[name]?.let {
            it.setBoundKey(InputUtil.fromKeyCode(newKey, 0))
            KeyBinding.updateKeysByCode()
        }
    }
}