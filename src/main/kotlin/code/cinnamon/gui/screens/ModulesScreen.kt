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
import code.cinnamon.gui.utils.GraphicsUtils
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
    private var isCategoryDropdownOpen = false
    private var scrollOffset = 0.0
    private var targetScrollOffset = 0.0
    private var dropdownAnimationProgress = 0.0f
    private var lastAnimationTime = 0L
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

    private val maxScrollOffset: Double
        get() {
            val items = getFilteredModules()
            if (items.isEmpty()) return 0.0
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
            val padding = 20
            return max(0.0, (effectiveTotalHeight - moduleListHeight + padding).toDouble())
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
            scrollOffset = 0.0
            targetScrollOffset = 0.0
        }
        addDrawableChild(searchBar)

        addButton(
            CinnamonButton(
                guiX + guiWidth - PADDING - 70,
                getFooterY() + (FOOTER_HEIGHT - CinnamonTheme.BUTTON_HEIGHT_SMALL) / 2,
                70,
                CinnamonTheme.BUTTON_HEIGHT_SMALL,
                Text.literal("Close").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                onClick = { _, _ -> close() }
            )
        )
        lastAnimationTime = System.currentTimeMillis()
    }

    override fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastAnimationTime) / 1000.0f
        lastAnimationTime = currentTime

        if (isCategoryDropdownOpen) {
            dropdownAnimationProgress = min(1.0f, dropdownAnimationProgress + deltaTime * 6f)
        } else {
            dropdownAnimationProgress = max(0.0f, dropdownAnimationProgress - deltaTime * 6f)
        }

        val scrollInertia = 8.0f
        scrollOffset += (targetScrollOffset - scrollOffset) * (deltaTime * scrollInertia)

        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val categoryAreaHeight = 66
        val moduleListY = contentY + categoryAreaHeight
        val moduleListHeight = contentHeight - categoryAreaHeight - 20

        renderModuleList(context, contentX + 10, moduleListY, contentWidth - 20, moduleListHeight, scaledMouseX, scaledMouseY, delta)

        renderCategoryDropdown(context, getContentX() + 220, getContentY() + 10, 120, 20, scaledMouseX, scaledMouseY, dropdownAnimationProgress)
    }

    private fun renderModuleList(context: DrawContext, x: Int, y: Int, width: Int, height: Int, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val items = getFilteredModules()
        val padding = 20
        var currentY = y.toDouble() - scrollOffset + padding

        context.enableScissor(x, y, x + width, y + height)
        items.forEach { item ->
            val itemHeight = when (item) {
                is Module -> if (expandedStates[item.name] == true) settingsModuleHeight(item) else baseModuleHeight
                is HudElement -> if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight
                else -> 0
            }
            if (currentY + itemHeight >= y && currentY <= y + height) {
                if (item is Module) {
                    renderModuleCard(context, x, currentY.toInt(), width, itemHeight, item, scaledMouseX, scaledMouseY, delta)
                } else if (item is HudElement) {
                    renderHudElementCard(context, x, currentY.toInt(), width, itemHeight, item, scaledMouseX, scaledMouseY, delta)
                }
            }
            currentY += itemHeight + moduleSpacing
        }
        context.disableScissor()

        val fadeHeight = 20
        val transparentBg = CinnamonTheme.coreBackgroundPrimary and 0x00FFFFFF
        context.fillGradient(x, y, x + width, y + fadeHeight, CinnamonTheme.coreBackgroundPrimary, transparentBg)
        context.fillGradient(x, y + height - fadeHeight, x + width, y + height, transparentBg, CinnamonTheme.coreBackgroundPrimary)
    }

    private fun renderHudElementCard(context: DrawContext, x: Int, y: Int, width: Int, height: Int, element: HudElement, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val isHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= y && scaledMouseY < y + height
        val cardBackgroundColor = if (element.isEnabled) {
            if (isHovered && height == baseModuleHeight) CinnamonTheme.moduleBackgroundEnabled else CinnamonTheme.moduleEnabledColor
        } else {
            if (isHovered && height == baseModuleHeight) CinnamonTheme.cardBackgroundHover else CinnamonTheme.cardBackground
        }

        GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), CORNER_RADIUS, cardBackgroundColor)
        val borderColor = if (element.isEnabled) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        GraphicsUtils.drawRoundedRectBorder(context, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), CORNER_RADIUS, borderColor)

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

        GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), y.toFloat(), width.toFloat(), moduleHeight.toFloat(), CORNER_RADIUS, cardBackgroundColor)
        val borderColor = if (module.isEnabled) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        GraphicsUtils.drawRoundedRectBorder(context, x.toFloat(), y.toFloat(), width.toFloat(), moduleHeight.toFloat(), CORNER_RADIUS, borderColor)

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
        GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), 4f, switchBg)
        val knobSize = height - 4
        val knobX = if (enabled) x + width - knobSize - 2 else x + 2
        val knobY = y + 2
        GraphicsUtils.drawFilledRoundedRect(context, knobX.toFloat(), knobY.toFloat(), knobSize.toFloat(), knobSize.toFloat(), 4f, CinnamonTheme.titleColor)
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
            "autoclicker" -> "Combat"
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
        if (super.mouseClicked(mouseX, mouseY, button)) return true

        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        val dropdownX = getContentX() + 220
        val dropdownY = getContentY() + 10
        val dropdownWidth = 120
        val dropdownHeight = 20

        if (scaledMouseX >= dropdownX && scaledMouseX < dropdownX + dropdownWidth && scaledMouseY >= dropdownY && scaledMouseY < dropdownY + dropdownHeight) {
            isCategoryDropdownOpen = !isCategoryDropdownOpen
            return true
        }

        if (isCategoryDropdownOpen && dropdownAnimationProgress >= 1.0f) {
            val dropdownListY = dropdownY + dropdownHeight + 2
            val dropdownListHeight = categories.size * (dropdownHeight + 2)

            var itemClicked = false
            categories.forEachIndexed { index, category ->
                val itemY = dropdownListY + index * (dropdownHeight + 2)
                if (scaledMouseX >= dropdownX && scaledMouseX < dropdownX + dropdownWidth && scaledMouseY >= itemY && scaledMouseY < itemY + dropdownHeight) {
                    selectedCategory = category
                    isCategoryDropdownOpen = false
                    scrollOffset = 0.0
                    targetScrollOffset = 0.0
                    itemClicked = true
                }
            }
            if (itemClicked) return true

            val isClickInsideDropdown = scaledMouseX >= dropdownX && scaledMouseX < dropdownX + dropdownWidth &&
                    scaledMouseY >= dropdownY && scaledMouseY < dropdownListY + dropdownListHeight

            if (!isClickInsideDropdown) {
                isCategoryDropdownOpen = false
            } else {
                return true
            }
        } else if (isCategoryDropdownOpen) {
            return true
        }

        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val moduleListY = contentY + 66
        val moduleListHeight = contentHeight - 76


        if (scaledMouseX >= contentX && scaledMouseX < contentX + contentWidth &&
            scaledMouseY >= moduleListY && scaledMouseY < moduleListY + moduleListHeight) {

            val items = getFilteredModules()
            val padding = 20
            var currentY = moduleListY.toDouble() - scrollOffset + padding
            val cardX = contentX + 10
            val cardWidth = contentWidth - 20

            items.forEach { item ->
                val itemHeight = when (item) {
                    is Module -> if (expandedStates[item.name] == true) settingsModuleHeight(item) else baseModuleHeight
                    is HudElement -> if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight
                    else -> 0
                }

                val intY = currentY.toInt()

                if (scaledMouseY >= intY && scaledMouseY < intY + itemHeight) {
                    if (item is Module) {
                        val bottomSectionYModule = if (expandedStates[item.name] == true) {
                            intY + settingsModuleHeight(item) - 30
                        } else {
                            intY + baseModuleHeight - 30
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
                            val expandButtonYModule = intY + 8


                            if (scaledMouseX >= expandButtonXModule && scaledMouseX < expandButtonXModule + expandButtonWidthModule &&
                                scaledMouseY >= expandButtonYModule && scaledMouseY < expandButtonYModule + textRenderer.fontHeight) {
                                expandedStates[item.name] = !(expandedStates[item.name] ?: false)
                                targetScrollOffset = targetScrollOffset.coerceAtMost(maxScrollOffset)
                                scrollOffset = scrollOffset.coerceAtMost(maxScrollOffset)
                                return true
                            }
                        }

                        if (expandedStates[item.name] == true) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = intY + 40 + 5
                            if (SettingsHelper.handleMouseClick(this, scaledMouseX, scaledMouseY, settingsContentX, settingsContentY, cardWidth - 24, settingsAreaHeight, item.settings)) {
                                return true
                            }
                        }
                    } else if (item is HudElement) {
                        val headerHeight = baseModuleHeight

                        val expandButtonTextHud = if (expandedStates[item.getName()] == true) "-" else "+"
                        val expandButtonWidthHud = textRenderer.getWidth(expandButtonTextHud)
                        val expandButtonXHud = cardX + cardWidth - expandButtonWidthHud - 12
                        val expandButtonYHud = intY + 8

                        val toggleWidth = 30
                        val toggleHeight = 16
                        val toggleXHud = expandButtonXHud - toggleWidth - 8
                        val toggleYHud = intY + (headerHeight - toggleHeight) / 2


                        if (scaledMouseX >= toggleXHud && scaledMouseX < toggleXHud + toggleWidth &&
                            scaledMouseY >= toggleYHud && scaledMouseY < toggleYHud + toggleHeight) {
                            item.isEnabled = !item.isEnabled
                            return true
                        }


                        if (scaledMouseX >= expandButtonXHud && scaledMouseX < expandButtonXHud + expandButtonWidthHud &&
                            scaledMouseY >= expandButtonYHud && scaledMouseY < expandButtonYHud + textRenderer.fontHeight) {
                            expandedStates[item.getName()] = !(expandedStates[item.getName()] ?: false)
                            targetScrollOffset = targetScrollOffset.coerceAtMost(maxScrollOffset)
                            scrollOffset = scrollOffset.coerceAtMost(maxScrollOffset)
                            return true
                        }

                        if (expandedStates[item.getName()] == true) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = intY + baseModuleHeight
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
                currentY += itemHeight + moduleSpacing
            }
        }

        return false
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
                val scrollAmount = verticalAmount * 20
                targetScrollOffset = (targetScrollOffset - scrollAmount).coerceIn(0.0, maxScrollOffset)
                return true
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    private fun renderCategoryDropdown(context: DrawContext, x: Int, y: Int, width: Int, height: Int, scaledMouseX: Int, scaledMouseY: Int, animationProgress: Float) {
        val isHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= y && scaledMouseY < y + height
        val bgColor = if (isHovered) CinnamonTheme.buttonBackgroundHover else CinnamonTheme.buttonBackground
        GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), 4f, bgColor)
        GraphicsUtils.drawRoundedRectBorder(context, x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat(), 4f, CinnamonTheme.borderColor)

        val buttonText = Text.literal(selectedCategory + " ▾").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val textWidth = textRenderer.getWidth(buttonText)
        context.drawText(
            textRenderer,
            buttonText,
            x + (width - textWidth) / 2,
            y + (height - textRenderer.fontHeight) / 2,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )

        if (animationProgress > 0) {
            val dropdownY = y + height + 2
            val dropdownHeight = (categories.size * (height + 2) * animationProgress).toInt()
            val alpha = (animationProgress * 255).toInt()

            context.enableScissor(x, dropdownY, x + width, dropdownY + dropdownHeight)
            GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), dropdownY.toFloat(), width.toFloat(), dropdownHeight.toFloat(), 4f, GraphicsUtils.withAlpha(CinnamonTheme.coreBackgroundPrimary, alpha))
            GraphicsUtils.drawRoundedRectBorder(context, x.toFloat(), dropdownY.toFloat(), width.toFloat(), dropdownHeight.toFloat(), 4f, CinnamonTheme.borderColor)

            categories.forEachIndexed { index, category ->
                val itemY = dropdownY + index * (height + 2)
                val isItemHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= itemY && scaledMouseY < itemY + height
                if (isItemHovered) {
                    context.fill(x, itemY, x + width, itemY + height, GraphicsUtils.withAlpha(CinnamonTheme.buttonBackgroundHover, alpha))
                }
                val categoryText = Text.literal(category).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
                val categoryTextWidth = textRenderer.getWidth(categoryText)
                context.drawText(
                    textRenderer,
                    categoryText,
                    x + (width - categoryTextWidth) / 2,
                    itemY + (height - textRenderer.fontHeight) / 2,
                    GraphicsUtils.withAlpha(CinnamonTheme.primaryTextColor, alpha),
                    CinnamonTheme.enableTextShadow
                )
            }
            context.disableScissor()
        }
    }
}