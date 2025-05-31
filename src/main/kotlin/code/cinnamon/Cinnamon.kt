package code.cinnamon

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.modules.ModuleManager
import code.cinnamon.keybindings.KeybindingManager
import code.cinnamon.gui.theme.ThemeConfigManager

object Cinnamon : ModInitializer {
    private val logger = LoggerFactory.getLogger("cinnamon")
    private lateinit var openGuiKeybinding: KeyBinding

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        logger.info("Initializing Cinnamon mod...")

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
                "CinnamonClient"
            )
        )

        // Register tick event to check for key presses
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Check if GUI key was pressed
            if (openGuiKeybinding.wasPressed()) {
                CinnamonGuiManager.openMainMenu()
            }

            // Check AutoClicker keybinding
            if (KeybindingManager.wasPressed("cinnamon.toggle_autoclicker")) {
                ModuleManager.toggleModule("AutoClicker")
            }
        }

        logger.info("Cinnamon mod initialized successfully!")
    }
}