package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import net.minecraft.client.gl.RenderPipelines

class Background {
    private val backgroundImage = Identifier.of("cinnamon", "textures/gui/background.png")

    fun render(context: DrawContext, width: Int, height: Int) {
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            backgroundImage,
            0, // x position
            0, // y position
            0f, // u (texture x)
            0f, // v (texture y)
            width, // width
            height, // height
            width, // texture width
            height // texture height
        )
    }
}