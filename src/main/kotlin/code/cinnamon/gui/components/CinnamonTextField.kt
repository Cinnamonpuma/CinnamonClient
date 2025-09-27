package code.cinnamon.gui.components

import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.gui.utils.GraphicsUtils
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class CinnamonTextField(
    textRenderer: TextRenderer,
    x: Int,
    y: Int,
    width: Int,
    height: Int
) : TextFieldWidget(textRenderer, x, y, width, height, Text.literal("")) {

    private val CORNER_RADIUS = 4f

    init {
        // We are drawing our own background, so disable the default one.
        this.setDrawsBackground(false)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 1. Draw our custom background and border.
        // The border color changes when the widget is focused.
        val borderColor = if (isFocused) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        GraphicsUtils.drawFilledRoundedRect(context, this.x.toFloat(), this.y.toFloat(), this.width.toFloat(), this.height.toFloat(), CORNER_RADIUS, CinnamonTheme.buttonBackground)
        GraphicsUtils.drawRoundedRectBorder(context, this.x.toFloat(), this.y.toFloat(), this.width.toFloat(), this.height.toFloat(), CORNER_RADIUS, borderColor)

        // 2. Apply the "offset trick" to render the text with padding.
        // We temporarily move the widget's position before calling the superclass's renderer.
        val originalX = this.x
        val originalY = this.y
        this.x += 4 // Horizontal padding
        this.y += (this.height - 8) / 2 // Vertical padding to center the text

        // 3. Call the vanilla renderer.
        // It will now draw the text, cursor, and selection highlight at the offset position,
        // effectively inside our custom background.
        super.renderWidget(context, mouseX, mouseY, delta)

        // 4. Revert the offset so the widget's actual position remains correct for other logic.
        this.x = originalX
        this.y = originalY
    }

    fun setChangedListener(listener: (String) -> Unit) {
        super.setChangedListener(listener)
    }
}