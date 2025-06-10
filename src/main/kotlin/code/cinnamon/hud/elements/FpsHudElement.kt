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
    private var animationTime = 0f
    private var lastFps = 0
    private var fpsChangeAnimation = 0f
    private var fpsHistory = mutableListOf<Int>()
    private val maxHistorySize = 60 // Store 60 fps values for smoothing
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        animationTime += tickDelta * 0.05f
        
        val currentFps = mc.currentFps
        
        // Update FPS history for smoothing
        fpsHistory.add(currentFps)
        if (fpsHistory.size > maxHistorySize) {
            fpsHistory.removeAt(0)
        }
        
        // Animate FPS changes
        if (currentFps != lastFps) {
            fpsChangeAnimation = 1f
            lastFps = currentFps
        }
        fpsChangeAnimation = maxOf(0f, fpsChangeAnimation - tickDelta * 0.1f)
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        val width = getWidth()
        val height = getHeight()
        val padding = 8
        val cornerRadius = 6f
        
        // Animated glow effect
        val glowIntensity = (sin(animationTime * 2f) * 0.3f + 0.7f).coerceIn(0.4f, 1f)
        
        // Background with gradient and glow
        drawRoundedRectWithGlow(context, -padding, -padding, width + padding * 2, height + padding * 2, cornerRadius, glowIntensity)
        
        // FPS color coding
        val fpsColor = when {
            currentFps >= 120 -> 0x00FF88 // Green
            currentFps >= 60 -> 0xFFDD00  // Yellow
            currentFps >= 30 -> 0xFF8800  // Orange
            else -> 0xFF4444              // Red
        }
        
        // FPS text with shadow and glow
        val fpsText = Text.literal("${currentFps} FPS").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        // Text shadow
        context.drawText(mc.textRenderer, fpsText, 1, 1, 0x000000, false)
        
        // Main text with color and change animation
        val changeScale = 1f + fpsChangeAnimation * 0.2f
        context.matrices.push()
        context.matrices.scale(changeScale, changeScale, 1f)
        context.drawText(mc.textRenderer, fpsText, 0, 0, fpsColor or 0xFF000000.toInt(), false)
        context.matrices.pop()
        
        // Mini FPS graph
        drawMiniGraph(context, width - 40, height - 2, 35, 8)
        
        context.matrices.pop()
    }
    
    private fun drawRoundedRectWithGlow(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, glowIntensity: Float) {
        // Outer glow
        val glowColor = (0x20FFFFFF * glowIntensity).toInt()
        for (i in 3 downTo 1) {
            val alpha = (0x10 * glowIntensity / i).toInt()
            drawRoundedRect(context, x - i, y - i, width + i * 2, height + i * 2, radius + i, alpha shl 24)
        }
        
        // Main background gradient
        drawGradientRoundedRect(context, x, y, width, height, radius, 
            0xE0000000.toInt(), 0xC0101010.toInt())
        
        // Border
        drawRoundedRectBorder(context, x, y, width, height, radius, 0x80FFFFFF.toInt())
    }
    
    private fun drawGradientRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, topColor: Int, bottomColor: Int) {
        // Simplified gradient - draw multiple horizontal lines
        for (i in 0 until height) {
            val progress = i.toFloat() / height
            val color = blendColors(topColor, bottomColor, progress)
            
            val lineY = y + i
            val startX = if (i < radius || i > height - radius) {
                x + (radius - sqrt(radius * radius - (min(i, height - i - 1) - radius + 1).pow(2))).toInt()
            } else x
            
            val endX = if (i < radius || i > height - radius) {
                x + width - (radius - sqrt(radius * radius - (min(i, height - i - 1) - radius + 1).pow(2))).toInt()
            } else x + width
            
            if (startX < endX) {
                context.fill(startX, lineY, endX, lineY + 1, color)
            }
        }
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, color: Int) {
        if (color == 0) return
        
        // Simple rounded rectangle approximation
        context.fill(x + radius.toInt(), y, x + width - radius.toInt(), y + height, color)
        context.fill(x, y + radius.toInt(), x + width, y + height - radius.toInt(), color)
        
        // Corner approximation
        val r = radius.toInt()
        for (i in 0 until r) {
            val w = sqrt((r * r - i * i).toFloat()).toInt()
            context.fill(x + r - w, y + i, x + r + w, y + i + 1, color)
            context.fill(x + r - w, y + height - i - 1, x + r + w, y + height - i, color)
            context.fill(x + width - r - w, y + i, x + width - r + w, y + i + 1, color)
            context.fill(x + width - r - w, y + height - i - 1, x + width - r + w, y + height - i, color)
        }
    }
    
    private fun drawRoundedRectBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, color: Int) {
        // Top and bottom borders
        context.fill(x + radius.toInt(), y, x + width - radius.toInt(), y + 1, color)
        context.fill(x + radius.toInt(), y + height - 1, x + width - radius.toInt(), y + height, color)
        
        // Left and right borders
        context.fill(x, y + radius.toInt(), x + 1, y + height - radius.toInt(), color)
        context.fill(x + width - 1, y + radius.toInt(), x + width, y + height - radius.toInt(), color)
    }
    
    private fun drawMiniGraph(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        if (fpsHistory.size < 2) return
        
        val maxFps = fpsHistory.maxOrNull() ?: 1
        val minFps = fpsHistory.minOrNull() ?: 0
        val range = maxOf(1, maxFps - minFps)
        
        val stepX = width.toFloat() / (fpsHistory.size - 1)
        
        for (i in 1 until fpsHistory.size) {
            val x1 = x + ((i - 1) * stepX).toInt()
            val x2 = x + (i * stepX).toInt()
            val y1 = y - ((fpsHistory[i - 1] - minFps) * height / range)
            val y2 = y - ((fpsHistory[i] - minFps) * height / range)
            
            // Draw line
            drawLine(context, x1, y1, x2, y2, 0x80FFFF00.toInt())
        }
    }
    
    private fun drawLine(context: DrawContext, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy
        
        var x = x1
        var y = y1
        
        while (true) {
            context.fill(x, y, x + 1, y + 1, color)
            
            if (x == x2 && y == y2) break
            
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }
    
    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        val a1 = (color1 shr 24) and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        val a2 = (color2 shr 24) and 0xFF
        
        val r = (r1 + (r2 - r1) * ratio).toInt()
        val g = (g1 + (g2 - g1) * ratio).toInt()
        val b = (b1 + (b2 - b1) * ratio).toInt()
        val a = (a1 + (a2 - a1) * ratio).toInt()
        
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
    
    override fun getWidth(): Int {
        val text = Text.literal("${mc.currentFps} FPS").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        return maxOf(mc.textRenderer.getWidth(text), 60)
    }
    
    override fun getHeight(): Int = mc.textRenderer.fontHeight + 8
    override fun getName(): String = "FPS"
}