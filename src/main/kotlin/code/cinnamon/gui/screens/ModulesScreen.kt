package code.cinnamon.gui.screens

import code.cinnamon.modules.all.AutoclickerModule // Import AutoClickerModule
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.keybindings.KeybindingManager
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.util.InputUtil
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.modules.ModuleManager
import code.cinnamon.modules.Module
import kotlin.math.max
import kotlin.math.min

class ModulesScreen : CinnamonScreen(Text.literal("Modules")) {
    private var selectedCategory = "All"
    private val categories = listOf("All", "Combat", "Movement", "Render", "Player", "World")
    private var scrollOffset = 0
    internal val expandedStates = mutableMapOf<String, Boolean>()
    private val baseModuleHeight = 60
    private val settingsModuleHeight = 160 // Changed from 120
    private val moduleSpacing = 5

    private val maxScrollOffset: Int
        get() {
            val modules = getFilteredModules()
            val totalHeight = modules.sumOf { module ->
                if (expandedStates[module.name] == true) settingsModuleHeight + moduleSpacing else baseModuleHeight + moduleSpacing
            }
            return max(0, totalHeight - getContentHeight() + 40 - moduleSpacing) // Deduct last module spacing
        }
    
    override fun initializeComponents() {
        val contentX = getContentX()
        val contentWidth = getContentWidth()
        
        // Back button
        addButton(CinnamonButton(
            guiX + PADDING,
            getFooterY() + 8,
            60,
            CinnamonTheme.BUTTON_HEIGHT_SMALL,
            Text.literal("Back"),
            { _, _ -> CinnamonGuiManager.openMainMenu() }
        ))
        
        // Category buttons
        val categoryButtonWidth = 80
        val categorySpacing = 5
        val totalCategoryWidth = categories.size * categoryButtonWidth + (categories.size - 1) * categorySpacing
        val categoryStartX = contentX + (contentWidth - totalCategoryWidth) / 2
        
        categories.forEachIndexed { index, category ->
            val buttonX = categoryStartX + index * (categoryButtonWidth + categorySpacing)
            addButton(CinnamonButton(
                buttonX,
                getContentY() + 10,
                categoryButtonWidth,
                CinnamonTheme.BUTTON_HEIGHT_SMALL,
                Text.literal(category),
                { _, _ -> selectedCategory = category },
                selectedCategory == category
            ))
        }
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        
        // Content background
        context.fill(
            contentX,
            contentY,
            contentX + contentWidth,
            contentY + contentHeight,
            CinnamonTheme.contentBackground
        )
        
        // Category selection area
        val categoryAreaHeight = 50
        context.fill(
            contentX,
            contentY,
            contentX + contentWidth,
            contentY + categoryAreaHeight,
            CinnamonTheme.cardBackground
        )
        
        // Category area border
        context.fill(
            contentX,
            contentY + categoryAreaHeight - 1,
            contentX + contentWidth,
            contentY + categoryAreaHeight,
            CinnamonTheme.borderColor
        )
        
        // Module list area
        val moduleListY = contentY + categoryAreaHeight + 10
        val moduleListHeight = contentHeight - categoryAreaHeight - 20
        
        // Render modules
        renderModuleList(context, contentX + 10, moduleListY, contentWidth - 20, moduleListHeight, mouseX, mouseY, delta)
        
        // Scroll indicator if needed
        if (maxScrollOffset > 0) {
            renderScrollbar(context, contentX + contentWidth - 8, moduleListY, 6, moduleListHeight)
        }
    }
    
    private fun renderModuleList(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
        val modules = getFilteredModules()
        var currentY = y - scrollOffset
        
        // Enable scissor test for clipping
        context.enableScissor(x, y, x + width, y + height)
        
        modules.forEach { module ->
            val moduleHeight = if (expandedStates[module.name] == true) settingsModuleHeight else baseModuleHeight
            if (currentY + moduleHeight >= y && currentY <= y + height) {
                renderModuleCard(context, x, currentY, width, moduleHeight, module, mouseX, mouseY, delta)
            }
            currentY += moduleHeight + moduleSpacing
        }
        
        context.disableScissor()
    }
    
    private fun renderModuleCard(context: DrawContext, x: Int, y: Int, width: Int, moduleHeight: Int, module: Module, mouseX: Int, mouseY: Int, delta: Float) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + moduleHeight
        val backgroundColor = if (module.isEnabled) {
            if (isHovered) CinnamonTheme.moduleBackgroundEnabled else CinnamonTheme.moduleEnabledColor
        } else {
            if (isHovered) CinnamonTheme.cardBackgroundHover else CinnamonTheme.cardBackground
        }
        
