package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text

class FpsHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        renderBackground(context)
        
        val fps = "${mc.currentFps} FPS"
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        val fpsText = Text.literal(fps).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        context.drawText(mc.textRenderer, fpsText, 0, 0, 0xFFFFFF, true)
        
        context.matrices.pop()
    }
    
    override fun getWidth(): Int = mc.textRenderer.getWidth(Text.literal("${mc.currentFps} FPS").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)))
    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "FPS"
}