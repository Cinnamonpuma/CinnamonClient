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

                // Debug logging
                println("[Spotify Debug] Track data received: $newTrackData")

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
                e.printStackTrace()
            }
        }.start()
    }

    private fun renderSpotifyPlayer(context: DrawContext) {
        val trackData = currentTrackData

        if (trackData != null) {
            println("[Spotify Debug] Rendering track: ${trackData.title} by ${trackData.artist}")

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
                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        albumTexture!!,
                        xOffset,
                        0,
                        0f,
                        0f,
                        albumSize,
                        albumSize,
                        albumSize,
                        albumSize
                    )
                    println("[Spotify Debug] Album cover drawn successfully")
                } catch (e: Exception) {
                    println("[Spotify] Error drawing texture: ${e.message}")
                    e.printStackTrace()
                    drawAlbumPlaceholder(context, xOffset, 0)
                }
                xOffset += albumSize + padding
            } else {
                // Draw placeholder while loading
                drawAlbumPlaceholder(context, xOffset, 0)
                xOffset += albumSize + padding
            }

            // Draw track info with scrolling
            val maxTextWidth = progressBarWidth - 8

            // Title
            val titleText = Text.literal(trackData.title)
                .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            drawScrollingText(context, trackData.title, xOffset, 2, maxTextWidth, textColor, true)

            // Artist
            val artistText = Text.literal(trackData.artist)
                .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            drawScrollingText(context, trackData.artist, xOffset, 14, maxTextWidth, 0x888888, false)

            // Progress bar area
            val progressY = albumSize - progressBarHeight - 12 // Move up to make room for times

            // Draw current time
            val currentTimeStr = formatTime(trackData.currentTimeMs)
            val currentTimeText = Text.literal(currentTimeStr)
                .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            context.drawText(mc.textRenderer, currentTimeText,
                xOffset,
                progressY + progressBarHeight + 2,
                0xAAAAAA, textShadowEnabled)

            // Draw duration
            val durationStr = formatTime(trackData.durationMs)
            val durationText = Text.literal(durationStr)
                .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            val durationTextWidth = mc.textRenderer.getWidth(durationText)
            context.drawText(mc.textRenderer, durationText,
                xOffset + progressBarWidth - durationTextWidth,
                progressY + progressBarHeight + 2,
                0xAAAAAA, textShadowEnabled)

            // Draw progress bar
            drawProgressBar(context, xOffset, progressY, progressBarWidth, progressBarHeight, trackData.progress)

            println("[Spotify Debug] Progress: ${trackData.progress}, Time: ${currentTimeStr}/${durationStr}")
        } else {
            println("[Spotify Debug] No track data available")
            // No track playing
            val text = Text.literal("♪ No track playing")
                .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            context.drawText(mc.textRenderer, text, 0, (albumSize / 2) - 4, textColor, textShadowEnabled)
        }
    }

    private fun drawAlbumPlaceholder(context: DrawContext, x: Int, y: Int) {
        // Use a darker gray for better visibility
        context.fill(x, y, x + albumSize, y + albumSize, 0xFF444444.toInt())
        val placeholderText = Text.literal("♪")
            .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
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
                val scrollDistance = textWidth - maxWidth + 20
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
        // Background - slightly more visible
        context.fill(x, y, x + width, y + height, 0x66FFFFFF)

        // Progress
        val progressWidth = (width * progress.coerceIn(0f, 1f)).toInt()
        if (progressWidth > 0) {
            context.fill(x, y, x + progressWidth, y + height, 0xFF1DB954.toInt()) // Spotify green
        }
    }

    private fun loadAlbumCover(imageUrl: String) {
        loadingAlbumUrl = imageUrl
        println("[Spotify Debug] Loading album cover: $imageUrl")

        // Load image asynchronously
        Thread {
            try {
                val url = URL(imageUrl)
                val bufferedImage = ImageIO.read(url)

                if (bufferedImage != null) {
                    val scaledImage = scaleImage(bufferedImage, albumSize, albumSize)
                    val nativeImage = convertToNativeImage(scaledImage)

                    val textureName = "spotify_album_${System.currentTimeMillis()}"
                    val identifier = Identifier.of("cinnamon", textureName)
                    val texture = NativeImageBackedTexture({ textureName }, nativeImage)

                    mc.execute {
                        try {
                            mc.textureManager.registerTexture(identifier, texture)
                            albumTexture = identifier
                            loadingAlbumUrl = null
                            println("[Spotify Debug] Album cover loaded successfully")
                        } catch (e: Exception) {
                            println("[Spotify] Failed to register texture: ${e.message}")
                            e.printStackTrace()
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

    override fun getHeight(): Int = elementHeight + 12 // Extra space for time display
    override fun getName(): String = "Spotify"
}