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
import java.net.URI
import javax.imageio.ImageIO

class SpotifyHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private val albumSize = 32
    private val progressBarWidth = 120
    private val progressBarHeight = 3
    private val padding = 6
    private val textSpacing = 11

    private var albumTexture: Identifier? = null
    private var currentTrackData: SpotifyTrackData? = null
    private var loadingAlbumUrl: String? = null
    private var lastUpdateTime = 0L
    private val updateInterval = 1000L

    private var titleScrollOffset = 0f
    private var artistScrollOffset = 0f
    private var lastScrollTime = 0L
    private val scrollSpeed = 0.5f
    private val scrollDelay = 2000L
    private val scrollPause = 1000L

    private var titleScrollStartTime = 0L
    private var artistScrollStartTime = 0L
    private val smoothScrollSpeed = 20f

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        val token = SpotifyAuthManager.getAccessToken()
        if (token != null) {
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
        Thread {
            try {
                val newTrackData = SpotifyApi.getCurrentTrackData(token)
                mc.execute {
                    if (currentTrackData?.albumImageUrl != newTrackData?.albumImageUrl) {
                        albumTexture?.let { mc.textureManager.destroyTexture(it) }
                        albumTexture = null
                    }

                    if (currentTrackData?.title != newTrackData?.title ||
                        currentTrackData?.artist != newTrackData?.artist) {
                        titleScrollOffset = 0f
                        artistScrollOffset = 0f
                        lastScrollTime = System.currentTimeMillis()
                        titleScrollStartTime = System.currentTimeMillis()
                        artistScrollStartTime = System.currentTimeMillis()
                    }

                    currentTrackData = newTrackData
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun renderSpotifyPlayer(context: DrawContext) {
        val trackData = currentTrackData

        if (trackData != null) {
            if (trackData.albumImageUrl != null &&
                trackData.albumImageUrl != loadingAlbumUrl &&
                albumTexture == null) {
                loadAlbumCover(trackData.albumImageUrl)
            }

            var xOffset = 0

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
                } catch (e: Exception) {
                    drawAlbumPlaceholder(context, xOffset, 0)
                }
            } else {
                drawAlbumPlaceholder(context, xOffset, 0)
            }

            xOffset += albumSize + padding

            val maxTextWidth = progressBarWidth - 4

            drawScrollingText(context, trackData.title, xOffset, 1, maxTextWidth, textColor, true)

            drawScrollingText(context, trackData.artist, xOffset, 1 + textSpacing, maxTextWidth, textColor, false)

            val progressY = 1 + textSpacing * 2 + 2
            drawProgressBar(context, xOffset, progressY, progressBarWidth, progressBarHeight, trackData.progress)

            val timeY = progressY + progressBarHeight + 3
            val currentTimeStr = formatTime(trackData.currentTimeMs)
            val durationStr = formatTime(trackData.durationMs)

            val currentTimeText = Text.literal(currentTimeStr).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            val durationText = Text.literal(durationStr).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

            val durationTextWidth = mc.textRenderer.getWidth(durationText)

            context.drawText(mc.textRenderer, currentTimeText, xOffset, timeY, textColor, textShadowEnabled)
            context.drawText(mc.textRenderer, durationText, xOffset + progressBarWidth - durationTextWidth, timeY, textColor, textShadowEnabled)
        } else {
            val text = Text.literal("♪ No track playing")
                .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            context.drawText(mc.textRenderer, text, 0, (albumSize / 2) - 4, textColor, textShadowEnabled)
        }
    }

    private fun drawAlbumPlaceholder(context: DrawContext, x: Int, y: Int) {
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
            context.drawText(mc.textRenderer, textObj, x, y, color, textShadowEnabled)
        } else {
            val currentTime = System.currentTimeMillis()
            var scrollOffset: Float

            if (isTitle) {
                val timeSinceStart = currentTime - titleScrollStartTime
                if (timeSinceStart > scrollDelay) {
                    val scrollTime = (timeSinceStart - scrollDelay) / 1000f
                    val totalScrollDistance = textWidth - maxWidth + 40f
                    val scrollProgress = (scrollTime * smoothScrollSpeed) % (totalScrollDistance + 100f)

                    scrollOffset = if (scrollProgress <= totalScrollDistance) {
                        -scrollProgress
                    } else {
                        textWidth.toFloat()
                    }
                } else {
                    scrollOffset = 0f
                }
                titleScrollOffset = scrollOffset
            } else {
                val timeSinceStart = currentTime - lastScrollTime
                scrollOffset = if (timeSinceStart > scrollDelay) {
                    val scrollDistance = textWidth - maxWidth + 20
                    val scrollTime = timeSinceStart - scrollDelay

                    if (scrollTime < scrollDistance / scrollSpeed) {
                        -(scrollTime * scrollSpeed)
                    } else if (scrollTime < scrollDistance / scrollSpeed + scrollPause) {
                        -scrollDistance.toFloat()
                    } else {
                        lastScrollTime = currentTime
                        0f
                    }
                } else {
                    artistScrollOffset
                }
                artistScrollOffset = scrollOffset
            }

            context.enableScissor(x, y, x + maxWidth, y + mc.textRenderer.fontHeight)
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
        context.fill(x, y, x + width, y + height, 0x66FFFFFF)
        val progressWidth = (width * progress.coerceIn(0f, 1f)).toInt()
        if (progressWidth > 0) {
            context.fill(x, y, x + progressWidth, y + height, 0xFF1DB954.toInt())
        }
    }

    private fun loadAlbumCover(imageUrl: String) {
        loadingAlbumUrl = imageUrl

        Thread {
            try {
                val url = URI(imageUrl).toURL()
                val bufferedImage = ImageIO.read(url)

                if (bufferedImage != null) {
                    val scaledImage = scaleImage(bufferedImage, albumSize, albumSize)
                    mc.execute {
                        try {
                            val nativeImage = convertToNativeImage(scaledImage)
                            val textureName = "spotify_album_${System.currentTimeMillis()}"
                            val identifier = Identifier.of("cinnamon", textureName)
                            val texture = NativeImageBackedTexture({ textureName }, nativeImage)

                            albumTexture?.let { mc.textureManager.destroyTexture(it) }
                            mc.textureManager.registerTexture(identifier, texture)
                            albumTexture = identifier
                            loadingAlbumUrl = null
                        } catch (e: Exception) {
                            loadingAlbumUrl = null
                        }
                    }
                } else {
                    loadingAlbumUrl = null
                }
            } catch (e: Exception) {
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

    override fun getHeight(): Int {
        val token = SpotifyAuthManager.getAccessToken()
        return if (token != null) {
            val textHeight = mc.textRenderer.fontHeight
            maxOf(albumSize, textHeight * 2 + textSpacing + progressBarHeight + 6)
        } else {
            albumSize
        }
    }

    override fun getName(): String = "Spotify"
}