package code.cinnamon.client.wave

import net.minecraft.client.gui.DrawContext
import kotlin.math.*
import kotlin.random.Random

class WaveSystem {
    
    companion object {
        private const val WAVE_COUNT = 8
        private const val TIME_SCALE = 0.025f
        private const val FOAM_PARTICLES = 80
    }
    
    private val waves = Array(WAVE_COUNT) { i -> 
        OceanWave(
            amplitude = 12f + i * 6f,
            frequency = 0.004f + i * 0.001f,
            speed = 0.5f + i * 0.2f,
            phase = i * PI.toFloat() / 3f,
            depth = i.toFloat() / WAVE_COUNT
        )
    }
    
    private val foamParticles = Array(FOAM_PARTICLES) { 
        FoamParticle(
            x = Random.nextFloat() * 800f,
            y = Random.nextFloat() * 200f + 300f,
            life = Random.nextFloat()
        )
    }
    
    private var time: Float = 0f
    private var screenWidth: Int = 800
    private var screenHeight: Int = 600
    
    fun resize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }
    
    fun update() {
        time += TIME_SCALE
        
        waves.forEach { wave ->
            wave.update(time)
        }
        
        foamParticles.forEach { particle ->
            particle.update(time, screenWidth, screenHeight)
            if (particle.shouldReset(screenWidth)) {
                particle.reset(screenWidth, screenHeight)
            }
        }
    }
    
    fun render(context: DrawContext, width: Int, height: Int) {
        renderSunsetSky(context, width, height)
        renderSun(context, width, height)
        renderOcean(context, width, height)
        renderFoam(context, width, height)
        renderSunReflection(context, width, height)
    }
    
    private fun renderSunsetSky(context: DrawContext, width: Int, height: Int) {
        val skyHeight = height * 0.7f
        val segments = 40
        
        for (i in 0 until segments) {
            val progress = i.toFloat() / segments
            val y = (skyHeight * progress).toInt()
            val nextY = (skyHeight * (i + 1f) / segments).toInt()
            
            val skyColor = when {
                progress < 0.3f -> interpolateColor(0xFF1a1a2e.toInt(), 0xFF16213e.toInt(), progress / 0.3f) // Deep night to dark blue
                progress < 0.6f -> interpolateColor(0xFF16213e.toInt(), 0xFF0f3460.toInt(), (progress - 0.3f) / 0.3f) // Dark blue to blue
                progress < 0.8f -> interpolateColor(0xFF0f3460.toInt(), 0xFF533483.toInt(), (progress - 0.6f) / 0.2f) // Blue to purple
                else -> interpolateColor(0xFF533483.toInt(), 0xFF8B5A3C.toInt(), (progress - 0.8f) / 0.2f) // Purple to orange
            }
            
            context.fill(0, y, width, nextY, skyColor)
        }
        
        // Horizon glow
        val horizonY = skyHeight.toInt()
        for (i in 0..20) {
            val alpha = (60 - i * 2).coerceAtLeast(0)
            val glowColor = (alpha shl 24) or 0xFF6B4E3D.toInt()
            context.fill(0, horizonY - i, width, horizonY - i + 1, glowColor)
        }
    }
    
    private fun renderSun(context: DrawContext, width: Int, height: Int) {
        val sunX = width * 0.75f
        val sunY = height * 0.25f
        val sunRadius = 40f
        
        // Sun glow layers
        for (ring in 0..8) {
            val radius = sunRadius + ring * 8f
            val alpha = (80 - ring * 8).coerceAtLeast(10)
            val glowColor = when {
                ring < 3 -> (alpha shl 24) or 0xFFD700.toInt() // Golden center
                ring < 6 -> (alpha shl 24) or 0xFF8B4513.toInt() // Orange middle
                else -> (alpha shl 24) or 0xFF4A4A4A.toInt() // Soft outer glow
            }
            
            drawCircle(context, sunX.toInt(), sunY.toInt(), radius.toInt(), glowColor)
        }
        
        // Sun core
        drawCircle(context, sunX.toInt(), sunY.toInt(), sunRadius.toInt(), 0xFFFFD700.toInt())
    }
    
    private fun renderOcean(context: DrawContext, width: Int, height: Int) {
        val oceanStart = (height * 0.7f).toInt()
        
        // Base ocean fill
        context.fill(0, oceanStart, width, height, 0xFF1a237e.toInt())
        
        // Render waves
        waves.forEachIndexed { index, wave ->
            val baseY = oceanStart + index * 8
            renderWave(context, wave, baseY, width, height)
        }
    }
    
    private fun renderWave(context: DrawContext, wave: OceanWave, baseY: Int, width: Int, height: Int) {
        val points = mutableListOf<Pair<Int, Int>>()
        
        for (x in 0 until width step 3) {
            val waveHeight = wave.getHeightAt(x.toFloat(), time)
            val y = baseY + waveHeight.toInt()
            points.add(Pair(x, y))
        }
        
        // Fill wave area
        for (i in 0 until points.size - 1) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[i + 1]
            
            val waveColor = getWaveColor(wave.depth, y1 - baseY)
            fillWaveSegment(context, x1, y1, x2, y2, height, waveColor)
        }
    }
    
    private fun fillWaveSegment(context: DrawContext, x1: Int, y1: Int, x2: Int, y2: Int, height: Int, color: Int) {
        val minY = minOf(y1, y2).coerceAtLeast(0)
        val maxY = height
        
        if (minY < maxY) {
            context.fill(x1, minY, x2, maxY, color)
        }
    }
    
    private fun renderFoam(context: DrawContext, width: Int, height: Int) {
        foamParticles.forEach { particle ->
            if (particle.life > 0.1f) {
                val size = (1f + particle.life * 2f).toInt()
                val alpha = (particle.life * 150f).toInt().coerceIn(0, 150)
                val foamColor = (alpha shl 24) or 0xFFFFFF.toInt()
                
                val x = particle.x.toInt()
                val y = particle.y.toInt()
                
                context.fill(x, y, x + size, y + size, foamColor)
            }
        }
    }
    
    private fun renderSunReflection(context: DrawContext, width: Int, height: Int) {
        val oceanStart = (height * 0.7f).toInt()
        val sunX = width * 0.75f
        val reflectionWidth = 80f
        
        for (y in oceanStart until height step 2) {
            val distanceFromSurface = y - oceanStart
            val waveOffset = waves[0].getHeightAt(sunX, time) * 0.5f
            
            val reflectionIntensity = (1f - distanceFromSurface.toFloat() / (height - oceanStart)) * 0.6f
            val shimmer = sin(time * 4f + y * 0.1f) * 0.3f + 0.7f
            
            val alpha = (reflectionIntensity * shimmer * 120f).toInt().coerceIn(0, 120)
            if (alpha > 10) {
                val reflectionColor = (alpha shl 24) or 0xFFD700.toInt()
                val x1 = (sunX - reflectionWidth * reflectionIntensity + waveOffset).toInt()
                val x2 = (sunX + reflectionWidth * reflectionIntensity + waveOffset).toInt()
                
                context.fill(x1, y, x2, y + 1, reflectionColor)
            }
        }
    }
    
    private fun getWaveColor(depth: Float, waveHeight: Int): Int {
        val baseColor = when {
            depth < 0.3f -> 0x1a237e.toInt() // Deep blue
            depth < 0.6f -> 0x283593.toInt() // Medium blue
            else -> 0x3949ab.toInt() // Light blue
        }
        
        val heightFactor = (waveHeight.toFloat() / 20f).coerceIn(-1f, 1f)
        val brightness = (1f + heightFactor * 0.3f).coerceIn(0.7f, 1.3f)
        
        return adjustBrightness(baseColor, brightness)
    }
    
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val r = ((color shr 16) and 0xFF)
        val g = ((color shr 8) and 0xFF)
        val b = (color and 0xFF)
        
        val newR = (r * factor).toInt().coerceIn(0, 255)
        val newG = (g * factor).toInt().coerceIn(0, 255)
        val newB = (b * factor).toInt().coerceIn(0, 255)
        
        return 0xFF000000.toInt() or (newR shl 16) or (newG shl 8) or newB
    }
    
    private fun drawCircle(context: DrawContext, centerX: Int, centerY: Int, radius: Int, color: Int) {
        for (y in -radius..radius) {
            for (x in -radius..radius) {
                if (x * x + y * y <= radius * radius) {
                    context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color)
                }
            }
        }
    }
    
    private fun interpolateColor(color1: Int, color2: Int, factor: Float): Int {
        val f = factor.coerceIn(0f, 1f)
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val r = (r1 + (r2 - r1) * f).toInt()
        val g = (g1 + (g2 - g1) * f).toInt()
        val b = (b1 + (b2 - b1) * f).toInt()
        
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
    }
}

data class OceanWave(
    val amplitude: Float,
    val frequency: Float,
    val speed: Float,
    val phase: Float,
    val depth: Float
) {
    private var timeOffset: Float = 0f
    
    fun update(globalTime: Float) {
        timeOffset = globalTime * speed
    }
    
    fun getHeightAt(x: Float, time: Float): Float {
        return amplitude * sin(frequency * x + phase + timeOffset) * (1f - depth * 0.3f)
    }
}

data class FoamParticle(
    var x: Float,
    var y: Float,
    var life: Float
) {
    private val speed = 0.3f + Random.nextFloat() * 0.4f
    private val drift = Random.nextFloat() * 0.5f - 0.25f
    
    fun update(globalTime: Float, screenWidth: Int, screenHeight: Int) {
        x += speed
        y += drift + sin(globalTime * 3f + x * 0.01f) * 0.2f
        life -= 0.01f
    }
    
    fun shouldReset(screenWidth: Int): Boolean {
        return life <= 0f || x > screenWidth
    }
    
    fun reset(screenWidth: Int, screenHeight: Int) {
        x = -10f
        y = screenHeight * 0.7f + Random.nextFloat() * screenHeight * 0.2f
        life = 0.8f + Random.nextFloat() * 0.4f
    }
}