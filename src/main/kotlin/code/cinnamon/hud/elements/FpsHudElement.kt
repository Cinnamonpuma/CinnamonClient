package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.*

class FpsHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private var lastFps = 0
    private var fpsChangeAnimation = 0f
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        val currentFps = mc.currentFps
        
        // Simple fade animation for FPS changes
        if (currentFps != lastFps) {
            fpsChangeAnimation = 0.3f
            lastFps = currentFps
        }
        fpsChangeAnimation = maxOf(0f, fpsChangeAnimation - tickDelta * 0.1f)
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        // Background matching the image style
        val width = getWidth()
        val height = getHeight()
        val padding = 6
        
        // Dark gray background with rounded corners like in the image
        drawRoundedBackground(context, -padding, -padding, width + padding * 2, height + padding * 2)
        
        // Clean white FPS text with slight shadow for readability
        val fpsText = Text.literal("$currentFps").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        // Subtle shadow for better readability
        context.drawText(mc.textRenderer, fpsText, 1, 1, 0x40000000, false)
        
        // White text
        val textColor = 0xFFFFFF
        context.drawText(mc.textRenderer, fpsText, 0, 0, textColor, false)
        
        context.matrices.pop()
    }
    
    private fun drawRoundedBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Simple clean rectangle - no rounded corners to avoid jaggedness
        context.fill(x, y, x + width, y + height, 0x80000000.toInt())
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return
        
        // Just draw a simple rectangle - clean and sharp
        context.fill(x, y, x + width, y + height, color)
    }
    
    override fun getWidth(): Int {
        val text = Text.literal("${mc.currentFps}").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        return mc.textRenderer.getWidth(text)
    }
    
    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "FPS"
}