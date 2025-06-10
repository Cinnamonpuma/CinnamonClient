package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.*

class KeystrokesHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private val keySize = 28
    private val spacing = 3
    private var animationTime = 0f
    
    // Animation states for each key
    private var wPressed = false
    private var aPressed = false
    private var sPressed = false
    private var dPressed = false
    private var spacePressed = false
    
    // Press animations
    private var wPressAnim = 0f
    private var aPressAnim = 0f
    private var sPressAnim = 0f
    private var dPressAnim = 0f
    private var spacePressAnim = 0f
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        animationTime += tickDelta * 0.05f
        
        // Update key states and animations
        updateKeyAnimations(tickDelta)
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        // Background panel
        val totalWidth = getWidth()
        val totalHeight = getHeight()
        val padding = 6
        
        drawStylizedBackground(context, -padding, -padding, totalWidth + padding * 2, totalHeight + padding * 2)
        
        // W key (centered)
        val wX = keySize + spacing
        drawStylizedKey(context, "W", wX, 0, wPressed, wPressAnim)
        
        // A S D keys
        drawStylizedKey(context, "A", 0, keySize + spacing, aPressed, aPressAnim)
        drawStylizedKey(context, "S", keySize + spacing, keySize + spacing, sPressed, sPressAnim)
        drawStylizedKey(context, "D", (keySize + spacing) * 2, keySize + spacing, dPressed, dPressAnim)
        
        // Space bar
        val spaceWidth = keySize * 3 + spacing * 2
        drawStylizedKey(context, "SPACE", 0, (keySize + spacing) * 2, spaceWidth, keySize, spacePressed, spacePressAnim)
        
        context.matrices.pop()
    }
    
    private fun updateKeyAnimations(tickDelta: Float) {
        val currentW = mc.options.forwardKey.isPressed
        val currentA = mc.options.leftKey.isPressed
        val currentS = mc.options.backKey.isPressed
        val currentD = mc.options.rightKey.isPressed
        val currentSpace = mc.options.jumpKey.isPressed
        
        // Update press animations
        wPressAnim = updatePressAnimation(wPressAnim, currentW, wPressed, tickDelta)
        aPressAnim = updatePressAnimation(aPressAnim, currentA, aPressed, tickDelta)
        sPressAnim = updatePressAnimation(sPressAnim, currentS, sPressed, tickDelta)
        dPressAnim = updatePressAnimation(dPressAnim, currentD, dPressed, tickDelta)
        spacePressAnim = updatePressAnimation(spacePressAnim, currentSpace, spacePressed, tickDelta)
        
        // Update states
        wPressed = currentW
        aPressed = currentA
        sPressed = currentS
        dPressed = currentD
        spacePressed = currentSpace
    }
    
    private fun updatePressAnimation(currentAnim: Float, isPressed: Boolean, wasPressed: Boolean, tickDelta: Float): Float {
        return when {
            isPressed && !wasPressed -> 1f // Just pressed
            isPressed -> maxOf(0.3f, currentAnim - tickDelta * 0.1f) // Holding
            else -> maxOf(0f, currentAnim - tickDelta * 0.15f) // Released
        }
    }
    
    private fun drawStylizedBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val cornerRadius = 8f
        
        // Outer glow
        val glowIntensity = (sin(animationTime * 1.5f) * 0.2f + 0.8f).coerceIn(0.6f, 1f)
        for (i in 4 downTo 1) {
            val alpha = (0x15 * glowIntensity / i).toInt()
            drawRoundedRect(context, x - i, y - i, width + i * 2, height + i * 2, cornerRadius + i, alpha shl 24 or 0x4A90E2)
        }
        
        // Main background with gradient
        drawGradientRoundedRect(context, x, y, width, height, cornerRadius, 0xE0000000.toInt(), 0xC0111111.toInt())
        
        // Border
        drawRoundedRectBorder(context, x, y, width, height, cornerRadius, 0x60FFFFFF.toInt())
    }
    
    private fun drawStylizedKey(context: DrawContext, key: String, x: Int, y: Int, pressed: Boolean, pressAnim: Float) {
        drawStylizedKey(context, key, x, y, keySize, keySize, pressed, pressAnim)
    }
    
    private fun drawStylizedKey(context: DrawContext, key: String, x: Int, y: Int, width: Int, height: Int, pressed: Boolean, pressAnim: Float) {
        val cornerRadius = 4f
        val pressDepth = (pressAnim * 2f).toInt()
        val glowIntensity = pressAnim
        
        context.matrices.push()
        context.matrices.translate(0.0, pressDepth.toDouble(), 0.0)
        
        // Key shadow (deeper when pressed)
        val shadowOffset = 2 + (pressAnim * 2f).toInt()
        drawRoundedRect(context, x + shadowOffset, y + shadowOffset, width, height, cornerRadius, 0x40000000.toInt())
        
        // Key glow when pressed
        if (glowIntensity > 0) {
            val glowColor = when (key) {
                "W" -> 0x00FF88
                "A" -> 0xFF8800
                "S" -> 0xFF4444
                "D" -> 0x8800FF
                "SPACE" -> 0x00AAFF
                else -> 0xFFFFFF
            }
            
            for (i in 3 downTo 1) {
                val alpha = ((0x30 * glowIntensity) / i).toInt()
                drawRoundedRect(context, x - i, y - i, width + i * 2, height + i * 2, cornerRadius + i, alpha shl 24 or glowColor)
            }
        }
        
        // Key background
        val bgColor = if (pressed) {
            // Pressed state - brighter gradient
            drawGradientRoundedRect(context, x, y, width, height, cornerRadius, 0xE0FFFFFF.toInt(), 0xC0DDDDDD.toInt())
        } else {
            // Normal state - dark gradient
            drawGradientRoundedRect(context, x, y, width, height, cornerRadius, 0xE0333333.toInt(), 0xC0111111.toInt())
        }
        
        // Key border
        val borderColor = if (pressed) 0x80000000.toInt() else 0x80FFFFFF.toInt()
        drawRoundedRectBorder(context, x, y, width, height, cornerRadius, borderColor)
        
        // Key text
        val keyText = Text.literal(key).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        val textColor = if (pressed) 0x000000 else 0xFFFFFF
        
        // Text positioning
        val textWidth = mc.textRenderer.getWidth(keyText)
        val textHeight = mc.textRenderer.fontHeight
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - textHeight) / 2
        
        // Text shadow
        if (!pressed) {
            context.drawText(mc.textRenderer, keyText, textX + 1, textY + 1, 0x80000000.toInt(), false)
        }
        
        // Main text with press animation
        val textScale = 1f + pressAnim * 0.1f
        context.matrices.push()
        context.matrices.translate(textX.toDouble(), textY.toDouble(), 0.0)
        context.matrices.scale(textScale, textScale, 1f)
        context.matrices.translate(-textX.toDouble(), -textY.toDouble(), 0.0)
        context.drawText(mc.textRenderer, keyText, textX, textY, textColor, false)
        context.matrices.pop()
        
        context.matrices.pop()
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, color: Int) {
        if (color == 0) return
        
        val r = radius.toInt()
        
        // Main rectangles
        context.fill(x + r, y, x + width - r, y + height, color)
        context.fill(x, y + r, x + width, y + height - r, color)
        
        // Corners
        for (i in 0 until r) {
            val w = sqrt(maxOf(0f, (r * r - i * i).toFloat())).toInt()
            if (w > 0) {
                // Top corners
                context.fill(x + r - w, y + i, x + r + w, y + i + 1, color)
                context.fill(x + width - r - w, y + i, x + width - r + w, y + i + 1, color)
                // Bottom corners
                context.fill(x + r - w, y + height - i - 1, x + r + w, y + height - i, color)
                context.fill(x + width - r - w, y + height - i - 1, x + width - r + w, y + height - i, color)
            }
        }
    }
    
    private fun drawGradientRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, topColor: Int, bottomColor: Int) {
        for (i in 0 until height) {
            val progress = i.toFloat() / height
            val color = blendColors(topColor, bottomColor, progress)
            
            val lineY = y + i
            val r = radius.toInt()
            
            val startX = if (i < r) {
                val offset = sqrt(maxOf(0f, (r * r - (r - i) * (r - i)).toFloat())).toInt()
                x + r - offset
            } else if (i >= height - r) {
                val offset = sqrt(maxOf(0f, (r * r - (i - (height - r - 1)) * (i - (height - r - 1))).toFloat())).toInt()
                x + r - offset
            } else {
                x
            }
            
            val endX = if (i < r) {
                val offset = sqrt(maxOf(0f, (r * r - (r - i) * (r - i)).toFloat())).toInt()
                x + width - r + offset
            } else if (i >= height - r) {
                val offset = sqrt(maxOf(0f, (r * r - (i - (height - r - 1)) * (i - (height - r - 1))).toFloat())).toInt()
                x + width - r + offset
            } else {
                x + width
            }
            
            if (startX < endX) {
                context.fill(startX, lineY, endX, lineY + 1, color)
            }
        }
    }
    
    private fun drawRoundedRectBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Float, color: Int) {
        val r = radius.toInt()
        
        // Straight borders
        context.fill(x + r, y, x + width - r, y + 1, color) // Top
        context.fill(x + r, y + height - 1, x + width - r, y + height, color) // Bottom
        context.fill(x, y + r, x + 1, y + height - r, color) // Left
        context.fill(x + width - 1, y + r, x + width, y + height - r, color) // Right
        
        // Corner borders (simplified)
        for (i in 0 until r) {
            val w = sqrt(maxOf(0f, (r * r - i * i).toFloat())).toInt()
            if (w > 0) {
                // Top corners
                context.fill(x + r - w, y + i, x + r - w + 1, y + i + 1, color)
                context.fill(x + r + w - 1, y + i, x + r + w, y + i + 1, color)
                context.fill(x + width - r - w, y + i, x + width - r - w + 1, y + i + 1, color)
                context.fill(x + width - r + w - 1, y + i, x + width - r + w, y + i + 1, color)
                
                // Bottom corners
                context.fill(x + r - w, y + height - i - 1, x + r - w + 1, y + height - i, color)
                context.fill(x + r + w - 1, y + height - i - 1, x + r + w, y + height - i, color)
                context.fill(x + width - r - w, y + height - i - 1, x + width - r - w + 1, y + height - i, color)
                context.fill(x + width - r + w - 1, y + height - i - 1, x + width - r + w, y + height - i, color)
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
    
    override fun getWidth(): Int = keySize * 3 + spacing * 2
    override fun getHeight(): Int = keySize * 3 + spacing * 2
    override fun getName(): String = "Keystrokes"
}