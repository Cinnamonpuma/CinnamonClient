package code.cinnamon.hud.elements

import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.hud.HudElement
import code.cinnamon.util.PacketHandlerAPI
import code.cinnamon.SharedVariables
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

    // Position is managed by parent HudElement class

    // Custom font reference
    private val CINNA_FONT: Identifier = SharedVariables.CINNA_FONT

    private fun createStyledText(text: String): Text {
        return Text.literal(text).fillStyle(Style.EMPTY.withFont(CINNA_FONT))
    }

    private val closeWithoutPacketButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("Close without packet"),
        onClick = { _: Double, _: Double ->
            client.setScreen(null)
        }
    )

    private val deSyncButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("De-sync"),
        onClick = { _: Double, _: Double ->
            client.player?.let { player ->
                client.networkHandler?.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
            }
        }
    )

    private val sendPacketsButton: CinnamonButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("Send packets: ${SharedVariables.packetSendingEnabled}"),
        onClick = { _: Double, _: Double ->
            SharedVariables.packetSendingEnabled = !SharedVariables.packetSendingEnabled
            updateSendPacketsButtonText()
        }
    )

    private val delayPacketsButton: CinnamonButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("Delay packets: ${PacketHandlerAPI.isPacketBlocking()}"),
        onClick = { _: Double, _: Double ->
            if (PacketHandlerAPI.isPacketBlocking()) {
                PacketHandlerAPI.stopPacketBlocking()
                if (client.networkHandler != null) {
                    PacketHandlerAPI.flushPacketQueue()
                }
            } else {
                PacketHandlerAPI.startPacketBlocking()
            }
            updateDelayPacketsButtonText()
        }
    )

    private val saveGuiButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("Save GUI"),
        onClick = { _: Double, _: Double ->
            client.player?.let { player ->
                try {
                    val storedScreenField = SharedVariables::class.java.getDeclaredField("storedScreen")
                    val storedScreenHandlerField = SharedVariables::class.java.getDeclaredField("storedScreenHandler")
                    storedScreenField.isAccessible = true
                    storedScreenHandlerField.isAccessible = true
                    storedScreenField.set(null, client.currentScreen)
                    storedScreenHandlerField.set(null, player.currentScreenHandler)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    private val disconnectAndSendButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("Disconnect and send packets"),
        onClick = { _: Double, _: Double ->
            if (PacketHandlerAPI.isPacketBlocking()) {
                PacketHandlerAPI.stopPacketBlocking()
            }
            client.networkHandler?.let { handler ->
                PacketHandlerAPI.flushPacketQueue()
                handler.connection.disconnect(Text.literal("Disconnecting (CINNAMON)"))
            }
        }
    )

    private val fabricatePacketButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("Fabricate packet"),
        onClick = { _: Double, _: Double ->
            if (!MinecraftClient.IS_SYSTEM_MAC) {
                val frame = JFrame("Choose Packet")
                frame.setBounds(0, 0, 450, 100)
                frame.isResizable = false
                frame.setLocationRelativeTo(null)
                frame.layout = null

                val clickSlotButton = JButton("Click Slot")
                clickSlotButton.setBounds(100, 25, 110, 20)
                clickSlotButton.addActionListener {
                    frame.isVisible = false
                }

                val buttonClickButton = JButton("Button Click")
                buttonClickButton.setBounds(250, 25, 110, 20)
                buttonClickButton.addActionListener {
                    frame.isVisible = false
                }

                frame.add(clickSlotButton)
                frame.add(buttonClickButton)
                frame.isVisible = true
            }
        }
    )

    private val copyTitleJsonButton = CinnamonButton(
        0, 0, 0, 0, 
        createStyledText("Copy GUI Title JSON"),
        onClick = { _: Double, _: Double ->
            try {
                val screen = client.currentScreen ?: throw IllegalStateException("The current minecraft screen (mc.currentScreen) is null")
                client.keyboard.setClipboard(screen.title.string)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
        sendPacketsButton.text = createStyledText("Send packets: ${SharedVariables.packetSendingEnabled}")
    }

    private fun updateDelayPacketsButtonText() {
        delayPacketsButton.text = createStyledText("Delay packets: ${PacketHandlerAPI.isPacketBlocking()}")
    }

    private fun shouldRender(): Boolean {
        val screen = client.currentScreen
        return SharedVariables.enabled && screen != null && (screen !is net.minecraft.client.gui.screen.GameMenuScreen)
    }

    // Method to update position using parent class setters
    fun updatePosition(newX: Float, newY: Float) {
        setX(newX)
        setY(newY)
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!shouldRender()) return
        
        // Use parent class position getters
        val hudXInt = getX().toInt()
        val hudYInt = getY().toInt()
        
        // Render background directly
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
    }

    override fun getWidth(): Int {
        val longestTextWidth = buttons.maxOfOrNull { client.textRenderer.getWidth(it.text) } ?: 100
        return longestTextWidth + buttonMargin * 4
    }

    override fun getHeight(): Int {
        return buttons.size * (buttonHeight + buttonMargin) + buttonMargin
    }

    override fun getName(): String = "PacketHandler"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false
        
        // Check if click is within our HUD bounds first
        if (!isMouseOver(mouseX, mouseY)) return false
        
        for (cinnamonButton in buttons) {
            if (cinnamonButton.mouseClicked(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (!shouldRender()) return
        for (button in buttons) {
            button.mouseMoved(mouseX, mouseY)
        }
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false
        for (cinnamonButton in buttons) {
            if (cinnamonButton.mouseReleased(mouseX, mouseY, button)) {
                return true
            }
        }
        return false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (!shouldRender()) return false
        for (button in buttons) {
            if (button.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true
            }
        }
        return false
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