package code.cinnamon.hud.elements

import code.cinnamon.hud.HudElement
import net.minecraft.client.gui.DrawContext
import code.cinnamon.gui.components.CinnamonButton
import net.minecraft.text.Text
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element

// Standard dimensions for these buttons
private const val DEFAULT_BUTTON_WIDTH = 150
private const val DEFAULT_BUTTON_HEIGHT = 20

class CloseButtonHudElement(initialX: Float, initialY: Float) :
    HudElement(initialX, initialY), Element {
    lateinit var button: CinnamonButton

    init {
        val buttonText = Text.literal("Close without packet")
        val clickHandler: (Double, Double) -> Unit = { _, _ ->
            println("Close button clicked: ${this.getName()} - Attempting to close screen.")
            // MinecraftClient.getInstance().setScreen(null)
        }
        button = CinnamonButton(
            this.getX().toInt(), this.getY().toInt(),
            DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT,
            buttonText,
            onClick = clickHandler
        )
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (this::button.isInitialized) {
            button.setX(this.getX().toInt())
            button.setY(this.getY().toInt())
            val mc = MinecraftClient.getInstance()
            button.render(context, mc.mouse.x.toInt(), mc.mouse.y.toInt(), tickDelta)
        }
    }

    override fun getWidth(): Int = if (this::button.isInitialized) button.getWidth() else DEFAULT_BUTTON_WIDTH
    override fun getHeight(): Int = if (this::button.isInitialized) button.getHeight() else DEFAULT_BUTTON_HEIGHT
    override fun getName(): String = "Close Button"

    override fun mouseMoved(mouseX: Double, mouseY: Double) { if (this::button.isInitialized) button.mouseMoved(mouseX, mouseY) }
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseClicked(mouseX, mouseY, button) else false
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseReleased(mouseX, mouseY, button) else false
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = if (this::button.isInitialized) this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = if (this::button.isInitialized) this.button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) else false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyPressed(keyCode, scanCode, modifiers) else false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyReleased(keyCode, scanCode, modifiers) else false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = if (this::button.isInitialized) button.charTyped(chr, modifiers) else false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return if (this::button.isInitialized) button.isMouseOver(mouseX, mouseY)
        else super<HudElement>.isMouseOver(mouseX, mouseY)
    }

    override fun setFocused(focused: Boolean) { if (this::button.isInitialized) button.setFocused(focused) }
    override fun isFocused(): Boolean = if (this::button.isInitialized) button.isFocused() else false
}

class DesyncButtonHudElement(initialX: Float, initialY: Float) :
    HudElement(initialX, initialY), Element {
    lateinit var button: CinnamonButton

    init {
        val buttonText = Text.literal("De-Sync")
        val clickHandler: (Double, Double) -> Unit = { _, _ ->
            println("De-Sync button clicked: ${this.getName()} - Triggering de-sync (placeholder).")
        }
        button = CinnamonButton(
            this.getX().toInt(), this.getY().toInt(),
            DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT,
            buttonText,
            onClick = clickHandler
        )
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (this::button.isInitialized) {
            button.setX(this.getX().toInt())
            button.setY(this.getY().toInt())
            val mc = MinecraftClient.getInstance()
            button.render(context, mc.mouse.x.toInt(), mc.mouse.y.toInt(), tickDelta)
        }
    }

    override fun getWidth(): Int = if (this::button.isInitialized) button.getWidth() else DEFAULT_BUTTON_WIDTH
    override fun getHeight(): Int = if (this::button.isInitialized) button.getHeight() else DEFAULT_BUTTON_HEIGHT
    override fun getName(): String = "Desync Button"

    override fun mouseMoved(mouseX: Double, mouseY: Double) { if (this::button.isInitialized) button.mouseMoved(mouseX, mouseY) }
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseClicked(mouseX, mouseY, button) else false
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseReleased(mouseX, mouseY, button) else false
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = if (this::button.isInitialized) this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = if (this::button.isInitialized) this.button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) else false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyPressed(keyCode, scanCode, modifiers) else false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyReleased(keyCode, scanCode, modifiers) else false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = if (this::button.isInitialized) button.charTyped(chr, modifiers) else false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return if (this::button.isInitialized) button.isMouseOver(mouseX, mouseY)
        else super<HudElement>.isMouseOver(mouseX, mouseY)
    }

    override fun setFocused(focused: Boolean) { if (this::button.isInitialized) button.setFocused(focused) }
    override fun isFocused(): Boolean = if (this::button.isInitialized) button.isFocused() else false
}

