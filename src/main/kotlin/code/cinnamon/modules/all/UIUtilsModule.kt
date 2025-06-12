package code.cinnamon.modules.all

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.components.CinnamonDropdown
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
import net.minecraft.util.collection.DefaultedList
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
            onClick = { _, _ -> closeWithoutPacket() }
        ))
        
        // De-sync button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 1,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("De-sync"),
            onClick = { _, _ -> desyncScreen() }
        ))
        
        // Send packets toggle button
        val sendPacketsButton = CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 2,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Send packets: $sendUIPackets"),
            onClick = { _, _ -> 
                toggleSendPackets()
                // Button text will be updated externally
            }
        )
        buttons.add(sendPacketsButton)
        
        // Delay packets toggle button
        val delayPacketsButton = CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 3,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Delay packets: $delayUIPackets"),
            onClick = { _, _ -> 
                toggleDelayPackets()
                // Button text will be updated externally
            }
        )
        buttons.add(delayPacketsButton)
        
        // Save GUI button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 4,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Save GUI"),
            onClick = { _, _ -> saveCurrentGUI() }
        ))
        
        // Disconnect and send packets button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 5,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Disconnect & Send"),
            onClick = { _, _ -> disconnectAndSendPackets() }
        ))
        
        // Packet Utils button (opens dropdown menu)
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 6,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Packet Utils"),
            onClick = { _, _ -> openPacketUtilsScreen() }
        ))
        
        // Copy GUI title JSON button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 7,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Copy Title JSON"),
            onClick = { _, _ -> copyGUITitleJSON() }
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
    
    private fun openPacketUtilsScreen() {
        mc.setScreen(PacketUtilsScreen())
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
            player.currentScreenHandler = storedScreenHandler!!
            LOGGER.info("Restored saved screen")
        }
    }
    
    // Utility functions
    fun isInteger(string: String): Boolean {
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
 * Screen with dropdown menus for packet utilities
 */
class PacketUtilsScreen : CinnamonScreen(Text.of("Packet Utils")) {
    private var selectedPacketType = "Click Slot"
    private val packetTypes = listOf("Click Slot", "Button Click")
    
    // Click Slot fields
    private var syncId = ""
    private var revision = ""
    private var slot = ""
    private var button = ""
    private var selectedAction = "PICKUP"
    private val actionTypes = SlotActionType.values().map { it.name }
    
    // Button Click fields
    private var buttonId = ""
    
    // Common fields
    private var timesToSend = "1"
    private var delayPacket = false
    private var statusMessage = ""
    private var statusColor = 0xFFFFFF
    
    override fun initializeComponents() {
        val centerX = getContentX() + getContentWidth() / 2
        val buttonWidth = 100
        val buttonHeight = 25
        
        // Packet type dropdown
        addDropdown(CinnamonDropdown(
            x = centerX - 60,
            y = getContentY() + 50,
            width = 120,
            height = 20,
            options = packetTypes,
            selectedOption = selectedPacketType,
            onSelectionChanged = { selection -> 
                selectedPacketType = selection
                statusMessage = ""
            }
        ))
        
        // Action type dropdown for Click Slot packets
        addDropdown(CinnamonDropdown(
            x = centerX - 60,
            y = getContentY() + 200,
            width = 120,
            height = 20,
            options = actionTypes,
            selectedOption = selectedAction,
            onSelectionChanged = { selection -> selectedAction = selection }
        ))
        
        // Send button
        addButton(CinnamonButton(
            x = centerX - buttonWidth - 10,
            y = getContentY() + 280,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Send Packet"),
            onClick = { _, _ -> sendPacket() }
        ))
        
        // Back button
        addButton(CinnamonButton(
            x = centerX + 10,
            y = getContentY() + 280,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Back"),
            onClick = { _, _ -> mc.setScreen(null) }
        ))
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val leftX = getContentX() + 20
        var currentY = getContentY() + 20
        val labelWidth = 100
        val fieldWidth = 120
        val rowHeight = 25
        
        // Title
        context.drawText(
            textRenderer,
            Text.of("Packet Type:"),
            leftX,
            currentY + 35,
            0xFFFFFF,
            false
        )
        
        currentY += 60
        
        // Draw form fields based on selected packet type
        if (selectedPacketType == "Click Slot") {
            drawInputField(context, leftX, currentY, "Sync ID", syncId, labelWidth, fieldWidth)
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Revision", revision, labelWidth, fieldWidth)
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Slot", slot, labelWidth, fieldWidth)
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Button", button, labelWidth, fieldWidth)
            currentY += rowHeight
            
            context.drawText(
                textRenderer,
                Text.of("Action Type:"),
                leftX,
                currentY + 35,
                0xFFFFFF,
                false
            )
            currentY += 60
            
        } else if (selectedPacketType == "Button Click") {
            drawInputField(context, leftX, currentY, "Sync ID", syncId, labelWidth, fieldWidth)
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Button ID", buttonId, labelWidth, fieldWidth)
            currentY += 60
        }
        
        // Common fields
        drawInputField(context, leftX, currentY, "Times to Send", timesToSend, labelWidth, fieldWidth)
        currentY += rowHeight
        
        // Delay checkbox
        context.drawText(textRenderer, Text.of("Delay Packet:"), leftX, currentY + 5, 0xFFFFFF, false)
        val checkboxX = leftX + labelWidth
        context.fill(checkboxX, currentY + 2, checkboxX + 16, currentY + 18, 0x40FFFFFF)
        context.drawBorder(checkboxX, currentY + 2, 16, 16, 0xFFFFFFFF.toInt())
        if (delayPacket) {
            context.drawText(textRenderer, Text.of("✓"), checkboxX + 4, currentY + 6, 0xFF55FF55.toInt(), false)
        }
        
        // Status message
        if (statusMessage.isNotEmpty()) {
            context.drawText(
                textRenderer, 
                Text.of(statusMessage), 
                leftX, 
                getContentY() + 250, 
                statusColor, 
                false
            )
        }
    }
    
    private fun drawInputField(context: DrawContext, x: Int, y: Int, label: String, value: String, labelWidth: Int, fieldWidth: Int) {
        context.drawText(textRenderer, Text.of("$label:"), x, y + 5, 0xFFFFFF, false)
        
        val fieldX = x + labelWidth
        context.fill(fieldX, y, fieldX + fieldWidth, y + 20, 0x40FFFFFF)
        context.drawBorder(fieldX, y, fieldWidth, 20, 0xFFFFFFFF.toInt())
        
        context.drawText(textRenderer, Text.of(value), fieldX + 5, y + 6, 0xFFFFFF, false)
    }
    
    private fun sendPacket() {
        if (selectedPacketType == "Click Slot") {
            sendClickSlotPacket()
        } else if (selectedPacketType == "Button Click") {
            sendButtonClickPacket()
        }
    }
    
    private fun sendClickSlotPacket() {
        if (!validateClickSlotInputs()) {
            showStatus("Invalid arguments!", 0xFFFF5555.toInt())
            return
        }
        
        try {
            val actionType = SlotActionType.valueOf(selectedAction)
            val changedSlots = Int2ObjectArrayMap<ItemStack>()
            
            val packet = ClickSlotC2SPacket(
                syncId.toInt(),
                revision.toInt(),
                slot.toInt(),
                button.toInt(),
                actionType,
                ItemStack.EMPTY,
                changedSlots
            )
            
            repeat(timesToSend.toInt()) {
                UIUtilsModule.sendOrDelayPacket(packet)
            }
            
            showStatus("Sent successfully!", 0xFF55FF55.toInt())
        } catch (e: Exception) {
            showStatus("Error sending packet!", 0xFFFF5555.toInt())
            LOGGER.error("Error sending click slot packet", e)
        }
    }
    
    private fun sendButtonClickPacket() {
        if (!validateButtonClickInputs()) {
            showStatus("Invalid arguments!", 0xFFFF5555.toInt())
            return
        }
        
        try {
            val packet = ButtonClickC2SPacket(syncId.toInt(), buttonId.toInt())
            
            repeat(timesToSend.toInt()) {
                UIUtilsModule.sendOrDelayPacket(packet)
            }
            
            showStatus("Sent successfully!", 0xFF55FF55.toInt())
        } catch (e: Exception) {
            showStatus("Error sending packet!", 0xFFFF5555.toInt())
            LOGGER.error("Error sending button click packet", e)
        }
    }
    
    private fun validateClickSlotInputs(): Boolean {
        return listOf(syncId, revision, slot, button, timesToSend).all { 
            it.isNotEmpty() && UIUtilsModule.isInteger(it) 
        }
    }
    
    private fun validateButtonClickInputs(): Boolean {
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
    
    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        // Handle text input - this would need to be implemented based on your GUI framework
        // For now, this is a placeholder
        return super.charTyped(chr, modifiers)
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle checkbox clicking
        val checkboxX = getContentX() + 120
        val checkboxY = getContentY() + 227 // Adjust based on layout
        
        if (mouseX >= checkboxX && mouseX < checkboxX + 16 && 
            mouseY >= checkboxY && mouseY < checkboxY + 16) {
            delayPacket = !delayPacket
            return true
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    // Helper method to add dropdown (assuming this method exists in CinnamonScreen)
    private fun addDropdown(dropdown: CinnamonDropdown) {
        // This would need to be implemented based on your GUI framework
        // For now, this is a placeholder
    }
}