        // Module card background
        drawRoundedRect(context, x, y, width, height, backgroundColor)
        
        // Module card border
        val borderColor = if (module.isEnabled) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        drawRoundedBorder(context, x, y, width, height, borderColor)
        
        // Module name
        context.drawText(
            textRenderer,
            Text.literal(module.name),
            x + 12,
            y + 8,
            if (module.isEnabled) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor,
            true
        )

        // Expand button
        val expandButtonText = if (expandedStates[module.name] == true) "[-]" else "[+]"
        val expandButtonWidth = textRenderer.getWidth(expandButtonText)
        context.drawText(
            textRenderer,
            Text.literal(expandButtonText),
            x + width - expandButtonWidth - 12,
            y + 8,
            CinnamonTheme.primaryTextColor,
            true
        )
        
        // Module description
        context.drawText(
            textRenderer,
            Text.literal(module.description),
            x + 12,
            y + 22,
            CinnamonTheme.secondaryTextColor,
            false
        )
        
        // Toggle switch
        renderToggleSwitch(context, x + width - 50, y + moduleHeight - 20 - 16, 30, 16, module.isEnabled, mouseX, mouseY) // Adjusted Y for bottom alignment
        
        // Status indicator
        val statusColor = if (module.isEnabled) CinnamonTheme.successColor else CinnamonTheme.moduleDisabledColor
        context.fill(x + 12, y + moduleHeight - 12, x + 20, y + moduleHeight - 4, statusColor)
        
        // Keybinding info (if available)
        val keybindText = getModuleKeybind(module.name)
        if (keybindText.isNotEmpty()) {
            val keybindWidth = textRenderer.getWidth(keybindText)
            context.drawText(
                textRenderer,
                Text.literal(keybindText),
                x + width - keybindWidth - 60,
                y + moduleHeight - 14,
                CinnamonTheme.secondaryTextColor,
                false
            )
        }

