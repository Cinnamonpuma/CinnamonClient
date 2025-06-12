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
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.util.collection.DefaultedList
import java.util.*
import kotlin.collections.mutableListOf
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.screen.sync.ItemStackHash
import code.cinnamon.mixin.accessor.ScreenAccessor

object UIUtilsModule {
    val LOGGER = LoggerFactory.getLogger("ui-utils")
    val mc = MinecraftClient.getInstance()
    
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
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 2,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Send packets: $sendUIPackets"),
            onClick = { _, _ -> 
                toggleSendPackets()
                // Button text will be updated externally
            }
        ))
        
        // Delay packets toggle button
        buttons.add(CinnamonButton(
            x = baseX,
            y = baseY + (buttonHeight + spacing) * 3,
            width = buttonWidth,
            height = buttonHeight,
            text = Text.of("Delay packets: $delayUIPackets"),
            onClick = { _, _ -> 
                toggleDelayPackets()
                // Button text will be updated externally
            }
        ))
        
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
     * Creates UI utility buttons.
     */
    public fun createWidgets(mc: MinecraftClient, screen: Screen) {
        createUIUtilsButtons(0, 0) // Assuming baseX and baseY are 0 for now
        // Buttons are created but not added to the screen as per updated requirement
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
            Text.of("Sync Id: ${player.currentScreenHandler.syncId}"),
            x, y,
            0xFFFFFF,
            false
        )
        context.drawText(
            textRenderer,
            Text.of("Revision: ${player.currentScreenHandler.revision}"),
            x, y + 15,
            0xFFFFFF,
            false
        )
        context.drawText(
            textRenderer,
            Text.of("Delayed Packets: ${delayedUIPackets.size}"),
            x, y + 30,
            0xFFFFFF,
            false
        )
    }

    /**
     * Renders debug information using the provided context and text renderer.
     */
    public fun createText(mc: MinecraftClient, context: DrawContext, textRenderer: net.minecraft.client.font.TextRenderer) {
        renderDebugInfo(context, 5, 5)
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
                mc.player?.sendMessage(Text.of("Sent ${delayedUIPackets.size} delayed packets"), false)
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
        MinecraftClient.getInstance().setScreen(PacketUtilsScreen())
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
}

/**
 * Screen with simple text inputs for packet utilities
 */
class PacketUtilsScreen : CinnamonScreen(Text.of("Packet Utils")) {
    private val LOGGER = LoggerFactory.getLogger("packet-utils")
    
    private var selectedPacketType = "Click Slot"
    private val packetTypes = listOf("Click Slot", "Button Click")
    private var currentPacketIndex = 0
    
    // Click Slot fields
    private var syncId = ""
    private var revision = ""
    private var slot = ""
    private var button = ""
    private var selectedAction = "PICKUP"
    private val actionTypes = SlotActionType.values().map { it.name }
    private var currentActionIndex = 0
    
    // Button Click fields
    private var buttonId = ""
    
    // Common fields
    private var timesToSend = "1"
    private var delayPacket = false
    private var statusMessage = ""
    private var statusColor = 0xFFFFFF
    
    // Input handling
    private var activeField = ""
    
    override fun initializeComponents() {
        val centerX = getContentX() + getContentWidth() / 2
        val buttonWidth = 100
        val buttonHeight = 25
        
        // Packet type toggle button
        addButton(CinnamonButton(
            x = centerX - 60,
            y = getContentY() + 50,
            width = 120,
            height = 20,
            text = Text.of(selectedPacketType),
            onClick = { _, _ -> 
                currentPacketIndex = (currentPacketIndex + 1) % packetTypes.size
                selectedPacketType = packetTypes[currentPacketIndex]
                statusMessage = ""
                refreshButtons()
            }
        ))
        
        // Action type toggle button for Click Slot packets
        addButton(CinnamonButton(
            x = centerX - 60,
            y = getContentY() + 200,
            width = 120,
            height = 20,
            text = Text.of(selectedAction),
            onClick = { _, _ -> 
                currentActionIndex = (currentActionIndex + 1) % actionTypes.size
                selectedAction = actionTypes[currentActionIndex]
                refreshButtons()
            }
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
            onClick = { _, _ -> MinecraftClient.getInstance().setScreen(null) }
        ))
    }
    
    private fun refreshButtons() {
        // Clear and recreate buttons with updated text
        buttons.clear()
        initializeComponents()
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
            drawInputField(context, leftX, currentY, "Sync ID", syncId, labelWidth, fieldWidth, "syncId")
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Revision", revision, labelWidth, fieldWidth, "revision")
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Slot", slot, labelWidth, fieldWidth, "slot")
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Button", button, labelWidth, fieldWidth, "button")
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
            drawInputField(context, leftX, currentY, "Sync ID", syncId, labelWidth, fieldWidth, "syncId")
            currentY += rowHeight
            
            drawInputField(context, leftX, currentY, "Button ID", buttonId, labelWidth, fieldWidth, "buttonId")
            currentY += 60
        }
        
