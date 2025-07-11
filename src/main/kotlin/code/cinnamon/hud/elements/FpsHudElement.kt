package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.*

class FpsHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    // private var lastFps = 0 // Animation state can be re-added if desired
    // private var fpsChangeAnimation = 0f
    private val cornerRadius = 6
    private val internalPadding = 6 // Padding for the background around the text

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices) // Use Floats
        context.matrices.scale(this.scale, this.scale, context.matrices) // scale is already Float

        val currentFps = mc.currentFps
        // Animation logic can be re-inserted here if needed.

        // Drawing is relative to (0,0).
        // getWidth() and getHeight() define the text content area.

        if (backgroundColor != 0) {
            drawRoundedBackground(
                context,
                -internalPadding, // Draw background relative to (0,0), extending by padding
                -internalPadding,
                getWidth() + internalPadding * 2,
                getHeight() + internalPadding * 2,
                this.backgroundColor
            )
        }

        val fpsText = Text.literal("$currentFps Fps").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

        // Text is drawn at (0,0) relative to the element's top-left (which is the start of the text content area).
        if (this.textShadowEnabled) {
            context.drawText(mc.textRenderer, fpsText, 1, 1, 0x40000000, false) // Shadow offset by 1
        }
        context.drawText(mc.textRenderer, fpsText, 0, 0, this.textColor, false)
        context.matrices.popMatrix()
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

    override fun getWidth(): Int {
        val text = Text.literal("${mc.currentFps} Fps").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        return mc.textRenderer.getWidth(text)
    }

    override fun getHeight(): Int = mc.textRenderer.fontHeight
    override fun getName(): String = "FPS"
}