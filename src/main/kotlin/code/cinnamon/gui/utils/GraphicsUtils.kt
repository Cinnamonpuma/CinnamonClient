package code.cinnamon.gui.utils

import net.minecraft.client.gui.DrawContext
import kotlin.math.sqrt

object GraphicsUtils {

    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    private fun drawPixel(context: DrawContext, x: Int, y: Int, color: Int) {
        context.fill(x, y, x + 1, y + 1, color)
    }

    private fun drawFilledSingleCorner(context: DrawContext, cornerX: Int, cornerY: Int, radius: Int, color: Int, type: Corner) {
        if (radius <= 0) return
        for (i in 0 until radius) {
            for (j in 0 until radius) {
                val dist = sqrt((i * i + j * j).toDouble())
                if (dist <= radius - 0.5f) { 
                    val pixelX: Int
                    val pixelY: Int
                    when (type) {
                        Corner.TOP_LEFT -> {
                            pixelX = cornerX + radius - 1 - i
                            pixelY = cornerY + radius - 1 - j
                        }
                        Corner.TOP_RIGHT -> {
                            pixelX = cornerX + i
                            pixelY = cornerY + radius - 1 - j
                        }
                        Corner.BOTTOM_LEFT -> {
                            pixelX = cornerX + radius - 1 - i
                            pixelY = cornerY + j
                        }
                        Corner.BOTTOM_RIGHT -> {
                            pixelX = cornerX + i
                            pixelY = cornerY + j
                        }
                    }
                    drawPixel(context, pixelX, pixelY, color)
                }
            }
        }
    }

    private fun drawSingleCornerBorder(context: DrawContext, cornerX: Int, cornerY: Int, radius: Int, color: Int, type: Corner) {
        if (radius <= 0) return
        for (i in 0 until radius) {
            for (j in 0 until radius) {
                val dist = sqrt((i * i + j * j).toDouble())
                if (dist >= radius - 1.5f && dist <= radius - 0.5f) { 
                    val pixelX: Int
                    val pixelY: Int
                    when (type) {
                        Corner.TOP_LEFT -> {
                            pixelX = cornerX + radius - 1 - i
                            pixelY = cornerY + radius - 1 - j
                        }
                        Corner.TOP_RIGHT -> {
                            pixelX = cornerX + i
                            pixelY = cornerY + radius - 1 - j
                        }
                        Corner.BOTTOM_LEFT -> {
                            pixelX = cornerX + radius - 1 - i
                            pixelY = cornerY + j
                        }
                        Corner.BOTTOM_RIGHT -> {
                            pixelX = cornerX + i
                            pixelY = cornerY + j
                        }
                    }
                    drawPixel(context, pixelX, pixelY, color)
                }
            }
        }
    }

    fun drawFilledRoundedRect(context: DrawContext, xF: Float, yF: Float, widthF: Float, heightF: Float, radiusF: Float, color: Int) {
        val x = xF.toInt()
        val y = yF.toInt()
        val width = widthF.toInt()
        val height = heightF.toInt()
        var r = radiusF.toInt() 

        if (width <= 0 || height <= 0) return
        r = r.coerceAtMost(width / 2).coerceAtMost(height / 2).coerceAtLeast(0)

        if (r == 0) {
            context.fill(x, y, x + width, y + height, color)
            return
        }

        
        context.fill(x + r, y + r, x + width - r, y + height - r, color)
        
        
        context.fill(x + r, y, x + width - r, y + r, color)
        
        
        context.fill(x + r, y + height - r, x + width - r, y + height, color)
        
        
        context.fill(x, y + r, x + r, y + height - r, color)
        
        
        context.fill(x + width - r, y + r, x + width, y + height - r, color)

        
        drawFilledSingleCorner(context, x, y, r, color, Corner.TOP_LEFT)
        drawFilledSingleCorner(context, x + width - r, y, r, color, Corner.TOP_RIGHT)
        drawFilledSingleCorner(context, x, y + height - r, r, color, Corner.BOTTOM_LEFT)
        drawFilledSingleCorner(context, x + width - r, y + height - r, r, color, Corner.BOTTOM_RIGHT)
    }

    fun drawRoundedRectBorder(context: DrawContext, xF: Float, yF: Float, widthF: Float, heightF: Float, radiusF: Float, color: Int) {
        val x = xF.toInt()
        val y = yF.toInt()
        val width = widthF.toInt()
        val height = heightF.toInt()
        var radius = radiusF.toInt()

        if (width <= 0 || height <= 0) return
        radius = radius.coerceAtMost(width / 2).coerceAtMost(height / 2).coerceAtLeast(0)

        if (radius == 0) { 
            context.fill(x, y, x + width, y + 1, color) 
            context.fill(x, y + height - 1, x + width, y + height, color) 
            context.fill(x, y + 1, x + 1, y + height - 1, color) 
            context.fill(x + width - 1, y + 1, x + width, y + height - 1, color)
            return
        }
        
        context.fill(x + radius, y, x + width - radius, y + 1, color) 
        context.fill(x + radius, y + height - 1, x + width - radius, y + height, color) 
        context.fill(x, y + radius, x + 1, y + height - radius, color) 
        context.fill(x + width - 1, y + radius, x + width, y + height - radius, color) 

        drawSingleCornerBorder(context, x, y, radius, color, Corner.TOP_LEFT)
        drawSingleCornerBorder(context, x + width - radius, y, radius, color, Corner.TOP_RIGHT)
        drawSingleCornerBorder(context, x, y + height - radius, radius, color, Corner.BOTTOM_LEFT)
        drawSingleCornerBorder(context, x + width - radius, y + height - radius, radius, color, Corner.BOTTOM_RIGHT)
    }
}
