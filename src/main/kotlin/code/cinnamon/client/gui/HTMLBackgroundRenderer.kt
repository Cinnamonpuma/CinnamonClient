package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.*

class HTMLBackgroundRenderer {
    
    private var backgroundTexture: NativeImageBackedTexture? = null
    private var textureId: Identifier? = null
    private var isInitialized = false
    private var animationTime = 0f
    
    // Animation parameters
    private var lastWidth = 0
    private var lastHeight = 0
    
    fun render(context: DrawContext, width: Int, height: Int) {
        // Initialize or resize if needed
        if (!isInitialized || width != lastWidth || height != lastHeight) {
            createBackgroundTexture(width, height)
            lastWidth = width
            lastHeight = height
            isInitialized = true
        }
        
        // Update animation
        animationTime += 0.016f // ~60fps
        updateBackgroundTexture(width, height)
        
        // Render the texture
        textureId?.let { id ->
            context.drawTexture(
                id, 0, 0, width, height,
                0f, 0f, width, height,
                width, height
            )
        }
    }
    
    private fun createBackgroundTexture(width: Int, height: Int) {
        // Clean up previous texture
        backgroundTexture?.close()
        
        // Create new texture
        val nativeImage = NativeImage(width, height, false)
        backgroundTexture = NativeImageBackedTexture(nativeImage)
        
        // Register texture
        val id = Identifier("cinnamon", "html_background")
        val textureManager = net.minecraft.client.MinecraftClient.getInstance().textureManager
        textureManager.registerTexture(id, backgroundTexture!!)
        textureId = id
    }
    
    private fun updateBackgroundTexture(width: Int, height: Int) {
        backgroundTexture?.let { texture ->
            val nativeImage = texture.image
            
            // Choose your background style here
            when (getCurrentBackgroundStyle()) {
                BackgroundStyle.ANIMATED_GRADIENT -> renderAnimatedGradient(nativeImage, width, height)
                BackgroundStyle.FLOATING_PARTICLES -> renderFloatingParticles(nativeImage, width, height)
                BackgroundStyle.MATRIX_RAIN -> renderMatrixRain(nativeImage, width, height)
                BackgroundStyle.GEOMETRIC_WAVES -> renderGeometricWaves(nativeImage, width, height)
                BackgroundStyle.STARFIELD -> renderStarfield(nativeImage, width, height)
            }
            
            texture.upload()
        }
    }
    
    private fun getCurrentBackgroundStyle(): BackgroundStyle {
        // You can change this or make it configurable
        return BackgroundStyle.ANIMATED_GRADIENT
    }
    
    private fun renderAnimatedGradient(image: NativeImage, width: Int, height: Int) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                val time = animationTime * 0.5f
                val gradientFactor = y.toFloat() / height
                
                // Create animated color waves
                val r = (sin(time + x * 0.01f) * 0.3f + 0.4f + gradientFactor * 0.3f).coerceIn(0f, 1f)
                val g = (sin(time * 1.3f + y * 0.01f) * 0.3f + 0.2f + gradientFactor * 0.5f).coerceIn(0f, 1f)
                val b = (sin(time * 0.8f + (x + y) * 0.005f) * 0.4f + 0.6f).coerceIn(0f, 1f)
                
                val color = Color(r, g, b, 1f).rgb
                image.setColor(x, y, color)
            }
        }
    }
    
    private fun renderFloatingParticles(image: NativeImage, width: Int, height: Int) {
        // Fill with dark background
        image.fillRect(0, 0, width, height, 0xFF0a0a0a.toInt())
        
        // Draw floating particles
        val particleCount = 50
        for (i in 0 until particleCount) {
            val seed = i * 127.3f
            val x = ((sin(animationTime * 0.3f + seed) * 0.4f + 0.5f) * width).toInt()
            val y = ((sin(animationTime * 0.2f + seed * 1.7f) * 0.4f + 0.5f) * height).toInt()
            val size = (sin(animationTime * 0.5f + seed * 2.1f) * 2f + 4f).toInt()
            
            val alpha = (sin(animationTime * 0.8f + seed) * 0.3f + 0.7f).coerceIn(0f, 1f)
            val color = Color(0.8f, 0.9f, 1f, alpha).rgb
            
            drawCircle(image, x, y, size, color, width, height)
        }
    }
    
    private fun renderMatrixRain(image: NativeImage, width: Int, height: Int) {
        // Dark background
        image.fillRect(0, 0, width, height, 0xFF000000.toInt())
        
        val columns = width / 10
        for (col in 0 until columns) {
            val x = col * 10
            val speed = 2f + (col % 3) * 1.5f
            val offset = (animationTime * speed) % (height + 100)
            
            for (i in 0..10) {
                val y = (offset - i * 15).toInt()
                if (y >= 0 && y < height) {
                    val alpha = (1f - i * 0.1f).coerceIn(0f, 1f)
                    val green = (0.8f * alpha).coerceIn(0f, 1f)
                    val color = Color(0f, green, 0f, alpha).rgb
                    
                    drawRect(image, x, y, 8, 12, color, width, height)
                }
            }
        }
    }
    
    private fun renderGeometricWaves(image: NativeImage, width: Int, height: Int) {
        // Dark blue background
        image.fillRect(0, 0, width, height, 0xFF0a1a2a.toInt())
        
        // Draw geometric wave lines
        val waveCount = 8
        for (wave in 0 until waveCount) {
            val amplitude = 30f + wave * 10f
            val frequency = 0.01f + wave * 0.005f
            val phase = animationTime * (0.5f + wave * 0.2f)
            val baseY = height * 0.3f + wave * 20f
            
            val color = Color(
                0.2f + wave * 0.1f,
                0.4f + wave * 0.05f,
                0.8f - wave * 0.05f,
                0.8f
            ).rgb
            
            for (x in 0 until width - 1) {
                val y1 = baseY + sin(x * frequency + phase) * amplitude
                val y2 = baseY + sin((x + 1) * frequency + phase) * amplitude
                
                drawLine(image, x, y1.toInt(), x + 1, y2.toInt(), color, width, height)
            }
        }
    }
    
    private fun renderStarfield(image: NativeImage, width: Int, height: Int) {
        // Space background
        image.fillRect(0, 0, width, height, 0xFF000011.toInt())
        
        val starCount = 200
        for (i in 0 until starCount) {
            val seed = i * 73.7f
            val x = ((sin(seed) * 0.5f + 0.5f) * width).toInt()
            val y = ((cos(seed * 1.3f) * 0.5f + 0.5f) * height).toInt()
            
            val twinkle = sin(animationTime * (1f + i % 3) + seed) * 0.3f + 0.7f
            val brightness = (twinkle * 255).toInt().coerceIn(0, 255)
            val color = (0xFF shl 24) or (brightness shl 16) or (brightness shl 8) or brightness
            
            image.setColor(x, y, color)
            
            // Larger stars
            if (i % 10 == 0) {
                drawCircle(image, x, y, 2, color, width, height)
            }
        }
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
        val dx = abs(x2 - x1)
        val dy = abs(y2 - y1)
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
    
    fun cleanup() {
        backgroundTexture?.close()
        backgroundTexture = null
        textureId = null
        isInitialized = false
    }
}

enum class BackgroundStyle {
    ANIMATED_GRADIENT,
    FLOATING_PARTICLES,
    MATRIX_RAIN,
    GEOMETRIC_WAVES,
    STARFIELD
}