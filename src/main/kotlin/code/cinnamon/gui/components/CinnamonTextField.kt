package code.cinnamon.gui.components

import code.cinnamon.gui.theme.CinnamonTheme
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

    init {
        this.setDrawsBackground(false)
    }

    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(this.x, this.y, this.x + this.width, this.y + this.height, CinnamonTheme.buttonBackground)
        context.drawBorder(this.x, this.y, this.width, this.height, CinnamonTheme.borderColor)
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
