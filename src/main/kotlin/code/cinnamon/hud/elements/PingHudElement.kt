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
    private var animationTime = 0f
    private var lastPing = 0
    private var pingChangeAnimation = 0f
    private var pingHistory = mutableListOf<Int>()
    private val maxHistorySize = 60 // Store 60 ping values for graph
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        animationTime += tickDelta * 0.05f
        
        val currentPing = getPing()
        
        // Update ping history for graph
        pingHistory.add(currentPing)
        if (pingHistory.size > maxHistorySize) {
            pingHistory.removeAt(0)
        }
        
        // Animate ping changes
        if (currentPing != lastPing) {
            pingChangeAnimation = 1f
            lastPing = currentPing
        }
        pingChangeAnimation = maxOf(0f, pingChangeAnimation - tickDelta * 0.1f)
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        val width = getWidth()
        val height = getHeight()
        val padding = 8
        val cornerRadius = 6f
        
        // Animated glow effect based on ping quality
        val pingQuality = when {
            currentPing <= 50 -> 1f
            currentPing <= 100 -> 0.8f
            currentPing <= 200 -> 0.6f
            else -> 0.4f
        }
        val glowIntensity = (sin(animationTime * (2f - pingQuality)) * 0.3f + 0.7f).coerceIn(0.4f, 1f)
        
        // Background with gradient and glow
        drawRoundedRectWithGlow(context, -padding, -padding, width + padding * 2, height + padding * 2, cornerRadius, glowIntensity, currentPing)
        
        // Ping color coding (similar to FPS but for ping)
        val pingColor = when {
            currentPing <= 50 -> 0x00FF88   // Green - excellent
            currentPing <= 100 -> 0xFFDD00  // Yellow - good  
            currentPing <= 200 -> 0xFF8800  // Orange - fair
            else -> 0xFF4444                // Red - poor
        }
        
        // Ping text with shadow and change animation
        val pingText = Text.literal("${currentPing}ms").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        // Text shadow
        context.drawText(mc.textRenderer, pingText, 1, 1, 0x000000, false)
        
        // Main text with color and change animation
        val changeScale = 1f + pingChangeAnimation * 0.2f
        context.matrices.push()
        context.matrices.scale(changeScale, changeScale, 1f)
        context.drawText(mc.textRenderer, pingText, 0, 0, pingColor or 0xFF000000.toInt(), false)
        context.matrices.pop()
        
        // Mini ping graph
        drawMiniGraph(context, width - 40, height - 2, 35, 8)
        
        context.matrices.pop()
    }
    
    private fun drawRoundedRectWithGlow(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, glowIntensity: Float, ping: Int) {
        // Ping-based glow color
        val glowBaseColor = when {
            ping <= 50 -> 0x0088FF   // Blue glow for good ping
            ping <= 100 -> 0x00FF88  // Green glow
            ping <= 200 -> 0xFFDD00  // Yellow glow
            else -> 0xFF4444         // Red glow for bad ping
        }
        
        // Outer glow with ping-based color
        val glowColor = ((0x20 * glowIntensity).toInt() shl 24) or (glowBaseColor and 0xFFFFFF)
        for (i in 3 downTo 1) {
            val alpha = (0x10 * glowIntensity / i).toInt()
            drawRoundedRect(context, x - i, y - i, width + i * 2, height + i * 2, radius + i, (alpha shl 24) or (glowBaseColor and 0xFFFFFF))
        }
        
        // Main background gradient
        drawGradientRoundedRect(context, x, y, width, height, radius, 
            0xE0000000.toInt(), 0xC0101010.toInt())
        
        // Border with ping color
        val borderColor = ((0x80 shl 24) or (glowBaseColor and 0xFFFFFF))
        drawRoundedRectBorder(context, x, y, width, height, radius, borderColor)
    }
    
    private fun drawGradientRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, topColor: Int, bottomColor: Int) {
        // Simplified gradient - draw multiple horizontal lines
        for (i in 0 until height) {
            val progress = i.toFloat() / height
            val color = blendColors(topColor, bottomColor, progress)
            
            val lineY = y + i
            val startX = if (i < radius || i > height - radius) {
                val offset = min(i, height - i - 1) - radius + 1
                x + maxOf(0, (radius - sqrt(maxOf(0f, radius * radius - offset * offset))).toInt())
            } else x
            
            val endX = if (i < radius || i > height - radius) {
                val offset = min(i, height - i - 1) - radius + 1
                x + width - maxOf(0, (radius - sqrt(maxOf(0f, radius * radius - offset * offset))).toInt())
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
            val w = sqrt(maxOf(0f, (r * r - i * i).toFloat())).toInt()
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
        if (pingHistory.size < 2) return
        val maxPing = pingHistory.maxOrNull() ?: 1
        val minPing = pingHistory.minOrNull() ?: 0
        val range = maxOf(1, maxPing - minPing)
        val stepX = width.toFloat() / (pingHistory.size - 1)
        for (i in 1 until pingHistory.size) {
            val prevPing = pingHistory[i - 1]
            val currPing = pingHistory[i]
            val x1 = x + ((i - 1) * stepX).toInt()
            val x2 = x + (i * stepX).toInt()
            val deltaPrev = (maxPing - prevPing)
            val deltaCurr = (maxPing - currPing)
            val y1 = y - ((deltaPrev.toInt() * height) / range)
            val y2 = y - ((deltaCurr.toInt() * height) / range)
            val lineColor = when {
                currPing <= 50 -> 0x8000FF88.toInt()   // Green
                currPing <= 100 -> 0x80FFDD00.toInt()  // Yellow
                currPing <= 200 -> 0x80FF8800.toInt()  // Orange
                else -> 0x80FF4444.toInt()             // Red
            }
            drawLine(context, x1, y1, x2, y2, lineColor)
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
    
    private fun getPing(): Int {
        return mc.networkHandler?.getPlayerListEntry(mc.player?.uuid)?.latency ?: 0
    }
    
    override fun getWidth(): Int {
        val text = Text.literal("${getPing()}ms").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        return maxOf(mc.textRenderer.getWidth(text), 60)
    }
    
    override fun getHeight(): Int = mc.textRenderer.fontHeight + 8
    override fun getName(): String = "Ping"
}