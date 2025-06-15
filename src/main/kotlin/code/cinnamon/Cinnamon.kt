package code.cinnamon

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.modules.ModuleManager
import code.cinnamon.keybindings.KeybindingManager
import code.cinnamon.gui.theme.ThemeConfigManager
import code.cinnamon.hud.HudManager
import code.cinnamon.hud.HudScreen
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen

object Cinnamon : ModInitializer {
    private val logger = LoggerFactory.getLogger("cinnamon")
    private lateinit var openGuiKeybinding: KeyBinding

    override fun onInitialize() {
        logger.info("Initializing Cinnamon mod...")

        ThemeConfigManager.loadTheme()
        ModuleManager.initialize()
        KeybindingManager.initialize()
        
        HudManager.init()
        logger.info("HUD system initialized")

        openGuiKeybinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.cinnamon.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "CinnamonClient"
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (openGuiKeybinding.wasPressed()) {
                CinnamonGuiManager.openMainMenu()
            }

            if (KeybindingManager.wasPressed("cinnamon.toggle_autoclicker")) {
                ModuleManager.toggleModule("AutoClicker")
            }

            if (KeybindingManager.wasPressed("cinnamon.open_saved_gui")) {
                val storedScreen = code.cinnamon.SharedVariables.storedScreen
                if (storedScreen is Screen) {
                    MinecraftClient.getInstance().setScreen(storedScreen)
                }
            }
        }


        HudRenderCallback.EVENT.register { drawContext, renderTickCounter ->
            val partialTick = renderTickCounter.getTickProgress(false)
            HudManager.render(drawContext, partialTick)
        }

        logger.info("Cinnamon mod initialized successfully!")
    }
}