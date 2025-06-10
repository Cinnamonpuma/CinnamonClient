package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.awt.Color
import kotlin.math.*

class CSSStyleBackgroundRenderer {
    
    private var backgroundTexture: NativeImageBackedTexture? = null
    private var textureId: Identifier? = null
    private var animationTime = 0f
    
    // CSS-like properties
    data class BackgroundConfig(
        val type: String = "gradient",
        val colors: List<String> = listOf("#1a1a2e", "#16213e", "#0f3460"),
        val animation: String = "wave",
        val animationDuration: Float = 4f,
        val animationDirection: String = "normal",
        val particles: ParticleConfig? = null
    )
    
    data class ParticleConfig(
        val count: Int = 50,
        val color: String = "#ffffff",
        val size: IntRange = 1..4,
        val speed: Float = 1f,
        val opacity: FloatRange = 0.3f..1f
    )
    
    // Current configuration (CSS-like)
    private var config = BackgroundConfig(
        type = "animated-gradient",
        colors = listOf("#ff6b6b", "#4ecdc4", "#45b7d1", "#96ceb4"),
        animation = "floating",
        animationDuration = 6f,
        particles = ParticleConfig(count = 30, color = "#ffffff80", size = 2..6)
    )
    
    fun render(context: DrawContext, width: Int, height: Int) {
        updateTexture(width, height)
        animationTime += 0.016f
        
        textureId?.let { id ->
            context.drawTexture(
                id, 0, 0, width, height,
                0f, 0f, width, height,
                width, height
            )
        }
    }
    
    private fun updateTexture(width: Int, height: Int) {
        if (backgroundTexture?.image?.width != width || backgroundTexture?.image?.height != height) {
            createTexture(width, height)
        }
        
        backgroundTexture?.let { texture ->
            val image = texture.image
            
            when (config.type) {
                "gradient" -> renderGradient(image, width, height)
                "animated-gradient" -> renderAnimatedGradient(image, width, height)
                "particles" -> renderParticles(image, width, height)
                "waves" -> renderWaves(image, width, height)
                "matrix" -> renderMatrix(image, width, height)
                else -> renderGradient(image, width, height)
            }
            
            // Add particles if configured
            config.particles?.let { particles ->
                renderParticlesOverlay(image, width, height, particles)
            }
            
            texture.upload()
        }
    }
    
    private fun createTexture(width: Int, height: Int) {
        backgroundTexture?.close()
        
        val nativeImage = NativeImage(width, height, false)
        backgroundTexture = NativeImageBackedTexture(nativeImage)
        
        val id = Identifier("cinnamon", "css_background")
        val textureManager = net.minecraft.client.MinecraftClient.getInstance().textureManager
        textureManager.registerTexture(id, backgroundTexture!!)
        textureId = id
    }
    
