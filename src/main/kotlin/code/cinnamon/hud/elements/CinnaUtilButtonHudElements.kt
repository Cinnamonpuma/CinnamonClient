package code.cinnamon.hud.elements

import code.cinnamon.hud.HudElement
import code.cinnamon.gui.CinnamonScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.util.math.MathHelper

private const val DEFAULT_BUTTON_WIDTH = 150
private const val DEFAULT_BUTTON_HEIGHT = 20
private const val BUTTON_PADDING = 2

private fun isInGui(): Boolean {
    val screen = MinecraftClient.getInstance().currentScreen
    return screen is HandledScreen<*>
}

private fun isInHudEditor(): Boolean {
    val screen = MinecraftClient.getInstance().currentScreen
    return screen != null && screen.javaClass.simpleName.contains("HudEditor")
}

/**
 * A custom button renderer for HUD elements that renders on top of everything
 */
class HudButton(
    private var x: Int,
    private var y: Int,
    private val width: Int,
    private val height: Int,
    private val text: Text,
    private val onClick: () -> Unit
) {
    private var isHovered = false
    private var isPressed = false
    private var isFocused = false

    fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun getX(): Int = x
    fun getY(): Int = y
    fun getWidth(): Int = width
    fun getHeight(): Int = height

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }

    fun render(context: DrawContext, mouseX: Double, mouseY: Double, delta: Float) {
        isHovered = isMouseOver(mouseX, mouseY)
        
        // Push matrix to render on top
        val matrices = context.matrices
        matrices.push()
        matrices.translate(0f, 0f, 1000f) // Move to front
        
        // Button background colors
        val backgroundColor = when {
            isPressed -> 0xDD000000.toInt() // Darker when pressed
            isHovered -> 0xDD666666.toInt() // Lighter when hovered
            else -> 0xDD333333.toInt() // Default background
        }
        
        val borderColor = when {
            isFocused -> 0xFFFFFFFF.toInt() // White border when focused
            isHovered -> 0xFFCCCCCC.toInt() // Light gray when hovered
            else -> 0xFF999999.toInt() // Default border
        }

        // Draw button background
        context.fill(x, y, x + width, y + height, backgroundColor)
        
        // Draw button border (draw each side individually for better control)
        context.fill(x, y, x + width, y + 1, borderColor) // Top
        context.fill(x, y + height - 1, x + width, y + height, borderColor) // Bottom
        context.fill(x, y, x + 1, y + height, borderColor) // Left
        context.fill(x + width - 1, y, x + width, y + height, borderColor) // Right
        
        // Draw button text
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textWidth = textRenderer.getWidth(text)
        val textHeight = textRenderer.fontHeight
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - textHeight) / 2
        
        val textColor = if (isHovered) 0xFFFFFF else 0xE0E0E0
        context.drawText(textRenderer, text, textX, textY, textColor, false)
        
        matrices.pop()
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && isMouseOver(mouseX, mouseY)) { // Left click
            isPressed = true
            onClick()
            return true
        }
        return false
    }

    fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0) {
            isPressed = false
            return isMouseOver(mouseX, mouseY)
        }
        return false
    }

    fun mouseMoved(mouseX: Double, mouseY: Double) {
        // Update hover state - handled in render method
    }

    fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return false
    }

    fun setFocused(focused: Boolean) {
        this.isFocused = focused
    }

    fun isFocused(): Boolean = isFocused
}

/**
 * Base class for HUD button elements that can be moved around and clicked
 */
