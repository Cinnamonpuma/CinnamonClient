package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.max 

class CoordinatesHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private val cornerRadius = 6
    private val lineSpacing = 2 

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        val player = mc.player ?: return
        val pos = player.pos

        val xText = Text.literal(String.format("X: %.1f", pos.x)).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val yText = Text.literal(String.format("Y: %.1f", pos.y)).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val zText = Text.literal(String.format("Z: %.1f", pos.z)).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

        val scaledX = (getX() / scale).toInt()
        val scaledY = (getY() / scale).toInt()
        // val width = getWidth() // Available via this.getWidth()
        // val height = getHeight() // Available via this.getHeight()
        val padding = 2 // This element uses a specific padding for its background

        if (backgroundColor != 0) {
            // Add scaledX and scaledY to the background drawing coordinates
            drawRoundedBackground(context, scaledX - padding, scaledY - padding, getWidth() + padding * 2, getHeight() + padding * 2, this.backgroundColor)
        }

        val relativeTextYOffset = 0

        // Add scaledX and scaledY to all text drawing coordinates
        if (this.textShadowEnabled) {
            context.drawText(mc.textRenderer, xText, scaledX + 1, scaledY + relativeTextYOffset + 1, 0x40000000, false)
            context.drawText(mc.textRenderer, yText, scaledX + 1, scaledY + relativeTextYOffset + mc.textRenderer.fontHeight + lineSpacing + 1, 0x40000000, false)
            context.drawText(mc.textRenderer, zText, scaledX + 1, scaledY + relativeTextYOffset + (mc.textRenderer.fontHeight + lineSpacing) * 2 + 1, 0x40000000, false)
        }
        context.drawText(mc.textRenderer, xText, scaledX, scaledY + relativeTextYOffset, this.textColor, false)
        context.drawText(mc.textRenderer, yText, scaledX, scaledY + relativeTextYOffset + mc.textRenderer.fontHeight + lineSpacing, this.textColor, false)
        context.drawText(mc.textRenderer, zText, scaledX, scaledY + relativeTextYOffset + (mc.textRenderer.fontHeight + lineSpacing) * 2, this.textColor, false)
    }

    private fun drawRoundedBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, backgroundColor: Int) {
        // Pass scaled coordinates directly to drawRoundedRect
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
                        0 -> { pixelX = x + (radius - 1 - dx); pixelY = y + (radius - 1 - dy) }
                        1 -> { pixelX = x + dx; pixelY = y + (radius - 1 - dy) }
                        2 -> { pixelX = x + (radius - 1 - dx); pixelY = y + dy }
                        3 -> { pixelX = x + dx; pixelY = y + dy }
                        else -> continue
                    }
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }

    override fun getWidth(): Int {
        val player = mc.player ?: return 0
        val pos = player.pos
        val xText = Text.literal(String.format("X: %.1f", pos.x)).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val yText = Text.literal(String.format("Y: %.1f", pos.y)).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val zText = Text.literal(String.format("Z: %.1f", pos.z)).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        
        return maxOf(mc.textRenderer.getWidth(xText), mc.textRenderer.getWidth(yText), mc.textRenderer.getWidth(zText))
    }

    override fun getHeight(): Int {
        return (mc.textRenderer.fontHeight * 3) + (lineSpacing * 2) 
    }

    override fun getName(): String = "Coordinates"
}
