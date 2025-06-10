package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext

class CustomTitleScreenRenderer {

    fun render(context: DrawContext, width: Int, height: Int) {
        // Use the global background renderer
        NewGlobalShaderBackgroundRenderer.render(context, width, height)
    }
}