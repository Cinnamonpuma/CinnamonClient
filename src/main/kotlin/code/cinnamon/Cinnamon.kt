package code.cinnamon

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.AnimatedScreenTransition
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import code.cinnamon.modules.ModuleManager
import code.cinnamon.keybindings.KeybindingManager
import code.cinnamon.gui.theme.ThemeConfigManager
import code.cinnamon.hud.HudManager
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.render.RenderTickCounter

object Cinnamon : ModInitializer {
    private val logger = LoggerFactory.getLogger("cinnamon")
    private lateinit var openGuiKeybinding: KeyBinding

    override fun onInitialize() {
        logger.info("Initializing Cinnamon mod...")

        ThemeConfigManager.loadTheme()
        ModuleManager.initialize()
        code.cinnamon.modules.ModuleConfigManager.loadModules()
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
            // Update screen transition animation
            // It's important to get the delta time for smooth animations.
            // ClientTickEvents doesn't directly provide delta, so we'll use a simple 1/20 for now (assuming 20 TPS)
            // For more accuracy, a proper delta time calculation would be needed, possibly from RenderTickCounter if available here,
            // or by tracking time between ticks.
            AnimatedScreenTransition.update(1f / 20f) // Assuming 20 ticks per second

            if (openGuiKeybinding.wasPressed()) {
                CinnamonGuiManager.openMainMenu()
            }

            if (KeybindingManager.wasPressed("cinnamon.toggle_autoclicker")) {
                ModuleManager.toggleModule("AutoClicker")
            }

            if (KeybindingManager.wasPressed("cinnamon.toggle_fullbright")) {
                ModuleManager.toggleModule("Fullbright")
            }

            if (KeybindingManager.wasPressed("cinnamon.open_saved_gui")) {
                val storedScreen = code.cinnamon.SharedVariables.storedScreen
                if (storedScreen is Screen) {
                    // Ensure this also uses the transition manager if it's a CinnamonScreen
                    if (storedScreen is code.cinnamon.gui.CinnamonScreen) {
                        CinnamonGuiManager.openScreen(storedScreen)
                    } else {
                        MinecraftClient.getInstance().setScreen(storedScreen)
                    }
                }
            }
        }


        HudElementRegistry.addLast(Identifier.of("cinnamon", "main_hud_renderer")) { drawContext: DrawContext, renderTickCounter: RenderTickCounter ->
            val mc = MinecraftClient.getInstance()
            if (mc != null && mc.window != null) {
                val currentGuiScale = mc.window.scaleFactor.toFloat()
                val safeCurrentGuiScale = if (currentGuiScale > 0f) currentGuiScale else code.cinnamon.gui.CinnamonScreen.TARGET_SCALE_FACTOR

                val scaleRatio = code.cinnamon.gui.CinnamonScreen.TARGET_SCALE_FACTOR / safeCurrentGuiScale

                drawContext.matrices.pushMatrix()
                drawContext.matrices.scale(scaleRatio, scaleRatio, drawContext.matrices)

                val partialTick = renderTickCounter.getTickProgress(false)
                HudManager.render(drawContext, partialTick)

                drawContext.matrices.popMatrix()
            } else {
                val partialTick = renderTickCounter.getTickProgress(false)
                HudManager.render(drawContext, partialTick)
            }
        }
        logger.info("Cinnamon HUD renderer registered with HudElementRegistry.")

        logger.info("Cinnamon mod initialized successfully!")
    }
}