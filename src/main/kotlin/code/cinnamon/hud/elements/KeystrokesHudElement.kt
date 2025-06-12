package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudElement
// import code.cinnamon.hud.HudManager // No longer needed for config fetching
// import code.cinnamon.hud.HudElementConfig // No longer needed as parameter to drawKey
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.*

class KeystrokesHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private val keySize = 32
    private val spacing = 4
    private val cornerRadius = 4 // Smaller radius for individual keys
    
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
        drawKey(context, "W", wX, 0, wPressed) // Removed config parameter
        
        // A S D keys
        drawKey(context, "A", 0, keySize + spacing, aPressed) // Removed config parameter
        drawKey(context, "S", keySize + spacing, keySize + spacing, sPressed) // Removed config parameter
        drawKey(context, "D", (keySize + spacing) * 2, keySize + spacing, dPressed) // Removed config parameter
        
        context.matrices.pop()
    }
    
    private fun updateKeyStates() {
        wPressed = mc.options.forwardKey.isPressed
        aPressed = mc.options.leftKey.isPressed
        sPressed = mc.options.backKey.isPressed
        dPressed = mc.options.rightKey.isPressed
    }
    
    private fun drawKey(context: DrawContext, key: String, x: Int, y: Int, pressed: Boolean) { // Removed config parameter
        val currentBgColor = if (pressed) {
            this.keypressedBackgroundColor // Pressed background is this element's keypressed background color
        } else {
            this.backgroundColor // Not pressed background is this element's background color
        }
        
        // Draw rounded rectangular key background
        drawRoundedRect(context, x, y, keySize, keySize, cornerRadius, currentBgColor)
        
        // Key text with good contrast
        val keyText = Text.literal(key).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        val currentTextColor = if (pressed) {
            this.keypressedTextColor // Pressed text is this element's keypressed text color
        } else {
            this.textColor // Not pressed text is this element's text color
        }
        
        // Center the text
        val textWidth = mc.textRenderer.getWidth(keyText)
        val textHeight = mc.textRenderer.fontHeight
        val textX = x + (keySize - textWidth) / 2
        val textY = y + (keySize - textHeight) / 2
        
        // Add subtle shadow for unpressed keys
        if (!pressed && this.textShadowEnabled) { // Use this.textShadowEnabled
            context.drawText(mc.textRenderer, keyText, textX + 1, textY + 1, 0x40000000, false)
        }
        
        // Draw main text
        context.drawText(mc.textRenderer, keyText, textX, textY, currentTextColor, false)
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return

        val r = minOf(radius, minOf(width / 2, height / 2))
        
        val clampedRadius = minOf(radius, minOf(width / 2, height / 2))
        
        if (clampedRadius <= 0) {
            context.fill(x, y, x + width, y + height, color)
            return
        }
        
        // Draw main rectangle (middle section)
        context.fill(x + clampedRadius, y, x + width - clampedRadius, y + height, color)
        
        // Draw left and right rectangles
        context.fill(x, y + clampedRadius, x + clampedRadius, y + height - clampedRadius, color)
        context.fill(x + width - clampedRadius, y + clampedRadius, x + width, y + height - clampedRadius, color)
        
        // Draw the four rounded corners
        drawRoundedCorner(context, x, y, r, color, 0) // Top-left
        drawRoundedCorner(context, x + width - r, y, r, color, 1) // Top-right
        drawRoundedCorner(context, x, y + height - r, r, color, 2) // Bottom-left
        drawRoundedCorner(context, x + width - r, y + height - r, r, color, 3) // Bottom-right
    }
    
    private fun drawRoundedCorner(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, corner: Int) {
        // Use proper circle equation: x² + y² <= r²
        for (dy in 0 until radius) {
            for (dx in 0 until radius) {
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared <= radius * radius) {
                    val pixelX: Int
                    val pixelY: Int
                    
                    when (corner) {
                        0 -> { // Top-left: draw in 3rd quadrant relative to corner
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + (radius - 1 - dy)
                        }
                        1 -> { // Top-right: draw in 4th quadrant relative to corner
                            pixelX = x + dx
                            pixelY = y + (radius - 1 - dy)
                        }
                        2 -> { // Bottom-left: draw in 2nd quadrant relative to corner
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + dy
                        }
                        3 -> { // Bottom-right: draw in 1st quadrant relative to corner
                            pixelX = x + dx
                            pixelY = y + dy
                        }
                        else -> continue
                    }
                    
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }
    
    override fun getWidth(): Int = keySize * 3 + spacing * 2
    override fun getHeight(): Int = keySize * 2 + spacing
    override fun getName(): String = "Keystrokes"
}