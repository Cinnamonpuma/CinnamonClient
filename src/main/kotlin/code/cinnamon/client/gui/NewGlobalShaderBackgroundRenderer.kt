package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.MinecraftClient
import org.lwjgl.opengl.GL11

object NewGlobalShaderBackgroundRenderer {
    private val particleRenderer = InteractiveParticleBackground()
    private var initialized = false
    // private var startTime: Long = 0L // No longer strictly needed for particleRenderer's render method

    fun render(context: DrawContext, width: Int, height: Int) {
        // if (startTime == 0L) { // Not essential for particle system
        //     startTime = System.currentTimeMillis()
        // }

        // The particleRenderer handles its own internal initialization (initParticles)
        // based on screen size changes within its render method.
        // We just need to ensure it's called.

        // Fetch mouse coordinates for the particle system
        val client = MinecraftClient.getInstance()
        // It's good practice to scale mouse coordinates if GUI scaling is in effect
        val mouseX = (client.mouse.x * width / client.window.width).toInt()
        val mouseY = (client.mouse.y * height / client.window.height).toInt()
        
        // val timeSeconds = (System.currentTimeMillis() - startTime) / 1000.0f // Not used by particleRenderer.render

        val prevBlend = GL11.glGetBoolean(GL11.GL_BLEND) // Save current state

        try {
            // Setup rendering state (optional, can be kept if desired)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            
            particleRenderer.render(context, width, height, mouseX, mouseY)
            initialized = true // Mark as initialized after first successful render call

        } catch (e: Exception) {
            System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during render: ${e.message}")
            e.printStackTrace()
            // Potentially set initialized = false here if render fails catastrophically
        } finally {
            // Restore previous state
            if (!prevBlend) {
                GL11.glDisable(GL11.GL_BLEND)
            }
        }
    }

    fun reset() {
        try {
            // Re-initialize particles with dummy values, will be properly set on next render
            // or pass current screen dimensions if readily available and makes sense.
            // Forcing re-init by clearing:
            particleRenderer.initParticles(0,0) // This effectively clears particles for re-init on next render.
            initialized = false
            // startTime = 0L // Reset if it were used
            println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Reset called, particle system state cleared.")
        } catch (e: Exception) {
            System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during reset: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // Optional: Add a method to configure particle count if desired
    fun setParticleCount(count: Int) {
        particleRenderer.setParticleCount(count)
        // To make the change immediate, you might need to call particleRenderer.initParticles again
        // with current dimensions, if they are known here. Otherwise, it takes effect on next auto-init.
        // For simplicity, current behavior of InteractiveParticleBackground is that count change
        // is picked up by its internal initParticles call.
    }
}