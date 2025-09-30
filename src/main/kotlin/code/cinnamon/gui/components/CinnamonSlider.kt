package code.cinnamon.gui.components

import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.gui.utils.GraphicsUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.text.Text
import kotlin.math.round

class CinnamonSlider(
    private var x: Int,
    private var y: Int,
    private var width: Int,
    private var height: Int,
    private var value: Double,
    private val min: Double,
    private val max: Double,
    private val step: Double,
    private val onValueChanged: (Double) -> Unit
) : Element, Drawable, Selectable {

    private var isHovered = false
    private var isDragging = false
    private val client = MinecraftClient.getInstance()

    fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun setValue(newValue: Double) {
        this.value = newValue.coerceIn(min, max)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        isHovered = isMouseOver(mouseX.toDouble(), mouseY.toDouble())

        if (isDragging) {
            val relativeMouseX = mouseX - x
            val percentage = (relativeMouseX.toDouble() / width).coerceIn(0.0, 1.0)
            val newValue = min + percentage * (max - min)
            val steppedValue = round(newValue / step) * step
            if (steppedValue != value) {
                value = steppedValue.coerceIn(min, max)
                onValueChanged(value)
            }
        }

        // Draw slider track
        GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), (y + height / 2 - 2).toFloat(), width.toFloat(), 4f, 2f, CinnamonTheme.buttonBackground)

        // Draw slider progress
        val progressWidth = ((value - min) / (max - min) * width).toFloat()
        GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), (y + height / 2 - 2).toFloat(), progressWidth, 4f, 2f, CinnamonTheme.accentColor)

        // Draw slider handle
        val handleX = (x + progressWidth - 4).toInt()
        val handleY = y + height / 2 - 4
        GraphicsUtils.drawFilledRoundedRect(context, handleX.toFloat(), handleY.toFloat(), 8f, 8f, 3f, if (isHovered || isDragging) CinnamonTheme.accentColorHover else CinnamonTheme.accentColor)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            isDragging = true
            return true
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            isDragging = false
        }
        return false
    }

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }

    override fun setFocused(focused: Boolean) {}
    override fun isFocused(): Boolean = false
    override fun getType(): Selectable.SelectionType = if (isHovered) Selectable.SelectionType.HOVERED else Selectable.SelectionType.NONE
    override fun appendNarrations(builder: NarrationMessageBuilder) {}
}