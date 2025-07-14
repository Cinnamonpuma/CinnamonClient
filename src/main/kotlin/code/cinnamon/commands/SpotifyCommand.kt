package code.cinnamon.commands

import code.cinnamon.spotify.SpotifyAuthManager
import code.cinnamon.spotify.SpotifyLoginPageServer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Util
import java.net.URI

object SpotifyCommand {

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommandManager.literal("spotify")
                .executes { context ->
                    executeSpotifyCommand(context)
                }
                .then(
                    ClientCommandManager.literal("disconnect")
                        .executes { context ->
                            disconnectSpotify(context)
                        }
                )
        )
    }

    private fun executeSpotifyCommand(context: CommandContext<FabricClientCommandSource>): Int {
        val mc = MinecraftClient.getInstance()

        val existingToken = SpotifyAuthManager.getAccessToken()
        if (existingToken != null) {
            mc.inGameHud.chatHud.addMessage(
                Text.literal("§a[Spotify] Already connected to Spotify!")
            )
            return 1
        }

        try {

            val authUrl = SpotifyAuthManager.getAuthorizationUrl()


            SpotifyLoginPageServer.start(authUrl)


            Util.getOperatingSystem().open(URI("http://127.0.0.1:21852/"))

            mc.inGameHud.chatHud.addMessage(
                Text.literal("§a[Spotify] Opening browser for authentication...")
            )

        } catch (e: Exception) {
            mc.inGameHud.chatHud.addMessage(
                Text.literal("§c[Spotify] Failed to start authentication: ${e.message}")
            )
            println("[Spotify] Error starting authentication: ${e.message}")
        }

        return 1
    }

    private fun disconnectSpotify(context: CommandContext<FabricClientCommandSource>): Int {
        val mc = MinecraftClient.getInstance()

        SpotifyAuthManager.disconnect()

        mc.inGameHud.chatHud.addMessage(
            Text.literal("§6[Spotify] Disconnected from Spotify. Use /spotify to reconnect.")
        )

        return 1
    }
}