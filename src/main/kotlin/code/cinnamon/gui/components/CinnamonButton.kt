package code.cinnamon.gui.components

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.text.Text
import code.cinnamon.gui.theme.CinnamonTheme
import kotlin.math.max
import kotlin.math.min

class CinnamonButton(
    private var x: Int,
    private var y: Int,
    private var width: Int,
    private var height: Int,
    var text: Text,
    val onClick: (mouseX: Double, mouseY: Double) -> Unit,
    var isPrimary: Boolean = false
) : Element, Drawable, Selectable {

    private var isHovered = false
    private var isPressed = false
    private var isEnabled = true
    private var animationProgress = 0f
    private val client = MinecraftClient.getInstance()

    fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun setHovered(hovered: Boolean) {
        isHovered = hovered
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun isEnabled(): Boolean = isEnabled

    // Implement Element interface required methods:
    override fun setFocused(focused: Boolean) { /* no-op */ }
    override fun isFocused(): Boolean = false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateAnimation(delta)

        // Update hover state based on current mouse position
        isHovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())

        context.matrices.pushMatrix()
        context.matrices.translate(x.toFloat(), y.toFloat(), context.matrices)

        val backgroundColor = getBackgroundColor()
        val textColor = getTextColor()

        drawRoundedRect(context, 0, 0, width, height, backgroundColor)

        val borderColor = if (isHovered || isPressed) {
            CinnamonTheme.buttonOutlineHoverColor
        } else {
            CinnamonTheme.buttonOutlineColor
        }

        drawBorder(context, 0, 0, width, height, borderColor)

        val textWidth = client.textRenderer.getWidth(text)
        val textX = (width - textWidth) / 2
        val textY = (height - client.textRenderer.fontHeight) / 2

        context.drawText(
            client.textRenderer,
            text,
            textX,
            textY,
            textColor,
            CinnamonTheme.enableTextShadow
        )

        if (isHovered && isEnabled) {
            val alpha = (animationProgress * 0.1f * 255).toInt().coerceIn(0, 255)
            val overlayColor = (alpha shl 24) or 0xFFFFFF
            context.fill(0, 0, width, height, overlayColor)
        }

        context.matrices.popMatrix()
    }

    private fun updateAnimation(delta: Float) {
        val target = if (isHovered) 1f else 0f
        val speed = delta * 0.2f
        animationProgress = if (target > animationProgress)
            min(animationProgress + speed, target)
        else
            max(animationProgress - speed, target)
    }

    private fun getBackgroundColor(): Int {
        return when {
            !isEnabled -> CinnamonTheme.buttonBackgroundDisabled
            isPrimary -> CinnamonTheme.primaryButtonBackground
            else -> CinnamonTheme.buttonBackground
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
        context.fill(x + 1, y, x + width - 1, y + height, color)
        context.fill(x, y + 1, x + width, y + height - 1, color)
    }

    private fun drawBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        context.fill(x + 1, y, x + width - 1, y + 1, color)
        context.fill(x + 1, y + height - 1, x + width - 1, y + height, color)
        context.fill(x, y + 1, x + 1, y + height - 1, color)
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isMouseOver(mouseX, mouseY) && isEnabled && button == 0) {
            isPressed = true
            onClick(mouseX, mouseY)
            isPressed = false
            return true
        }
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        isHovered = isMouseOver(mouseX, mouseY)
    }

    override fun getType(): Selectable.SelectionType {
        return if (isHovered && isEnabled) Selectable.SelectionType.HOVERED else Selectable.SelectionType.NONE
    }

    override fun appendNarrations(builder: net.minecraft.client.gui.screen.narration.NarrationMessageBuilder) {
        try {
            val method = builder.javaClass.getMethod(
                "put",
                net.minecraft.client.gui.screen.narration.NarrationPart::class.java,
                Text::class.java
            )
            method.invoke(builder, net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, text)
        } catch (_: Exception) {
        }
    }
}