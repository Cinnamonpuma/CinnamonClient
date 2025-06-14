package code.cinnamon.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min

abstract class HudElement(
    initialX: Float,
    initialY: Float
) {
    private var _x: Float = initialX
    private var _y: Float = initialY

    var scale: Float = 1.0f
        set(value) {
            field = max(0.5f, min(3.0f, value))
        }

    var isEnabled: Boolean = true

    var textColor: Int = 0xFFFFFF 
    var backgroundColor: Int = 0x80000000.toInt() 
    var textShadowEnabled: Boolean = true

    private var isDragging: Boolean = false
    private var dragOffsetX: Float = 0f
    private var dragOffsetY: Float = 0f

    abstract fun render(context: DrawContext, tickDelta: Float)
    abstract fun getWidth(): Int
    abstract fun getHeight(): Int
    abstract fun getName(): String

    open fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= _x && mouseX <= _x + getWidth() * scale &&
               mouseY >= _y && mouseY <= _y + getHeight() * scale
    }

    fun startDragging(mouseX: Double, mouseY: Double) {
        isDragging = true
        dragOffsetX = (mouseX - _x).toFloat()
        dragOffsetY = (mouseY - _y).toFloat()
    }

    fun updateDragging(mouseX: Double, mouseY: Double) {
        if (isDragging) {
            _x = (mouseX - dragOffsetX).toFloat()
            _y = (mouseY - dragOffsetY).toFloat()

            val mc = MinecraftClient.getInstance()
            _x = max(0f, min(_x, mc.window.scaledWidth - getWidth() * scale))
            _y = max(0f, min(_y, mc.window.scaledHeight - getHeight() * scale))
        }
    }

    fun stopDragging() {
        isDragging = false
    }

    fun getX(): Float = _x
    fun getY(): Float = _y
    fun setX(newX: Float) { _x = newX }
    fun setY(newY: Float) { _y = newY }
}