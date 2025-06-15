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
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    var text: Text,
    val onClick: (mouseX: Double, mouseY: Double) -> Unit,
    var isPrimary: Boolean = false
) : Element, Drawable, Selectable {
    private var _x: Int = x
    private var _y: Int = y
    private var _width: Int = width
    private var _height: Int = height

    private var isHovered = false
    private var isPressed = false
    private var isEnabled = true
    private var animationProgress = 0f
    private val client = MinecraftClient.getInstance()

    fun getX(): Int = _x
    fun setX(x: Int) { _x = x }
    fun getY(): Int = _y
    fun setY(y: Int) { _y = y }
    fun getWidth(): Int = _width
    fun setWidth(width: Int) { _width = width }
    fun getHeight(): Int = _height
    fun setHeight(height: Int) { _height = height }

    override fun setFocused(focused: Boolean) { /* No-op for now */ }
    override fun isFocused(): Boolean = false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= _x && mouseX < _x + _width && mouseY >= _y && mouseY < _y + _height
    }


    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        updateAnimation(delta)

        val backgroundColor = getBackgroundColor()
        val textColor = getTextColor()


        drawRoundedRect(context, _x, _y, _width, _height, backgroundColor)


        if (isHovered || isPressed) {
            drawBorder(context, _x, _y, _width, _height, CinnamonTheme.accentColor)
        }

        val textWidth = client.textRenderer.getWidth(text)
        val textX = _x + (_width - textWidth) / 2
        val textY = _y + (_height - client.textRenderer.fontHeight) / 2

        context.drawText(
            client.textRenderer,
            text,
            textX,
            textY,
            textColor,
            true
        )


        if (isHovered && isEnabled) {
            val alpha = (animationProgress * 0.1f).toInt()
            val overlayColor = (alpha shl 24) or 0xffffff
            context.fill(_x, _y, _x + _width, _y + _height, overlayColor)
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
        context.fill(x + 1, y, x + width - 1, y + height, color)
        context.fill(x, y + 1, x + width, y + height - 1, color)

        context.fill(x + 1, y + 1, x + 2, y + 2, color)
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color)
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color)
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color)
    }

    private fun drawBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {

        context.fill(x + 1, y, x + width - 1, y + 1, color)
        context.fill(x + 1, y + height - 1, x + width - 1, y + height, color)


        context.fill(x, y + 1, x + 1, y + height - 1, color)
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
    }


    fun handleOnClick(mouseX: Double, mouseY: Double) {
        if (isEnabled) {
            isPressed = true
            onClick.invoke(mouseX, mouseY)
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

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        setHovered(isMouseOver(mouseX, mouseY))
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isMouseOver(mouseX, mouseY) && isEnabled && button == 0) {
            handleOnClick(mouseX, mouseY)
            return true
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return false
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        return false
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        return false
    }


    override fun getType(): Selectable.SelectionType {
        return if (isHovered && isEnabled) Selectable.SelectionType.HOVERED else Selectable.SelectionType.NONE
    }


    override fun appendNarrations(builder: net.minecraft.client.gui.screen.narration.NarrationMessageBuilder) {
        try {

            val titleMethod = builder.javaClass.getMethod("put", net.minecraft.client.gui.screen.narration.NarrationPart::class.java, net.minecraft.text.Text::class.java)
            titleMethod.invoke(builder, net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, text)
        } catch (e: Exception) {
        }
    }
}