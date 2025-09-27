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
        this.setDrawsBackground(false)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        GraphicsUtils.drawFilledRoundedRect(context, this.x.toFloat(), this.y.toFloat(), this.width.toFloat(), this.height.toFloat(), CORNER_RADIUS, CinnamonTheme.buttonBackground)
        GraphicsUtils.drawRoundedRectBorder(context, this.x.toFloat(), this.y.toFloat(), this.width.toFloat(), this.height.toFloat(), CORNER_RADIUS, CinnamonTheme.borderColor)
        this.x += 4
        this.y += (this.height - 8) / 2
        super.renderWidget(context, mouseX, mouseY, delta)
        this.x -= 4
        this.y -= (this.height - 8) / 2
    }

    fun setChangedListener(listener: (String) -> Unit) {
        super.setChangedListener(listener)
    }
}