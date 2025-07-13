package code.cinnamon.hud.elements

import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudElement
import code.cinnamon.spotify.SpotifyAuthManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text

class SpotifyHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        val displayText = getSpotifyText()
        val text = Text.literal(displayText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

        context.drawText(mc.textRenderer, text, 0, 0, textColor, textShadowEnabled)
        context.matrices.popMatrix()
    }

    private fun getSpotifyText(): String {
        val token = SpotifyAuthManager.getAccessToken()
        return if (token != null) {
            // You can expand this to show actual song info later
            "♪ Spotify Player"
        } else {
            "Spotify - Not Connected"
        }
    }

    override fun getWidth(): Int {
        return mc.textRenderer.getWidth(Text.literal(getSpotifyText()))
    }

    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "Spotify"
}