package code.cinnamon.hud.elements

import code.cinnamon.hud.HudElement
import code.cinnamon.spotify.SpotifyApi
import code.cinnamon.spotify.SpotifyAuthManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.awt.image.BufferedImage
import java.net.URL
import java.util.concurrent.Executors
import javax.imageio.ImageIO

class SpotifyHudElement(initialX: Float, initialY: Float) : HudElement(initialX, initialY), Element {

    private val client = MinecraftClient.getInstance()
    private var songTitle: String = "Spotify"
    private var albumArt: BufferedImage? = null
    private var iconTexture: Identifier? = null

    private val threadPool = Executors.newSingleThreadExecutor()

    init {
        SpotifyAuthManager.authenticate()
        fetchSpotifyInfoAsync()
    }

    private fun fetchSpotifyInfoAsync() {
        threadPool.execute {
            while (true) {
                try {
                    val token = SpotifyAuthManager.getAccessToken()
                    if (!token.isNullOrEmpty()) {
                        val (title, iconUrl) = SpotifyApi.getCurrentSong(token) ?: ("Unknown" to null)
                        songTitle = title
                        if (!iconUrl.isNullOrBlank()) {
                            val image = ImageIO.read(URL(iconUrl))
                            albumArt = image
                            val id = Identifier.tryParse("cinnamon:spotify_icon")
                            iconTexture = id
                        }
                    }
                    Thread.sleep(5000)
                } catch (e: Exception) {
                    println("[Spotify HUD] Error: ${e.message}")
                }
            }
        }
    }

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(scale, scale, context.matrices)

        val iconSize = 20
        val textX = iconSize + 6
        context.drawText(client.textRenderer, Text.literal(songTitle), textX, 6, 0xFFFFFF, true)

        context.matrices.popMatrix()
    }

    override fun getWidth(): Int = 120
    override fun getHeight(): Int = 24
    override fun getName(): String = "Spotify"

    override fun isFocused(): Boolean = false
    override fun setFocused(focused: Boolean) {}
    override fun isMouseOver(scaledMouseX: Double, scaledMouseY: Double): Boolean {
        return scaledMouseX >= getX() && scaledMouseX <= getX() + getWidth() * scale &&
                scaledMouseY >= getY() && scaledMouseY <= getY() + getHeight() * scale
    }
}
