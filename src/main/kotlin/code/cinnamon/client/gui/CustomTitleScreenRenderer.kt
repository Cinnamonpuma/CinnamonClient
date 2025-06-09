package code.cinnamon.client.gui

import code.cinnamon.client.particle.Particle
import net.minecraft.client.gui.DrawContext
import kotlin.random.Random

class CustomTitleScreenRenderer {

    companion object {
        private val random = Random(System.currentTimeMillis())
        private const val PARTICLE_COUNT = 100
    }

    private val particles = Array(PARTICLE_COUNT) { Particle(0, 0, random) }
    private var lastWidth: Int = -1
    private var lastHeight: Int = -1

    fun render(context: DrawContext, width: Int, height: Int) {
        if (width != lastWidth || height != lastHeight) {
            regenerateParticles(width, height)
            lastWidth = width
            lastHeight = height
        }

        // Draw a gradient background
        // 0x801A237E is ARGB, alpha = 0x80 (128), R=0x1A, G=0x23, B=0x7E
        // 0x80882DBD is ARGB, alpha = 0x80 (128), R=0x88, G=0x2D, B=0xBD
        context.fillGradient(0, 0, width, height, 0x801A237E.toInt(), 0x80882DBD.toInt())

        for (particle in particles) {
            particle.update(width, height)
            // Draw each particle as a white square
            // 0x80FFFFFF is ARGB, alpha = 0x80 (128), R=0xFF, G=0xFF, B=0xFF
            context.fill(particle.x, particle.y, particle.x + 2, particle.y + 2, 0x80FFFFFF.toInt())
        }
    }

    private fun regenerateParticles(width: Int, height: Int) {
        for (particle in particles) {
            particle.reset(width, height)
        }
    }
}
