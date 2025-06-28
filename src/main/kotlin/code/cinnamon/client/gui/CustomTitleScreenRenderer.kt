package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import org.lwjgl.opengl.GL11

class CustomTitleScreenRenderer {
    private val backgroundRenderer = Background()

    fun render(context: DrawContext, width: Int, height: Int) {
        val prevBlend = GL11.glGetBoolean(GL11.GL_BLEND)

        try {
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            backgroundRenderer.render(context, width, height)
        } catch (e: Exception) {
            System.err.println("[Cinnamon/CustomTitleScreenRenderer] Error during render: ${e.message}")
            e.printStackTrace()
        } finally {
            if (!prevBlend) {
                GL11.glDisable(GL11.GL_BLEND)
            }
        }
    }
}
