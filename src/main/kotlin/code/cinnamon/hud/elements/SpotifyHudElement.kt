package code.cinnamon.hud.elements

import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudElement
import code.cinnamon.spotify.SpotifyAuthManager
import code.cinnamon.spotify.SpotifyApi
import code.cinnamon.spotify.SpotifyTrackData
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

class SpotifyHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private val albumSize = 32
    private val progressBarWidth = 120
    private val progressBarHeight = 3
    private val elementHeight = albumSize + 4
    private val padding = 4

    private var albumTexture: Identifier? = null
    private var currentTrackData: SpotifyTrackData? = null
    private var loadingAlbumUrl: String? = null
    private var lastUpdateTime = 0L
    private val updateInterval = 1000L // Update every second

    // Scrolling text variables
    private var titleScrollOffset = 0f
    private var artistScrollOffset = 0f
    private var lastScrollTime = 0L
    private val scrollSpeed = 0.5f // pixels per millisecond
    private val scrollDelay = 2000L // delay before scrolling starts
    private val scrollPause = 1000L // pause at the end before restarting

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        val token = SpotifyAuthManager.getAccessToken()
        if (token != null) {
            // Update track data periodically
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime > updateInterval) {
                updateTrackData(token)
                lastUpdateTime = currentTime
            }

            renderSpotifyPlayer(context)
        } else {
            renderNotConnected(context)
        }

        context.matrices.popMatrix()
    }

    private fun updateTrackData(token: String) {
        // Update in background thread to avoid blocking render
        Thread {
            try {
                val newTrackData = SpotifyApi.getCurrentTrackData(token)

                // Update on main thread
                mc.execute {
                    // Clear album texture if track changed
                    if (currentTrackData?.albumImageUrl != newTrackData?.albumImageUrl) {
                        albumTexture?.let { oldTexture ->
                            mc.textureManager.destroyTexture(oldTexture)
                        }
                        albumTexture = null
                    }

                    // Reset scroll offsets when track changes
                    if (currentTrackData?.title != newTrackData?.title ||
                        currentTrackData?.artist != newTrackData?.artist) {
                        titleScrollOffset = 0f
                        artistScrollOffset = 0f
                        lastScrollTime = System.currentTimeMillis()
                    }

                    currentTrackData = newTrackData
                }
            } catch (e: Exception) {
                println("[Spotify] Error updating track data: ${e.message}")
            }
        }.start()
    }

    private fun renderSpotifyPlayer(context: DrawContext) {
        val trackData = currentTrackData

        if (trackData != null) {
            // Load album cover if needed
            if (trackData.albumImageUrl != null &&
                trackData.albumImageUrl != loadingAlbumUrl &&
                albumTexture == null) {
                loadAlbumCover(trackData.albumImageUrl)
            }

            var xOffset = 0

            // Draw album cover
            if (albumTexture != null) {
                try {
                    // Use the correct drawTexture method with RenderPipelines.GUI_TEXTURED
                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        albumTexture!!,
                        xOffset, // x position
                        0, // y position
                        0f, // u (texture x)
                        0f, // v (texture y)
                        albumSize, // width
                        albumSize, // height
                        albumSize, // texture width
                        albumSize // texture height
                    )
                } catch (e: Exception) {
                    println("[Spotify] Error drawing texture: ${e.message}")
                    // If texture drawing fails, draw a placeholder
                    drawAlbumPlaceholder(context, xOffset, 0)
                }
                xOffset += albumSize + padding
            } else {
                // Draw placeholder while loading
                drawAlbumPlaceholder(context, xOffset, 0)
                xOffset += albumSize + padding
            }

            // Draw track info with scrolling
            val maxTextWidth = progressBarWidth - 8 // Leave some margin
            drawScrollingText(context, trackData.title, xOffset, 2, maxTextWidth, textColor, true)
            drawScrollingText(context, trackData.artist, xOffset, 14, maxTextWidth, 0x888888, false)

            // Draw current time on the left side of progress bar
            val currentTimeStr = formatTime(trackData.currentTimeMs)
            context.drawText(mc.textRenderer, Text.literal(currentTimeStr),
                xOffset,
                albumSize - 12,
                0xAAAAAA, textShadowEnabled)

            // Draw duration on the right side of progress bar
            val durationStr = formatTime(trackData.durationMs)
            val durationTextWidth = mc.textRenderer.getWidth(Text.literal(durationStr))
            context.drawText(mc.textRenderer, Text.literal(durationStr),
                xOffset + progressBarWidth - durationTextWidth,
                albumSize - 12,
                0xAAAAAA, textShadowEnabled)

            // Draw progress bar
            val progressY = albumSize - progressBarHeight - 2
            drawProgressBar(context, xOffset, progressY, progressBarWidth, progressBarHeight, trackData.progress)
        } else {
            // No track playing
            val text = Text.literal("♪ No track playing")
                .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            context.drawText(mc.textRenderer, text, 0, (albumSize / 2) - 4, textColor, textShadowEnabled)
        }
    }

    private fun drawAlbumPlaceholder(context: DrawContext, x: Int, y: Int) {
        context.fill(x, y, x + albumSize, y + albumSize, 0xFF333333.toInt())
        val placeholderText = Text.literal("♪")
        val textWidth = mc.textRenderer.getWidth(placeholderText)
        val textHeight = mc.textRenderer.fontHeight
        context.drawText(mc.textRenderer, placeholderText,
            x + (albumSize - textWidth) / 2,
            y + (albumSize - textHeight) / 2,
            0xFFFFFF, false)
    }

    private fun drawScrollingText(context: DrawContext, text: String, x: Int, y: Int, maxWidth: Int, color: Int, isTitle: Boolean) {
        val textObj = Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val textWidth = mc.textRenderer.getWidth(textObj)

        if (textWidth <= maxWidth) {
            // Text fits, no scrolling needed
            context.drawText(mc.textRenderer, textObj, x, y, color, textShadowEnabled)
        } else {
            // Text needs scrolling
            val currentTime = System.currentTimeMillis()
            val timeSinceStart = currentTime - lastScrollTime

            var scrollOffset = if (isTitle) titleScrollOffset else artistScrollOffset

            if (timeSinceStart > scrollDelay) {
                val scrollDistance = textWidth - maxWidth + 20 // Extra space at the end
                val scrollTime = timeSinceStart - scrollDelay

                if (scrollTime < scrollDistance / scrollSpeed) {
                    // Scrolling phase
                    scrollOffset = -(scrollTime * scrollSpeed)
                } else if (scrollTime < scrollDistance / scrollSpeed + scrollPause) {
                    // Pause phase
                    scrollOffset = -scrollDistance.toFloat()
                } else {
                    // Reset
                    scrollOffset = 0f
                    lastScrollTime = currentTime
                }
            }

            if (isTitle) {
                titleScrollOffset = scrollOffset
            } else {
                artistScrollOffset = scrollOffset
            }

            // Create a clipping rectangle
            val scissor = context.enableScissor(x, y, x + maxWidth, y + mc.textRenderer.fontHeight)
            context.drawText(mc.textRenderer, textObj, x + scrollOffset.toInt(), y, color, textShadowEnabled)
            context.disableScissor()
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun renderNotConnected(context: DrawContext) {
        val text = Text.literal("♪ Spotify - Not Connected")
            .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        context.drawText(mc.textRenderer, text, 0, (albumSize / 2) - 4, 0x888888, textShadowEnabled)
    }

    private fun drawProgressBar(context: DrawContext, x: Int, y: Int, width: Int, height: Int, progress: Float) {
        // Background
        context.fill(x, y, x + width, y + height, 0x44FFFFFF)

        // Progress
        val progressWidth = (width * progress.coerceIn(0f, 1f)).toInt()
        if (progressWidth > 0) {
            context.fill(x, y, x + progressWidth, y + height, 0xFF1DB954.toInt()) // Spotify green
        }
    }

    private fun loadAlbumCover(imageUrl: String) {
        loadingAlbumUrl = imageUrl

        // Load image asynchronously to avoid blocking the render thread
        Thread {
            try {
                val url = URL(imageUrl)
                val bufferedImage = ImageIO.read(url)

                if (bufferedImage != null) {
                    val scaledImage = scaleImage(bufferedImage, albumSize, albumSize)
                    val nativeImage = convertToNativeImage(scaledImage)

                    // Create texture identifier
                    val textureName = "spotify_album_${System.currentTimeMillis()}"
                    val identifier = Identifier.of("cinnamon", textureName)

                    // Create texture with correct constructor
                    val texture = NativeImageBackedTexture({ textureName }, nativeImage)

                    // Register texture on main thread
                    mc.execute {
                        try {
                            mc.textureManager.registerTexture(identifier, texture)
                            albumTexture = identifier
                            loadingAlbumUrl = null
                            println("[Spotify] Album cover loaded successfully")
                        } catch (e: Exception) {
                            println("[Spotify] Failed to register texture: ${e.message}")
                            loadingAlbumUrl = null
                        }
                    }
                } else {
                    println("[Spotify] Failed to read album image from URL: $imageUrl")
                    loadingAlbumUrl = null
                }
            } catch (e: Exception) {
                println("[Spotify] Failed to load album cover: ${e.message}")
                e.printStackTrace()
                loadingAlbumUrl = null
            }
        }.start()
    }

    private fun scaleImage(original: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        val scaled = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = scaled.createGraphics()
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null)
        g2d.dispose()
        return scaled
    }

    private fun convertToNativeImage(bufferedImage: BufferedImage): NativeImage {
        val width = bufferedImage.width
        val height = bufferedImage.height
        val nativeImage = NativeImage(width, height, false)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val argb = bufferedImage.getRGB(x, y)
                // Convert ARGB to ABGR for NativeImage
                val alpha = (argb shr 24) and 0xFF
                val red = (argb shr 16) and 0xFF
                val green = (argb shr 8) and 0xFF
                val blue = argb and 0xFF
                val abgr = (alpha shl 24) or (blue shl 16) or (green shl 8) or red
                nativeImage.setColor(x, y, abgr)
            }
        }

        return nativeImage
    }

    override fun getWidth(): Int {
        val token = SpotifyAuthManager.getAccessToken()
        return if (token != null) {
            albumSize + padding + progressBarWidth
        } else {
            mc.textRenderer.getWidth(Text.literal("♪ Spotify - Not Connected"))
        }
    }

    override fun getHeight(): Int = elementHeight
    override fun getName(): String = "Spotify"
}