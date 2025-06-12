package code.cinnamon.modules.all

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.components.CinnamonButton
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.ButtonClickC2SPacket
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap
import java.util.*
import kotlin.collections.mutableListOf

object UIUtilsModule {
    private val LOGGER = LoggerFactory.getLogger("ui-utils")
    private val mc = MinecraftClient.getInstance()
    
    // Shared variables for UI state
    var sendUIPackets = true
    var delayUIPackets = false
    val delayedUIPackets = mutableListOf<Packet<*>>()
    var storedScreen: Screen? = null
    var storedScreenHandler: net.minecraft.screen.ScreenHandler? = null
    
    /**
     * Creates and returns a list of UI utility buttons for HandledScreens
     */
    fun createUIUtilsButtons(baseX: Int, baseY: Int): List<CinnamonButton> {
        val buttons = mutableListOf<CinnamonButton>()
        val buttonWidth = 140
        val buttonHeight = 25
        val spacing = 5
        
        // Close without packet button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Close without packet"),
            onClick = { closeWithoutPacket() }
        ))
        
        // De-sync button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 1,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("De-sync"),
            onClick = { desyncScreen() }
        ))
        
        // Send packets toggle button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 2,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Send packets: $sendUIPackets"),
            onClick = { toggleSendPackets() }
        ).apply {
            // Update button text when clicked
            this.onClick = {
                toggleSendPackets()
                this.text = Text.of("Send packets: $sendUIPackets")
            }
        })
        
        // Delay packets toggle button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 3,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Delay packets: $delayUIPackets"),
            onClick = { toggleDelayPackets() }
        ).apply {
            this.onClick = {
                toggleDelayPackets()
                this.text = Text.of("Delay packets: $delayUIPackets")
            }
        })
        
        // Save GUI button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 4,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Save GUI"),
            onClick = { saveCurrentGUI() }
        ))
        
        // Disconnect and send packets button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 5,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Disconnect & Send"),
            onClick = { disconnectAndSendPackets() }
        ))
        
        // Fabricate packet button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 6,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Fabricate Packet"),
            onClick = { openPacketFabricationScreen() }
        ))
        
        // Copy GUI title JSON button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 7,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Copy Title JSON"),
            onClick = { copyGUITitleJSON() }
        ))
        
        return buttons
    }
    
    /**
     * Renders debug information on screen
     */
    fun renderDebugInfo(context: DrawContext, x: Int, y: Int) {
        val player = mc.player ?: return
        val textRenderer = mc.textRenderer
        
        // Display sync ID and revision
        context.drawText(
            textRenderer, 
            "Sync Id: ${player.currentScreenHandler.syncId}", 
            x, y, 
            0xFFFFFF, 
            false
        )
        context.drawText(
            textRenderer, 
            "Revision: ${player.currentScreenHandler.revision}", 
            x, y + 15, 
            0xFFFFFF, 
            false
        )
        context.drawText(
            textRenderer, 
            "Delayed Packets: ${delayedUIPackets.size}", 
            x, y + 30, 
            0xFFFFFF, 
            false
        )
    }
    
    // Button action implementations
    private fun closeWithoutPacket() {
        mc.setScreen(null)
        LOGGER.info("Closed screen without sending packet")
    }
    
    private fun desyncScreen() {
        val networkHandler = mc.networkHandler
        val player = mc.player
        
        if (networkHandler != null && player != null) {
            networkHandler.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
            LOGGER.info("De-synced screen (closed server-side, kept client-side)")
        } else {
            LOGGER.warn("Cannot de-sync: network handler or player is null")
        }
    }
    
    private fun toggleSendPackets() {
        sendUIPackets = !sendUIPackets
        LOGGER.info("Send UI packets: $sendUIPackets")
    }
    
    private fun toggleDelayPackets() {
        delayUIPackets = !delayUIPackets
        
        if (!delayUIPackets && delayedUIPackets.isNotEmpty()) {
            val networkHandler = mc.networkHandler
            if (networkHandler != null) {
                delayedUIPackets.forEach { packet ->
                    networkHandler.sendPacket(packet)
                }
                mc.player?.sendMessage(Text.of("Sent ${delayedUIPackets.size} delayed packets"))
                delayedUIPackets.clear()
                LOGGER.info("Sent all delayed packets")
            }
        }
        
        LOGGER.info("Delay UI packets: $delayUIPackets")
    }
    
    private fun saveCurrentGUI() {
        val player = mc.player
        if (player != null) {
            storedScreen = mc.currentScreen
            storedScreenHandler = player.currentScreenHandler
            LOGGER.info("Saved current GUI state")
        }
    }
    
    private fun disconnectAndSendPackets() {
        val networkHandler = mc.networkHandler
        
        if (networkHandler != null) {
            // Send all delayed packets first
            delayedUIPackets.forEach { packet ->
                networkHandler.sendPacket(packet)
            }
            
            // Disconnect
            networkHandler.connection.disconnect(Text.of("Disconnecting (UI-UTILS)"))
            delayedUIPackets.clear()
            LOGGER.info("Disconnected after sending delayed packets")
        } else {
            LOGGER.warn("Cannot disconnect: network handler is null")
        }
    }
    
    private fun openPacketFabricationScreen() {
        mc.setScreen(PacketFabricationScreen())
    }
    
    private fun copyGUITitleJSON() {
        try {
            val currentScreen = mc.currentScreen
            if (currentScreen != null) {
                val server = mc.server
                if (server != null) {
                    val json = Text.Serialization.toJsonString(
                        currentScreen.title, 
                        server.registryManager
                    )
                    mc.keyboard.setClipboard(json)
                    LOGGER.info("Copied GUI title JSON to clipboard")
                } else {
                    LOGGER.warn("Cannot copy title JSON: server is null")
                }
            } else {
                LOGGER.warn("Cannot copy title JSON: current screen is null")
            }
        } catch (e: Exception) {
            LOGGER.error("Error copying title JSON to clipboard", e)
        }
    }
    
    /**
     * Utility method to send or delay a packet based on current settings
     */
    fun sendOrDelayPacket(packet: Packet<*>) {
        if (!sendUIPackets) {
            return // Don't send if disabled
        }
        
        if (delayUIPackets) {
            delayedUIPackets.add(packet)
            LOGGER.debug("Delayed packet: ${packet.javaClass.simpleName}")
        } else {
            val networkHandler = mc.networkHandler
            if (networkHandler != null) {
                networkHandler.sendPacket(packet)
                LOGGER.debug("Sent packet: ${packet.javaClass.simpleName}")
            }
        }
    }
    
    /**
     * Restore saved screen (for keybinding)
     */
    fun restoreSavedScreen() {
        val player = mc.player
        if (storedScreen != null && storedScreenHandler != null && player != null) {
            mc.setScreen(storedScreen)
            player.currentScreenHandler = storedScreenHandler
            LOGGER.info("Restored saved screen")
        }
    }
    
    // Utility functions
    private fun isInteger(string: String): Boolean {
        return try {
            string.toInt()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    private fun stringToSlotActionType(string: String): SlotActionType? {
        return when (string) {
            "PICKUP" -> SlotActionType.PICKUP
            "QUICK_MOVE" -> SlotActionType.QUICK_MOVE
            "SWAP" -> SlotActionType.SWAP
            "CLONE" -> SlotActionType.CLONE
            "THROW" -> SlotActionType.THROW
            "QUICK_CRAFT" -> SlotActionType.QUICK_CRAFT
            "PICKUP_ALL" -> SlotActionType.PICKUP_ALL
            else -> null
        }
    }
}

/**
 * Screen for fabricating custom packets
 */
class PacketFabricationScreen : CinnamonScreen(Text.of("Packet Fabrication")) {
    
    override fun initializeComponents() {
        val buttonWidth = 120
        val buttonHeight = 30
        val centerX = getContentX() + (getContentWidth() - buttonWidth) / 2
        val startY = getContentY() + 50
        
        // Click Slot Packet button
        addButton(CinnamonButton(
            x = centerX,
            y = startY,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Click Slot Packet"),
            onClick = { mc.setScreen(ClickSlotPacketScreen()) }
        ))
        
        // Button Click Packet button
        addButton(CinnamonButton(
            x = centerX,
            y = startY + buttonHeight + 10,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Button Click Packet"),
            onClick = { mc.setScreen(ButtonClickPacketScreen()) }
        ))
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val titleText = Text.of("Choose a packet type to fabricate:")
        val titleWidth = textRenderer.getWidth(titleText)
        val titleX = getContentX() + (getContentWidth() - titleWidth) / 2
        
        context.drawText(
            textRenderer,
            titleText,
            titleX,
            getContentY() + 20,
            theme.primaryTextColor,
            false
        )
    }
}

/**
 * Screen for creating Click Slot packets
 */
class ClickSlotPacketScreen : CinnamonScreen(Text.of("Click Slot Packet")) {
    private var syncId = ""
    private var revision = ""
    private var slot = ""
    private var button = ""
    private var selectedAction = SlotActionType.PICKUP
    private var timesToSend = "1"
    private var delayPacket = false
    private var statusMessage = ""
    private var statusColor = 0xFFFFFF
    
    override fun initializeComponents() {
        val buttonWidth = 100
        val buttonHeight = 25
        
        // Send button
        addButton(CinnamonButton(
            x = getContentX() + 20,
            y = getContentY() + 200,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Send Packet"),
            onClick = { sendClickSlotPacket() }
        ))
        
        // Back button
        addButton(CinnamonButton(
            x = getContentX() + 140,
            y = getContentY() + 200,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Back"),
            onClick = { mc.setScreen(PacketFabricationScreen()) }
        ))
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val leftX = getContentX() + 20
        var currentY = getContentY() + 20
        val labelWidth = 100
        val fieldWidth = 80
        val rowHeight = 25
        
        // Helper function to draw a form row
        fun drawFormRow(label: String, value: String, isEditable: Boolean = true) {
            context.drawText(textRenderer, Text.of("$label:"), leftX, currentY + 5, theme.primaryTextColor, false)
            
            // Draw input field background
            val fieldX = leftX + labelWidth
            val fieldColor = if (isEditable) theme.inputBackgroundColor else theme.disabledBackgroundColor
            context.fill(fieldX, currentY, fieldX + fieldWidth, currentY + 20, fieldColor)
            context.drawBorder(fieldX, currentY, fieldWidth, 20, theme.borderColor)
            
            // Draw text
            context.drawText(textRenderer, Text.of(value), fieldX + 5, currentY + 6, theme.primaryTextColor, false)
            currentY += rowHeight
        }
        
        // Draw form fields
        drawFormRow("Sync ID", syncId)
        drawFormRow("Revision", revision)
        drawFormRow("Slot", slot)
        drawFormRow("Button", button)
        drawFormRow("Action", selectedAction.name, false)
        drawFormRow("Times", timesToSend)
        
        // Delay checkbox
        val checkboxX = leftX + labelWidth
        context.drawText(textRenderer, Text.of("Delay:"), leftX, currentY + 5, theme.primaryTextColor, false)
        context.fill(checkboxX, currentY + 2, checkboxX + 16, currentY + 18, theme.inputBackgroundColor)
        context.drawBorder(checkboxX, currentY + 2, 16, 16, theme.borderColor)
        if (delayPacket) {
            context.drawText(textRenderer, Text.of("✓"), checkboxX + 4, currentY + 6, theme.accentColor, false)
        }
        
        // Status message
        if (statusMessage.isNotEmpty()) {
            context.drawText(
                textRenderer, 
                Text.of(statusMessage), 
                leftX, 
                getContentY() + 170, 
                statusColor, 
                false
            )
        }
    }
    
    private fun sendClickSlotPacket() {
        if (!validateInputs()) {
            showStatus("Invalid arguments!", 0xFF5555)
            return
        }
        
        try {
            val packet = ClickSlotC2SPacket(
                syncId.toInt(),
                revision.toInt(),
                slot.toInt(),
                button.toInt(),
                selectedAction,
                ItemStack.EMPTY,
                Int2ObjectArrayMap()
            )
            
            repeat(timesToSend.toInt()) {
                UIUtilsModule.sendOrDelayPacket(packet)
            }
            
            showStatus("Sent successfully!", 0x55FF55)
        } catch (e: Exception) {
            showStatus("Error sending packet!", 0xFF5555)
        }
    }
    
    private fun validateInputs(): Boolean {
        return listOf(syncId, revision, slot, button, timesToSend).all { 
            it.isNotEmpty() && UIUtilsModule.isInteger(it) 
        }
    }
    
    private fun showStatus(message: String, color: Int) {
        statusMessage = message
        statusColor = color
        
        // Clear status after 3 seconds
        Timer().schedule(object : TimerTask() {
            override fun run() {
                statusMessage = ""
            }
        }, 3000)
    }
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        // Handle text input for form fields
        // This is a simplified implementation - you'd want to track which field is focused
        return super.charTyped(chr, modifiers)
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle checkbox clicking and field focusing
        val checkboxX = getContentX() + 120
        val checkboxY = getContentY() + 152
        
        if (mouseX >= checkboxX && mouseX < checkboxX + 16 && 
            mouseY >= checkboxY && mouseY < checkboxY + 16) {
            delayPacket = !delayPacket
            return true
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
}

/**
 * Screen for creating Button Click packets
 */
class ButtonClickPacketScreen : CinnamonScreen(Text.of("Button Click Packet")) {
    private var syncId = ""
    private var buttonId = ""
    private var timesToSend = "1"
    private var delayPacket = false
    private var statusMessage = ""
    private var statusColor = 0xFFFFFF
    
    override fun initializeComponents() {
        val buttonWidth = 100
        val buttonHeight = 25
        
        // Send button
        addButton(CinnamonButton(
            x = getContentX() + 20,
            y = getContentY() + 150,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Send Packet"),
            onClick = { sendButtonClickPacket() }
        ))
        
        // Back button
        addButton(CinnamonButton(
            x = getContentX() + 140,
            y = getContentY() + 150,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Back"),
            onClick = { mc.setScreen(PacketFabricationScreen()) }
        ))
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val leftX = getContentX() + 20
        var currentY = getContentY() + 20
        val labelWidth = 100
        val fieldWidth = 80
        val rowHeight = 25
        
        // Helper function to draw a form row
        fun drawFormRow(label: String, value: String) {
            context.drawText(textRenderer, Text.of("$label:"), leftX, currentY + 5, theme.primaryTextColor, false)
            
            // Draw input field background
            val fieldX = leftX + labelWidth
            context.fill(fieldX, currentY, fieldX + fieldWidth, currentY + 20, theme.inputBackgroundColor)
            context.drawBorder(fieldX, currentY, fieldWidth, 20, theme.borderColor)
            
            // Draw text
            context.drawText(textRenderer, Text.of(value), fieldX + 5, currentY + 6, theme.primaryTextColor, false)
            currentY += rowHeight
        }
        
        // Draw form fields
        drawFormRow("Sync ID", syncId)
        drawFormRow("Button ID", buttonId)
        drawFormRow("Times", timesToSend)
        
        // Delay checkbox
        val checkboxX = leftX + labelWidth
        context.drawText(textRenderer, Text.of("Delay:"), leftX, currentY + 5, theme.primaryTextColor, false)
        context.fill(checkboxX, currentY + 2, checkboxX + 16, currentY + 18, theme.inputBackgroundColor)
        context.drawBorder(checkboxX, currentY + 2, 16, 16, theme.borderColor)
        if (delayPacket) {
            context.drawText(textRenderer, Text.of("✓"), checkboxX + 4, currentY + 6, theme.accentColor, false)
        }
        
        // Status message
        if (statusMessage.isNotEmpty()) {
            context.drawText(
                textRenderer, 
                Text.of(statusMessage), 
                leftX, 
                getContentY() + 120, 
                statusColor, 
                false
            )
        }
    }
    
    private fun sendButtonClickPacket() {
        if (!validateInputs()) {
            showStatus("Invalid arguments!", 0xFF5555)
            return
        }
        
        try {
            val packet = ButtonClickC2SPacket(syncId.toInt(), buttonId.toInt())
            
            repeat(timesToSend.toInt()) {
                UIUtilsModule.sendOrDelayPacket(packet)
            }
            
            showStatus("Sent successfully!", 0x55FF55)
        } catch (e: Exception) {
            showStatus("Error sending packet!", 0xFF5555)
        }
    }
    
    private fun validateInputs(): Boolean {
        return listOf(syncId, buttonId, timesToSend).all { 
            it.isNotEmpty() && UIUtilsModule.isInteger(it) 
        }
    }
    
    private fun showStatus(message: String, color: Int) {
        statusMessage = message
        statusColor = color
        
        // Clear status after 3 seconds
        Timer().schedule(object : TimerTask() {
            override fun run() {
                statusMessage = ""
            }
        }, 3000)
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle checkbox clicking
        val checkboxX = getContentX() + 120
        val checkboxY = getContentY() + 77
        
        if (mouseX >= checkboxX && mouseX < checkboxX + 16 && 
            mouseY >= checkboxY && mouseY < checkboxY + 16) {
            delayPacket = !delayPacket
            return true
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
}

// Extension function to make UIUtilsModule.isInteger accessible
private fun UIUtilsModule.isInteger(string: String): Boolean {
    return try {
        string.toInt()
        true
    } catch (e: NumberFormatException) {
        false
    }
}