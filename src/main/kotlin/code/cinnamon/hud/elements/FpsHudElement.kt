package code.cinnamon.hud.elements

import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

class FpsHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        renderBackground(context)
        
        val fps = "${mc.currentFps} FPS"
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        context.drawText(mc.textRenderer, fps, 0, 0, 0xFFFFFF, true)
        
        context.matrices.pop()
    }
    
    override fun getWidth(): Int = mc.textRenderer.getWidth("${mc.currentFps} FPS")
    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "FPS"
}