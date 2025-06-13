package code.cinnamon.hud.elements

import code.cinnamon.modules.all.UIUtilsModule
import code.cinnamon.hud.HudElement
import code.cinnamon.gui.CinnamonScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.util.math.MathHelper
import kotlin.math.min
import kotlin.math.max

// Color manipulation utilities
private fun darkenColor(color: Int, factor: Float = 0.7f): Int {
    val alpha = (color shr 24) and 0xFF
    val red = max(0f, min(255f, ((color shr 16) and 0xFF) * factor)).toInt()
    val green = max(0f, min(255f, ((color shr 8) and 0xFF) * factor)).toInt()
    val blue = max(0f, min(255f, (color and 0xFF) * factor)).toInt()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun lightenColor(color: Int, factor: Float = 1.3f): Int {
    val alpha = (color shr 24) and 0xFF
    val red = max(0f, min(255f, ((color shr 16) and 0xFF) * factor)).toInt()
    val green = max(0f, min(255f, ((color shr 8) and 0xFF) * factor)).toInt()
    val blue = max(0f, min(255f, (color and 0xFF) * factor)).toInt()
    return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
}

private fun changeAlpha(color: Int, newAlpha: Int): Int {
    return (newAlpha shl 24) or (color and 0x00FFFFFF)
}

private const val DEFAULT_BUTTON_WIDTH = 150
private const val DEFAULT_BUTTON_HEIGHT = 20
private const val BUTTON_PADDING = 2

/**
 * A custom button renderer for HUD elements that renders on top of everything
 */
class HudButton(
    private var x: Int,
    private var y: Int,
    private val width: Int,
    private val height: Int,
    initialText: Text, // Changed parameter name
    private val onClick: () -> Unit,
    private val ownerElement: HudElement
) {
    var text: Text = initialText // Public mutable property
    private var isHovered = false
    private var isPressed = false
    private var isFocused = false
    var scale: Float = 1.0f

    fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun getX(): Int = x
    fun getY(): Int = y
    fun getWidth(): Int = width
    fun getHeight(): Int = height

    fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        // Check against the top-left corner (x, y) and the scaled dimensions
        return mouseX >= x && mouseX < x + scaledWidth && mouseY >= y && mouseY < y + scaledHeight
    }

    fun render(context: DrawContext, mouseX: Double, mouseY: Double, delta: Float) {
        isHovered = isMouseOver(mouseX, mouseY)
        
        // Push matrix to render on top
        val matrices = context.matrices
        matrices.push()
        matrices.translate(0f, 0f, 2000f) // Increased from 1000f

        val baseBackgroundColor = ownerElement.backgroundColor
        val baseTextColor = ownerElement.textColor

        // Determine finalBackgroundColor
        val finalBackgroundColor: Int
        if (isPressed) {
            finalBackgroundColor = ownerElement.keypressedBackgroundColor.takeIf { it != 0 } ?: darkenColor(baseBackgroundColor, 0.5f)
        } else if (isHovered) {
            finalBackgroundColor = lightenColor(baseBackgroundColor, 1.3f)
        } else {
            finalBackgroundColor = baseBackgroundColor
        }

        // Determine finalTextColor
        val finalTextColor: Int
        if (isPressed) {
            // Use keypressedTextColor if available and non-zero, otherwise fallback to baseTextColor
            finalTextColor = ownerElement.keypressedTextColor.takeIf { it != 0 } ?: baseTextColor
        } else if (isHovered) {
            finalTextColor = lightenColor(baseTextColor, 1.2f) // Use lightened base text color for hover
        } else {
            finalTextColor = baseTextColor
        }
        
        // Determine finalBorderColor
        val finalBorderColor: Int
        if (isFocused) {
            // For focused, use a very light version of the finalTextColor or a distinct color like white
            // Ensuring the alpha is fully opaque for the border.
            finalBorderColor = changeAlpha(lightenColor(finalTextColor, 1.8f), 0xFF)
        } else if (isHovered) {
            // For hovered, use a slightly lighter version of the finalBackgroundColor
            finalBorderColor = changeAlpha(lightenColor(finalBackgroundColor, 1.2f), 0xFF)
        } else {
            // Default border is a slightly darker version of the finalBackgroundColor
            finalBorderColor = changeAlpha(darkenColor(finalBackgroundColor, 0.8f), 0xFF)
        }

        // Draw button background
        context.fill(x, y, x + width, y + height, finalBackgroundColor)
        
        // Draw button border (draw each side individually for better control)
        // Ensure border is opaque by using the alpha from finalBorderColor (which should be set to FF by changeAlpha)
        context.fill(x, y, x + width, y + 1, finalBorderColor) // Top
        context.fill(x, y + height - 1, x + width, y + height, finalBorderColor) // Bottom
        context.fill(x, y, x + 1, y + height, finalBorderColor) // Left
        context.fill(x + width - 1, y, x + width, y + height, finalBorderColor) // Right
        
        // Draw button text
        val textRenderer = MinecraftClient.getInstance().textRenderer
        val textWidth = textRenderer.getWidth(text)
        val textHeight = textRenderer.fontHeight
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - textHeight) / 2
        
        context.drawText(textRenderer, text, textX, textY, finalTextColor, false)
        
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

    private var _hudButton: HudButton? = null
    protected val hudButton: HudButton get() = _hudButton!!
    protected val isHudButtonInitialized: Boolean get() = _hudButton != null

    init {
        initializeButton()
    }

    private fun initializeButton() {
        val buttonText = Text.literal(buttonLabel)
            .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        
        _hudButton = HudButton(
            this.getX().toInt(),
            this.getY().toInt(),
            buttonWidth,
            buttonHeight,
            buttonText,
            { onButtonClick() },
            this // Pass the BaseHudButtonElement instance as the owner
        )
    }

    /**
     * Override this method to define what happens when the button is clicked
     */
    protected open fun onButtonClick() {
        println("$buttonLabel button clicked: ${this.getName()} - (override onButtonClick() for custom behavior)")
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        // At the beginning of BaseHudButtonElement.render method
        val inHudEditor = UIUtilsModule.isInHudEditor()
        val inGui = UIUtilsModule.isInGui()
        // Consider using a logger instance if available, otherwise System.out.println for now.
        println("CinnaUtilButtonHudElements: Attempting to render ${this.getName()}. isInHudEditor: $inHudEditor, isInGui: $inGui, Current Screen: ${MinecraftClient.getInstance().currentScreen}")

        // Show in HUD editor OR when in GUI
        if (!inHudEditor && !inGui) {
            // Add a log here too, to see if it's returning early
            println("CinnaUtilButtonHudElements: Skipping render for ${this.getName()} because not in HUD editor or GUI.")
            return
        }
        
        if (isHudButtonInitialized) {
            // Update button position to match HUD element position
            hudButton.setPosition(this.getX().toInt(), this.getY().toInt())
            hudButton.scale = this.scale // Update hudButton's scale from HudElement's scale

            val mc = MinecraftClient.getInstance()
            // Use actual mouse coordinates from MinecraftClient
            val mouseStateX = mc.mouse.x * mc.window.scaledWidth / mc.window.width
            val mouseStateY = mc.mouse.y * mc.window.scaledHeight / mc.window.height
            
            hudButton.render(context, mouseStateX, mouseStateY, tickDelta)
        }
    }

    override fun getWidth(): Int = if (isHudButtonInitialized) hudButton.getWidth() else buttonWidth
    override fun getHeight(): Int = if (isHudButtonInitialized) hudButton.getHeight() else buttonHeight

    // Mouse event forwarding with proper coordinate scaling
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!UIUtilsModule.isInHudEditor() && !UIUtilsModule.isInGui()) return
        if (isHudButtonInitialized) hudButton.mouseMoved(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!UIUtilsModule.isInHudEditor() && !UIUtilsModule.isInGui()) return false
        return if (isHudButtonInitialized) hudButton.mouseClicked(mouseX, mouseY, button) else false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!UIUtilsModule.isInHudEditor() && !UIUtilsModule.isInGui()) return false
        return if (isHudButtonInitialized) hudButton.mouseReleased(mouseX, mouseY, button) else false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!UIUtilsModule.isInHudEditor() && !UIUtilsModule.isInGui()) return false
        return if (isHudButtonInitialized) hudButton.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = false

    // Focus handling
    override fun setFocused(focused: Boolean) {
        if (isHudButtonInitialized) hudButton.setFocused(focused)
    }

    override fun isFocused(): Boolean = if (isHudButtonInitialized) hudButton.isFocused() else false

    // Mouse over detection
    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean =
        isHudButtonInitialized && hudButton.isMouseOver(mouseX, mouseY)
}

