package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext

class CustomTitleScreenRenderer {
    fun render(context: DrawContext, width: Int, height: Int) {
        NewGlobalShaderBackgroundRenderer.render(context, width, height)
    }
}
