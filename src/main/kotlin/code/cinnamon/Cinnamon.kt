package code.cinnamon

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import code.cinnamon.gui.CinnamonGuiManager
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
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import code.cinnamon.spotify.SpotifyAuthManager
import net.minecraft.text.ClickEvent

object Cinnamon : ModInitializer {
    private val logger = LoggerFactory.getLogger("cinnamon")
    private lateinit var openGuiKeybinding: KeyBinding
    private var sentSpotifyMessage = false

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
                    MinecraftClient.getInstance().setScreen(storedScreen)
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

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                ClientCommandManager.literal("spotify")
                    .executes { context ->
                        val authUrl = SpotifyAuthManager.getAuthorizationUrl()
                        code.cinnamon.spotify.SpotifyLoginPageServer.start(authUrl)

                        // Simple message without click event to avoid conflicts
                        val messageText = Text.literal("§a[Cinnamon] §fSpotify authentication started! Go to: §bhttp://127.0.0.1:21852/")
                        context.source.sendFeedback(messageText)
                        1
                    }
            )
        }

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (client.player != null && !sentSpotifyMessage) {
                client.inGameHud.chatHud.addMessage(Text.of("§a[Cinnamon] §fUse /spotify to authenticate with Spotify."))
                sentSpotifyMessage = true
            }
        }

        logger.info("Cinnamon mod initialized successfully!")
    }
}