class SendPacketsButtonHudElement(initialX: Float, initialY: Float) :
    HudElement(initialX, initialY), Element {
    private var sendPacketsEnabled = true
    lateinit var button: CinnamonButton

    init {
        recreateButton()
    }

    private fun recreateButton() {
        val buttonText = Text.literal("Send packets: $sendPacketsEnabled")
        val currentX = getX().toInt()
        val currentY = getY().toInt()
        val currentWidth = DEFAULT_BUTTON_WIDTH
        val currentHeight = DEFAULT_BUTTON_HEIGHT

        val clickHandler: (Double, Double) -> Unit = { _, _ ->
            sendPacketsEnabled = !sendPacketsEnabled
            recreateButton()
            println("Send Packets state: $sendPacketsEnabled for ${this.getName()}")
        }

        button = CinnamonButton(
            currentX, currentY,
            currentWidth, currentHeight,
            buttonText,
            onClick = clickHandler
        )
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (this::button.isInitialized) {
            button.setX(this.getX().toInt())
            button.setY(this.getY().toInt())
            val mc = MinecraftClient.getInstance()
            button.render(context, mc.mouse.x.toInt(), mc.mouse.y.toInt(), tickDelta)
        }
    }

    override fun getWidth(): Int = if (this::button.isInitialized) button.getWidth() else DEFAULT_BUTTON_WIDTH
    override fun getHeight(): Int = if (this::button.isInitialized) button.getHeight() else DEFAULT_BUTTON_HEIGHT
    override fun getName(): String = "Send Packets Button"

    override fun mouseMoved(mouseX: Double, mouseY: Double) { if (this::button.isInitialized) button.mouseMoved(mouseX, mouseY) }
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseClicked(mouseX, mouseY, button) else false
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseReleased(mouseX, mouseY, button) else false
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = if (this::button.isInitialized) this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = if (this::button.isInitialized) this.button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) else false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyPressed(keyCode, scanCode, modifiers) else false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyReleased(keyCode, scanCode, modifiers) else false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = if (this::button.isInitialized) button.charTyped(chr, modifiers) else false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return if (this::button.isInitialized) button.isMouseOver(mouseX, mouseY)
        else super<HudElement>.isMouseOver(mouseX, mouseY)
    }

    override fun setFocused(focused: Boolean) { if (this::button.isInitialized) button.setFocused(focused) }
    override fun isFocused(): Boolean = if (this::button.isInitialized) button.isFocused() else false
}

class DelayPacketsButtonHudElement(initialX: Float, initialY: Float) :
    HudElement(initialX, initialY), Element {
    private var delayPacketsEnabled = false
    lateinit var button: CinnamonButton

    init {
        recreateButton()
    }

    private fun recreateButton() {
        val buttonText = Text.literal("Delay Packets: $delayPacketsEnabled")
        val currentX = getX().toInt()
        val currentY = getY().toInt()
        val currentWidth = DEFAULT_BUTTON_WIDTH
        val currentHeight = DEFAULT_BUTTON_HEIGHT

        val clickHandler: (Double, Double) -> Unit = { _, _ ->
            delayPacketsEnabled = !delayPacketsEnabled
            recreateButton()
            println("Delay Packets state: $delayPacketsEnabled for ${this.getName()}")
        }
        button = CinnamonButton(
            currentX, currentY,
            currentWidth, currentHeight,
            buttonText,
            onClick = clickHandler
        )
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (this::button.isInitialized) {
            button.setX(this.getX().toInt())
            button.setY(this.getY().toInt())
            val mc = MinecraftClient.getInstance()
            button.render(context, mc.mouse.x.toInt(), mc.mouse.y.toInt(), tickDelta)
        }
    }

    override fun getWidth(): Int = if (this::button.isInitialized) button.getWidth() else DEFAULT_BUTTON_WIDTH
    override fun getHeight(): Int = if (this::button.isInitialized) button.getHeight() else DEFAULT_BUTTON_HEIGHT
    override fun getName(): String = "Delay Packets Button"

    override fun mouseMoved(mouseX: Double, mouseY: Double) { if (this::button.isInitialized) button.mouseMoved(mouseX, mouseY) }
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseClicked(mouseX, mouseY, button) else false
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseReleased(mouseX, mouseY, button) else false
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = if (this::button.isInitialized) this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = if (this::button.isInitialized) this.button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) else false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyPressed(keyCode, scanCode, modifiers) else false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyReleased(keyCode, scanCode, modifiers) else false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = if (this::button.isInitialized) button.charTyped(chr, modifiers) else false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return if (this::button.isInitialized) button.isMouseOver(mouseX, mouseY)
        else super<HudElement>.isMouseOver(mouseX, mouseY)
    }

    override fun setFocused(focused: Boolean) { if (this::button.isInitialized) button.setFocused(focused) }
    override fun isFocused(): Boolean = if (this::button.isInitialized) button.isFocused() else false
}

