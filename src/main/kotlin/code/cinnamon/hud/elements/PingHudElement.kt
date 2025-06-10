package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.*

class PingHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private var lastPing = 0
    private var pingChangeAnimation = 0f
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        val currentPing = getPing()
        
        // Simple fade animation for ping changes
        if (currentPing != lastPing) {
            pingChangeAnimation = 0.3f
            lastPing = currentPing
        }
        pingChangeAnimation = maxOf(0f, pingChangeAnimation - tickDelta * 0.1f)
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        // Minimal background
        val width = getWidth()
        val height = getHeight()
        val padding = 4
        
        // Clean rounded background
        drawSimpleBackground(context, -padding, -padding, width + padding * 2, height + padding * 2)
        
        // Ping color coding - clean and readable
        val pingColor = when {
            currentPing <= 50 -> 0x4CAF50   // Green
            currentPing <= 100 -> 0xFFC107  // Amber
            currentPing <= 200 -> 0xFF9800  // Orange
            else -> 0xF44336                // Red
        }
        
        // Clean ping text
        val pingText = Text.literal("${currentPing}ms").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        // Simple drop shadow
        context.drawText(mc.textRenderer, pingText, 1, 1, 0x40000000, false)
        
        // Main text with subtle change animation
        val alpha = (0xFF - (pingChangeAnimation * 100).toInt()).coerceIn(0xAA, 0xFF)
        val finalColor = (alpha shl 24) or (pingColor and 0xFFFFFF)
        context.drawText(mc.textRenderer, pingText, 0, 0, finalColor, false)
        
        context.matrices.pop()
    }
    
    private fun drawSimpleBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val cornerRadius = 3
        
        // Main background - dark with slight transparency
        drawRoundedRect(context, x, y, width, height, cornerRadius, 0xCC000000.toInt())
        
        // Subtle border
        drawRoundedRectBorder(context, x, y, width, height, cornerRadius, 0x33FFFFFF)
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
    
    private fun drawRoundedRectBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        // Top and bottom borders
        context.fill(x + radius, y, x + width - radius, y + 1, color)
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color)
        
        // Left and right borders
        context.fill(x, y + radius, x + 1, y + height - radius, color)
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color)
        
        // Corner borders
        for (i in 0 until radius) {
            val w = sqrt(maxOf(0f, (radius * radius - i * i).toFloat())).toInt()
            if (w > 0) {
                context.fill(x + radius - w, y + i, x + radius - w + 1, y + i + 1, color)
                context.fill(x + radius + w - 1, y + i, x + radius + w, y + i + 1, color)
                context.fill(x + width - radius - w, y + i, x + width - radius - w + 1, y + i + 1, color)
                context.fill(x + width - radius + w - 1, y + i, x + width - radius + w, y + i + 1, color)
                
                context.fill(x + radius - w, y + height - i - 1, x + radius - w + 1, y + height - i, color)
                context.fill(x + radius + w - 1, y + height - i - 1, x + radius + w, y + height - i, color)
                context.fill(x + width - radius - w, y + height - i - 1, x + width - radius - w + 1, y + height - i, color)
                context.fill(x + width - radius + w - 1, y + height - i - 1, x + width - radius + w, y + height - i, color)
            }
        }
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