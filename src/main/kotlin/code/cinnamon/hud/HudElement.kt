package code.cinnamon.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min
import code.cinnamon.gui.CinnamonScreen // For TARGET_SCALE_FACTOR

abstract class HudElement(
    initialX: Float, // Represents position in the SCALED coordinate system of CinnamonScreen
    initialY: Float  // Represents position in the SCALED coordinate system of CinnamonScreen
) {
    private var _x: Float = initialX // Stored in SCALED coordinates
    private var _y: Float = initialY // Stored in SCALED coordinates

    var scale: Float = 1.0f // Element's individual scale, applied on top of global CinnamonScreen scale
        set(value) {
            field = max(0.5f, min(3.0f, value))
        }

    var isEnabled: Boolean = true

    var textColor: Int = 0xFFFFFFFF.toInt()
    var backgroundColor: Int = 0x00000000.toInt()
    var textShadowEnabled: Boolean = false

    private var isDragging: Boolean = false
    // Offset from the element's top-left (_x, _y) to the mouse click point, in SCALED coordinates
    private var dragOffsetX: Float = 0f
    private var dragOffsetY: Float = 0f

    /**
     * Renders the element's content.
     * The DrawContext is already globally scaled by CinnamonScreen.
     * This method should translate to the element's scaled (_x, _y) position,
     * then apply its individual `this.scale` factor, draw its content (using base width/height),
     * and then restore the matrix.
     */
    abstract fun renderElement(context: DrawContext, tickDelta: Float)

    /**
     * Returns the BASE (unscaled by this.scale or global scale) width of the element.
     * Dimensions are used for layout and interaction calculations.
     */
    abstract fun getWidth(): Int // Base width
    abstract fun getHeight(): Int // Base height
    abstract fun getName(): String

    /**
     * Checks if the given SCALED mouse coordinates (from CinnamonScreen's scaled system)
     * are over this element.
     */
    open fun isMouseOver(scaledMouseX: Double, scaledMouseY: Double): Boolean {
        // _x and _y are in the CinnamonScreen scaled system.
        // getWidth() and getHeight() are base dimensions.
        // this.scale is the element's individual scale factor.
        // The effective width/height in the CinnamonScreen scaled system is getWidth() * this.scale.
        val elementScaledWidth = getWidth() * this.scale
        val elementScaledHeight = getHeight() * this.scale
        return scaledMouseX >= _x && scaledMouseX <= _x + elementScaledWidth &&
                scaledMouseY >= _y && scaledMouseY <= _y + elementScaledHeight
    }

    /**
     * Call when dragging starts. scaledMouseX and scaledMouseY are in CinnamonScreen's scaled system.
     */
    fun startDragging(scaledMouseX: Double, scaledMouseY: Double) {
        isDragging = true
        // Calculate offset from the element's top-left (_x, _y) to the mouse click point.
        // All coordinates are already in the same scaled system.
        dragOffsetX = (scaledMouseX - _x).toFloat()
        dragOffsetY = (scaledMouseY - _y).toFloat()
    }

    /**
     * Call to update position during dragging.
     * scaledMouseX, scaledMouseY are in CinnamonScreen's scaled system.
     * screenScaledWidth, screenScaledHeight are the total dimensions of the parent scaled viewport (e.g., HudScreen.guiWidth).
     */
    fun updateDragging(scaledMouseX: Double, scaledMouseY: Double, screenScaledWidth: Int, screenScaledHeight: Int) {
        if (isDragging) {
            _x = (scaledMouseX - dragOffsetX).toFloat()
            _y = (scaledMouseY - dragOffsetY).toFloat()

            val elementScaledWidth = getWidth() * this.scale
            val elementScaledHeight = getHeight() * this.scale

            // Clamp _x and _y (which are in scaled coords) against the passed scaled screen boundaries
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
}