    private fun renderAnimatedGradient(image: NativeImage, width: Int, height: Int) {
        val colors = config.colors.map { parseColor(it) }
        val time = animationTime / config.animationDuration
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val gradientPos = when (config.animation) {
                    "wave" -> {
                        val wave = sin(time * 2 * PI.toFloat() + x * 0.01f + y * 0.005f) * 0.2f + 0.5f
                        (y.toFloat() / height + wave).coerceIn(0f, 1f)
                    }
                    "radial" -> {
                        val centerX = width / 2f
                        val centerY = height / 2f
                        val distance = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
                        val maxDistance = sqrt(centerX.pow(2) + centerY.pow(2))
                        val pulse = sin(time * 2 * PI.toFloat()) * 0.3f + 0.7f
                        ((distance / maxDistance) * pulse).coerceIn(0f, 1f)
                    }
                    "floating" -> {
                        val floatX = sin(time * PI.toFloat() + y * 0.01f) * 0.1f
                        val floatY = cos(time * PI.toFloat() * 0.7f + x * 0.01f) * 0.1f
                        ((y.toFloat() / height) + floatX + floatY).coerceIn(0f, 1f)
                    }
                    else -> y.toFloat() / height
                }
                
                val color = interpolateColors(colors, gradientPos)
                image.setColor(x, y, color.rgb)
            }
        }
    }
    
    private fun renderGradient(image: NativeImage, width: Int, height: Int) {
        val colors = config.colors.map { parseColor(it) }
        
        for (y in 0 until height) {
            val gradientPos = y.toFloat() / height
            val color = interpolateColors(colors, gradientPos)
            
            for (x in 0 until width) {
                image.setColor(x, y, color.rgb)
            }
        }
    }
    
    private fun renderParticles(image: NativeImage, width: Int, height: Int) {
        // Dark background
        image.fillRect(0, 0, width, height, parseColor("#0a0a0a").rgb)
        
        val particleCount = 100
        for (i in 0 until particleCount) {
            val seed = i * 123.45f
            val x = ((sin(animationTime * 0.2f + seed) * 0.4f + 0.5f) * width).toInt()
            val y = ((cos(animationTime * 0.15f + seed * 1.7f) * 0.4f + 0.5f) * height).toInt()
            val size = (sin(animationTime * 0.3f + seed * 2.1f) * 2f + 3f).toInt()
            
            val alpha = (sin(animationTime * 0.5f + seed) * 0.3f + 0.7f).coerceIn(0f, 1f)
            val color = Color(1f, 1f, 1f, alpha)
            
            drawCircle(image, x, y, size, color.rgb, width, height)
        }
    }
    
    private fun renderWaves(image: NativeImage, width: Int, height: Int) {
        // Background gradient
        for (y in 0 until height) {
            val gradientColor = Color.HSBtoRGB(0.6f, 0.8f, 0.1f + (y.toFloat() / height) * 0.3f)
            for (x in 0 until width) {
                image.setColor(x, y, gradientColor)
            }
        }
        
        // Wave lines
        val waveCount = 6
        for (wave in 0 until waveCount) {
            val amplitude = 20f + wave * 15f
            val frequency = 0.008f + wave * 0.003f
            val phase = animationTime * (0.3f + wave * 0.1f)
            val baseY = height * 0.2f + wave * (height * 0.1f)
            
            val hue = (wave * 0.15f + animationTime * 0.1f) % 1f
            val waveColor = Color.HSBtoRGB(hue, 0.8f, 0.9f)
            
            for (x in 0 until width - 1) {
                val y1 = (baseY + sin(x * frequency + phase) * amplitude).toInt()
                val y2 = (baseY + sin((x + 1) * frequency + phase) * amplitude).toInt()
                drawLine(image, x, y1, x + 1, y2, waveColor, width, height)
            }
        }
    }
    
    private fun renderMatrix(image: NativeImage, width: Int, height: Int) {
        image.fillRect(0, 0, width, height, Color.BLACK.rgb)
        
        val columns = width / 12
        for (col in 0 until columns) {
            val x = col * 12
            val speed = 1f + (col % 4) * 0.5f
            val offset = (animationTime * speed * 20) % (height + 50)
            
            for (i in 0..8) {
                val y = (offset - i * 18).toInt()
                if (y >= 0 && y < height - 12) {
                    val alpha = (1f - i * 0.12f).coerceIn(0f, 1f)
                    val green = (alpha * 255).toInt()
                    val matrixColor = (255 shl 24) or (green shl 8)
                    
                    drawRect(image, x, y, 10, 15, matrixColor, width, height)
                }
            }
        }
    }
    
    private fun renderParticlesOverlay(image: NativeImage, width: Int, height: Int, particleConfig: ParticleConfig) {
        for (i in 0 until particleConfig.count) {
            val seed = i * 67.3f
            val x = ((sin(animationTime * particleConfig.speed * 0.1f + seed) * 0.4f + 0.5f) * width).toInt()
            val y = ((cos(animationTime * particleConfig.speed * 0.08f + seed * 1.3f) * 0.4f + 0.5f) * height).toInt()
            
            val size = particleConfig.size.random()
            val alpha = particleConfig.opacity.start + 
                       (particleConfig.opacity.endInclusive - particleConfig.opacity.start) * 
                       (sin(animationTime * 0.5f + seed) * 0.5f + 0.5f)
            
            val baseColor = parseColor(particleConfig.color)
            val particleColor = Color(baseColor.red / 255f, baseColor.green / 255f, baseColor.blue / 255f, alpha)
            
            drawCircle(image, x, y, size, particleColor.rgb, width, height)
        }
    }
    
    // Utility functions
    private fun parseColor(colorString: String): Color {
        val hex = colorString.removePrefix("#")
        return when (hex.length) {
            6 -> Color(Integer.parseInt(hex, 16))
            8 -> Color(Integer.parseInt(hex.substring(0, 6), 16), true) // With alpha
            else -> Color.WHITE
        }
    }
    
    private fun interpolateColors(colors: List<Color>, position: Float): Color {
        if (colors.isEmpty()) return Color.WHITE
        if (colors.size == 1) return colors[0]
        
        val scaledPos = position * (colors.size - 1)
        val index = scaledPos.toInt().coerceIn(0, colors.size - 2)
        val fraction = scaledPos - index
        
        val color1 = colors[index]
        val color2 = colors[index + 1]
        
        return Color(
            (color1.red + (color2.red - color1.red) * fraction).toInt(),
            (color1.green + (color2.green - color1.green) * fraction).toInt(),
            (color1.blue + (color2.blue - color1.blue) * fraction).toInt()
        )
    }
    
    private fun drawCircle(image: NativeImage, centerX: Int, centerY: Int, radius: Int, color: Int, width: Int, height: Int) {
        for (y in -radius..radius) {
            for (x in -radius..radius) {
                if (x * x + y * y <= radius * radius) {
                    val px = centerX + x
                    val py = centerY + y
                    if (px in 0 until width && py in 0 until height) {
                        image.setColor(px, py, color)
                    }
                }
            }
        }
    }
    
    private fun drawRect(image: NativeImage, x: Int, y: Int, w: Int, h: Int, color: Int, width: Int, height: Int) {
        for (dy in 0 until h) {
            for (dx in 0 until w) {
                val px = x + dx
                val py = y + dy
                if (px in 0 until width && py in 0 until height) {
                    image.setColor(px, py, color)
                }
            }
        }
    }
    
    private fun drawLine(image: NativeImage, x1: Int, y1: Int, x2: Int, y2: Int, color: Int, width: Int, height: Int) {
        val dx = kotlin.math.abs(x2 - x1)
        val dy = kotlin.math.abs(y2 - y1)
        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1
        var err = dx - dy
        
        var x = x1
        var y = y1
        
        while (true) {
            if (x in 0 until width && y in 0 until height) {
                image.setColor(x, y, color)
            }
            
            if (x == x2 && y == y2) break
            
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }
    
    // Configuration methods (CSS-like API)
    fun setStyle(newConfig: BackgroundConfig) {
        config = newConfig
    }
    
    fun setGradient(vararg colors: String) {
        config = config.copy(type = "animated-gradient", colors = colors.toList())
    }
    
    fun setAnimation(type: String, duration: Float = 4f) {
        config = config.copy(animation = type, animationDuration = duration)
    }
    
    fun addParticles(count: Int = 50, color: String = "#ffffff80") {
        config = config.copy(particles = ParticleConfig(count = count, color = color))
    }
    
    fun cleanup() {
        backgroundTexture?.close()
        backgroundTexture = null
        textureId = null
    }
}