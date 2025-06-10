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
        
        // Simple background - no border
        val width = getWidth()
        val height = getHeight()
        val padding = 4
        
        // Clean background only
        drawSimpleBackground(context, -padding, -padding, width + padding * 2, height + padding * 2)
        
        // FPS color coding - clean and simple
        val fpsColor = when {
            currentFps >= 120 -> 0x4CAF50 // Green
            currentFps >= 60 -> 0xFFC107  // Amber
            currentFps >= 30 -> 0xFF9800  // Orange
            else -> 0xF44336              // Red
        }
        
        // Clean FPS text
        val fpsText = Text.literal("$currentFps FPS").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        // Simple drop shadow
        context.drawText(mc.textRenderer, fpsText, 1, 1, 0x40000000, false)
        
        // Main text with subtle change animation
        val alpha = (0xFF - (fpsChangeAnimation * 100).toInt()).coerceIn(0xAA, 0xFF)
        val finalColor = (alpha shl 24) or (fpsColor and 0xFFFFFF)
        context.drawText(mc.textRenderer, fpsText, 0, 0, finalColor, false)
        
        context.matrices.pop()
    }
    
    private fun drawSimpleBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val cornerRadius = 3
        
        // Simple dark background only - no border
        drawRoundedRect(context, x, y, width, height, cornerRadius, 0xCC000000.toInt())
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return
        
        // Main rectangles
        context.fill(x + radius, y, x + width - radius, y + height, color)
        context.fill(x, y + radius, x + width, y + height - radius, color)
        
        // Simple corner approximation
        for (i in 0 until radius) {
            val w = sqrt(maxOf(0f, (radius * radius - i * i).toFloat())).toInt()
            if (w > 0) {
                context.fill(x + radius - w, y + i, x + radius + w, y + i + 1, color)
                context.fill(x + radius - w, y + height - i - 1, x + radius + w, y + height - i, color)
                context.fill(x + width - radius - w, y + i, x + width - radius + w, y + i + 1, color)
                context.fill(x + width - radius - w, y + height - i - 1, x + width - radius + w, y + height - i, color)
            }
        }
    }
    
    override fun getWidth(): Int {
        val text = Text.literal("${mc.currentFps} FPS").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        return mc.textRenderer.getWidth(text)
    }
    
    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "FPS"
}