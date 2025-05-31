package code.cinnamon.gui.screens

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
    private val maxScrollOffset get() = max(0, getFilteredModules().size * 65 - getContentHeight() + 40)
    
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
        val moduleHeight = 60
        val moduleSpacing = 5
        
        // Enable scissor test for clipping
        context.enableScissor(x, y, x + width, y + height)
        
        modules.forEachIndexed { index, module ->
            val moduleY = y - scrollOffset + index * (moduleHeight + moduleSpacing)
            
            // Only render if visible
            if (moduleY + moduleHeight >= y && moduleY <= y + height) {
                renderModuleCard(context, x, moduleY, width, moduleHeight, module, mouseX, mouseY, delta)
            }
        }
        
        context.disableScissor()
    }
    
    private fun renderModuleCard(context: DrawContext, x: Int, y: Int, width: Int, height: Int, module: Module, mouseX: Int, mouseY: Int, delta: Float) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
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
        renderToggleSwitch(context, x + width - 50, y + 15, 30, 16, module.isEnabled, mouseX, mouseY)
        
        // Status indicator
        val statusColor = if (module.isEnabled) CinnamonTheme.successColor else CinnamonTheme.moduleDisabledColor
        context.fill(x + 12, y + height - 12, x + 20, y + height - 4, statusColor)
        
        // Keybinding info (if available)
        val keybindText = getModuleKeybind(module.name)
        if (keybindText.isNotEmpty()) {
            val keybindWidth = textRenderer.getWidth(keybindText)
            context.drawText(
                textRenderer,
                Text.literal(keybindText),
                x + width - keybindWidth - 60,
                y + height - 14,
                CinnamonTheme.secondaryTextColor,
                false
            )
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
            val moduleHeight = 60
            val moduleSpacing = 5
            
            modules.forEachIndexed { index, module ->
                val moduleY = moduleListY - scrollOffset + index * (moduleHeight + moduleSpacing)
                
                if (mouseY >= moduleY && mouseY < moduleY + moduleHeight) {
                    // Check if clicking on toggle switch
                    val toggleX = contentX + contentWidth - 50
                    val toggleY = moduleY + 15
                    
                    if (mouseX >= toggleX && mouseX < toggleX + 30 &&
                        mouseY >= toggleY && mouseY < toggleY + 16) {
                        ModuleManager.toggleModule(module.name)
                        return true
                    }
                }
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