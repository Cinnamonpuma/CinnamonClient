package code.cinnamon.gui.components

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import code.cinnamon.gui.theme.CinnamonTheme
import kotlin.math.max
import kotlin.math.min

class CinnamonButton(
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val height: Int,
    private val text: Text,
    private val onClick: (mouseX: Double, mouseY: Double) -> Unit,
    private val isPrimary: Boolean = false
) {
    private var isHovered = false
    private var isPressed = false
    private var isEnabled = true
    private var animationProgress = 0f
    private val client = MinecraftClient.getInstance()
    
    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateAnimation(delta)
        
        val backgroundColor = getBackgroundColor()
        val textColor = getTextColor()
        
        // Draw button background with rounded corners effect
        drawRoundedRect(context, x, y, width, height, backgroundColor)
        
        // Draw border
        if (isHovered || isPressed) {
            drawBorder(context, x, y, width, height, CinnamonTheme.accentColor)
        }
        
        // Draw text
        val textWidth = client.textRenderer.getWidth(text)
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - client.textRenderer.fontHeight) / 2
        
        context.drawText(
            client.textRenderer,
            text,
            textX,
            textY,
            textColor,
            true
        )
        
        // Draw hover effect
        if (isHovered && isEnabled) {
            val alpha = (animationProgress * 0.1f).toInt()
            val overlayColor = (alpha shl 24) or 0xffffff
            context.fill(x, y, x + width, y + height, overlayColor)
        }
    }
    
    private fun updateAnimation(delta: Float) {
        val targetProgress = if (isHovered) 1f else 0f
        val speed = delta * 0.1f
        
        animationProgress = if (targetProgress > animationProgress) {
            min(animationProgress + speed, targetProgress)
        } else {
            max(animationProgress - speed, targetProgress)
        }
    }
    
    private fun getBackgroundColor(): Int {
        return when {
            !isEnabled -> if (isPrimary) CinnamonTheme.buttonBackgroundDisabled else CinnamonTheme.buttonBackgroundDisabled
            isPressed -> if (isPrimary) CinnamonTheme.primaryButtonBackgroundPressed else CinnamonTheme.buttonBackgroundPressed
            isHovered -> if (isPrimary) CinnamonTheme.primaryButtonBackgroundHover else CinnamonTheme.buttonBackgroundHover
            else -> if (isPrimary) CinnamonTheme.primaryButtonBackground else CinnamonTheme.buttonBackground
        }
    }
    
    private fun getTextColor(): Int {
        return when {
            !isEnabled -> CinnamonTheme.disabledTextColor
            isPrimary -> CinnamonTheme.titleColor
            else -> CinnamonTheme.primaryTextColor
        }
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        // Simple rounded rectangle effect by drawing main rect and corner pixels
        context.fill(x + 1, y, x + width - 1, y + height, color)
        context.fill(x, y + 1, x + width, y + height - 1, color)
        
        // Corner pixels for rounded effect
        context.fill(x + 1, y + 1, x + 2, y + 2, color)
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color)
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color)
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color)
    }
    
    private fun drawBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        // Top and bottom borders
        context.fill(x + 1, y, x + width - 1, y + 1, color)
        context.fill(x + 1, y + height - 1, x + width - 1, y + height, color)
        
        // Left and right borders
        context.fill(x, y + 1, x + 1, y + height - 1, color)
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
    }
    
    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }
    
    fun onClick(mouseX: Double, mouseY: Double) {
        if (isEnabled) {
            isPressed = true
            onClick.invoke(mouseX, mouseY)
            // Reset pressed state after a short delay
            Thread {
                Thread.sleep(100)
                isPressed = false
            }.start()
        }
    }
    
    fun setHovered(hovered: Boolean) {
        isHovered = hovered
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    fun isEnabled(): Boolean = isEnabled
}