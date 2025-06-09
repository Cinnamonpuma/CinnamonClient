package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text

class PingHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        renderBackground(context)
        
        val ping = "${getPing()}ms"
        val pingText = Text.literal(ping).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        context.drawText(mc.textRenderer, pingText, 0, 0, 0xFFFFFF, true)
        
        context.matrices.pop()
    }
    
    private fun getPing(): Int {
        return mc.networkHandler?.getPlayerListEntry(mc.player?.uuid)?.latency ?: 0
    }
    
    override fun getWidth(): Int {
        val text = Text.literal("${getPing()}ms").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        return mc.textRenderer.getWidth(text)
    }
    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "Ping"
}