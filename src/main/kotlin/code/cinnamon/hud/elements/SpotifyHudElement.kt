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
    private val internalPadding = 6
    private val cornerRadius = 6

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        val token = SpotifyAuthManager.getAccessToken()
        val text = if (token != null) "Spotify Linked" else "Login Spotify"
        val display = Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

        if (backgroundColor != 0) {
            drawRoundedRect(context, -internalPadding, -internalPadding,
                getWidth() + internalPadding * 2, getHeight() + internalPadding * 2,
                cornerRadius, backgroundColor)
        }

        context.drawText(mc.textRenderer, display, 0, 0, textColor, textShadowEnabled)
        context.matrices.popMatrix()
    }

    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, w: Int, h: Int, r: Int, color: Int) {
        context.fill(x, y, x + w, y + h, color)
        // Rounded edges are optional for now
    }

    override fun getWidth(): Int {
        val text = SpotifyAuthManager.getAccessToken()?.let { "Spotify Linked" } ?: "Login Spotify"
        return mc.textRenderer.getWidth(Text.literal(text))
    }

    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "Spotify"
}
