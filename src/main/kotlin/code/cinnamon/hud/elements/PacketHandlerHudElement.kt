package code.cinnamon.hud.elements

import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.hud.HudElement
import net.minecraft.client.gui.DrawContext
import code.cinnamon.util.PacketHandlerAPI
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.text.Text

class PacketHandlerHudElement(initialX: Float, initialY: Float) : HudElement(initialX, initialY), Element {

    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val buttonHeight = 20
    private val buttonMargin = 2

    private lateinit var blockingButton: CinnamonButton
    private lateinit var safeCloseButton: CinnamonButton
    private lateinit var clearQueuesButton: CinnamonButton
    private lateinit var queueStatusButton: CinnamonButton

    private val buttons: List<CinnamonButton>

    init {
        blockingButton = CinnamonButton(0, 0, 0, 0, Text.of(getBlockingButtonText()), onClick = { _, _ ->
            if (PacketHandlerAPI.isPacketBlocking()) {
                PacketHandlerAPI.stopPacketBlocking()
            } else {
                PacketHandlerAPI.startPacketBlocking()
            }
            blockingButton.text = Text.of(getBlockingButtonText())
        })

        safeCloseButton = CinnamonButton(0, 0, 0, 0, Text.of(getSafeCloseButtonText()), onClick = { _, _ ->
            PacketHandlerAPI.enableSafeClose()
            safeCloseButton.text = Text.of(getSafeCloseButtonText())
        })

        clearQueuesButton = CinnamonButton(0, 0, 0, 0, Text.of("Clear Queues"), onClick = { _, _ ->
            PacketHandlerAPI.clearQueues()
        })

        queueStatusButton = CinnamonButton(0, 0, 0, 0, Text.of("Queue Status"), onClick = { _, _ ->
            val queuedCount = PacketHandlerAPI.getQueuedPacketCount()
            val delayedCount = PacketHandlerAPI.getDelayedPacketCount()
            val blocking = PacketHandlerAPI.isPacketBlocking()
            val safeClose = PacketHandlerAPI.isSafeCloseEnabled()
            println("=== Packet Handler Status ===")
            println("Queued packets: $queuedCount")
            println("Delayed packets: $delayedCount")
            println("Blocking enabled: $blocking")
            println("Safe close enabled: $safeClose")
            println("=============================")
        })

        buttons = listOf(blockingButton, safeCloseButton, clearQueuesButton, queueStatusButton)
    }

    private fun getBlockingButtonText(): String {
        return "Blocking: ${if (PacketHandlerAPI.isPacketBlocking()) "ON" else "OFF"}"
    }

    private fun getSafeCloseButtonText(): String {
        // Assuming enableSafeClose toggles it. If it only enables, this logic might need adjustment
        // For now, let's assume it means "Safe Close: ON" if PacketHandlerAPI.isSafeCloseEnabled() is true
        // and the button's action is to ensure it's enabled.
        // The prompt implies `enableSafeClose` is the action, and then `isSafeCloseEnabled` reflects state.
        return "Safe Close: ${if (PacketHandlerAPI.isSafeCloseEnabled()) "ON" else "OFF"}"
    }

    // Only render in GUIs and HUD editor
    private fun shouldRender(): Boolean {
        val screen = client.currentScreen
        // Replace HudEditorScreen with your actual HUD editor class
        return screen != null && (screen !is net.minecraft.client.gui.screen.GameMenuScreen)
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!shouldRender()) return
        super.renderBackground(context) // Optional: keep if you want a background for the whole element group
        var currentY = getY().toInt() + buttonMargin

        for (button in buttons) {
            button.setX(getX().toInt() + buttonMargin)
            button.setY(currentY)
            button.setWidth(getWidth() - 2 * buttonMargin)
            button.setHeight(buttonHeight)
            // Potentially update isPrimary for toggle buttons based on state
            if (button == blockingButton) {
                button.isPrimary = PacketHandlerAPI.isPacketBlocking()
            } else if (button == safeCloseButton) {
                button.isPrimary = PacketHandlerAPI.isSafeCloseEnabled()
            }
            button.render(context, client.mouse.x.toInt(), client.mouse.y.toInt(), tickDelta)
            currentY += buttonHeight + buttonMargin
        }
    }

    override fun getWidth(): Int {
        // Calculate width based on the longest text of the CinnamonButtons
        // This might need access to TextRenderer to be accurate.
        // CinnamonButton itself might have a method to get its preferred width.
        // For now, a fixed width or a simple calculation:
        val longestTextWidth = buttons.maxOfOrNull { client.textRenderer.getWidth(it.text) } ?: 100
        return longestTextWidth + buttonMargin * 4 // Adjusted margin for text padding within button
    }

    override fun getHeight(): Int {
        return buttons.size * (buttonHeight + buttonMargin) + buttonMargin
    }

    override fun getName(): String = "PacketHandler"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false // Removed isMouseOver check for the entire element, buttons handle their own
        for (cinnamonButton in buttons) {
            if (cinnamonButton.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }

    // Element interface methods - delegating to buttons or providing sensible defaults
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        for (button in buttons) {
            button.mouseMoved(mouseX, mouseY)
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (cinnamonButton in buttons) {
            if (cinnamonButton.mouseReleased(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        for (button in buttons) {
            if (button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true
            }
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        for (button in buttons) {
            if (button.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        for (button in buttons) {
            if (button.keyReleased(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        for (button in buttons) {
            if (button.charTyped(chr, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun setFocused(focused: Boolean) {
        // If the container itself can be focused, manage that state.
        // Otherwise, could delegate to a specific button or none.
        // For now, no specific button gets focus by default.
    }

    override fun isFocused(): Boolean = false // Or manage based on setFocused

    // isMouseOver for the entire element might still be useful for the Hud system
    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        val x = getX()
        val y = getY()
        // Ensure getWidth() and getHeight() are accurate for this check
        val w = getWidth() * scale
        val h = getHeight() * scale
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
    }
}