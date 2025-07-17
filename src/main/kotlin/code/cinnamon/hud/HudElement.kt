package code.cinnamon.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.modules.Setting

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

    var textColor: Int = 0xFFFFFFFF.toInt()
    var backgroundColor: Int = 0x00000000.toInt()
    var textShadowEnabled: Boolean = false

    val settings = mutableListOf<Setting<*>>()

    private var isDragging: Boolean = false
    private var dragOffsetX: Float = 0f
    private var dragOffsetY: Float = 0f

    private var currentMouseX: Double = 0.0
    private var currentMouseY: Double = 0.0

    abstract fun renderElement(context: DrawContext, tickDelta: Float)

    abstract fun getWidth(): Int
    abstract fun getHeight(): Int
    abstract fun getName(): String

    open fun isMouseOver(scaledMouseX: Double, scaledMouseY: Double): Boolean {
        val elementScaledWidth = getWidth() * this.scale
        val elementScaledHeight = getHeight() * this.scale
        return scaledMouseX >= _x && scaledMouseX <= _x + elementScaledWidth &&
                scaledMouseY >= _y && scaledMouseY <= _y + elementScaledHeight
    }

    fun startDragging(scaledMouseX: Double, scaledMouseY: Double) {
        isDragging = true
        dragOffsetX = (scaledMouseX - _x).toFloat()
        dragOffsetY = (scaledMouseY - _y).toFloat()
    }

    fun updateDragging(scaledMouseX: Double, scaledMouseY: Double, screenScaledWidth: Int, screenScaledHeight: Int) {
        if (isDragging) {
            _x = (scaledMouseX - dragOffsetX).toFloat()
            _y = (scaledMouseY - dragOffsetY).toFloat()

            val elementScaledWidth = getWidth() * this.scale
            val elementScaledHeight = getHeight() * this.scale

            _x = max(0f, min(_x, screenScaledWidth.toFloat() - elementScaledWidth))
            _y = max(0f, min(_y, screenScaledHeight.toFloat() - elementScaledHeight))
        }
    }

    fun stopDragging() {
        isDragging = false
    }

    fun getX(): Float = _x
    fun getY(): Float = _y
    fun setX(newX: Float) { _x = newX }
    fun setY(newY: Float) { _y = newY }


    fun setCurrentMousePosition(mouseX: Double, mouseY: Double) {
        currentMouseX = mouseX
        currentMouseY = mouseY
    }

    fun getCurrentMouseX(): Double = currentMouseX
    fun getCurrentMouseY(): Double = currentMouseY


    fun isCurrentlyHovered(): Boolean {
        return isMouseOver(currentMouseX, currentMouseY)
    }
}