// Concrete implementations of each button type
class CloseButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Close without packet") {
    
    override fun getName(): String = "Close Button"
    
    override fun onButtonClick() {
        if (UIUtilsModule.isInHudEditor()) {
            println("Close button preview clicked in HUD editor")
            return
        }
        println("Close button clicked - closing GUI without sending packet")
        UIUtilsModule.closeWithoutPacket()
    }
}

class DesyncButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "De-Sync") {
    
    override fun getName(): String = "Desync Button"
    
    override fun onButtonClick() {
        if (UIUtilsModule.isInHudEditor()) {
            println("Desync button preview clicked in HUD editor")
            return
        }
        println("Desync button clicked - triggering desync")
        UIUtilsModule.desyncScreen()
    }
}

class SendPacketsButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Send Packets") {
    
    override fun getName(): String = "Send Packets Button"
    
    override fun onButtonClick() {
        if (isHudButtonInitialized) {
            if (UIUtilsModule.isInHudEditor()) {
                println("Send Packets button preview clicked in HUD editor (hudButton initialized)")
                // In the HUD editor, just toggle for visual feedback, don't change actual setting
                val currentText = hudButton.text.string // Safely access text
                val newStatusText = if (currentText.contains("ON")) "OFF" else "ON"
                hudButton.text = Text.literal("Send Packets: $newStatusText")
                                  .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
            } else {
                println("Send Packets button clicked (hudButton initialized)")
                UIUtilsModule.toggleSendPackets() // Core action
                // Update text for immediate feedback
                val status = if (UIUtilsModule.sendUIPackets) "ON" else "OFF"
                hudButton.text = Text.literal("Send Packets: $status")
                                  .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
            }
        } else {
            // hudButton not initialized
            println("[WARN] SendPacketsButtonHudElement.onButtonClick: hudButton not initialized")
            if (!UIUtilsModule.isInHudEditor()) {
                println("Send Packets button clicked (hudButton not initialized, performing core action)")
                UIUtilsModule.toggleSendPackets() // Perform core action anyway if not in editor
            } else {
                println("Send Packets button preview clicked in HUD editor (hudButton not initialized, no action)")
            }
        }
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (isHudButtonInitialized) { // Check if hudButton is initialized
            // Update status dynamically during render, especially if not in editor.
            // In editor, click updates it. Otherwise, it reflects real state.
            if (!UIUtilsModule.isInHudEditor()) {
                 val status = if (UIUtilsModule.sendUIPackets) "ON" else "OFF"
                 hudButton.text = Text.literal("Send Packets: $status")
                                   .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
            } else {
                // Ensure initial text in editor mode if not yet set by a click
                if (!hudButton.text.string.startsWith("Send Packets:")) {
                     hudButton.text = Text.literal("Send Packets: OFF")
                                   .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
                }
            }
        }
        super.render(context, tickDelta) // Call super to ensure original render logic including visibility checks
    }
}

class DelayPacketsButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Delay Packets") {
    
    override fun getName(): String = "Delay Packets Button"
    
    override fun onButtonClick() {
        if (isHudButtonInitialized) {
            if (UIUtilsModule.isInHudEditor()) {
                println("Delay Packets button preview clicked in HUD editor (hudButton initialized)")
                // In the HUD editor, just toggle for visual feedback, don't change actual setting
                val currentText = hudButton.text.string // Safely access text
                val newStatusText = if (currentText.contains("ON")) "OFF" else "ON"
                hudButton.text = Text.literal("Delay Packets: $newStatusText")
                                  .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
            } else {
                println("Delay Packets button clicked (hudButton initialized)")
                UIUtilsModule.toggleDelayPackets() // Core action
                // Update text for immediate feedback
                val status = if (UIUtilsModule.delayUIPackets) "ON" else "OFF"
                hudButton.text = Text.literal("Delay Packets: $status")
                                  .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
            }
        } else {
            // hudButton not initialized
            println("[WARN] DelayPacketsButtonHudElement.onButtonClick: hudButton not initialized")
            if (!UIUtilsModule.isInHudEditor()) {
                println("Delay Packets button clicked (hudButton not initialized, performing core action)")
                UIUtilsModule.toggleDelayPackets() // Perform core action anyway if not in editor
            } else {
                println("Delay Packets button preview clicked in HUD editor (hudButton not initialized, no action)")
            }
        }
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (isHudButtonInitialized) { // Check if hudButton is initialized
            // Update status dynamically during render, especially if not in editor.
            // In editor, click updates it. Otherwise, it reflects real state.
            if (!UIUtilsModule.isInHudEditor()) {
                val status = if (UIUtilsModule.delayUIPackets) "ON" else "OFF"
                hudButton.text = Text.literal("Delay Packets: $status")
                                  .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
            } else {
                // Ensure initial text in editor mode if not yet set by a click
                 if (!hudButton.text.string.startsWith("Delay Packets:")) {
                     hudButton.text = Text.literal("Delay Packets: OFF")
                                   .fillStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
                }
            }
        }
        super.render(context, tickDelta) // Call super to ensure original render logic
    }
}

class SaveGuiButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Save GUI") {
    
    override fun getName(): String = "Save GUI Button"
    
    override fun onButtonClick() {
        if (UIUtilsModule.isInHudEditor()) {
            println("Save GUI button preview clicked in HUD editor")
            return
        }
        println("Save GUI button clicked - saving current GUI state")
        UIUtilsModule.saveCurrentGUI()
    }
}

class DisconnectButtonHudElement(initialX: Float, initialY: Float) :
    BaseHudButtonElement(initialX, initialY, "Disconnect and send packets") {
    
    override fun getName(): String = "Disconnect Button"
    
    override fun onButtonClick() {
        if (UIUtilsModule.isInHudEditor()) {
            println("Disconnect button preview clicked in HUD editor")
            return
        }
        println("Disconnect button clicked - disconnecting and sending packets")
        UIUtilsModule.disconnectAndSendPackets()
    }
}