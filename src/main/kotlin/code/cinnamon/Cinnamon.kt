package code.cinnamon

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.modules.ModuleManager
import code.cinnamon.keybindings.KeybindingManager
import code.cinnamon.gui.theme.ThemeConfigManager

class Cinnamon : ClientModInitializer {
    private lateinit var openGuiKeybinding: KeyBinding
    
    override fun onInitializeClient() {
        // Initialize managers
        ThemeConfigManager.loadTheme()
        ModuleManager.initialize()
        KeybindingManager.initialize()
        
        // Register main GUI keybinding
        openGuiKeybinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.cinnamon.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, // Right Shift to open GUI
                "CinnamonClient" // Changed line
            )
        )
        
        // Register tick event to check for key presses
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Check if GUI key was pressed
            if (openGuiKeybinding.wasPressed()) {
                CinnamonGuiManager.openMainMenu()
            }
            
            // Check module keybindings
            if (KeybindingManager.wasPressed("cinnamon.toggle_speed")) {
                ModuleManager.toggleModule("Speed")
            }
            
            if (KeybindingManager.wasPressed("cinnamon.toggle_flight")) {
                ModuleManager.toggleModule("Flight")
            }
            
            if (KeybindingManager.wasPressed("cinnamon.toggle_nofall")) {
                ModuleManager.toggleModule("NoFall")
            }
        }
    }
}