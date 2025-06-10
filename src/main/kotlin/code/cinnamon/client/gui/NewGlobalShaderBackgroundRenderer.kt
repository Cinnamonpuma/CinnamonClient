package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.MinecraftClient
import org.lwjgl.opengl.GL11

object NewGlobalShaderBackgroundRenderer {
    private val particleRenderer = InteractiveParticleBackground()
    private var initialized = false

    fun render(context: DrawContext, width: Int, height: Int) {
        // Fetch mouse coordinates for the particle system
        val client = MinecraftClient.getInstance()
        // Scale mouse coordinates if GUI scaling is in effect
        val mouseX = (client.mouse.x * width / client.window.width).toInt()
        val mouseY = (client.mouse.y * height / client.window.height).toInt()

        val prevBlend = GL11.glGetBoolean(GL11.GL_BLEND) // Save current state

        try {
            // Setup rendering state for proper alpha blending
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            
            // The particleRenderer handles its own initialization automatically
            particleRenderer.render(context, width, height, mouseX, mouseY)
            initialized = true // Mark as initialized after first successful render call

        } catch (e: Exception) {
            System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during render: ${e.message}")
            e.printStackTrace()
            // Could set initialized = false here if render fails catastrophically
        } finally {
            // Restore previous blend state
            if (!prevBlend) {
                GL11.glDisable(GL11.GL_BLEND)
            }
        }
    }

    fun reset() {
        try {
            // Force re-initialization by clearing particles
            // The particle system will auto-reinitialize on next render call
            particleRenderer.initParticles(0, 0) // This clears particles for re-init
            initialized = false
            println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Reset called, particle system state cleared.")
        } catch (e: Exception) {
            System.err.println("[Cinnamon/NewGlobalShaderBackgroundRenderer] Error during reset: ${e.message}")
            e.printStackTrace()
        }
    }

    // Legacy method for backward compatibility
    fun setParticleCount(count: Int) {
        particleRenderer.setParticleCount(count)
        // Force re-initialization to apply new counts immediately
        if (initialized) {
            particleRenderer.initParticles(0, 0) // Will be properly initialized on next render
        }
    }
    
    // New method for fine-grained control
    fun setParticleCounts(stars: Int, nebula: Int, brightStars: Int) {
        particleRenderer.setParticleCounts(stars, nebula, brightStars)
        // Force re-initialization to apply new counts immediately
        if (initialized) {
            particleRenderer.initParticles(0, 0) // Will be properly initialized on next render
        }
    }
    
    // Utility methods for common configurations
    fun setLowDensity() = setParticleCounts(60, 20, 8)
    fun setMediumDensity() = setParticleCounts(100, 40, 15) // Default
    fun setHighDensity() = setParticleCounts(150, 60, 25)
    fun setUltraDensity() = setParticleCounts(200, 80, 35)
}