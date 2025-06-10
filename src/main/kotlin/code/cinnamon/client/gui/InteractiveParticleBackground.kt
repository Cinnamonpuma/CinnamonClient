package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import java.util.Random // Add Random for particle initialization
import kotlin.math.sqrt

class InteractiveParticleBackground {

    private class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float // Base size, actual drawing size might vary with sparkle
        // color: Int is removed as it's now coreColor/glowColor
    ) {
        var coreColor: Int = 0
        var glowColor: Int = 0
        var baseSize: Float = 0f
        var sparkleTimer: Int = 0
        var isSparkling: Boolean = false

        companion object {
            private val random = Random()
            private const val MAX_INITIAL_SPEED = 0.5f // Reduced for gentle drift
            private const val MIN_SIZE = 1.0f
            private const val MAX_SIZE = 3.0f // Slightly smaller max size for typical stars
            private val STAR_COLORS = intArrayOf(
                0xFFADD8E6.toInt(), // Light Blue
                0xFFAFEEEE.toInt(), // Pale Turquoise
                0xFFE0FFFF.toInt(), // Light Cyan
                0xFFFFFFFF.toInt(), // White
                0xFFB0E0E6.toInt(), // Powder Blue
                0xFF7FFFD4.toInt()  // Aquamarine (a bit greener, for variety)
            )
        }

        // Initializer block to set random starting state if desired,
        // or rely on reset being called. For now, constructor takes explicit values,
        // and reset will be used for randomization.

        fun reset(screenWidth: Int, screenHeight: Int) {
            this.x = random.nextFloat() * screenWidth
            this.y = random.nextFloat() * screenHeight
            this.vx = (random.nextFloat() * 2 * MAX_INITIAL_SPEED) - MAX_INITIAL_SPEED
            this.vy = (random.nextFloat() * 2 * MAX_INITIAL_SPEED) - MAX_INITIAL_SPEED

            // Add collective drift
            this.vx += 0.05f // All stars drift slightly to the right

            this.size = MIN_SIZE + random.nextFloat() * (MAX_SIZE - MIN_SIZE)
            
            this.coreColor = STAR_COLORS[random.nextInt(STAR_COLORS.size)]
            
            // Generate a glow color: usually a more transparent version of the core or a generic light color
            val alpha = 0x30 // Low alpha for glow
            val coreR = (coreColor shr 16) and 0xFF
            val coreG = (coreColor shr 8) and 0xFF
            val coreB = coreColor and 0xFF
            // Make glow color slightly whiter/brighter or use a generic glow
            val glowR = ((coreR + 255) / 2).coerceIn(0, 255)
            val glowG = ((coreG + 255) / 2).coerceIn(0, 255)
            val glowB = ((coreB + 255) / 2).coerceIn(0, 255)
            this.glowColor = (alpha shl 24) or (glowR shl 16) or (glowG shl 8) or glowB
            
            this.baseSize = this.size 
            this.isSparkling = false
            this.sparkleTimer = 0
        }

        fun update(mouseX: Int, mouseY: Int, screenWidth: Int, screenHeight: Int) {
            // Mouse Interaction
            /*
            val dxMouse = x - mouseX
            val dyMouse = y - mouseY
            val distMouseSq = dxMouse * dxMouse + dyMouse * dyMouse // Using squared distance to avoid sqrt
            val mouseRepelRadiusSq = 100f * 100f // Repel within 100 pixels

            if (distMouseSq < mouseRepelRadiusSq && distMouseSq > 0) { // distMouseSq > 0 to avoid issues if mouse is exactly on particle
                val distMouse = sqrt(distMouseSq)
                val repelForce = 0.2f * (1f - distMouse / 100f) // Force stronger when closer
                
                // Apply force normalized by distance (pushes away from mouse)
                vx += (dxMouse / distMouse) * repelForce
                vy += (dyMouse / distMouse) * repelForce
            }
            */

            // Movement & Friction
            x += vx
            y += vy

            vx *= 0.95f // Friction/drag
            vy *= 0.95f // Friction/drag

            // Boundary Checks (Screen Wrapping)
            val halfSize = size / 2 // Use halfSize for more accurate edge detection
            if (x - halfSize > screenWidth) {
                x = -halfSize // Wrap to left edge
            } else if (x + halfSize < 0) {
                x = screenWidth + halfSize // Wrap to right edge
            }

            if (y - halfSize > screenHeight) {
                y = -halfSize // Wrap to top edge
            } else if (y + halfSize < 0) {
                y = screenHeight + halfSize // Wrap to bottom edge
            }

            // Sparkle Logic
            if (isSparkling) {
                sparkleTimer--
                if (sparkleTimer <= 0) {
                    isSparkling = false
                    this.size = baseSize // Reset to base size after sparkling
                }
            } else {
                // Random chance to start sparkling
                if (random.nextFloat() < 0.001f) { // Reduced chance for less frequent sparkles
                    isSparkling = true
                    sparkleTimer = random.nextInt(5) + 5 // Sparkle for 5 to 9 frames
                    this.size = baseSize * (1.5f + random.nextFloat() * 0.5f) // Increase size by 1.5x to 2.0x
                    // Optionally, could also make coreColor temporarily brighter here
                }
            }
        }

        fun draw(context: DrawContext) {
            // Draw a simple square for the particle
            // Note: DrawContext usually takes Ints for coordinates.
            val currentSize = this.size // Potentially modified by sparkle effect later
            val glowSize = currentSize * 2.0f // Glow is larger

            // Draw glow layer first
            context.fill(
                (x - glowSize / 2).toInt(),
                (y - glowSize / 2).toInt(),
                (x + glowSize / 2).toInt(),
                (y + glowSize / 2).toInt(),
                glowColor 
            )

            // Draw core star on top
            context.fill(
                (x - currentSize / 2).toInt(),
                (y - currentSize / 2).toInt(),
                (x + currentSize / 2).toInt(),
                (y + currentSize / 2).toInt(),
                coreColor
            )
        }
    }

    private val particles: MutableList<Particle> = mutableListOf()
    private var particleCount: Int = 150 // Default particle count

    // To keep track of screen dimensions for re-initialization
    private var currentScreenWidth: Int = 0
    private var currentScreenHeight: Int = 0

    fun initParticles(screenWidth: Int, screenHeight: Int) {
        currentScreenWidth = screenWidth
        currentScreenHeight = screenHeight
        particles.clear()
        for (i in 0 until particleCount) {
            val particle = Particle(0f,0f,0f,0f,0f) // New, color removed
            particle.reset(screenWidth, screenHeight)
            particles.add(particle)
        }
    }

    // Placeholder for the main render method
    fun render(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int) {
        // Initialize or re-initialize particles if screen size changed or first run
        if (particles.isEmpty() || width != currentScreenWidth || height != currentScreenHeight) {
            initParticles(width, height)
        }

        // Optional: Draw a solid background (dark gray)
        // Ensure an alpha component is included if you want it to be opaque, e.g., 0xFF for the first component.
        context.fill(0, 0, width, height, 0xFF02030A.toInt()) 

        // Update and draw each particle
        for (particle in particles) {
            particle.update(mouseX, mouseY, width, height)
            particle.draw(context)
        }
    }

    // Allow setting particle count externally if desired
    fun setParticleCount(count: Int) {
        if (count > 0) {
            particleCount = count
            // Optionally, re-initialize particles if count changes while running
            // For now, it will take effect on next explicit init or screen resize
        }
    }
}