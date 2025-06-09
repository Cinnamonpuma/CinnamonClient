package code.cinnamon.hud

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import kotlin.math.max
import kotlin.math.min

abstract class HudElement(
    protected var x: Float,
    protected var y: Float
) {
    var scale: Float = 1.0f
        set(value) {
            field = max(0.5f, min(3.0f, value))
        }
    
    var isEnabled: Boolean = true
    private var isDragging: Boolean = false
    private var dragOffsetX: Float = 0f
    private var dragOffsetY: Float = 0f
    
    abstract fun render(context: DrawContext, tickDelta: Float)
    abstract fun getWidth(): Int
    abstract fun getHeight(): Int
    abstract fun getName(): String
    
    fun renderBackground(context: DrawContext) {
        if (HudManager.isEditMode) {
            context.fill(
                x.toInt(),
                y.toInt(),
                (x + getWidth() * scale).toInt(),
                (y + getHeight() * scale).toInt(),
                0x80000000.toInt()
            )
        }
    }
    
    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX <= x + getWidth() * scale &&
               mouseY >= y && mouseY <= y + getHeight() * scale
    }
    
    fun startDragging(mouseX: Double, mouseY: Double) {
        isDragging = true
        dragOffsetX = (mouseX - x).toFloat()
        dragOffsetY = (mouseY - y).toFloat()
    }
    
    fun updateDragging(mouseX: Double, mouseY: Double) {
        if (isDragging) {
            x = (mouseX - dragOffsetX).toFloat()
            y = (mouseY - dragOffsetY).toFloat()
            
            val mc = MinecraftClient.getInstance()
            x = max(0f, min(x, mc.window.scaledWidth - getWidth() * scale))
            y = max(0f, min(y, mc.window.scaledHeight - getHeight() * scale))
        }
    }
    
    fun stopDragging() {
        isDragging = false
    }
    
    fun getX(): Float = x
    fun getY(): Float = y
    fun setX(x: Float) { this.x = x }
    fun setY(y: Float) { this.y = y }
}