package code.cinnamon.hud.elements

import code.cinnamon.hud.HudElement
import net.minecraft.client.gui.DrawContext
import code.cinnamon.util.PacketHandlerAPI
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element

class PacketHandlerHudElement(initialX: Float, initialY: Float) : HudElement(initialX, initialY), Element {

    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val buttonHeight = 20
    private val buttonMargin = 2

    // State tracking for toggles, update as needed
    private var isBlocking = PacketHandlerAPI.isPacketBlocking()
    private var safeCloseEnabled = PacketHandlerAPI.isSafeCloseEnabled()

    private data class ButtonData(
        val text: String,
        val isToggle: Boolean = false,
        val getState: (() -> Boolean)? = null,
        val action: () -> Unit
    )

    private val buttons: List<ButtonData> = listOf(
        ButtonData(
            text = "Blocking",
            isToggle = true,
            getState = { PacketHandlerAPI.isPacketBlocking() },
            action = {
                if (PacketHandlerAPI.isPacketBlocking()) {
                    PacketHandlerAPI.stopPacketBlocking()
                } else {
                    PacketHandlerAPI.startPacketBlocking()
                }
                isBlocking = PacketHandlerAPI.isPacketBlocking()
            }
        ),
        ButtonData(
            text = "Safe Close",
            isToggle = true,
            getState = { PacketHandlerAPI.isSafeCloseEnabled() },
            action = {
                PacketHandlerAPI.enableSafeClose()
                safeCloseEnabled = PacketHandlerAPI.isSafeCloseEnabled()
            }
        ),
        ButtonData(
            text = "Clear Queues",
            action = {
                PacketHandlerAPI.clearQueues()
            }
        ),
        ButtonData(
            text = "Queue Status",
            action = {
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
            }
        )
    )

    // Only render in GUIs and HUD editor
    private fun shouldRender(): Boolean {
        val screen = client.currentScreen
        // Replace HudEditorScreen with your actual HUD editor class
        return screen != null && (screen !is net.minecraft.client.gui.screen.GameMenuScreen)
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!shouldRender()) return
        super.renderBackground(context)
        var currentY = getY().toInt() + buttonMargin

        for (button in buttons) {
            val buttonX = getX().toInt() + buttonMargin
            val buttonRenderWidth = getWidth() - 2 * buttonMargin

            // Toggle coloring for toggle buttons
            val isActive = button.isToggle && (button.getState?.invoke() == true)
            val color = when {
                button.isToggle && isActive -> 0x8000FF00.toInt() // Semi-transparent green
                button.isToggle && !isActive -> 0x80FF0000.toInt() // Semi-transparent red
                else -> 0x80FFFFFF.toInt() // Default
            }

            // Button background
            context.fill(buttonX, currentY, buttonX + buttonRenderWidth, currentY + buttonHeight, color)

            // Button text
            context.drawTextWithShadow(
                client.textRenderer,
                button.text,
                buttonX + (buttonRenderWidth - client.textRenderer.getWidth(button.text)) / 2,
                currentY + (buttonHeight - client.textRenderer.fontHeight) / 2,
                0xFFFFFF
            )
            currentY += buttonHeight + buttonMargin
        }
    }

    override fun getWidth(): Int {
        val longestText = buttons.maxOfOrNull { client.textRenderer.getWidth(it.text) } ?: 100
        return longestText + buttonMargin * 2
    }

    override fun getHeight(): Int {
        return buttons.size * (buttonHeight + buttonMargin) + buttonMargin
    }

    override fun getName(): String = "PacketHandler"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender() || !isMouseOver(mouseX, mouseY)) return false

        var currentY = getY().toInt() + buttonMargin
        val buttonX = getX().toInt() + buttonMargin
        val buttonRenderWidth = getWidth() - 2 * buttonMargin

        for (btn in buttons) {
            if (mouseX >= buttonX && mouseX <= buttonX + buttonRenderWidth &&
                mouseY >= currentY && mouseY <= currentY + buttonHeight) {
                btn.action()
                return true
            }
            currentY += buttonHeight + buttonMargin
        }
        return false
    }

    override fun setFocused(focused: Boolean) {}
    override fun isFocused(): Boolean = false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        val x = getX()
        val y = getY()
        val w = getWidth() * scale
        val h = getHeight() * scale
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h
    }
}