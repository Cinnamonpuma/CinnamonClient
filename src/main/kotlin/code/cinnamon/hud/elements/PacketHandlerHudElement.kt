package code.cinnamon.hud.elements

import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.hud.HudElement
import code.cinnamon.util.PacketHandlerAPI
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.util.Identifier
import javax.swing.JButton
import javax.swing.JFrame

class PacketHandlerHudElement(initialX: Float, initialY: Float) : HudElement(initialX, initialY), Element {

    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val buttonHeight = 20
    private val buttonMargin = 2
    private val CINNA_FONT: Identifier = CinnamonScreen.CINNA_FONT

    private fun createStyledText(text: String): Text =
        Text.literal(text).setStyle(Style.EMPTY.withFont(CINNA_FONT))

    private val closeWithoutPacketButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("Close without packet"),
        onClick = { _, _ -> client.setScreen(null) }
    )

    private val deSyncButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("De-sync"),
        onClick = { _, _ ->
            client.player?.let { player ->
                client.networkHandler?.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
            }
        }
    )

    private val sendPacketsButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("Send packets: ${code.cinnamon.SharedVariables.packetSendingEnabled}"),
        onClick = { _, _ ->
            code.cinnamon.SharedVariables.packetSendingEnabled = !code.cinnamon.SharedVariables.packetSendingEnabled
            updateSendPacketsButtonText()
        }
    )

    private val delayPacketsButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("Delay packets: ${PacketHandlerAPI.isPacketBlocking()}"),
        onClick = { _, _ ->
            if (PacketHandlerAPI.isPacketBlocking()) {
                PacketHandlerAPI.stopPacketBlocking()
                client.networkHandler?.let { PacketHandlerAPI.flushPacketQueue() }
            } else {
                PacketHandlerAPI.startPacketBlocking()
            }
            updateDelayPacketsButtonText()
        }
    )

    private val saveGuiButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("Save GUI"),
        onClick = { _, _ ->
            client.player?.let { player ->
                try {
                    val storedScreenField = code.cinnamon.SharedVariables::class.java.getDeclaredField("storedScreen")
                    val storedScreenHandlerField = code.cinnamon.SharedVariables::class.java.getDeclaredField("storedScreenHandler")
                    storedScreenField.isAccessible = true
                    storedScreenHandlerField.isAccessible = true
                    storedScreenField.set(null, client.currentScreen)
                    storedScreenHandlerField.set(null, player.currentScreenHandler)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    )

    private val disconnectAndSendButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("Disconnect and send packets"),
        onClick = { _, _ ->
            if (PacketHandlerAPI.isPacketBlocking()) PacketHandlerAPI.stopPacketBlocking()
            client.networkHandler?.let { handler ->
                PacketHandlerAPI.flushPacketQueue()
                handler.connection.disconnect(Text.literal("Disconnecting (CINNAMON)"))
            }
        }
    )

    private val fabricatePacketButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("Fabricate packet"),
        onClick = { _, _ ->
            if (!MinecraftClient.IS_SYSTEM_MAC) {
                val frame = JFrame("Choose Packet")
                frame.setBounds(0, 0, 450, 100)
                frame.isResizable = false
                frame.setLocationRelativeTo(null)
                frame.layout = null

                val clickSlotButton = JButton("Click Slot")
                clickSlotButton.setBounds(100, 25, 110, 20)
                clickSlotButton.addActionListener { frame.isVisible = false }

                val buttonClickButton = JButton("Button Click")
                buttonClickButton.setBounds(250, 25, 110, 20)
                buttonClickButton.addActionListener { frame.isVisible = false }

                frame.add(clickSlotButton)
                frame.add(buttonClickButton)
                frame.isVisible = true
            }
        }
    )

    private val copyTitleJsonButton = CinnamonButton(
        0, 0, 0, 0,
        createStyledText("Copy GUI Title JSON"),
        onClick = { _, _ ->
            try {
                val screen = client.currentScreen ?: throw IllegalStateException("No current screen")
                client.keyboard.setClipboard(screen.title.string)
            } catch (e: Exception) { e.printStackTrace() }
        }
    )

    private val buttons = listOf(
        closeWithoutPacketButton,
        deSyncButton,
        sendPacketsButton,
        delayPacketsButton,
        saveGuiButton,
        disconnectAndSendButton,
        fabricatePacketButton,
        copyTitleJsonButton
    )

    private fun updateSendPacketsButtonText() {
        sendPacketsButton.text = createStyledText("Send packets: ${code.cinnamon.SharedVariables.packetSendingEnabled}")
    }

    private fun updateDelayPacketsButtonText() {
        delayPacketsButton.text = createStyledText("Delay packets: ${PacketHandlerAPI.isPacketBlocking()}")
    }

    private fun shouldRender(): Boolean {
        val screen = client.currentScreen
        // Render if HUD is enabled AND (a screen is open and not GameMenuScreen OR in HUD edit mode)
        return code.cinnamon.SharedVariables.enabled &&
               (HudManager.isEditMode() ||
                (screen != null && screen !is net.minecraft.client.gui.screen.GameMenuScreen))
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!shouldRender()) return

        val hudXInt = getX().toInt()
        val hudYInt = getY().toInt()
        context.fill(hudXInt, hudYInt, hudXInt + getWidth(), hudYInt + getHeight(), 0x80000000.toInt())

        var currentY = hudYInt + buttonMargin
        for (button in buttons) {
            button.setX(hudXInt + buttonMargin)
            button.setY(currentY)
            button.setWidth(getWidth() - 2 * buttonMargin)
            button.setHeight(buttonHeight)
            button.render(context, client.mouse.x.toInt(), client.mouse.y.toInt(), tickDelta)
            currentY += buttonHeight + buttonMargin
        }

        // Draw border or indicator if in edit mode (optional)
        if (HudManager.isEditMode()) {
            context.drawBorder(
                hudXInt, hudYInt, getWidth(), getHeight(), 0xFFFF0000.toInt() // Red border
            )
        }
    }

    override fun getWidth(): Int =
        buttons.maxOfOrNull { client.textRenderer.getWidth(it.text) }?.plus(buttonMargin * 4) ?: 100

    override fun getHeight(): Int =
        buttons.size * (buttonHeight + buttonMargin) + buttonMargin

    override fun getName(): String = "PacketHandler"

    // Right-click drag to move (only in HUD edit mode)
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false
        if (!isMouseOver(mouseX, mouseY)) return false
        // Only allow drag in HUD edit mode with right click (button 1)
        if (button == 1 && HudManager.isEditMode()) {
            startDragging(mouseX, mouseY)
            return true
        }
        var handled = false
        for (cinnamonButton in buttons) {
            handled = handled or cinnamonButton.mouseClicked(mouseX, mouseY, button)
        }
        return handled
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!shouldRender()) return false
        // Only drag while in edit mode and right mouse button is held
        if (HudManager.isEditMode()) {
            updateDragging(mouseX, mouseY)
            return true
        }
        return false
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false
        // Only stop dragging if in edit mode
        if (HudManager.isEditMode()) {
            stopDragging()
        }
        var handled = false
        for (cinnamonButton in buttons) {
            handled = handled or cinnamonButton.mouseReleased(mouseX, mouseY, button)
        }
        return handled
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!shouldRender()) return
        for (button in buttons) {
            button.mouseMoved(mouseX, mouseY)
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (!shouldRender()) return false
        var handled = false
        for (button in buttons) {
            handled = handled or button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }
        return handled
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!shouldRender()) return false
        for (button in buttons) {
            if (button.keyPressed(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!shouldRender()) return false
        for (button in buttons) {
            if (button.keyReleased(keyCode, scanCode, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (!shouldRender()) return false
        for (button in buttons) {
            if (button.charTyped(chr, modifiers)) {
                return true
            }
        }
        return false
    }

    override fun setFocused(focused: Boolean) {}
    override fun isFocused(): Boolean = false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        if (!shouldRender()) return false
        val hudXDouble = getX().toDouble()
        val hudYDouble = getY().toDouble()
        val w = getWidth().toDouble()
        val h = getHeight().toDouble()
        return mouseX >= hudXDouble && mouseX <= hudXDouble + w && mouseY >= hudYDouble && mouseY <= hudYDouble + h
    }
}