package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.MinecraftClient // For logging, if needed

object NewGlobalShaderBackgroundRenderer {

    private val shaderRenderer = ShaderBackgroundRenderer()
    private var shaderInitialized = false 

    private var startTime: Long = 0L

    // Public property to check if the shader is ready (optional)
    val isShaderReady: Boolean
        get() = shaderInitialized && shaderRenderer.programId != 0

    fun render(context: DrawContext, width: Int, height: Int) {
        if (startTime == 0L) {
            startTime = System.currentTimeMillis()
        }

        if (!shaderInitialized) {
            try {
                // Assuming shaderRenderer.init() is safe to call multiple times
                // or that this block is truly only entered once.
                // For safety, ensure init logic within ShaderBackgroundRenderer is idempotent.
                shaderRenderer.init() 
                shaderInitialized = true // Set true only if init() doesn't throw
                if (shaderRenderer.programId != 0) {
                    MinecraftClient.getInstance().logger.info("[Cinnamon/NewGlobalShaderBackgroundRenderer] Initialized ShaderBackgroundRenderer successfully.")
                } else {
                    // If programId is 0 after init, it means init failed silently or was a no-op.
                    // Treat this as an initialization failure for rendering purposes.
                    shaderInitialized = false 
                    MinecraftClient.getInstance().logger.warn("[Cinnamon/NewGlobalShaderBackgroundRenderer] ShaderBackgroundRenderer.init() completed but programId is 0. Shader not ready.")
                }
            } catch (e: Exception) {
                MinecraftClient.getInstance().logger.error("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error initializing ShaderBackgroundRenderer: ${e.message}", e)
                shaderInitialized = false // Explicitly mark as not initialized on error
                return // Don't attempt to render if init fails
            }
        }
        
        if (!isShaderReady) {
            // This check now uses the public property, which encapsulates the conditions.
            // Avoid log spamming here; initial failure is logged above.
            return;
        }
        
        val timeSeconds = (System.currentTimeMillis() - startTime) / 1000.0f
        try {
            shaderRenderer.render(width, height, timeSeconds)
        } catch (e: Exception) {
            MinecraftClient.getInstance().logger.error("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during ShaderBackgroundRenderer.render: ${e.message}", e)
            // Consider setting shaderInitialized = false here to prevent further render calls
            // if the error is likely to be persistent.
            // For example: if (e is SomeNonRecoverableRenderingError) shaderInitialized = false
        }
    }

    fun reset() {
        try {
            shaderRenderer.cleanup()
            shaderInitialized = false // Mark as not initialized
            startTime = 0L 
            MinecraftClient.getInstance().logger.info("[Cinnamon/NewGlobalShaderBackgroundRenderer] Reset called, shader resources cleaned up.")
        } catch (e: Exception) {
             MinecraftClient.getInstance().logger.error("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during reset: ${e.message}", e)
        }
    }
}
