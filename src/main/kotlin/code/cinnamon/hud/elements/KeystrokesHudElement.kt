package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text

class KeystrokesHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private val keySize = 20
    private val spacing = 2
    
    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return
        
        renderBackground(context)
        
        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)
        
        // W key
        drawKey(context, "W", keySize, 0, mc.options.forwardKey.isPressed)
        
        // A S D keys
        drawKey(context, "A", 0, keySize + spacing, mc.options.leftKey.isPressed)
        drawKey(context, "S", keySize + spacing, keySize + spacing, mc.options.backKey.isPressed)
        drawKey(context, "D", (keySize + spacing) * 2, keySize + spacing, mc.options.rightKey.isPressed)
        
        // Space bar
        drawKey(context, "Space", 0, (keySize + spacing) * 2, keySize * 3 + spacing * 2, keySize, mc.options.jumpKey.isPressed)
        
        context.matrices.pop()
    }
    
    private fun drawKey(context: DrawContext, key: String, x: Int, y: Int, pressed: Boolean) {
        drawKey(context, key, x, y, keySize, keySize, pressed)
    }
    
    private fun drawKey(context: DrawContext, key: String, x: Int, y: Int, width: Int, height: Int, pressed: Boolean) {
        val bgColor = if (pressed) 0x80FFFFFF.toInt() else 0x80000000.toInt()
        val textColor = if (pressed) 0x000000 else 0xFFFFFF
        
        context.fill(x, y, x + width, y + height, bgColor)
        context.fill(x, y, x + width, y + 1, 0xFFFFFFFF.toInt()) // Top border
        context.fill(x, y, x + 1, y + height, 0xFFFFFFFF.toInt()) // Left border
        context.fill(x + width - 1, y, x + width, y + height, 0xFF000000.toInt()) // Right border
        context.fill(x, y + height - 1, x + width, y + height, 0xFF000000.toInt()) // Bottom border
        
        val keyText = Text.literal(key).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        val textX = x + (width - mc.textRenderer.getWidth(keyText)) / 2
        val textY = y + (height - mc.textRenderer.fontHeight) / 2
        context.drawText(mc.textRenderer, keyText, textX, textY, textColor, false)
    }
    
    override fun getWidth(): Int = keySize * 3 + spacing * 2
    override fun getHeight(): Int = keySize * 3 + spacing * 2
    override fun getName(): String = "Keystrokes"
}