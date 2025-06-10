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
    private val keySize = 24
    private val spacing = 2
    
    // Simple press states
    private var wPressed = false
    private var aPressed = false
    private var sPressed = false
    private var dPressed = false
    private var spacePressed = false
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        // Update key states
        updateKeyStates()
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        // W key (centered)
        val wX = keySize + spacing
        drawCleanKey(context, "W", wX, 0, wPressed)
        
        // A S D keys
        drawCleanKey(context, "A", 0, keySize + spacing, aPressed)
        drawCleanKey(context, "S", keySize + spacing, keySize + spacing, sPressed)
        drawCleanKey(context, "D", (keySize + spacing) * 2, keySize + spacing, dPressed)
        
        // Space bar
        val spaceWidth = keySize * 3 + spacing * 2
        drawCleanKey(context, "━━━", 0, (keySize + spacing) * 2, spaceWidth, keySize, spacePressed)
        
        context.matrices.pop()
    }
    
    private fun updateKeyStates() {
        wPressed = mc.options.forwardKey.isPressed
        aPressed = mc.options.leftKey.isPressed
        sPressed = mc.options.backKey.isPressed
        dPressed = mc.options.rightKey.isPressed
        spacePressed = mc.options.jumpKey.isPressed
    }
    
    private fun drawCleanKey(context: DrawContext, key: String, x: Int, y: Int, pressed: Boolean) {
        drawCleanKey(context, key, x, y, keySize, keySize, pressed)
    }
    
    private fun drawCleanKey(context: DrawContext, key: String, x: Int, y: Int, width: Int, height: Int, pressed: Boolean) {
        val cornerRadius = 2
        
        // Simple background - no border
        val bgColor = if (pressed) {
            0xDDFFFFFF.toInt() // White when pressed
        } else {
            0xCC2A2A2A.toInt() // Dark gray when not pressed
        }
        
        // Draw key background only
        drawRoundedRect(context, x, y, width, height, cornerRadius, bgColor)
        
        // Key text - clean and simple
        val keyText = Text.literal(key).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        val textColor = if (pressed) 0x000000 else 0xFFFFFF
        
        // Center the text
        val textWidth = mc.textRenderer.getWidth(keyText)
        val textHeight = mc.textRenderer.fontHeight
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - textHeight) / 2
        
        // Draw text without shadow for cleaner look
        context.drawText(mc.textRenderer, keyText, textX, textY, textColor, false)
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return
        
        // Main rectangles
        context.fill(x + radius, y, x + width - radius, y + height, color)
        context.fill(x, y + radius, x + width, y + height - radius, color)
        
        // Simple corner approximation for minimal radius
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
    
    override fun getWidth(): Int = keySize * 3 + spacing * 2
    override fun getHeight(): Int = keySize * 3 + spacing * 2
    override fun getName(): String = "Keystrokes"
}