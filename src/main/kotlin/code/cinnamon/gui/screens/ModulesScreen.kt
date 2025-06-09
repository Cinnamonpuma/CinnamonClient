package code.cinnamon.gui.screens

import code.cinnamon.modules.all.AutoclickerModule // Import AutoClickerModule
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.util.Identifier
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

class ModulesScreen : CinnamonScreen(Text.literal("Modules").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {
    private var selectedCategory = "All"
    private val categories = listOf("All", "Combat", "Movement", "Render", "Player", "World")
    private var scrollOffset = 0
    internal val expandedStates = mutableMapOf<String, Boolean>()
    private val baseModuleHeight = 60
    private val settingsModuleHeight = 200 // Increased to accommodate more settings
    private val moduleSpacing = 8 // Increased spacing between modules
    private val settingsAreaHeight = 120 // Fixed height for settings area

    private val maxScrollOffset: Int
        get() {
            val modules = getFilteredModules()
            val totalHeight = modules.sumOf { module ->
                if (expandedStates[module.name] == true) settingsModuleHeight + moduleSpacing else baseModuleHeight + moduleSpacing
            }
            return max(0, totalHeight - getContentHeight() + 40 - moduleSpacing)
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
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
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
                Text.literal(category).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
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
        drawRoundedRect(context, x, y, width, moduleHeight, backgroundColor)
        
        // Module card border
        val borderColor = if (module.isEnabled) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        drawRoundedBorder(context, x, y, width, moduleHeight, borderColor)
        
        // Module name
        context.drawText(
            textRenderer,
            Text.literal(module.name).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
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
            Text.literal(expandButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + width - expandButtonWidth - 12,
            y + 8,
            CinnamonTheme.primaryTextColor,
            true
        )
        
        // Module description
        context.drawText(
            textRenderer,
            Text.literal(module.description).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + 12,
            y + 22,
            CinnamonTheme.secondaryTextColor,
            false
        )
        
        // Bottom section with controls - always at the bottom of the base height
        val bottomSectionY = if (expandedStates[module.name] == true) {
            y + settingsModuleHeight - 30 // Bottom of expanded module
        } else {
            y + baseModuleHeight - 30 // Bottom of base module
        }
        
        // Toggle switch
        renderToggleSwitch(context, x + width - 50, bottomSectionY + 6, 30, 16, module.isEnabled, mouseX, mouseY)
        
        // Status indicator
        val statusColor = if (module.isEnabled) CinnamonTheme.successColor else CinnamonTheme.moduleDisabledColor
        context.fill(x + 12, bottomSectionY + 18, x + 20, bottomSectionY + 26, statusColor)
        
        // Keybinding info (if available)
        val keybindText = getModuleKeybind(module.name)
        if (keybindText.isNotEmpty()) {
            val keybindWidth = textRenderer.getWidth(keybindText)
            context.drawText(
                textRenderer,
                Text.literal(keybindText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
                x + width - keybindWidth - 60,
                bottomSectionY + 16,
                CinnamonTheme.secondaryTextColor,
                false
            )
        }

        // Render settings area if expanded
        if (expandedStates[module.name] == true) {
            val settingsY = y + 40 // Start after description
            val settingsHeight = settingsAreaHeight // Fixed height
            
            // Settings background with subtle border
            context.fill(x + 8, settingsY, x + width - 8, settingsY + settingsHeight, CinnamonTheme.contentBackground)
            context.fill(x + 8, settingsY, x + width - 8, settingsY + 1, CinnamonTheme.borderColor) // Top border
            
            renderModuleSettings(
                context,
                x + 12,
                settingsY + 5,
                width - 24,
                settingsHeight - 10,
                module,
                mouseX,
                mouseY,
                delta
            )
        }
    }

    private fun renderModuleSettings(context: DrawContext, x: Int, y: Int, width: Int, height: Int, module: Module, mouseX: Int, mouseY: Int, delta: Float) {
        var settingY = y

        when (module) {
            is AutoclickerModule -> {
                // Title
                context.drawText(textRenderer, Text.literal("Settings").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, settingY, CinnamonTheme.titleColor, true)
                settingY += 15

                val buttonWidth = 16
                val buttonHeight = 12
                val settingSpacing = 14

                // Min CPS Setting
                val minCpsText = "Min CPS: %.1f".format(module.minCPS)
                context.drawText(textRenderer, Text.literal(minCpsText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, settingY, CinnamonTheme.primaryTextColor, false)
                
                val minCpsButtonsX = x + width - 40
                // Minus button
                drawSettingButton(context, minCpsButtonsX, settingY - 1, buttonWidth, buttonHeight, "-", false)
                // Plus button  
                drawSettingButton(context, minCpsButtonsX + 20, settingY - 1, buttonWidth, buttonHeight, "+", false)
                settingY += settingSpacing

                // Max CPS Setting
                val maxCpsText = "Max CPS: %.1f".format(module.maxCPS)
                context.drawText(textRenderer, Text.literal(maxCpsText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, settingY, CinnamonTheme.primaryTextColor, false)
                
                val maxCpsButtonsX = x + width - 40
                // Minus button
                drawSettingButton(context, maxCpsButtonsX, settingY - 1, buttonWidth, buttonHeight, "-", false)
                // Plus button  
                drawSettingButton(context, maxCpsButtonsX + 20, settingY - 1, buttonWidth, buttonHeight, "+", false)
                settingY += settingSpacing

                // Humanization Setting
                val humanizationEnabled = module.enableHumanization
                drawCheckbox(context, x, settingY - 1, "Humanization", humanizationEnabled)
                settingY += settingSpacing

                // Burst Mode Setting
                val burstModeEnabled = module.burstMode
                drawCheckbox(context, x, settingY - 1, "Burst Mode", burstModeEnabled)
                settingY += settingSpacing

                // Left Click Setting
                val leftClickEnabled = module.leftClickEnabled
                drawCheckbox(context, x, settingY - 1, "Left Click", leftClickEnabled)
                settingY += settingSpacing

                // Right Click Setting
                val rightClickEnabled = module.rightClickEnabled
                drawCheckbox(context, x, settingY - 1, "Right Click", rightClickEnabled)
            }
            else -> {
                context.drawText(
                    textRenderer,
                    Text.literal("No settings available").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
                    x,
                    settingY,
                    CinnamonTheme.secondaryTextColor,
                    false
                )
            }
        }
    }
    
    private fun drawSettingButton(context: DrawContext, x: Int, y: Int, width: Int, height: Int, text: String, pressed: Boolean) {
        val bgColor = if (pressed) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
        val textColor = if (pressed) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor
        
        // Button background
        context.fill(x, y, x + width, y + height, bgColor)
        
        // Button border
        context.fill(x, y, x + width, y + 1, CinnamonTheme.borderColor) // Top
        context.fill(x, y + height - 1, x + width, y + height, CinnamonTheme.borderColor) // Bottom
        context.fill(x, y, x + 1, y + height, CinnamonTheme.borderColor) // Left
        context.fill(x + width - 1, y, x + width, y + height, CinnamonTheme.borderColor) // Right
        
        // Button text (centered)
        val textWidth = textRenderer.getWidth(text)
        context.drawText(
            textRenderer,
            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + (width - textWidth) / 2,
            y + 2,
            textColor,
            false
        )
    }
    
    private fun drawCheckbox(context: DrawContext, x: Int, y: Int, text: String, checked: Boolean) {
        val checkboxSize = 10
        val checkboxBg = if (checked) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
        
        // Checkbox background
        context.fill(x, y, x + checkboxSize, y + checkboxSize, checkboxBg)
        
        // Checkbox border
        context.fill(x, y, x + checkboxSize, y + 1, CinnamonTheme.borderColor) // Top
        context.fill(x, y + checkboxSize - 1, x + checkboxSize, y + checkboxSize, CinnamonTheme.borderColor) // Bottom
        context.fill(x, y, x + 1, y + checkboxSize, CinnamonTheme.borderColor) // Left
        context.fill(x + checkboxSize - 1, y, x + checkboxSize, y + checkboxSize, CinnamonTheme.borderColor) // Right
        
        // Checkmark
        if (checked) {
            context.drawText(
                textRenderer,
                Text.literal("✓").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
                x + 1,
                y + 1,
                CinnamonTheme.titleColor,
                false
            )
        }
        
        // Label text
        context.drawText(
            textRenderer,
            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + checkboxSize + 6,
            y + 1,
            CinnamonTheme.primaryTextColor,
            false
        )
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
            allModules.filter { getModuleCategory(it.name) == selectedCategory }
        }
    }
    
    private fun getModuleCategory(moduleName: String): String {
        return when (moduleName.lowercase()) {
            "autoclicker" -> "Player"
            "speed", "flight", "nofall" -> "Movement"
            else -> "Player"
        }
    }
    
    private fun getModuleKeybind(moduleName: String): String {
        val internalKeybindingName = "cinnamon.toggle_${moduleName.lowercase()}"
        val keyBinding = KeybindingManager.getKeybinding(internalKeybindingName)
        if (keyBinding != null) {
            val boundKey = KeyBindingHelper.getBoundKeyOf(keyBinding)
            if (boundKey != InputUtil.UNKNOWN_KEY) {
                return boundKey.localizedText.string
            }
        }
        return "None"
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
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
                    
                    // Bottom section Y position
                    val bottomSectionY = if (expandedStates[module.name] == true) {
                        currentY + settingsModuleHeight - 30
                    } else {
                        currentY + baseModuleHeight - 30
                    }
                    
                    // Check if clicking on toggle switch
                    val toggleX = contentX + 10 + contentWidth - 20 - 50
                    val toggleY = bottomSectionY + 6

                    if (mouseX >= toggleX && mouseX < toggleX + 30 &&
                        mouseY >= toggleY && mouseY < toggleY + 16) {
                        ModuleManager.toggleModule(module.name)
                        return true
                    }

                    // Check if clicking on expand button
                    val expandButtonText = if (expandedStates[module.name] == true) "[-]" else "[+]"
                    val expandButtonWidth = textRenderer.getWidth(expandButtonText)
                    val expandButtonX = contentX + 10 + contentWidth - 20 - expandButtonWidth - 12
                    val expandButtonY = currentY + 8

                    if (mouseX >= expandButtonX && mouseX < expandButtonX + expandButtonWidth &&
                        mouseY >= expandButtonY && mouseY < expandButtonY + textRenderer.fontHeight) {
                        expandedStates[module.name] = !(expandedStates[module.name] ?: false)
                        scrollOffset = min(scrollOffset, maxScrollOffset)
                        return true
                    }

                    // Handle settings clicks if module is expanded
                    if (expandedStates[module.name] == true && module is AutoclickerModule) {
                        val settingsX = contentX + 10 + 12
                        val settingsY = currentY + 40 + 5
                        val settingsWidth = contentWidth - 20 - 24
                        
                        if (handleAutoClickerSettings(mouseX, mouseY, settingsX, settingsY, settingsWidth, module)) {
                            return true
                        }
                    }
                    return@forEach
                }
                currentY += moduleHeight + moduleSpacing
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    private fun handleAutoClickerSettings(mouseX: Double, mouseY: Double, settingsX: Int, settingsY: Int, settingsWidth: Int, module: AutoclickerModule): Boolean {
        var checkY = settingsY + 15 // Skip title
        val buttonWidth = 16
        val buttonHeight = 12
        val settingSpacing = 14
        val checkboxSize = 10
        
        // Min CPS buttons
        val minCpsButtonsX = settingsX + settingsWidth - 40
        if (mouseY >= checkY - 1 && mouseY < checkY - 1 + buttonHeight) {
            // Minus button
            if (mouseX >= minCpsButtonsX && mouseX < minCpsButtonsX + buttonWidth) {
                module.setMinCPS(module.minCPS - 1.0f)
                return true
            }
            // Plus button
            if (mouseX >= minCpsButtonsX + 20 && mouseX < minCpsButtonsX + 20 + buttonWidth) {
                module.setMinCPS(module.minCPS + 1.0f)
                return true
            }
        }
        checkY += settingSpacing
        
        // Max CPS buttons
        val maxCpsButtonsX = settingsX + settingsWidth - 40
        if (mouseY >= checkY - 1 && mouseY < checkY - 1 + buttonHeight) {
            // Minus button
            if (mouseX >= maxCpsButtonsX && mouseX < maxCpsButtonsX + buttonWidth) {
                module.setMaxCPS(module.maxCPS - 1.0f)
                return true
            }
            // Plus button
            if (mouseX >= maxCpsButtonsX + 20 && mouseX < maxCpsButtonsX + 20 + buttonWidth) {
                module.setMaxCPS(module.maxCPS + 1.0f)
                return true
            }
        }
        checkY += settingSpacing
        
        // Humanization checkbox
        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Humanization") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
            module.setHumanizationEnabled(!module.enableHumanization)
            return true
        }
        checkY += settingSpacing
        
        // Burst Mode checkbox
        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Burst Mode") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
            module.setBurstMode(!module.burstMode)
            return true
        }
        checkY += settingSpacing
        
        // Left click checkbox
        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Left Click") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
            module.setLeftClickEnabled(!module.leftClickEnabled)
            return true
        }
        checkY += settingSpacing
        
        // Right click checkbox
        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Right Click") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
            module.setRightClickEnabled(!module.rightClickEnabled)
            return true
        }
        
        return false
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