        if (expandedStates[module.name] == true) {
            // moduleHeight here is already settingsModuleHeight when expanded
            val settingsContentY = y + 45 
            val footerControlsTopY = y + moduleHeight - 40 // Top Y of the area reserved for bottom controls
            val calculatedSettingsRenderHeight = footerControlsTopY - settingsContentY

            if (calculatedSettingsRenderHeight > 0) { // Ensure positive height
                renderModuleSettings(
                    context,
                    x + 10, // settingsRenderX
                    settingsContentY,
                    width - 20, // settingsRenderWidth
                    calculatedSettingsRenderHeight,
                    module,
                    mouseX,
                    mouseY,
                    delta
                )
            }
        }
    }

    private fun renderModuleSettings(context: DrawContext, x: Int, y: Int, width: Int, height: Int, module: Module, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(x, y, x + width, y + height, CinnamonTheme.contentBackground) // Example background
        var settingY = y + 5

        when (module) {
            is AutoclickerModule -> {
                context.drawText(textRenderer, Text.literal("AutoClicker Settings:"), x + 5, settingY, CinnamonTheme.titleColor, false)
                settingY += 12 // Line spacing after title

                val buttonWidth = textRenderer.getWidth("[+]") + 2 // Width of [+] or [-]
                val settingItemX = x + 10
                val valueXOffset = x + width - 50 // Align values and buttons to the right
                val buttonSpacing = 2

                // CPS Setting
                val cpsText = "CPS: %.1f".format(module.getClicksPerSecond())
                context.drawText(textRenderer, Text.literal(cpsText), settingItemX, settingY + 1, CinnamonTheme.primaryTextColor, false)
                context.drawText(textRenderer, Text.literal("[-]"), valueXOffset, settingY + 1, CinnamonTheme.primaryTextColor, true)
                context.drawText(textRenderer, Text.literal("[+]"), valueXOffset + buttonWidth + buttonSpacing, settingY + 1, CinnamonTheme.primaryTextColor, true)
                settingY += textRenderer.fontHeight + 4 // Line spacing

                // Randomize Setting
                val randomizeText = "[${if (module.isRandomizeClicksEnabled()) "X" else " "}] Randomize"
                context.drawText(textRenderer, Text.literal(randomizeText), settingItemX, settingY + 1, CinnamonTheme.primaryTextColor, true)
                settingY += textRenderer.fontHeight + 4 // Line spacing

                // Click Hold Time Setting
                val holdTimeText = "Hold: ${module.getClickHoldTimeMs()}ms"
                context.drawText(textRenderer, Text.literal(holdTimeText), settingItemX, settingY + 1, CinnamonTheme.primaryTextColor, false)
                context.drawText(textRenderer, Text.literal("[-]"), valueXOffset, settingY + 1, CinnamonTheme.primaryTextColor, true)
                context.drawText(textRenderer, Text.literal("[+]"), valueXOffset + buttonWidth + buttonSpacing, settingY + 1, CinnamonTheme.primaryTextColor, true)
                settingY += textRenderer.fontHeight + 4 // Line spacing
                
                // Left Click Enabled Setting
                val leftClickText = "[${if (module.isLeftClickEnabled()) "X" else " "}] Left Click"
                context.drawText(textRenderer, Text.literal(leftClickText), settingItemX, settingY + 1, CinnamonTheme.primaryTextColor, true)
                settingY += textRenderer.fontHeight + 4 // Line spacing

                // Right Click Enabled Setting
                val rightClickText = "[${if (module.isRightClickEnabled()) "X" else " "}] Right Click"
                context.drawText(textRenderer, Text.literal(rightClickText), settingItemX, settingY + 1, CinnamonTheme.primaryTextColor, true)
                // settingY += textRenderer.fontHeight + 4 // No increment needed for the last item
            }
            // Add cases for other modules with settings here
            else -> {
                context.drawText(
                    textRenderer,
                    Text.literal("No specific settings for ${module.name}"),
                    x + 5,
                    settingY,
                    CinnamonTheme.secondaryTextColor,
                    false
                )
            }
        }
    }
    
    private fun renderToggleSwitch(context: DrawContext, x: Int, y: Int, width: Int, height: Int, enabled: Boolean, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
        
        // Switch background
        val switchBg = if (enabled) {
            if (isHovered) CinnamonTheme.accentColorHover else CinnamonTheme.accentColor
        } else {
            if (isHovered) CinnamonTheme.buttonBackgroundHover else CinnamonTheme.buttonBackground
        }
        
        drawRoundedRect(context, x, y, width, height, switchBg)
        
        // Switch knob
        val knobSize = height - 4
        val knobX = if (enabled) x + width - knobSize - 2 else x + 2
        val knobY = y + 2
        
        drawRoundedRect(context, knobX, knobY, knobSize, knobSize, CinnamonTheme.titleColor)
    }
    
    private fun renderScrollbar(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Scrollbar track
        context.fill(x, y, x + width, y + height, CinnamonTheme.borderColor)
        
        // Scrollbar thumb
        val thumbHeight = max(20, (height * height) / (maxScrollOffset + height))
        val thumbY = y + (scrollOffset * (height - thumbHeight)) / maxScrollOffset
        
        context.fill(x + 1, thumbY.toInt(), x + width - 1, thumbY.toInt() + thumbHeight, CinnamonTheme.accentColor)
    }
    
    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        // Main rectangle
        context.fill(x + 2, y, x + width - 2, y + height, color)
        context.fill(x, y + 2, x + width, y + height - 2, color)
        
        // Corner pixels for rounded effect
        context.fill(x + 1, y + 1, x + 2, y + 2, color)
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color)
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color)
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color)
    }
    
    private fun drawRoundedBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        // Top and bottom borders
        context.fill(x + 2, y, x + width - 2, y + 1, color)
        context.fill(x + 2, y + height - 1, x + width - 2, y + height, color)
        
        // Left and right borders
        context.fill(x, y + 2, x + 1, y + height - 2, color)
        context.fill(x + width - 1, y + 2, x + width, y + height - 2, color)
        
        // Corner borders
        context.fill(x + 1, y + 1, x + 2, y + 2, color)
        context.fill(x + width - 2, y + 1, x + width - 1, y + 2, color)
        context.fill(x + 1, y + height - 2, x + 2, y + height - 1, color)
        context.fill(x + width - 2, y + height - 2, x + width - 1, y + height - 1, color)
    }
    
    private fun getFilteredModules(): List<Module> {
        val allModules = ModuleManager.getModules()
        return if (selectedCategory == "All") {
            allModules
        } else {
            // Filter by category - you might want to add category property to Module class
            allModules.filter { getModuleCategory(it.name) == selectedCategory }
        }
    }
    
    private fun getModuleCategory(moduleName: String): String {
        // Simple category mapping - you can make this more sophisticated
        return when (moduleName.lowercase()) {
            "speed", "flight", "nofall" -> "Movement"
            else -> "Player"
        }
    }
    
    private fun getModuleKeybind(moduleName: String): String {
        // Construct the internal keybinding name (e.g., "cinnamon.toggle_speed")
        // This assumes a naming convention. If module names in ModuleManager
        // are "Speed", "Flight", etc., and keybinding names are "cinnamon.toggle_speed",
        // "cinnamon.toggle_flight", we need to map them.
        // A simple way is to lowercase and prepend:
        val internalKeybindingName = "cinnamon.toggle_${moduleName.lowercase()}"

        val keyBinding = KeybindingManager.getKeybinding(internalKeybindingName)
        if (keyBinding != null) {
            val boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding)
            // Use localizedText, which gives the proper name like "V", "F", "Mouse Button 1"
            // For unknown or unbound keys, localizedText might be empty or "None"
            // InputUtil.UNKNOWN_KEY is a Key object representing an unbound key.
            if (boundKey != InputUtil.UNKNOWN_KEY) {
                return boundKey.localizedText.string
            }
        }
        return "None" // Or an empty string, depending on desired display for unbound keys
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Handle module toggle clicks
        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val moduleListY = contentY + 60
        val moduleListHeight = contentHeight - 70
        
        if (mouseX >= contentX && mouseX < contentX + contentWidth &&
            mouseY >= moduleListY && mouseY < moduleListY + moduleListHeight) {
            
            val modules = getFilteredModules()
            var currentY = moduleListY - scrollOffset
            
            modules.forEach { module ->
                val moduleHeight = if (expandedStates[module.name] == true) settingsModuleHeight else baseModuleHeight
                if (mouseY >= currentY && mouseY < currentY + moduleHeight) {
                    // Check if clicking on toggle switch
                    val toggleX = contentX + 10 + contentWidth - 20 - 50 // x + width - 50, relative to module card
                    val toggleY = currentY + moduleHeight - 20 - 16 // Adjusted Y for bottom alignment

                    if (mouseX >= toggleX && mouseX < toggleX + 30 &&
                        mouseY >= toggleY && mouseY < toggleY + 16) {
                        ModuleManager.toggleModule(module.name)
                        return true
                    }

                    // Check if clicking on expand button
                    val expandButtonText = if (expandedStates[module.name] == true) "[-]" else "[+]"
                    val expandButtonWidth = textRenderer.getWidth(expandButtonText)
                    val expandButtonX = contentX + 10 + contentWidth - 20 - expandButtonWidth - 12 // x + width - expandButtonWidth - 12
                    val expandButtonY = currentY + 8

                    if (mouseX >= expandButtonX && mouseX < expandButtonX + expandButtonWidth &&
                        mouseY >= expandButtonY && mouseY < expandButtonY + textRenderer.fontHeight) {
                        expandedStates[module.name] = !(expandedStates[module.name] ?: false)
                        // Recalculate scroll offset if needed, though maxScrollOffset handles overall limit
                        scrollOffset = min(scrollOffset, maxScrollOffset)
                        return true
                    }

                    // If module is expanded and is AutoclickerModule, check for settings clicks
                    if (expandedStates[module.name] == true && module is AutoclickerModule) {
                        val settingsRenderX = contentX + 10 + 10 // As passed to renderModuleSettings (x + 10)
                        val settingsRenderY = currentY + 45      // As passed to renderModuleSettings (y + 45)
                        // val settingsRenderWidth = (contentWidth - 20) - 20 // As passed to renderModuleSettings (width - 20)
                        // val calculatedSettingsRenderHeight = moduleHeight - 40 - 45 // moduleHeight is settingsModuleHeight

                        // Check if click is within the general settings area (optional, but good for containment)
                        // val settingsContentY_abs = currentY + 45
                        // val footerControlsTopY_abs = currentY + moduleHeight - 40
                        // val calculatedSettingsRenderHeight_abs = footerControlsTopY_abs - settingsContentY_abs
                        // val settingsAreaWidth_abs = (contentX + 10 + contentWidth - 20) - (contentX + 10 + 10) // (cardX + cardWidth - padding) - (cardX + padding)


                        // Re-calculate layout similar to renderModuleSettings to find clickable areas
                        var checkY = settingsRenderY + 5 // Initial Y inside renderModuleSettings
                        
                        // Title (not clickable)
                        checkY += 12 // Line spacing after title

                        val buttonWidth = textRenderer.getWidth("[+]") + 2
                        val settingItemX_abs = settingsRenderX + 10
                        // Width of the settings area: (contentWidth -20 for module list) -20 for settings padding = contentWidth - 40
                        val valueXOffset_abs = settingsRenderX + ( (contentWidth - 20) - 20 ) - 50 // settings_area_x + settings_area_width - 50
                        val buttonElementWidth = textRenderer.getWidth("[+]") // Actual width of the text "[+]" or "[-]"

                        // CPS Setting
                        val cpsMinusButtonX = valueXOffset_abs
                        val cpsPlusButtonX = valueXOffset_abs + buttonWidth + 2
                        if (mouseY >= checkY && mouseY < checkY + textRenderer.fontHeight) {
                            if (mouseX >= cpsMinusButtonX && mouseX < cpsMinusButtonX + buttonElementWidth) {
                                module.setCPS(module.getClicksPerSecond() - 1.0f)
                                return true
                            }
                            if (mouseX >= cpsPlusButtonX && mouseX < cpsPlusButtonX + buttonElementWidth) {
                                module.setCPS(module.getClicksPerSecond() + 1.0f)
                                return true
                            }
                        }
                        checkY += textRenderer.fontHeight + 4 // Line spacing

                        // Randomize Setting
                        val randomizeTextX = settingItemX_abs
                        val randomizeTextWidth = textRenderer.getWidth("[X] Randomize") // Approx width
                        if (mouseX >= randomizeTextX && mouseX < randomizeTextX + randomizeTextWidth &&
                            mouseY >= checkY && mouseY < checkY + textRenderer.fontHeight) {
                            module.setRandomizeEnabled(!module.isRandomizeClicksEnabled())
                            return true
                        }
                        checkY += textRenderer.fontHeight + 4 // Line spacing

                        // Click Hold Time Setting
                        val holdMinusButtonX = valueXOffset_abs
                        val holdPlusButtonX = valueXOffset_abs + buttonWidth + 2
                        if (mouseY >= checkY && mouseY < checkY + textRenderer.fontHeight) {
                            if (mouseX >= holdMinusButtonX && mouseX < holdMinusButtonX + buttonElementWidth) {
                                module.setClickHoldTime(module.getClickHoldTimeMs() - 10)
                                return true
                            }
                            if (mouseX >= holdPlusButtonX && mouseX < holdPlusButtonX + buttonElementWidth) {
                                module.setClickHoldTime(module.getClickHoldTimeMs() + 10)
                                return true
                            }
                        }
                        checkY += textRenderer.fontHeight + 4 // Line spacing
                        
                        // Left Click Enabled Setting
                        val leftClickTextX = settingItemX_abs
                        val leftClickTextWidth = textRenderer.getWidth("[X] Left Click")
                        if (mouseX >= leftClickTextX && mouseX < leftClickTextX + leftClickTextWidth &&
                            mouseY >= checkY && mouseY < checkY + textRenderer.fontHeight) {
                            module.setLeftClickEnabled(!module.isLeftClickEnabled())
                            return true
                        }
                        checkY += textRenderer.fontHeight + 4 // Line spacing

                        // Right Click Enabled Setting
                        val rightClickTextX = settingItemX_abs
                        val rightClickTextWidth = textRenderer.getWidth("[X] Right Click")
                        if (mouseX >= rightClickTextX && mouseX < rightClickTextX + rightClickTextWidth &&
                            mouseY >= checkY && mouseY < checkY + textRenderer.fontHeight) {
                            module.setRightClickEnabled(!module.isRightClickEnabled())
                            return true
                        }
                    }
                    return@forEach // Found the clicked module, stop iterating this forEach
                }
                currentY += moduleHeight + moduleSpacing
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val contentX = getContentX()
        val contentY = getContentY() + 60
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight() - 70
        
        if (mouseX >= contentX && mouseX < contentX + contentWidth &&
            mouseY >= contentY && mouseY < contentY + contentHeight) {
            
            val scrollAmount = (verticalAmount * 20).toInt()
            scrollOffset = max(0, min(maxScrollOffset, scrollOffset - scrollAmount))
            return true
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
}