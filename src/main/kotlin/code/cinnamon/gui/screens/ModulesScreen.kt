package code.cinnamon.gui.screens

import code.cinnamon.modules.all.AutoclickerModule
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.keybindings.KeybindingManager
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.util.InputUtil
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.modules.ModuleManager
import code.cinnamon.modules.Module
import code.cinnamon.hud.HudManager
import code.cinnamon.hud.HudElement
import code.cinnamon.hud.elements.KeystrokesHudElement
import code.cinnamon.hud.elements.PacketHandlerHudElement
import kotlin.math.max
import kotlin.math.min

import code.cinnamon.gui.screens.ColorPickerScreen
import code.cinnamon.gui.screens.SettingsHelper
import code.cinnamon.modules.BooleanSetting
import code.cinnamon.modules.ColorSetting
import code.cinnamon.modules.DoubleSetting
import code.cinnamon.modules.ModeSetting
import code.cinnamon.modules.Setting
import code.cinnamon.modules.all.ChatPrefixModule
import code.cinnamon.util.MinecraftColorCodes

import code.cinnamon.gui.components.CinnamonTextField

class ModulesScreen : CinnamonScreen(Text.literal("Modules").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {
    private var selectedCategory = "All"
    private val categories = listOf("All", "Combat", "Movement", "Render", "Player", "World")
    private val categoryButtons = mutableListOf<CinnamonButton>()
    private var scrollOffset = 0
    private var searchQuery = ""
    private lateinit var searchBar: CinnamonTextField
    internal val expandedStates = mutableMapOf<String, Boolean>()
    private val baseModuleHeight = 60
    private fun getSettingHeight(setting: Setting<*>): Int {
        return when (setting) {
            is code.cinnamon.modules.LookAtHudSetting -> 14
            is BooleanSetting -> 14
            is DoubleSetting -> 14
            is ColorSetting -> 14
            is ModeSetting -> {
                if (setting.name == "Prefix Color") {
                    val allColors = code.cinnamon.util.MinecraftColorCodes.entries
                    val itemBoxHeight = 20
                    val itemBoxWidth = 110
                    val horizontalSpacing = 4
                    val verticalSpacing = 4
                    val itemsPerRow = kotlin.math.max(1, (getContentWidth() - 44 + horizontalSpacing) / (itemBoxWidth + horizontalSpacing))
                    val numRows = (allColors.size + itemsPerRow - 1) / itemsPerRow
                    numRows * (itemBoxHeight + verticalSpacing)
                } else {
                    14
                }
            }
        }
    }

    private fun getSettingsHeight(settings: List<Setting<*>>): Int {
        return settings.sumOf { getSettingHeight(it) }
    }

    private val settingsModuleHeight: (Module) -> Int = { module ->
        baseModuleHeight + getSettingsHeight(module.settings) + 20
    }
    private val settingsHudElementHeight = 148 + 14
    private val moduleSpacing = 8
    private val settingsAreaHeight = 120

    private fun Int.toRGBAHexString(): String = String.format("#%08X", this)
    private fun Int.toRGBHexString(): String = String.format("#%06X", this and 0xFFFFFF)

    private val maxScrollOffset: Int
        get() {
            val items = getFilteredModules()
            if (items.isEmpty()) return 0
            val totalHeightIncludingSpacing = items.sumOf { item ->
                when (item) {
                    is Module -> (if (expandedStates[item.name] == true) settingsModuleHeight(item) else baseModuleHeight) + moduleSpacing
                    is HudElement -> (if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight) + moduleSpacing
                    else -> 0
                }
            }
            val effectiveTotalHeight = totalHeightIncludingSpacing
            val categoryAreaHeight = 66
            val moduleListHeight = getContentHeight() - categoryAreaHeight - 20
            return max(0, effectiveTotalHeight - moduleListHeight)
        }

    override fun initializeComponents() {
        searchBar = CinnamonTextField(
            textRenderer,
            getContentX() + 10,
            getContentY() + 10,
            200,
            20
        )
        searchBar.setChangedListener { query ->
            searchQuery = query
            scrollOffset = 0
        }
        addDrawableChild(searchBar)

        addButton(CinnamonButton(
            guiX + PADDING,
            getFooterY() + 8,
            60,
            CinnamonTheme.BUTTON_HEIGHT_SMALL,
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> CinnamonGuiManager.openMainMenu() }
        ))
        updateCategoryButtons()
    }

    private fun selectCategory(newCategory: String) {
        this.selectedCategory = newCategory
        this.buttons.removeAll(this.categoryButtons.toSet())
        this.categoryButtons.clear()
        updateCategoryButtons()
        this.scrollOffset = 0
    }

    private fun updateCategoryButtons() {
        val contentX = getContentX()
        val contentWidth = getContentWidth()
        val categoryButtonWidth = 80
        val categorySpacing = 5
        val totalCategoryWidth = categories.size * categoryButtonWidth + (categories.size - 1) * categorySpacing
        val categoryStartX = contentX + (contentWidth - totalCategoryWidth) / 2

        categories.forEachIndexed { index, category ->
            val buttonX = categoryStartX + index * (categoryButtonWidth + categorySpacing)
            val catButton = CinnamonButton(
                buttonX,
                getContentY() + 40,
                categoryButtonWidth,
                CinnamonTheme.BUTTON_HEIGHT_SMALL,
                Text.literal(category).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                { _, _ -> selectCategory(category) },
                this.selectedCategory == category
            )
            this.categoryButtons.add(catButton)
            addButton(catButton)
        }
    }

    override fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        searchBar.render(context, scaledMouseX, scaledMouseY, delta)

        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val categoryAreaHeight = 66
        val moduleListY = contentY + categoryAreaHeight
        val moduleListHeight = contentHeight - categoryAreaHeight - 20

        renderModuleList(context, contentX + 10, moduleListY, contentWidth - 20, moduleListHeight, scaledMouseX, scaledMouseY, delta)

        if (maxScrollOffset > 0) {
            renderScrollbar(context, contentX + contentWidth - 8, moduleListY, 6, moduleListHeight)
        }
    }

    private fun renderModuleList(context: DrawContext, x: Int, y: Int, width: Int, height: Int, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val items = getFilteredModules()
        var currentY = y - scrollOffset

        context.enableScissor(x, y, x + width, y + height)
        items.forEach { item ->
            val itemHeight = when (item) {
                is Module -> if (expandedStates[item.name] == true) settingsModuleHeight(item) else baseModuleHeight
                is HudElement -> if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight
                else -> 0
            }
            if (currentY + itemHeight >= y && currentY <= y + height) {
                if (item is Module) {
                    renderModuleCard(context, x, currentY, width, itemHeight, item, scaledMouseX, scaledMouseY, delta)
                } else if (item is HudElement) {
                    renderHudElementCard(context, x, currentY, width, itemHeight, item, scaledMouseX, scaledMouseY, delta)
                }
            }
            currentY += itemHeight + moduleSpacing
        }
        context.disableScissor()
    }

    private fun renderHudElementCard(context: DrawContext, x: Int, y: Int, width: Int, height: Int, element: HudElement, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val isHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= y && scaledMouseY < y + height
        val cardBackgroundColor = if (element.isEnabled) {
            if (isHovered && height == baseModuleHeight) CinnamonTheme.moduleBackgroundEnabled else CinnamonTheme.moduleEnabledColor
        } else {
            if (isHovered && height == baseModuleHeight) CinnamonTheme.cardBackgroundHover else CinnamonTheme.cardBackground
        }

        drawRoundedRect(context, x, y, width, height, cardBackgroundColor)
        val borderColor = if (element.isEnabled) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        drawRoundedBorder(context, x, y, width, height, borderColor)

        context.drawText(
            textRenderer,
            Text.literal(element.getName()).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + 12,
            y + 8,
            if (element.isEnabled) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )

        val toggleWidth = 30
        val toggleHeight = 16
        val headerContentHeight = baseModuleHeight
        val toggleY = y + (headerContentHeight - toggleHeight) / 2

        val expandButtonText = if (expandedStates[element.getName()] == true) "-" else "+"
        val expandButtonWidth = textRenderer.getWidth(expandButtonText)
        val expandButtonX = x + width - expandButtonWidth - 12
        val toggleX = expandButtonX - toggleWidth - 8

        renderToggleSwitch(context, toggleX, toggleY, toggleWidth, toggleHeight, element.isEnabled, scaledMouseX, scaledMouseY)

        context.drawText(
            textRenderer,
            Text.literal(expandButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            expandButtonX,
            y + 8,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )

        if (expandedStates[element.getName()] == true) {
            val settingsY = y + baseModuleHeight
            val settingsContentHeight = height - baseModuleHeight - 8
            context.fill(x + 8, settingsY - 4, x + width - 8, settingsY - 3, CinnamonTheme.borderColor)
            renderHudElementSettings(
                context,
                x + 12,
                settingsY,
                width - 24,
                settingsContentHeight,
                element,
                scaledMouseX,
                scaledMouseY,
                delta
            )
        }
    }

    private fun renderHudElementSettings(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        element: HudElement,
        scaledMouseX: Int,
        scaledMouseY: Int,
        delta: Float
    ) {
        var currentY = y

        context.drawText(
            textRenderer,
            Text.literal("Settings").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x, currentY, CinnamonTheme.titleColor, CinnamonTheme.enableTextShadow
        )
        currentY += 15

        var newY = SettingsHelper.renderSettings(context, x, currentY, width, height, element.settings, scaledMouseX, scaledMouseY, delta)

        if (element is KeystrokesHudElement) {
            currentY = newY
            val keyPressedTextColorText = "Pressed Text: ${element.keypressedTextColor.toRGBHexString()}"
            context.drawText(textRenderer, Text.literal(keyPressedTextColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setKeyPressedTextColorButtonText = "[Set]"
            val setKeyPressedTextColorButtonWidth = textRenderer.getWidth(setKeyPressedTextColorButtonText)
            context.drawText(textRenderer, Text.literal(setKeyPressedTextColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setKeyPressedTextColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            val keyPressedBgColorText = "Pressed Background: ${element.keypressedBackgroundColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(keyPressedBgColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setKeyPressedBgColorButtonText = "[Set]"
            val setKeyPressedBgColorButtonWidth = textRenderer.getWidth(setKeyPressedBgColorButtonText)
            context.drawText(textRenderer, Text.literal(setKeyPressedBgColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setKeyPressedBgColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14
        }

    }

    private fun handleHudElementSettingsClick(
        scaledMouseX: Double,
        scaledMouseY: Double,
        settingsX: Int,
        settingsY: Int,
        settingsWidth: Int,
        element: HudElement
    ): Boolean {
        var currentY = settingsY
        currentY += 15
        val textElementHeight = textRenderer.fontHeight

        if (SettingsHelper.handleMouseClick(this, scaledMouseX, scaledMouseY, settingsX, currentY, settingsWidth, height, element.settings)) {
            return true
        }

        if (element is KeystrokesHudElement) {
            val setKeyPressedTextColorButtonText = "[Set]"
            val setKeyPressedTextColorButtonWidth = textRenderer.getWidth(setKeyPressedTextColorButtonText)
            val setKeyPressedTextColorButtonX = settingsX + settingsWidth - setKeyPressedTextColorButtonWidth
            val setKeyPressedTextColorButtonY = currentY
            if (scaledMouseX >= setKeyPressedTextColorButtonX && scaledMouseX < setKeyPressedTextColorButtonX + setKeyPressedTextColorButtonWidth &&
                scaledMouseY >= setKeyPressedTextColorButtonY && scaledMouseY < setKeyPressedTextColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.keypressedTextColor,
                    onPick = { pickedColor ->
                        element.keypressedTextColor = pickedColor
                        HudManager.markChangesForSave()
                        CinnamonGuiManager.openScreen(this)
                    },
                    onCancel = { CinnamonGuiManager.openScreen(this) }
                ))
                return true
            }
            currentY += 18

            val setKeyPressedBgColorButtonText = "[Set]"
            val setKeyPressedBgColorButtonWidth = textRenderer.getWidth(setKeyPressedBgColorButtonText)
            val setKeyPressedBgColorButtonX = settingsX + settingsWidth - setKeyPressedBgColorButtonWidth
            val setKeyPressedBgColorButtonY = currentY
            if (scaledMouseX >= setKeyPressedBgColorButtonX && scaledMouseX < setKeyPressedBgColorButtonX + setKeyPressedBgColorButtonWidth &&
                scaledMouseY >= setKeyPressedBgColorButtonY && scaledMouseY < setKeyPressedBgColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.keypressedBackgroundColor,
                    onPick = { pickedColor ->
                        element.keypressedBackgroundColor = pickedColor
                        HudManager.markChangesForSave()
                        CinnamonGuiManager.openScreen(this)
                    },
                    onCancel = { CinnamonGuiManager.openScreen(this) }
                ))
                return true
            }
            currentY += 14
        }

        return false
    }



    private fun drawCheckbox(context: DrawContext, x: Int, y: Int, text: String, checked: Boolean) {
        val checkboxSize = 10
        val checkboxBg = if (checked) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
        context.fill(x, y, x + checkboxSize, y + checkboxSize, checkboxBg)
        context.fill(x, y, x + checkboxSize, y + 1, CinnamonTheme.borderColor)
        context.fill(x, y + checkboxSize - 1, x + checkboxSize, y + checkboxSize, CinnamonTheme.borderColor)
        context.fill(x, y, x + 1, y + checkboxSize, CinnamonTheme.borderColor)
        context.fill(x + checkboxSize - 1, y, x + checkboxSize, y + checkboxSize, CinnamonTheme.borderColor)
        if (checked) {
            context.drawText(
                textRenderer,
                Text.literal("x").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                x + 1,
                y + 1,
                CinnamonTheme.titleColor,
                false
            )
        }
        context.drawText(
            textRenderer,
            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + checkboxSize + 6,
            y + 1,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )
    }

    private fun renderModuleCard(context: DrawContext, x: Int, y: Int, width: Int, moduleHeight: Int, module: Module, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val isHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= y && scaledMouseY < y + moduleHeight
        val cardBackgroundColor = if (module.isEnabled) {
            if (isHovered) CinnamonTheme.moduleBackgroundEnabled else CinnamonTheme.moduleEnabledColor
        } else {
            if (isHovered) CinnamonTheme.cardBackgroundHover else CinnamonTheme.cardBackground
        }

        drawRoundedRect(context, x, y, width, moduleHeight, cardBackgroundColor)
        val borderColor = if (module.isEnabled) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        drawRoundedBorder(context, x, y, width, moduleHeight, borderColor)

        context.drawText(
            textRenderer,
            Text.literal(module.name).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + 12,
            y + 8,
            if (module.isEnabled) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )

        if (module.settings.isNotEmpty()) {
            val expandButtonText = if (expandedStates[module.name] == true) "-" else "+"
            val expandButtonWidth = textRenderer.getWidth(expandButtonText)
            context.drawText(
                textRenderer,
                Text.literal(expandButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                x + width - expandButtonWidth - 12,
                y + 8,
                CinnamonTheme.primaryTextColor,
                CinnamonTheme.enableTextShadow
            )
        }

        context.drawText(
            textRenderer,
            Text.literal(module.description).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + 12,
            y + 22,
            CinnamonTheme.secondaryTextColor,
            CinnamonTheme.enableTextShadow
        )

        val bottomSectionY = if (expandedStates[module.name] == true) {
            y + settingsModuleHeight(module) - 30
        } else {
            y + baseModuleHeight - 30
        }

        renderToggleSwitch(context, x + width - 50, bottomSectionY + 6, 30, 16, module.isEnabled, scaledMouseX, scaledMouseY)

        val keybindText = getModuleKeybind(module.name)
        if (keybindText.isNotEmpty()) {
            val keybindWidth = textRenderer.getWidth(keybindText)
            context.drawText(
                textRenderer,
                Text.literal(keybindText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                x + width - keybindWidth - 60,
                bottomSectionY + 16,
                CinnamonTheme.secondaryTextColor,
                CinnamonTheme.enableTextShadow
            )
        }

        if (expandedStates[module.name] == true) {
            val settingsY = y + 40
            val settingsHeight = settingsAreaHeight
            context.fill(x + 8, settingsY, x + width - 8, settingsY + 1, CinnamonTheme.borderColor)
            renderModuleSettings(
                context,
                x + 12,
                settingsY + 5,
                width - 24,
                settingsHeight - 10,
                module,
                scaledMouseX,
                scaledMouseY,
                delta
            )
        }
    }

    private fun renderModuleSettings(context: DrawContext, x: Int, y: Int, width: Int, height: Int, module: Module, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        if (module.settings.isNotEmpty()) {
            SettingsHelper.renderSettings(context, x, y, width, height, module.settings, scaledMouseX, scaledMouseY, delta)
        } else {
            context.drawText(
                textRenderer,
                Text.literal("No settings available").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                x,
                y,
                CinnamonTheme.secondaryTextColor,
                CinnamonTheme.enableTextShadow
            )
        }
    }

    private fun drawSettingButton(context: DrawContext, x: Int, y: Int, width: Int, height: Int, text: String, pressed: Boolean) {
        val bgColor = if (pressed) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
        val textColor = if (pressed) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor
        context.fill(x, y, x + width, y + height, bgColor)
        context.fill(x, y, x + width, y + 1, CinnamonTheme.borderColor)
        context.fill(x, y + height - 1, x + width, y + height, CinnamonTheme.borderColor)
        context.fill(x, y, x + 1, y + height, CinnamonTheme.borderColor)
        context.fill(x + width - 1, y, x + width, y + height, CinnamonTheme.borderColor)
        val textWidth = textRenderer.getWidth(text)
        context.drawText(
            textRenderer,
            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + (width - textWidth) / 2,
            y + 2,
            textColor,
            false
        )
    }

    private fun renderToggleSwitch(context: DrawContext, x: Int, y: Int, width: Int, height: Int, enabled: Boolean, scaledMouseX: Int, scaledMouseY: Int) {
        val isHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= y && scaledMouseY < y + height
        val switchBg = if (enabled) {
            if (isHovered) CinnamonTheme.accentColorHover else CinnamonTheme.accentColor
        } else {
            if (isHovered) CinnamonTheme.buttonBackgroundHover else CinnamonTheme.buttonBackground
        }
        drawRoundedRect(context, x, y, width, height, switchBg)
        val knobSize = height - 4
        val knobX = if (enabled) x + width - knobSize - 2 else x + 2
        val knobY = y + 2
        drawRoundedRect(context, knobX, knobY, knobSize, knobSize, CinnamonTheme.titleColor)
    }

    private fun renderScrollbar(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        context.fill(x, y, x + width, y + height, CinnamonTheme.borderColor)
        val thumbHeight = max(20, (height * height) / (maxScrollOffset + height))
        val thumbY = y + (scrollOffset * (height - thumbHeight)) / maxScrollOffset
        context.fill(x + 1, thumbY.toInt(), x + width - 1, thumbY.toInt() + thumbHeight, CinnamonTheme.accentColor)
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

    private fun getFilteredModules(): List<Any> {
        val allModules = ModuleManager.getModules()
        val items = mutableListOf<Any>()
        if (selectedCategory == "All") {
            items.addAll(allModules)
        } else {
            items.addAll(allModules.filter { module -> getModuleCategory(module.name) == selectedCategory })
        }
        if (selectedCategory == "All" || selectedCategory == "Render") {
            items.addAll(HudManager.getElements())
        }

        val distinctItems = items.distinct()

        if (searchQuery.isBlank()) {
            return distinctItems
        }

        return distinctItems.filter { item ->
            when (item) {
                is Module -> item.name.contains(searchQuery, ignoreCase = true)
                is HudElement -> item.getName().contains(searchQuery, ignoreCase = true)
                else -> false
            }
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
        searchBar.mouseClicked(mouseX, mouseY, button)

        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val moduleListY = contentY + 66
        val moduleListHeight = contentHeight - 76


        if (scaledMouseX >= contentX && scaledMouseX < contentX + contentWidth &&
            scaledMouseY >= moduleListY && scaledMouseY < moduleListY + moduleListHeight) {

            val items = getFilteredModules()
            var currentY = moduleListY - scrollOffset
            val cardX = contentX + 10
            val cardWidth = contentWidth - 20

            items.forEach { item ->
                val currentItemHeight = when (item) {
                    is Module -> if (expandedStates[item.name] == true) settingsModuleHeight(item) else baseModuleHeight
                    is HudElement -> if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight
                    else -> 0
                }


                if (scaledMouseY >= currentY && scaledMouseY < currentY + currentItemHeight) {
                    if (item is Module) {
                        val bottomSectionYModule = if (expandedStates[item.name] == true) {
                            currentY + settingsModuleHeight(item) - 30
                        } else {
                            currentY + baseModuleHeight - 30
                        }
                        val toggleXModule = cardX + cardWidth - 50
                        val toggleYModule = bottomSectionYModule + 6


                        if (scaledMouseX >= toggleXModule && scaledMouseX < toggleXModule + 30 &&
                            scaledMouseY >= toggleYModule && scaledMouseY < toggleYModule + 16) {
                            ModuleManager.toggleModule(item.name)
                            return true
                        }

                        if (item.settings.isNotEmpty()) {
                            val expandButtonTextModule = if (expandedStates[item.name] == true) "-" else "+"
                            val expandButtonWidthModule = textRenderer.getWidth(expandButtonTextModule)
                            val expandButtonXModule = cardX + cardWidth - expandButtonWidthModule - 12
                            val expandButtonYModule = currentY + 8


                            if (scaledMouseX >= expandButtonXModule && scaledMouseX < expandButtonXModule + expandButtonWidthModule &&
                                scaledMouseY >= expandButtonYModule && scaledMouseY < expandButtonYModule + textRenderer.fontHeight) {
                                expandedStates[item.name] = !(expandedStates[item.name] ?: false)
                                scrollOffset = min(scrollOffset, maxScrollOffset)
                                return true
                            }
                        }

                        if (expandedStates[item.name] == true) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = currentY + 40 + 5
                            if (SettingsHelper.handleMouseClick(this, scaledMouseX, scaledMouseY, settingsContentX, settingsContentY, cardWidth - 24, settingsAreaHeight, item.settings)) {
                                return true
                            }
                        }
                    } else if (item is HudElement) {
                        val headerHeight = baseModuleHeight

                        val expandButtonTextHud = if (expandedStates[item.getName()] == true) "-" else "+"
                        val expandButtonWidthHud = textRenderer.getWidth(expandButtonTextHud)
                        val expandButtonXHud = cardX + cardWidth - expandButtonWidthHud - 12
                        val expandButtonYHud = currentY + 8

                        val toggleWidth = 30
                        val toggleHeight = 16
                        val toggleXHud = expandButtonXHud - toggleWidth - 8
                        val toggleYHud = currentY + (headerHeight - toggleHeight) / 2


                        if (scaledMouseX >= toggleXHud && scaledMouseX < toggleXHud + toggleWidth &&
                            scaledMouseY >= toggleYHud && scaledMouseY < toggleYHud + toggleHeight) {
                            item.isEnabled = !item.isEnabled
                            return true
                        }


                        if (scaledMouseX >= expandButtonXHud && scaledMouseX < expandButtonXHud + expandButtonWidthHud &&
                            scaledMouseY >= expandButtonYHud && scaledMouseY < expandButtonYHud + textRenderer.fontHeight) {
                            expandedStates[item.getName()] = !(expandedStates[item.getName()] ?: false)
                            scrollOffset = min(scrollOffset, maxScrollOffset)
                            return true
                        }

                        if (expandedStates[item.getName()] == true) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = currentY + baseModuleHeight
                            val settingsContentWidth = cardWidth - 24
                            val settingsAreaActualHeight = settingsHudElementHeight - baseModuleHeight - 8

                            if (scaledMouseY >= settingsContentY && scaledMouseY < settingsContentY + settingsAreaActualHeight) {

                                if (handleHudElementSettingsClick(scaledMouseX, scaledMouseY, settingsContentX, settingsContentY, settingsContentWidth, item)) {
                                    return true
                                }
                            }
                        }
                    }
                    return true
                }
                currentY += currentItemHeight + moduleSpacing
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun close() {
        CinnamonGuiManager.openMainMenu()
        HudManager.markChangesForSave()
        HudManager.saveHudConfig()
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (searchBar.keyPressed(keyCode, scanCode, modifiers)) {
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (searchBar.charTyped(chr, modifiers)) {
            return true
        }
        return super.charTyped(chr, modifiers)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        val contentX = getContentX()
        val contentWidth = getContentWidth()

        val categoryAreaHeight = 50
        val moduleListY = getContentY() + categoryAreaHeight + 10
        val moduleListHeight = getContentHeight() - categoryAreaHeight - 20


        if (scaledMouseX >= contentX && scaledMouseX < contentX + contentWidth &&
            scaledMouseY >= moduleListY && scaledMouseY < moduleListY + moduleListHeight) {

            if (maxScrollOffset > 0) {
                val scrollAmount = (verticalAmount * 20).toInt()
                scrollOffset = max(0, min(maxScrollOffset, scrollOffset - scrollAmount))
                return true
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
}