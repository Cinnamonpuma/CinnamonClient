package code.cinnamon.gui.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.util.Identifier
import net.minecraft.client.util.InputUtil
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import org.lwjgl.glfw.GLFW
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.keybindings.KeybindingManager
import kotlin.math.max
import kotlin.math.min

class KeybindingsScreen : CinnamonScreen(Text.literal("Keybindings").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {
    private var selectedKeybinding: String? = null
    private var isListening = false
    private var scrollOffset = 0
    private val keybindingHeight = 45
    private val keybindingSpacing = 5
    private val maxScrollOffset get() = max(0, getKeybindingEntries().size * (keybindingHeight + keybindingSpacing) - getKeybindingListHeight())

    data class KeybindingEntry(
        val name: String,
        val displayName: String,
        val description: String,
        val currentKey: Int
    )
    override fun close() {

        CinnamonGuiManager.openMainMenu()
    }
    override fun initializeComponents() {
        addButton(CinnamonButton(
            guiX + PADDING,
            getFooterY() + 8,
            60,
            CinnamonTheme.BUTTON_HEIGHT_SMALL,
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> CinnamonGuiManager.openMainMenu() }
        ))

        addButton(CinnamonButton(
            guiX + guiWidth - PADDING - 80,
            getFooterY() + 8,
            80,
            CinnamonTheme.BUTTON_HEIGHT_SMALL,
            Text.literal("Reset All").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> resetAllKeybindings() }
        ))
        addButton(CinnamonButton(
            guiX + guiWidth - PADDING - 170,
            getFooterY() + 8,
            80,
            CinnamonTheme.BUTTON_HEIGHT_SMALL,
            Text.literal("Save").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> saveKeybindings() },
            false
        ))
    }

    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()


        val headerHeight = 50

        context.drawText(
            textRenderer,
            Text.literal("Click on a key to change it").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            contentX + 15,
            contentY + 20,
            CinnamonTheme.titleColor,
            CinnamonTheme.enableTextShadow
        )



        if (isListening && selectedKeybinding != null) {
            val indicatorText = "Press a key to bind to ${getDisplayName(selectedKeybinding!!)} (ESC to cancel)"
            val indicatorWidth = textRenderer.getWidth(indicatorText)
            val indicatorX = contentX + (contentWidth - indicatorWidth) / 2
            val indicatorY = contentY + headerHeight + 10


            context.fill(
                indicatorX - 10,
                indicatorY - 5,
                indicatorX + indicatorWidth + 10,
                indicatorY + textRenderer.fontHeight + 5,
                CinnamonTheme.warningColor
            )

            context.drawText(
                textRenderer,
                Text.literal(indicatorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                indicatorX,
                indicatorY,
                CinnamonTheme.titleColor,
                CinnamonTheme.enableTextShadow
            )
        }


        val listY = contentY + headerHeight + (if (isListening) 35 else 10)
        val listHeight = getKeybindingListHeight() - (if (isListening) 45 else 10)

        renderKeybindingList(context, contentX + 10, listY, contentWidth - 20, listHeight, mouseX, mouseY)

        if (maxScrollOffset > 0) {
            renderScrollbar(context, contentX + contentWidth - 8, listY, 6, listHeight)
        }
    }

    private fun renderKeybindingList(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int) {
        val entries = getKeybindingEntries()


        context.enableScissor(x, y, x + width, y + height)

        entries.forEachIndexed { index, entry ->
            val entryY = y - scrollOffset + index * (keybindingHeight + keybindingSpacing)

            if (entryY + keybindingHeight >= y && entryY <= y + height) {
                renderKeybindingEntry(context, x, entryY, width, keybindingHeight, entry, mouseX, mouseY)
            }
        }

        context.disableScissor()
    }

    private fun renderKeybindingEntry(context: DrawContext, x: Int, y: Int, width: Int, height: Int, entry: KeybindingEntry, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
        val isSelected = selectedKeybinding == entry.name
        val isListeningToThis = isListening && isSelected

        val backgroundColor = when {
            isListeningToThis -> CinnamonTheme.warningColor
            isSelected -> CinnamonTheme.accentColor
            isHovered -> CinnamonTheme.cardBackgroundHover
            else -> CinnamonTheme.cardBackground
        }


        drawRoundedRect(context, x, y, width, height, backgroundColor)

        val borderColor = when {
            isListeningToThis -> CinnamonTheme.titleColor
            isSelected -> CinnamonTheme.accentColor
            else -> CinnamonTheme.borderColor
        }
        drawRoundedBorder(context, x, y, width, height, borderColor)


        val nameColor = if (isListeningToThis) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor
        context.drawText(
            textRenderer,
            Text.literal(entry.displayName).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + 12,
            y + 8,
            nameColor,
            CinnamonTheme.enableTextShadow
        )

        val descColor = if (isListeningToThis) CinnamonTheme.titleColor else CinnamonTheme.secondaryTextColor
        context.drawText(
            textRenderer,
            Text.literal(entry.description).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + 12,
            y + 22,
            descColor,
            CinnamonTheme.enableTextShadow
        )

        val keyName = getKeyName(entry.currentKey)
        val keyWidth = textRenderer.getWidth(keyName)
        val keyButtonWidth = maxOf(keyWidth + 20, 60)
        val keyButtonX = x + width - keyButtonWidth - 10
        val keyButtonY = y + (height - 24) / 2

        val keyButtonBg = when {
            isListeningToThis -> CinnamonTheme.primaryButtonBackgroundPressed
            isHovered && mouseX >= keyButtonX && mouseX < keyButtonX + keyButtonWidth -> CinnamonTheme.buttonBackgroundHover
            else -> CinnamonTheme.buttonBackground
        }

        drawRoundedRect(context, keyButtonX, keyButtonY, keyButtonWidth, 24, keyButtonBg)
        drawRoundedBorder(context, keyButtonX, keyButtonY, keyButtonWidth, 24, CinnamonTheme.borderColor)

        val keyTextColor = if (isListeningToThis) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor
        context.drawText(
            textRenderer,
            Text.literal(keyName).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            keyButtonX + (keyButtonWidth - keyWidth) / 2,
            keyButtonY + 8,
            keyTextColor,
            CinnamonTheme.enableTextShadow
        )

        if (hasConflict(entry)) {
            context.fill(x + width - 25, y + 5, x + width - 5, y + 10, CinnamonTheme.errorColor)
        }
    }

    private fun renderScrollbar(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        context.fill(x, y, x + width, y + height, CinnamonTheme.borderColor)

        if (maxScrollOffset > 0) {
            val thumbHeight = max(20, (height * height) / (maxScrollOffset + height))
            val thumbY = y + (scrollOffset * (height - thumbHeight)) / maxScrollOffset

            context.fill(x + 1, thumbY.toInt(), x + width - 1, thumbY.toInt() + thumbHeight, CinnamonTheme.accentColor)
        }
    }

    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        context.fill(x + 2, y, x + width - 2, y + height, color)
        context.fill(x, y + 2, x + width, y + height - 2, color)
        context.fill(x + 1, y + 1, x + 2, y + 2, color)
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color)
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color)
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color)
    }

    private fun drawRoundedBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        context.fill(x + 2, y, x + width - 2, y + 1, color)
        context.fill(x + 2, y + height - 1, x + width - 2, y + height, color)
        context.fill(x, y + 2, x + 1, y + height - 2, color)
        context.fill(x + width - 1, y + 2, x + width, y + height - 2, color)
        context.fill(x + 1, y + 1, x + 2, y + 2, color)
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color)
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color)
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color)
    }

    private fun getKeybindingEntries(): List<KeybindingEntry> {
        val keybindings = KeybindingManager.getAllKeybindings()
        return keybindings.map { (name, keyBinding) ->
            KeybindingEntry(
                name = name,
                displayName = getDisplayName(name),
                description = getDescription(name),
                currentKey = KeyBindingHelper.getBoundKeyOf(keyBinding).code
            )
        }
    }

    private fun getDisplayName(name: String): String {
        return when (name) {
            "cinnamon.toggle_speed" -> "Toggle Speed"
            "cinnamon.toggle_flight" -> "Toggle Flight"
            "cinnamon.toggle_nofall" -> "Toggle No Fall"
            else -> name.replace("cinnamon.", "").replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    private fun getDescription(name: String): String {
        return when (name) {
            "cinnamon.toggle_speed" -> "Toggles the speed module on/off"
            "cinnamon.toggle_flight" -> "Toggles the flight module on/off"
            "cinnamon.toggle_nofall" -> "Toggles the no fall damage module on/off"
            else -> "Custom keybinding"
        }
    }

    private fun getKeyName(keyCode: Int): String {
        return when (keyCode) {
            GLFW.GLFW_KEY_UNKNOWN -> "None"
            else -> InputUtil.fromKeyCode(keyCode, 0).localizedText.string
        }
    }

    private fun hasConflict(entry: KeybindingEntry): Boolean {
        val entries = getKeybindingEntries()
        return entries.count { it.currentKey == entry.currentKey && it.currentKey != GLFW.GLFW_KEY_UNKNOWN } > 1
    }

    private fun getKeybindingListHeight(): Int {
        return getContentHeight() - 60
    }

    private fun resetAllKeybindings() {
        KeybindingManager.updateKeybinding("cinnamon.toggle_speed", GLFW.GLFW_KEY_V)
        KeybindingManager.updateKeybinding("cinnamon.toggle_flight", GLFW.GLFW_KEY_F)
        KeybindingManager.updateKeybinding("cinnamon.toggle_nofall", GLFW.GLFW_KEY_N)

        selectedKeybinding = null
        isListening = false
    }

    private fun saveKeybindings() {
        selectedKeybinding = null
        isListening = false
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isListening) { // If was listening, any click stops it.
            isListening = false
            selectedKeybinding = null
            // Potentially consume the click or let super handle if it was on a CinnamonButton
            // For now, let's assume a click while listening always cancels listening and does nothing else.
            return true
        }

        // Let CinnamonButtons handle their clicks first
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true
        }

        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()

        // Mirror calculations from renderContent for listY and listHeight
        val renderTimeHeaderHeight = 50 // From renderContent
        val listTopY = contentY + renderTimeHeaderHeight + (if (isListening) 35 else 10) // Should always use isListening = false here, as we checked above
        val listActualHeight = getKeybindingListHeight() - (if (isListening) 45 else 10) // Same, isListening should be false for this click pass

        // Define the clickable list area (slightly inset like in renderKeybindingList call)
        val listAreaXStart = contentX + 10
        val listAreaXEnd = contentX + contentWidth - 10 // renderKeybindingList uses contentWidth - 20 for width, so x + width is contentX + 10 + contentWidth - 20 = contentX + contentWidth -10

        if (mouseX >= listAreaXStart && mouseX < listAreaXEnd &&
            mouseY >= listTopY && mouseY < listTopY + listActualHeight) {

            val entries = getKeybindingEntries()
            entries.forEachIndexed { index, entry ->
                // Calculate entryY exactly as in renderKeybindingList
                val entryY = listTopY - scrollOffset + index * (keybindingHeight + keybindingSpacing)

                if (mouseY >= entryY && mouseY < entryY + keybindingHeight) {
                    // Click is within this entry
                    selectedKeybinding = entry.name
                    isListening = true // Start listening for the next key press
                    return true
                }
            }
        }

        // If no list item was clicked, and no CinnamonButton was clicked.
        return false // Explicitly false if we didn't handle it. Let Screen default if necessary.
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()

        // Scroll area should match the visual list area defined in renderContent
        val renderTimeHeaderHeight = 50
        // Note: isListening state might change scrollable area visual size slightly,
        // but for hit-testing scroll, using the non-listening dimensions might be more stable
        // or ensure it always matches the largest possible scroll area.
        // Let's use the same logic as renderContent for consistency.
        val listTopY = contentY + renderTimeHeaderHeight + (if (isListening) 35 else 10)
        val listActualHeight = getKeybindingListHeight() - (if (isListening) 45 else 10)

        val listAreaXStart = contentX + 10
        val listAreaXEnd = contentX + contentWidth - 10

        if (mouseX >= listAreaXStart && mouseX < listAreaXEnd &&
            mouseY >= listTopY && mouseY < listTopY + listActualHeight) {

            val scrollAmount = (verticalAmount * 20).toInt() // Standard scroll multiplier
            scrollOffset = max(0, min(maxScrollOffset, scrollOffset - scrollAmount))
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (isListening && selectedKeybinding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                isListening = false
                selectedKeybinding = null
            } else {
                KeybindingManager.updateKeybinding(selectedKeybinding!!, keyCode)
                isListening = false
                selectedKeybinding = null
            }
            return true
        }

        return super.keyPressed(keyCode, scanCode, modifiers)
    }
}