        // Common fields
        drawInputField(context, leftX, currentY, "Times to Send", timesToSend, labelWidth, fieldWidth, "timesToSend")
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
    
    private fun drawInputField(context: DrawContext, x: Int, y: Int, label: String, value: String, labelWidth: Int, fieldWidth: Int, fieldName: String) {
        context.drawText(textRenderer, Text.of("$label:"), x, y + 5, 0xFFFFFF, false)
        
        val fieldX = x + labelWidth
        val isActive = activeField == fieldName
        val bgColor = if (isActive) 0x60FFFFFF else 0x40FFFFFF
        val borderColor = if (isActive) 0xFFFFFFFF.toInt() else 0x80FFFFFF
        
        context.fill(fieldX, y, fieldX + fieldWidth, y + 20, bgColor)
        context.drawBorder(fieldX, y, fieldWidth, 20, borderColor.toInt())
        
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
            val changedSlots: Int2ObjectMap<ItemStackHash> = Int2ObjectOpenHashMap()
            
            val packet = ClickSlotC2SPacket(
                syncId.toInt(),
                revision.toInt(),
                slot.toShort(),
                button.toByte(),
                actionType,
                changedSlots,
                ItemStackHash.EMPTY

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
        if (activeField.isNotEmpty()) {
            when (activeField) {
                "syncId" -> syncId += chr
                "revision" -> revision += chr
                "slot" -> slot += chr
                "button" -> button += chr
                "buttonId" -> buttonId += chr
                "timesToSend" -> timesToSend += chr
            }
            return true
        }
        return super.charTyped(chr, modifiers)
    }
    
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        // Handle backspace
        if (keyCode == 259 && activeField.isNotEmpty()) { // GLFW_KEY_BACKSPACE
            when (activeField) {
                "syncId" -> if (syncId.isNotEmpty()) syncId = syncId.dropLast(1)
                "revision" -> if (revision.isNotEmpty()) revision = revision.dropLast(1)
                "slot" -> if (slot.isNotEmpty()) slot = slot.dropLast(1)
                "button" -> if (button.isNotEmpty()) button = button.dropLast(1)
                "buttonId" -> if (buttonId.isNotEmpty()) buttonId = buttonId.dropLast(1)
                "timesToSend" -> if (timesToSend.isNotEmpty()) timesToSend = timesToSend.dropLast(1)
            }
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle checkbox clicking
        val leftX = getContentX() + 20
        val checkboxX = leftX + 100
        val checkboxY = getContentY() + 227 // Adjust based on layout
        
        if (mouseX >= checkboxX && mouseX < checkboxX + 16 && 
            mouseY >= checkboxY && mouseY < checkboxY + 16) {
            delayPacket = !delayPacket
            return true
        }
        
        // Handle input field clicking
        activeField = getClickedField(mouseX, mouseY)
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    private fun getClickedField(mouseX: Double, mouseY: Double): String {
        val leftX = getContentX() + 20
        val fieldX = leftX + 100
        val fieldWidth = 120
        val rowHeight = 25
        var currentY = getContentY() + 80
        
        if (selectedPacketType == "Click Slot") {
            // Check syncId field
            if (mouseX >= fieldX && mouseX < fieldX + fieldWidth && 
                mouseY >= currentY && mouseY < currentY + 20) return "syncId"
            currentY += rowHeight
            
            // Check revision field
            if (mouseX >= fieldX && mouseX < fieldX + fieldWidth && 
                mouseY >= currentY && mouseY < currentY + 20) return "revision"
            currentY += rowHeight
            
            // Check slot field
            if (mouseX >= fieldX && mouseX < fieldX + fieldWidth && 
                mouseY >= currentY && mouseY < currentY + 20) return "slot"
            currentY += rowHeight
            
            // Check button field
            if (mouseX >= fieldX && mouseX < fieldX + fieldWidth && 
                mouseY >= currentY && mouseY < currentY + 20) return "button"
            currentY += 60 // Skip action dropdown
            
        } else if (selectedPacketType == "Button Click") {
            // Check syncId field
            if (mouseX >= fieldX && mouseX < fieldX + fieldWidth && 
                mouseY >= currentY && mouseY < currentY + 20) return "syncId"
            currentY += rowHeight
            
            // Check buttonId field
            if (mouseX >= fieldX && mouseX < fieldX + fieldWidth && 
                mouseY >= currentY && mouseY < currentY + 20) return "buttonId"
            currentY += 60
        }
        
        // Check timesToSend field
        if (mouseX >= fieldX && mouseX < fieldX + fieldWidth && 
            mouseY >= currentY && mouseY < currentY + 20) return "timesToSend"
        
        return ""
    }
}