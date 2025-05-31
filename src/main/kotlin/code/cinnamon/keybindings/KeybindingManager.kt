package code.cinnamon.keybindings

import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import org.lwjgl.glfw.GLFW

/**
 * Manages the registration and state of keybindings for the Cinnamon mod.
 * 
 * **Keybinding Naming and Usage:**
 * - The `name` parameter provided to `registerKeybinding` (e.g., "cinnamon.toggle_flight")
 *   serves as the internal identifier for that keybinding within this manager.
 *   This exact `name` string must be used when calling `getKeybinding(name)`,
 *   `isPressed(name)`, or `wasPressed(name)`.
 *
 * - The `category` parameter in `registerKeybinding` (which defaults to "CinnamonClient")
 *   determines how keybindings are grouped in Minecraft's keybinding settings menu.
 *
 * - The actual string displayed in the Minecraft settings (e.g., "Open GUI" which might
 *   correspond to a translation key like "key.cinnamon.open_gui") comes from the
 *   first argument of the `net.minecraft.client.option.KeyBinding` constructor.
 *   This is distinct from the internal `name` used by `KeybindingManager`.
 *   For example, `KeyBinding("key.cinnamon.open_gui", ...)` uses "key.cinnamon.open_gui"
 *   as its display/translation key.
 *
 * **Important for Custom Keybindings:**
 *   If you register a keybinding (e.g., `KeybindingManager.registerKeybinding("my_module.my_action", ...)`),
 *   you must check it using the same name: `KeybindingManager.isPressed("my_module.my_action")`.
 *   Do not add prefixes like "key." when querying `KeybindingManager` unless the
 *   keybinding was registered with that prefix in its `name`.
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
        // Register default keybindings here
        registerKeybinding("cinnamon.toggle_speed", GLFW.GLFW_KEY_V)
        registerKeybinding("cinnamon.toggle_flight", GLFW.GLFW_KEY_F)
        registerKeybinding("cinnamon.toggle_nofall", GLFW.GLFW_KEY_N)
    }

    fun updateKeybinding(name: String, newKey: Int) {
        keybindings[name]?.let {
            it.setBoundKey(InputUtil.fromKeyCode(newKey, 0))
            KeyBinding.updateKeysByCode()
        }
        // If logging for a non-existent key is desired, it would be:
        // ?: run {
        //    println("Keybinding not found: $name")
        // }
    }
}