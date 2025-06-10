package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import java.util.Random
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

class InteractiveParticleBackground {

    private class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var type: ParticleType = ParticleType.STAR
    ) {
        var coreColor: Int = 0
        var glowColor: Int = 0
        var baseSize: Float = 0f
        var sparkleTimer: Int = 0
        var isSparkling: Boolean = false
        var brightness: Float = 1.0f
        var twinklePhase: Float = 0f

        enum class ParticleType {
            STAR,           // Small bright points
            NEBULA_DUST,    // Larger, more diffuse particles
            BRIGHT_STAR     // Larger, brighter stars
        }

        companion object {
            private val random = Random()
            private const val MAX_INITIAL_SPEED = 0.3f
            
            // Star colors - cooler blues and whites for space feel
            private val STAR_COLORS = intArrayOf(
                0xFFFFFFFF.toInt(), // Pure White
                0xFFE6F3FF.toInt(), // Very Light Blue
                0xFFCCE7FF.toInt(), // Light Blue
                0xFFB3DBFF.toInt(), // Soft Blue
                0xFFF0F8FF.toInt(), // Alice Blue
                0xFFE0FFFF.toInt(), // Light Cyan
                0xFFADD8E6.toInt()  // Light Blue
            )
            
            // Nebula colors - deeper blues and purples
            private val NEBULA_COLORS = intArrayOf(
                0xFF1E3A8A.toInt(), // Deep Blue
                0xFF1E40AF.toInt(), // Blue
                0xFF3730A3.toInt(), // Indigo
                0xFF4338CA.toInt(), // Violet
                0xFF0F172A.toInt(), // Very Dark Blue
                0xFF1E293B.toInt()  // Dark Slate
            )
        }

        fun reset(screenWidth: Int, screenHeight: Int) {
            this.x = random.nextFloat() * screenWidth
            this.y = random.nextFloat() * screenHeight
            
            // Very slow drift for cosmic feel
            this.vx = (random.nextFloat() * 2 * MAX_INITIAL_SPEED) - MAX_INITIAL_SPEED
            this.vy = (random.nextFloat() * 2 * MAX_INITIAL_SPEED) - MAX_INITIAL_SPEED
            
            // Add subtle collective drift
            this.vx += 0.02f
            this.vy += 0.01f
            
            // Set particle type and properties based on type
            when (type) {
                ParticleType.STAR -> {
                    this.size = 0.5f + random.nextFloat() * 1.5f
                    this.coreColor = STAR_COLORS[random.nextInt(STAR_COLORS.size)]
                    this.brightness = 0.7f + random.nextFloat() * 0.3f
                }
                ParticleType.NEBULA_DUST -> {
                    this.size = 2.0f + random.nextFloat() * 4.0f
                    this.coreColor = NEBULA_COLORS[random.nextInt(NEBULA_COLORS.size)]
                    this.brightness = 0.15f + random.nextFloat() * 0.25f
                }
                ParticleType.BRIGHT_STAR -> {
                    this.size = 1.5f + random.nextFloat() * 2.5f
                    this.coreColor = STAR_COLORS[random.nextInt(STAR_COLORS.size)]
                    this.brightness = 0.8f + random.nextFloat() * 0.2f
                }
            }
            
            // Generate glow color with appropriate alpha for type
            val alpha = when (type) {
                ParticleType.STAR -> 0x40
                ParticleType.NEBULA_DUST -> 0x20
                ParticleType.BRIGHT_STAR -> 0x60
            }
            
            val coreR = (coreColor shr 16) and 0xFF
            val coreG = (coreColor shr 8) and 0xFF
            val coreB = coreColor and 0xFF
            
            val glowR = ((coreR + 255) / 2).coerceIn(0, 255)
            val glowG = ((coreG + 255) / 2).coerceIn(0, 255)
            val glowB = ((coreB + 255) / 2).coerceIn(0, 255)
            
            this.glowColor = (alpha shl 24) or (glowR shl 16) or (glowG shl 8) or glowB
            
            this.baseSize = this.size
            this.isSparkling = false
            this.sparkleTimer = 0
            this.twinklePhase = random.nextFloat() * 2 * PI.toFloat()
        }

        fun update(mouseX: Int, mouseY: Int, screenWidth: Int, screenHeight: Int) {
            // Movement with very gentle physics
            x += vx
            y += vy

            // Gentle friction for smooth movement
            vx *= 0.98f
            vy *= 0.98f

            // Screen wrapping
            val margin = size
            if (x - margin > screenWidth) {
                x = -margin
            } else if (x + margin < 0) {
                x = screenWidth + margin
            }

            if (y - margin > screenHeight) {
                y = -margin
            } else if (y + margin < 0) {
                y = screenHeight + margin
            }

            // Twinkling effect for stars
            if (type == ParticleType.STAR || type == ParticleType.BRIGHT_STAR) {
                twinklePhase += 0.05f + random.nextFloat() * 0.03f
                brightness = 0.6f + 0.4f * (sin(twinklePhase) * 0.5f + 0.5f)
            }

            // Sparkle effect (less frequent for cosmic feel)
            if (isSparkling) {
                sparkleTimer--
                if (sparkleTimer <= 0) {
                    isSparkling = false
                    this.size = baseSize
                }
            } else {
                if (random.nextFloat() < 0.0005f && type != ParticleType.NEBULA_DUST) {
                    isSparkling = true
                    sparkleTimer = random.nextInt(8) + 5
                    this.size = baseSize * (1.3f + random.nextFloat() * 0.4f)
                }
            }
        }

        fun draw(context: DrawContext) {
            val currentSize = this.size * brightness
            
            when (type) {
                ParticleType.STAR -> {
                    // Draw cross-shaped star
                    drawStar(context, currentSize)
                }
                ParticleType.NEBULA_DUST -> {
                    // Draw soft circular nebula particle
                    drawNebulaParticle(context, currentSize)
                }
                ParticleType.BRIGHT_STAR -> {
                    // Draw larger star with more pronounced glow
                    drawBrightStar(context, currentSize)
                }
            }
        }
        
        private fun drawStar(context: DrawContext, currentSize: Float) {
            val glowSize = currentSize * 3.0f
            
            // Draw glow
            context.fill(
                (x - glowSize / 2).toInt(),
                (y - glowSize / 2).toInt(),
                (x + glowSize / 2).toInt(),
                (y + glowSize / 2).toInt(),
                applyBrightness(glowColor, brightness * 0.3f)
            )
            
            // Draw core point
            context.fill(
                (x - currentSize / 2).toInt(),
                (y - currentSize / 2).toInt(),
                (x + currentSize / 2).toInt(),
                (y + currentSize / 2).toInt(),
                applyBrightness(coreColor, brightness)
            )
        }
        
        private fun drawNebulaParticle(context: DrawContext, currentSize: Float) {
            val glowSize = currentSize * 2.5f
            
            // Draw multiple layers for nebula effect
            context.fill(
                (x - glowSize / 2).toInt(),
                (y - glowSize / 2).toInt(),
                (x + glowSize / 2).toInt(),
                (y + glowSize / 2).toInt(),
                applyBrightness(glowColor, brightness * 0.4f)
            )
            
            context.fill(
                (x - currentSize / 2).toInt(),
                (y - currentSize / 2).toInt(),
                (x + currentSize / 2).toInt(),
                (y + currentSize / 2).toInt(),
                applyBrightness(coreColor, brightness * 0.6f)
            )
        }
        
        private fun drawBrightStar(context: DrawContext, currentSize: Float) {
            val glowSize = currentSize * 4.0f
            val mediumGlow = currentSize * 2.5f
            
            // Multiple glow layers for bright stars
            context.fill(
                (x - glowSize / 2).toInt(),
                (y - glowSize / 2).toInt(),
                (x + glowSize / 2).toInt(),
                (y + glowSize / 2).toInt(),
                applyBrightness(glowColor, brightness * 0.2f)
            )
            
            context.fill(
                (x - mediumGlow / 2).toInt(),
                (y - mediumGlow / 2).toInt(),
                (x + mediumGlow / 2).toInt(),
                (y + mediumGlow / 2).toInt(),
                applyBrightness(glowColor, brightness * 0.4f)
            )
            
            // Bright core
            context.fill(
                (x - currentSize / 2).toInt(),
                (y - currentSize / 2).toInt(),
                (x + currentSize / 2).toInt(),
                (y + currentSize / 2).toInt(),
                applyBrightness(coreColor, brightness)
            )
        }
        
        private fun applyBrightness(color: Int, brightnessFactor: Float): Int {
            val alpha = (color shr 24) and 0xFF
            val r = ((color shr 16) and 0xFF)
            val g = ((color shr 8) and 0xFF)
            val b = (color and 0xFF)
            
            val newAlpha = (alpha * brightnessFactor).toInt().coerceIn(0, 255)
            return (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    private val particles: MutableList<Particle> = mutableListOf()
    private var starCount: Int = 100
    private var nebulaCount: Int = 40
    private var brightStarCount: Int = 15

    private var currentScreenWidth: Int = 0
    private var currentScreenHeight: Int = 0

    fun initParticles(screenWidth: Int, screenHeight: Int) {
        currentScreenWidth = screenWidth
        currentScreenHeight = screenHeight
        particles.clear()
        
        // Add nebula dust particles (background layer)
        for (i in 0 until nebulaCount) {
            val particle = Particle(0f, 0f, 0f, 0f, 0f, Particle.ParticleType.NEBULA_DUST)
            particle.reset(screenWidth, screenHeight)
            particles.add(particle)
        }
        
        // Add regular stars
        for (i in 0 until starCount) {
            val particle = Particle(0f, 0f, 0f, 0f, 0f, Particle.ParticleType.STAR)
            particle.reset(screenWidth, screenHeight)
            particles.add(particle)
        }
        
        // Add bright stars (foreground layer)
        for (i in 0 until brightStarCount) {
            val particle = Particle(0f, 0f, 0f, 0f, 0f, Particle.ParticleType.BRIGHT_STAR)
            particle.reset(screenWidth, screenHeight)
            particles.add(particle)
        }
    }

    fun render(context: DrawContext, width: Int, height: Int, mouseX: Int, mouseY: Int) {
        // Initialize particles if needed
        if (particles.isEmpty() || width != currentScreenWidth || height != currentScreenHeight) {
            initParticles(width, height)
        }

        // Draw deep space background - very dark with slight blue tint
        context.fill(0, 0, width, height, 0xFF0A0E1A.toInt())
        
        // Add subtle gradient overlay for depth (optional)
        val gradientHeight = height / 4
        for (i in 0 until gradientHeight) {
            val alpha = (0x15 * (1.0f - i.toFloat() / gradientHeight)).toInt()
            val gradientColor = (alpha shl 24) or 0x1E3A8A
            context.fill(0, i, width, i + 1, gradientColor)
        }

        // Update and draw particles in layers
        for (particle in particles) {
            particle.update(mouseX, mouseY, width, height)
            particle.draw(context)
        }
    }

    fun setParticleCounts(stars: Int, nebula: Int, brightStars: Int) {
        if (stars > 0) starCount = stars
        if (nebula > 0) nebulaCount = nebula
        if (brightStars > 0) brightStarCount = brightStars
    }
    
    // Legacy compatibility method for existing renderer
    fun setParticleCount(count: Int) {
        // Distribute total count across particle types
        val totalParticles = count
        starCount = (totalParticles * 0.65f).toInt()  // 65% regular stars
        nebulaCount = (totalParticles * 0.25f).toInt() // 25% nebula dust
        brightStarCount = (totalParticles * 0.10f).toInt() // 10% bright stars
    }
}