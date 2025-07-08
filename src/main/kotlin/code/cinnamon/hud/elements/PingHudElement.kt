package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.*

class PingHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private var lastPing = 0
    private var pingChangeAnimation = 0f
    private val cornerRadius = 6

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        val currentPing = getPing()

        if (currentPing != lastPing) {
            pingChangeAnimation = 0.3f
            lastPing = currentPing
        }
        pingChangeAnimation = maxOf(0f, pingChangeAnimation - tickDelta * 0.1f)

        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)

        val width = getWidth()
        val height = getHeight()
        val padding = 6

        drawRoundedBackground(context, -padding, -padding, width + padding * 2, height + padding * 2, this.backgroundColor)

        val pingText = Text.literal("${currentPing}ms").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

        if (this.textShadowEnabled) {
            context.drawText(mc.textRenderer, pingText, 1, 1, 0x40000000, false)
        }

        context.drawText(mc.textRenderer, pingText, 0, 0, this.textColor, false)

        context.matrices.pop()
    }

    private fun drawRoundedBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, backgroundColor: Int) {
        drawRoundedRect(context, x, y, width, height, cornerRadius, backgroundColor)
    }

    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return

        val r = minOf(radius, minOf(width / 2, height / 2))

        if (r <= 0) {
            context.fill(x, y, x + width, y + height, color)
            return
        }


        context.fill(x + r, y, x + width - r, y + height, color)

        context.fill(x, y + r, x + r, y + height - r, color)
        context.fill(x + width - r, y + r, x + width, y + height - r, color)

        drawRoundedCorner(context, x, y, r, color, 0)
        drawRoundedCorner(context, x + width - r, y, r, color, 1)
        drawRoundedCorner(context, x, y + height - r, r, color, 2)
        drawRoundedCorner(context, x + width - r, y + height - r, r, color, 3)
    }

    private fun drawRoundedCorner(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, corner: Int) {
        for (dy in 0 until radius) {
            for (dx in 0 until radius) {
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared <= radius * radius) {
                    val pixelX: Int
                    val pixelY: Int

                    when (corner) {
                        0 -> {
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + (radius - 1 - dy)
                        }
                        1 -> {
                            pixelX = x + dx
                            pixelY = y + (radius - 1 - dy)
                        }
                        2 -> {
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + dy
                        }
                        3 -> {
                            pixelX = x + dx
                            pixelY = y + dy
                        }
                        else -> continue
                    }

                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }

    private fun getPing(): Int {
        return mc.networkHandler?.getPlayerListEntry(mc.player?.uuid)?.latency ?: 0
    }

    override fun getWidth(): Int {
        val text = Text.literal("${getPing()}ms").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        return mc.textRenderer.getWidth(text)
    }

    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "Ping"
}