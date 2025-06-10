package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.MinecraftClient
import org.lwjgl.opengl.GL11

object NewGlobalShaderBackgroundRenderer {
    private val shaderRenderer = ShaderBackgroundRenderer()
    private var shaderInitialized = false
    private var startTime: Long = 0L

    // Public property to check if the shader is ready
    val isShaderReady: Boolean
        get() = shaderInitialized && shaderRenderer.programId != 0

    fun render(context: DrawContext, width: Int, height: Int) {
        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
        }

        if (!shaderInitialized) {
            try {
                shaderRenderer.init()
                shaderInitialized = true
                if (shaderRenderer.programId != 0) {
                    println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Initialized ShaderBackgroundRenderer successfully.")
                } else {
                    shaderInitialized = false
                    System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] ShaderBackgroundRenderer.init() completed but programId is 0. Shader not ready.")
                }
            } catch (e: Exception) {
                System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error initializing ShaderBackgroundRenderer: ${e.message}")
                e.printStackTrace()
                shaderInitialized = false
                return
            }
        }

        if (!isShaderReady) {
            return
        }

        val timeSeconds = (System.currentTimeMillis() - startTime) / 1000.0f
        
        // Save current state
        val prevBlend = GL11.glGetBoolean(GL11.GL_BLEND)

        try {
            // Setup rendering state
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            
            shaderRenderer.render(context, width, height, timeSeconds)

        } catch (e: Exception) {
            System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during render: ${e.message}")
            e.printStackTrace()
        } finally {
            // Restore previous state
            if (!prevBlend) {
                GL11.glDisable(GL11.GL_BLEND)
            }
        }
    }

    fun reset() {
        try {
            shaderRenderer.cleanup()
            shaderInitialized = false
            startTime = 0L
            println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Reset called, shader resources cleaned up.")
        } catch (e: Exception) {
            System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during reset: ${e.message}")
            e.printStackTrace()
        }
    }
}