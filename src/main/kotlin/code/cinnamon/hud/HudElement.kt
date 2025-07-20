package code.cinnamon.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.modules.BooleanSetting
import code.cinnamon.modules.ColorSetting
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

    private val textColorSetting = ColorSetting("Text Color", 0xFFFFFFFF.toInt())
    private val backgroundColorSetting = ColorSetting("Background Color", 0x00000000.toInt())
    private val textShadowEnabledSetting = BooleanSetting("Text Shadow", false)

    val settings = mutableListOf<Setting<*>>()

    init {
        settings.add(textColorSetting)
        settings.add(backgroundColorSetting)
        settings.add(textShadowEnabledSetting)
    }

    var textColor: Int
        get() = textColorSetting.value
        set(value) { textColorSetting.value = value }
    var backgroundColor: Int
        get() = backgroundColorSetting.value
        set(value) { backgroundColorSetting.value = value }
    var textShadowEnabled: Boolean
        get() = textShadowEnabledSetting.value
        set(value) { textShadowEnabledSetting.value = value }

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