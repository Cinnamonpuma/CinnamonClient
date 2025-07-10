package code.cinnamon.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min
import code.cinnamon.gui.CinnamonScreen // For TARGET_SCALE_FACTOR

abstract class HudElement(
    initialX: Float, // These will now be in CinnamonScreen's scaled coordinate system
    initialY: Float  // These will now be in CinnamonScreen's scaled coordinate system
) {
    private var _x: Float = initialX
    private var _y: Float = initialY

    var scale: Float = 1.0f // This is the element's individual scale factor
        set(value) {
            field = max(0.5f, min(3.0f, value))
        }

    var isEnabled: Boolean = true

    var textColor: Int = 0xFFFFFFFF.toInt()
    var backgroundColor: Int = 0x00000000.toInt()
    var textShadowEnabled: Boolean = false

    private var isDragging: Boolean = false
    private var dragOffsetX: Float = 0f // Offset within the element, in scaled coordinates
    private var dragOffsetY: Float = 0f // Offset within the element, in scaled coordinates

    /**
     * Renders the element's content. Assumes matrix is already translated to (_x, _y)
     * and scaled by the element's own `scale` factor, on top of any global scaling.
     * The implementation should draw its content starting from (0,0) using its base width/height.
     */
    abstract fun renderElement(context: DrawContext, tickDelta: Float)

    // getWidth and getHeight should return the BASE, UNSCALED dimensions of the element.
    // The actual displayed size will be getWidth() * scale * globalScaleRatio.
    abstract fun getWidth(): Int
    abstract fun getHeight(): Int
    abstract fun getName(): String

    /**
     * Checks if the given mouse coordinates (assumed to be in CinnamonScreen's scaled system)
     * are over this element.
     */
    open fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        // _x and _y are already in the CinnamonScreen scaled system.
        // getWidth() and getHeight() are base dimensions.
        // element.scale is the individual scale factor.
        // The effective width/height in the CinnamonScreen scaled system is getWidth() * scale.
        return mouseX >= _x && mouseX <= _x + getWidth() * scale &&
                mouseY >= _y && mouseY <= _y + getHeight() * scale
    }

    /**
     * Call when dragging starts. mouseX and mouseY are in CinnamonScreen's scaled system.
     */
    fun startDragging(mouseX: Double, mouseY: Double) {
        isDragging = true
        // dragOffsetX/Y is the point *within* the element (in its scaled dimensions) where the click occurred.
        // However, since _x and _y are top-left, it's simpler to store the offset from mouse to top-left.
        dragOffsetX = (mouseX - _x).toFloat()
        dragOffsetY = (mouseY - _y).toFloat()
    }

    /**
     * Call to update position during dragging. mouseX and mouseY are in CinnamonScreen's scaled system.
     */
    fun updateDragging(mouseX: Double, mouseY: Double) {
        if (isDragging) {
            _x = (mouseX - dragOffsetX).toFloat()
            _y = (mouseY - dragOffsetY).toFloat()

            val mc = MinecraftClient.getInstance()
            // Calculate effective screen dimensions in the CinnamonScreen scaled system
            val currentGuiScale = mc.window.scaleFactor.toFloat()
            val globalScaleRatio = CinnamonScreen.TARGET_SCALE_FACTOR / currentGuiScale

            // This check is tricky. mc.window.width is unscaled pixels.
            // mc.window.scaledWidth is actual screen pixels for UI.
            // We need the width of the screen if it were scaled by TARGET_SCALE_FACTOR.
            // This is what CinnamonScreen.getEffectiveWidth() provides.

            // Simplified: Use CinnamonScreen's effective dimensions for clamping
            // This requires HudElement to somehow access these, or they are passed in.
            // For now, let's assume a utility method or direct access for simplicity of this change.
            // This is a conceptual placeholder for how CinnamonScreen calculates its bounds.
            val effectiveScreenWidth = mc.window.width * currentGuiScale / CinnamonScreen.TARGET_SCALE_FACTOR
            val effectiveScreenHeight = mc.window.height * currentGuiScale / CinnamonScreen.TARGET_SCALE_FACTOR

            // Clamp _x and _y (which are in scaled coords) against the effective scaled screen boundaries
            _x = max(0f, min(_x, effectiveScreenWidth - getWidth() * scale))
            _y = max(0f, min(_y, effectiveScreenHeight - getHeight() * scale))
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