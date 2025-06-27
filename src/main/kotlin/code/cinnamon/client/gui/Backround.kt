package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier

class Background {
    private val backgroundImage = Identifier.of("cinnamon", "textures/gui/Backround.png")
    
    fun render(context: DrawContext, width: Int, height: Int) {
        context.drawTexture(
            { id -> RenderLayer.getGuiTextured(id) },
            backgroundImage,
            0, 0,
            0f, 0f,
            width, height,
            width, height
        )
    }
}