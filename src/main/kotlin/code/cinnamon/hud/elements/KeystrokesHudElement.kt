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
    private val keySize = 32
    private val spacing = 4
    
    // Simple press states
    private var wPressed = false
    private var aPressed = false
    private var sPressed = false
    private var dPressed = false
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        // Update key states
        updateKeyStates()
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        // W key (centered)
        val wX = keySize + spacing
        drawKey(context, "W", wX, 0, wPressed)
        
        // A S D keys
        drawKey(context, "A", 0, keySize + spacing, aPressed)
        drawKey(context, "S", keySize + spacing, keySize + spacing, sPressed)
        drawKey(context, "D", (keySize + spacing) * 2, keySize + spacing, dPressed)
        
        context.matrices.pop()
    }
    
    private fun updateKeyStates() {
        wPressed = mc.options.forwardKey.isPressed
        aPressed = mc.options.leftKey.isPressed
        sPressed = mc.options.backKey.isPressed
        dPressed = mc.options.rightKey.isPressed
    }
    
    private fun drawKey(context: DrawContext, key: String, x: Int, y: Int, pressed: Boolean) {
        // Semi-transparent background that blends better
        val bgColor = if (pressed) {
            0xAAFFFFFF.toInt() // Semi-transparent white when pressed
        } else {
            0x80000000.toInt() // Semi-transparent black when not pressed
        }
        
        // Draw clean rectangular key background
        context.fill(x, y, x + keySize, y + keySize, bgColor)
        
        // Key text with good contrast
        val keyText = Text.literal(key).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        val textColor = if (pressed) 0x000000 else 0xFFFFFF // Black on white, white on dark
        
        // Center the text
        val textWidth = mc.textRenderer.getWidth(keyText)
        val textHeight = mc.textRenderer.fontHeight
        val textX = x + (keySize - textWidth) / 2
        val textY = y + (keySize - textHeight) / 2
        
        // Add subtle shadow for unpressed keys
        if (!pressed) {
            context.drawText(mc.textRenderer, keyText, textX + 1, textY + 1, 0x40000000, false)
        }
        
        // Draw main text
        context.drawText(mc.textRenderer, keyText, textX, textY, textColor, false)
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return
        
        // Just draw a simple rectangle - clean and sharp
        context.fill(x, y, x + width, y + height, color)
    }
    
    override fun getWidth(): Int = keySize * 3 + spacing * 2
    override fun getHeight(): Int = keySize * 2 + spacing
    override fun getName(): String = "Keystrokes"
}