class SaveGuiButtonHudElement(initialX: Float, initialY: Float) :
    HudElement(initialX, initialY), Element {
    lateinit var button: CinnamonButton

    init {
        val buttonText = Text.literal("Save GUI")
        val clickHandler: (Double, Double) -> Unit = { _, _ ->
            println("Save GUI button clicked: ${this.getName()} - Triggering GUI save (placeholder).")
        }
        button = CinnamonButton(
            this.getX().toInt(), this.getY().toInt(),
            DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT,
            buttonText,
            onClick = clickHandler
        )
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (this::button.isInitialized) {
            button.setX(this.getX().toInt())
            button.setY(this.getY().toInt())
            val mc = MinecraftClient.getInstance()
            button.render(context, mc.mouse.x.toInt(), mc.mouse.y.toInt(), tickDelta)
        }
    }

    override fun getWidth(): Int = if (this::button.isInitialized) button.getWidth() else DEFAULT_BUTTON_WIDTH
    override fun getHeight(): Int = if (this::button.isInitialized) button.getHeight() else DEFAULT_BUTTON_HEIGHT
    override fun getName(): String = "Save GUI Button"

    override fun mouseMoved(mouseX: Double, mouseY: Double) { if (this::button.isInitialized) button.mouseMoved(mouseX, mouseY) }
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseClicked(mouseX, mouseY, button) else false
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseReleased(mouseX, mouseY, button) else false
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = if (this::button.isInitialized) this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = if (this::button.isInitialized) this.button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) else false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyPressed(keyCode, scanCode, modifiers) else false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyReleased(keyCode, scanCode, modifiers) else false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = if (this::button.isInitialized) button.charTyped(chr, modifiers) else false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return if (this::button.isInitialized) button.isMouseOver(mouseX, mouseY)
        else super<HudElement>.isMouseOver(mouseX, mouseY)
    }

    override fun setFocused(focused: Boolean) { if (this::button.isInitialized) button.setFocused(focused) }
    override fun isFocused(): Boolean = if (this::button.isInitialized) button.isFocused() else false
}

class DisconnectButtonHudElement(initialX: Float, initialY: Float) :
    HudElement(initialX, initialY), Element {
    lateinit var button: CinnamonButton

    init {
        val buttonText = Text.literal("Disconnect and send packets")
        val clickHandler: (Double, Double) -> Unit = { _, _ ->
            println("Disconnect button clicked: ${this.getName()} - Sending queued packets and disconnecting (placeholder).")
        }
        button = CinnamonButton(
            this.getX().toInt(), this.getY().toInt(),
            DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT,
            buttonText,
            onClick = clickHandler
        )
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (this::button.isInitialized) {
            button.setX(this.getX().toInt())
            button.setY(this.getY().toInt())
            val mc = MinecraftClient.getInstance()
            button.render(context, mc.mouse.x.toInt(), mc.mouse.y.toInt(), tickDelta)
        }
    }

    override fun getWidth(): Int = if (this::button.isInitialized) button.getWidth() else DEFAULT_BUTTON_WIDTH
    override fun getHeight(): Int = if (this::button.isInitialized) button.getHeight() else DEFAULT_BUTTON_HEIGHT
    override fun getName(): String = "Disconnect Button"

    override fun mouseMoved(mouseX: Double, mouseY: Double) { if (this::button.isInitialized) button.mouseMoved(mouseX, mouseY) }
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseClicked(mouseX, mouseY, button) else false
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = if (this::button.isInitialized) this.button.mouseReleased(mouseX, mouseY, button) else false
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = if (this::button.isInitialized) this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) else false
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = if (this::button.isInitialized) this.button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount) else false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyPressed(keyCode, scanCode, modifiers) else false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = if (this::button.isInitialized) button.keyReleased(keyCode, scanCode, modifiers) else false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = if (this::button.isInitialized) button.charTyped(chr, modifiers) else false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return if (this::button.isInitialized) button.isMouseOver(mouseX, mouseY)
        else super<HudElement>.isMouseOver(mouseX, mouseY)
    }

    override fun setFocused(focused: Boolean) { if (this::button.isInitialized) button.setFocused(focused) }
    override fun isFocused(): Boolean = if (this::button.isInitialized) button.isFocused() else false
}