abstract class BaseHudButtonElement(
    initialX: Float, 
    initialY: Float, 
    private val buttonLabel: String,
    private val buttonWidth: Int = DEFAULT_BUTTON_WIDTH,
    private val buttonHeight: Int = DEFAULT_BUTTON_HEIGHT
) : HudElement(initialX, initialY), Element {

    private lateinit var hudButton: HudButton

    init {
        initializeButton()
    }

    private fun initializeButton() {
        val buttonText = Text.literal(buttonLabel)
            .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        hudButton = HudButton(
            this.getX().toInt(),
            this.getY().toInt(),
            buttonWidth,
            buttonHeight,
            buttonText
        ) {
            onButtonClick()
        }
    }

    /**
     * Override this method to define what happens when the button is clicked
     */
    protected open fun onButtonClick() {
        println("$buttonLabel button clicked: ${this.getName()} - (override onButtonClick() for custom behavior)")
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        // Show in HUD editor OR when in GUI
        if (!isInHudEditor() && !isInGui()) return
        
        if (this::hudButton.isInitialized) {
            // Update button position to match HUD element position
            hudButton.setPosition(this.getX().toInt(), this.getY().toInt())
            
            val mc = MinecraftClient.getInstance()
            // Use actual mouse coordinates from MinecraftClient
            val mouseX = mc.mouse.x * mc.window.scaledWidth / mc.window.width
            val mouseY = mc.mouse.y * mc.window.scaledHeight / mc.window.height
            
            hudButton.render(context, mouseX, mouseY, tickDelta)
        }
    }

    override fun getWidth(): Int = if (this::hudButton.isInitialized) hudButton.getWidth() else buttonWidth
    override fun getHeight(): Int = if (this::hudButton.isInitialized) hudButton.getHeight() else buttonHeight

    // Mouse event forwarding with proper coordinate scaling
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!isInHudEditor() && !isInGui()) return
        if (this::hudButton.isInitialized) hudButton.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isInHudEditor() && !isInGui()) return false
        return if (this::hudButton.isInitialized) hudButton.mouseClicked(mouseX, mouseY, button) else false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isInHudEditor() && !isInGui()) return false
        return if (this::hudButton.isInitialized) hudButton.mouseReleased(mouseX, mouseY, button) else false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!isInHudEditor() && !isInGui()) return false
        return if (this::hudButton.isInitialized) hudButton.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = false

    // Focus handling
    override fun setFocused(focused: Boolean) {
        if (this::hudButton.isInitialized) hudButton.setFocused(focused)
    }

    override fun isFocused(): Boolean = if (this::hudButton.isInitialized) hudButton.isFocused() else false

    // Mouse over detection
    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        this::hudButton.isInitialized && hudButton.isMouseOver(mouseX, mouseY)
}

// Concrete implementations of each button type
class CloseButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Close without packet") {
    
    override fun getName(): String = "Close Button"
    
    override fun onButtonClick() {
        if (isInHudEditor()) {
            println("Close button preview clicked in HUD editor")
            return
        }
        println("Close button clicked - closing GUI without sending packet")
        // Add your close logic here
        MinecraftClient.getInstance().currentScreen?.close()
    }
}

class DesyncButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "De-Sync") {
    
    override fun getName(): String = "Desync Button"
    
    override fun onButtonClick() {
        if (isInHudEditor()) {
            println("Desync button preview clicked in HUD editor")
            return
        }
        println("Desync button clicked - triggering desync")
        // Add your desync logic here
    }
}

class SendPacketsButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Send Packets") {
    
    override fun getName(): String = "Send Packets Button"
    
    override fun onButtonClick() {
        if (isInHudEditor()) {
            println("Send Packets button preview clicked in HUD editor")
            return
        }
        println("Send Packets button clicked - sending queued packets")
        // Add your packet sending logic here
    }
}

class DelayPacketsButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Delay Packets") {
    
    override fun getName(): String = "Delay Packets Button"
    
    override fun onButtonClick() {
        if (isInHudEditor()) {
            println("Delay Packets button preview clicked in HUD editor")
            return
        }
        println("Delay Packets button clicked - delaying packet transmission")
        // Add your packet delay logic here
    }
}

class SaveGuiButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Save GUI") {
    
    override fun getName(): String = "Save GUI Button"
    
    override fun onButtonClick() {
        if (isInHudEditor()) {
            println("Save GUI button preview clicked in HUD editor")
            return
        }
        println("Save GUI button clicked - saving current GUI state")
        // Add your GUI save logic here
    }
}

class DisconnectButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Disconnect and send packets") {
    
    override fun getName(): String = "Disconnect Button"
    
    override fun onButtonClick() {
        if (isInHudEditor()) {
            println("Disconnect button preview clicked in HUD editor")
            return
        }
        println("Disconnect button clicked - disconnecting and sending packets")
        // Add your disconnect logic here
        val mc = MinecraftClient.getInstance()
        mc.world?.disconnect()
    }
}

/**
 * Manager class to handle top-level rendering of all HUD buttons
 * Add this to your main HUD rendering system to ensure buttons render on top
 */
object HudButtonManager {
    private val buttons = mutableListOf<BaseHudButtonElement>()
    
    fun registerButton(button: BaseHudButtonElement) {
        buttons.add(button)
    }
    
    fun unregisterButton(button: BaseHudButtonElement) {
        buttons.remove(button)
    }
    
    fun renderAllButtons(context: DrawContext, tickDelta: Float) {
        // Render all buttons with high Z-index to ensure they're on top
        val matrices = context.matrices
        matrices.push()
        matrices.translate(0f, 0f, 2000f) // Even higher Z for absolute top rendering
        
        buttons.forEach { button ->
            if (isInHudEditor() || isInGui()) {
                button.render(context, tickDelta)
            }
        }
        
        matrices.pop()
    }
    
    fun handleMouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isInHudEditor() && !isInGui()) return false
        
        // Check buttons in reverse order (top to bottom)
        for (hudButton in buttons.reversed()) {
            if (hudButton.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }
}