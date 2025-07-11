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
import code.cinnamon.modules.all.ChatPrefixModule
import code.cinnamon.util.MinecraftColorCodes

class ModulesScreen : CinnamonScreen(Text.literal("Modules").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {
    private var selectedCategory = "All"
    private val categories = listOf("All", "Combat", "Movement", "Render", "Player", "World")
    private val categoryButtons = mutableListOf<CinnamonButton>()
    private var scrollOffset = 0
    internal val expandedStates = mutableMapOf<String, Boolean>()
    private val baseModuleHeight = 60
    private val settingsModuleHeight = 200
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
                    is Module -> (if (expandedStates[item.name] == true) settingsModuleHeight else baseModuleHeight) + moduleSpacing
                    is HudElement -> (if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight) + moduleSpacing
                    else -> 0
                }
            }
            val effectiveTotalHeight = totalHeightIncludingSpacing - moduleSpacing
            val categoryAreaHeight = 50
            val moduleListHeight = getContentHeight() - categoryAreaHeight - 20
            return max(0, effectiveTotalHeight - moduleListHeight)
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
                getContentY() + 10,
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

    override fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) { // Match super
        // Parameters are already scaled as per CinnamonScreen's contract for renderContent
        // Use scaledMouseX, scaledMouseY directly where needed

        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val categoryAreaHeight = 50
        val moduleListY = contentY + categoryAreaHeight + 10
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
                is Module -> if (expandedStates[item.name] == true) settingsModuleHeight else baseModuleHeight
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

        if (element !is PacketHandlerHudElement) {
            val textColorText = "Text Color: ${element.textColor.toRGBHexString()}"
            context.drawText(textRenderer, Text.literal(textColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setTextColorButtonText = "[Set]"
            val setTextColorButtonWidth = textRenderer.getWidth(setTextColorButtonText)
            context.drawText(textRenderer, Text.literal(setTextColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setTextColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            val bgColorText = "Background: ${element.backgroundColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(bgColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setBgColorButtonText = "[Set]"
            val setBgColorButtonWidth = textRenderer.getWidth(setBgColorButtonText)
            context.drawText(textRenderer, Text.literal(setBgColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setBgColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            drawCheckbox(context, x, currentY, "Enable Text Shadow", element.textShadowEnabled)
            currentY += 14
        }

        if (element is KeystrokesHudElement) {
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

        if (element is PacketHandlerHudElement) {
            val buttonColorText = "Button Color: ${element.buttonColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(buttonColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setButtonColorButtonText = "[Set]"
            val setButtonColorButtonWidth = textRenderer.getWidth(setButtonColorButtonText)
            context.drawText(textRenderer, Text.literal(setButtonColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setButtonColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            val buttonTextColorText = "Button Text: ${element.buttonTextColor.toRGBHexString()}"
            context.drawText(textRenderer, Text.literal(buttonTextColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setButtonTextColorButtonText = "[Set]"
            val setButtonTextColorButtonWidth = textRenderer.getWidth(setButtonTextColorButtonText)
            context.drawText(textRenderer, Text.literal(setButtonTextColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setButtonTextColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            drawCheckbox(context, x, currentY, "Button Text Shadow", element.buttonTextShadowEnabled)
            currentY += 14

            val buttonHoverColorText = "Hover Outline: ${element.buttonHoverColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(buttonHoverColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setButtonHoverColorButtonText = "[Set]"
            val setButtonHoverColorButtonWidth = textRenderer.getWidth(setButtonHoverColorButtonText)
            context.drawText(textRenderer, Text.literal(setButtonHoverColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setButtonHoverColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            val buttonOutlineColorText = "Button Outline: ${element.buttonOutlineColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(buttonOutlineColorText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, currentY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
            val setButtonOutlineColorButtonText = "[Set]"
            val setButtonOutlineColorButtonWidth = textRenderer.getWidth(setButtonOutlineColorButtonText)
            context.drawText(textRenderer, Text.literal(setButtonOutlineColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x + width - setButtonOutlineColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
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

        if (element !is PacketHandlerHudElement) {
            val setTextColorButtonText = "[Set]"
            val setTextColorButtonWidth = textRenderer.getWidth(setTextColorButtonText)
            val setTextColorButtonX = settingsX + settingsWidth - setTextColorButtonWidth
            val setTextColorButtonY = currentY
            if (scaledMouseX >= setTextColorButtonX && scaledMouseX < setTextColorButtonX + setTextColorButtonWidth &&
                scaledMouseY >= setTextColorButtonY && scaledMouseY < setTextColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.textColor,
                    onPick = { pickedColor ->
                        element.textColor = pickedColor
                        HudManager.markChangesForSave()
                        CinnamonGuiManager.openScreen(this)
                    },
                    onCancel = { CinnamonGuiManager.openScreen(this) }
                ))
                return true
            }
            currentY += 14
            val setBgColorButtonText = "[Set]"
            val setBgColorButtonWidth = textRenderer.getWidth(setBgColorButtonText)
            val setBgColorButtonX = settingsX + settingsWidth - setBgColorButtonWidth
            val setBgColorButtonY = currentY
            if (scaledMouseX >= setBgColorButtonX && scaledMouseX < setBgColorButtonX + setBgColorButtonWidth &&
                scaledMouseY >= setBgColorButtonY && scaledMouseY < setBgColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.backgroundColor,
                    onPick = { pickedColor ->
                        element.backgroundColor = pickedColor
                        HudManager.markChangesForSave()
                        CinnamonGuiManager.openScreen(this)
                    },
                    onCancel = { CinnamonGuiManager.openScreen(this) }
                ))
                return true
            }
            currentY += 14

            val checkboxSize = 10
            val shadowCheckboxX = settingsX
            val shadowCheckboxY = currentY
            val shadowCheckboxText = "Enable Text Shadow"
            val shadowCheckboxTextWidth = textRenderer.getWidth(shadowCheckboxText)
            if (scaledMouseX >= shadowCheckboxX && scaledMouseX < shadowCheckboxX + checkboxSize + 6 + shadowCheckboxTextWidth &&
                scaledMouseY >= shadowCheckboxY && scaledMouseY < shadowCheckboxY + checkboxSize
            ) {
                element.textShadowEnabled = !element.textShadowEnabled
                HudManager.markChangesForSave()
                return true
            }
            currentY += 14
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
            currentY += 14

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

        if (element is PacketHandlerHudElement) {
            val setButtonColorButtonText = "[Set]"
            val setButtonColorButtonWidth = textRenderer.getWidth(setButtonColorButtonText)
            val setButtonColorButtonX = settingsX + settingsWidth - setButtonColorButtonWidth
            val setButtonColorButtonY = currentY
            if (scaledMouseX >= setButtonColorButtonX && scaledMouseX < setButtonColorButtonX + setButtonColorButtonWidth &&
                scaledMouseY >= setButtonColorButtonY && scaledMouseY < setButtonColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.buttonColor,
                    onPick = { pickedColor ->
                        element.buttonColor = pickedColor
                        HudManager.markChangesForSave()
                        CinnamonGuiManager.openScreen(this)
                    },
                    onCancel = { CinnamonGuiManager.openScreen(this) }
                ))
                return true
            }
            currentY += 14

            val setButtonTextColorButtonText = "[Set]"
            val setButtonTextColorButtonWidth = textRenderer.getWidth(setButtonTextColorButtonText)
            val setButtonTextColorButtonX = settingsX + settingsWidth - setButtonTextColorButtonWidth
            val setButtonTextColorButtonY = currentY
            if (scaledMouseX >= setButtonTextColorButtonX && scaledMouseX < setButtonTextColorButtonX + setButtonTextColorButtonWidth &&
                scaledMouseY >= setButtonTextColorButtonY && scaledMouseY < setButtonTextColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.buttonTextColor,
                    onPick = { pickedColor ->
                        element.buttonTextColor = pickedColor
                        HudManager.markChangesForSave()
                        CinnamonGuiManager.openScreen(this)
                    },
                    onCancel = { CinnamonGuiManager.openScreen(this) }
                ))
                return true
            }
            currentY += 14

            val checkboxSize = 10
            val buttonShadowCheckboxX = settingsX
            val buttonShadowCheckboxY = currentY
            val buttonShadowCheckboxText = "Button Text Shadow"
            val buttonShadowCheckboxTextWidth = textRenderer.getWidth(buttonShadowCheckboxText)
            if (scaledMouseX >= buttonShadowCheckboxX && scaledMouseX < buttonShadowCheckboxX + checkboxSize + 6 + buttonShadowCheckboxTextWidth &&
                scaledMouseY >= buttonShadowCheckboxY && scaledMouseY < buttonShadowCheckboxY + checkboxSize
            ) {
                element.buttonTextShadowEnabled = !element.buttonTextShadowEnabled
                HudManager.markChangesForSave()
                return true
            }
            currentY += 14

            val setButtonHoverColorButtonText = "[Set]"
            val setButtonHoverColorButtonWidth = textRenderer.getWidth(setButtonHoverColorButtonText)
            val setButtonHoverColorButtonX = settingsX + settingsWidth - setButtonHoverColorButtonWidth
            val setButtonHoverColorButtonY = currentY
            if (scaledMouseX >= setButtonHoverColorButtonX && scaledMouseX < setButtonHoverColorButtonX + setButtonHoverColorButtonWidth &&
                scaledMouseY >= setButtonHoverColorButtonY && scaledMouseY < setButtonHoverColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.buttonHoverColor,
                    onPick = { pickedColor ->
                        element.buttonHoverColor = pickedColor
                        HudManager.markChangesForSave()
                        CinnamonGuiManager.openScreen(this)
                    },
                    onCancel = { CinnamonGuiManager.openScreen(this) }
                ))
                return true
            }
            currentY += 14

            val setButtonOutlineColorButtonText = "[Set]"
            val setButtonOutlineColorButtonWidth = textRenderer.getWidth(setButtonOutlineColorButtonText)
            val setButtonOutlineColorButtonX = settingsX + settingsWidth - setButtonOutlineColorButtonWidth
            val setButtonOutlineColorButtonY = currentY
            if (scaledMouseX >= setButtonOutlineColorButtonX && scaledMouseX < setButtonOutlineColorButtonX + setButtonOutlineColorButtonWidth &&
                scaledMouseY >= setButtonOutlineColorButtonY && scaledMouseY < setButtonOutlineColorButtonY + textElementHeight
            ) {
                CinnamonGuiManager.openScreen(ColorPickerScreen(
                    initialColor = element.buttonOutlineColor,
                    onPick = { pickedColor ->
                        element.buttonOutlineColor = pickedColor
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

    private fun handleChatPrefixSettingsClick(scaledMouseX: Double, scaledMouseY: Double, settingsX: Int, settingsY: Int, settingsWidth: Int, module: ChatPrefixModule): Boolean {
        var currentY = settingsY
        currentY += 15

        val selectableColors = MinecraftColorCodes.entries.filter { it.isColor || it == MinecraftColorCodes.RESET }

        val itemBoxHeight = 20
        val itemBoxWidth = 110
        val horizontalSpacing = 4
        val verticalSpacing = 4

        val gridStartX = settingsX
        var gridCurrentY = currentY


        val effectiveSettingsContentHeight = settingsAreaHeight - 10
        // val availableGridHeight = (settingsY + effectiveSettingsContentHeight) - gridCurrentY - 5 // Not directly used for click logic

        val itemsPerRow = max(1, (settingsWidth + horizontalSpacing) / (itemBoxWidth + horizontalSpacing))

        var currentX = gridStartX
        var itemsInCurrentRow = 0

        for (colorEnumEntry in selectableColors) {
            if (itemsInCurrentRow >= itemsPerRow) {
                currentX = gridStartX
                gridCurrentY += itemBoxHeight + verticalSpacing
                itemsInCurrentRow = 0
            }

            if (gridCurrentY + itemBoxHeight > (settingsY + effectiveSettingsContentHeight) - 5) {
                break
            }

            val itemHitboxX = currentX
            val itemHitboxY = gridCurrentY
            val itemHitboxEndX = itemHitboxX + itemBoxWidth
            val itemHitboxEndY = itemHitboxY + itemBoxHeight

            if (scaledMouseX >= itemHitboxX && scaledMouseX < itemHitboxEndX &&
                scaledMouseY >= itemHitboxY && scaledMouseY < itemHitboxEndY) {
                module.setSelectedColorCode(colorEnumEntry.code)
                return true
            }

            currentX += itemBoxWidth + horizontalSpacing
            itemsInCurrentRow++
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
                Text.literal("✓").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
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

        context.drawText(
            textRenderer,
            Text.literal(module.description).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + 12,
            y + 22,
            CinnamonTheme.secondaryTextColor,
            CinnamonTheme.enableTextShadow
        )

        val bottomSectionY = if (expandedStates[module.name] == true) {
            y + settingsModuleHeight - 30
        } else {
            y + baseModuleHeight - 30
        }

        renderToggleSwitch(context, x + width - 50, bottomSectionY + 6, 30, 16, module.isEnabled, scaledMouseX, scaledMouseY)
        val statusColor = if (module.isEnabled) CinnamonTheme.successColor else CinnamonTheme.moduleDisabledColor
        context.fill(x + 12, bottomSectionY + 18, x + 20, bottomSectionY + 26, statusColor)

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
        var settingY = y
        when (module) {
            is AutoclickerModule -> {
                context.drawText(textRenderer, Text.literal("Settings").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, settingY, CinnamonTheme.titleColor, CinnamonTheme.enableTextShadow)
                settingY += 15
                val buttonWidth = 16
                val buttonHeight = 12
                val settingSpacing = 14
                val minCpsText = "Min CPS: %.1f".format(module.minCPS)
                context.drawText(textRenderer, Text.literal(minCpsText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, settingY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
                val minCpsButtonsX = x + width - 40
                drawSettingButton(context, minCpsButtonsX, settingY - 1, buttonWidth, buttonHeight, "-", false)
                drawSettingButton(context, minCpsButtonsX + 20, settingY - 1, buttonWidth, buttonHeight, "+", false)
                settingY += settingSpacing
                val maxCpsText = "Max CPS: %.1f".format(module.maxCPS)
                context.drawText(textRenderer, Text.literal(maxCpsText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, settingY, CinnamonTheme.primaryTextColor, CinnamonTheme.enableTextShadow)
                val maxCpsButtonsX = x + width - 40
                drawSettingButton(context, maxCpsButtonsX, settingY - 1, buttonWidth, buttonHeight, "-", false)
                drawSettingButton(context, maxCpsButtonsX + 20, settingY - 1, buttonWidth, buttonHeight, "+", false)
                settingY += settingSpacing
                drawCheckbox(context, x, settingY - 1, "Humanization", module.enableHumanization)
                settingY += settingSpacing
                drawCheckbox(context, x, settingY - 1, "Burst Mode", module.burstMode)
                settingY += settingSpacing
                drawCheckbox(context, x, settingY - 1, "Left Click", module.leftClickEnabled)
                settingY += settingSpacing
                drawCheckbox(context, x, settingY - 1, "Right Click", module.rightClickEnabled)
            }
            is ChatPrefixModule -> {
                context.drawText(textRenderer, Text.literal("Settings").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())), x, settingY, CinnamonTheme.titleColor, CinnamonTheme.enableTextShadow)
                settingY += 15

                val allColors = MinecraftColorCodes.entries
                val itemBoxHeight = 20
                val itemBoxWidth = 110
                val horizontalSpacing = 4
                val verticalSpacing = 4

                val gridStartX = x
                var gridCurrentY = settingY
                val availableGridWidth = width
                val availableGridHeight = (y + height) - gridCurrentY - 5

                val itemsPerRow = max(1, (availableGridWidth + horizontalSpacing) / (itemBoxWidth + horizontalSpacing))

                var currentX = gridStartX
                var itemsInCurrentRow = 0
                var rowsRenderedCount = 0
                var firstItemInRowY = gridCurrentY

                if (allColors.isNotEmpty()) rowsRenderedCount = 1

                for (colorEnumEntry in allColors) {
                    if (itemsInCurrentRow >= itemsPerRow) {
                        currentX = gridStartX
                        gridCurrentY += itemBoxHeight + verticalSpacing
                        itemsInCurrentRow = 0
                        rowsRenderedCount++
                        firstItemInRowY = gridCurrentY
                    }

                    if (gridCurrentY + itemBoxHeight > (y + height) - 5) {
                        break
                    }

                    val isSelected = (module.selectedColorCode == colorEnumEntry.code)

                    val checkboxX = currentX + 2
                    val checkboxY = gridCurrentY + (itemBoxHeight - 10) / 2
                    val checkboxSize = 10
                    val checkboxBg = if (isSelected) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
                    context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, checkboxBg)
                    context.drawBorder(checkboxX, checkboxY, checkboxSize, checkboxSize, CinnamonTheme.borderColor)
                    if (isSelected) {
                        context.drawText(
                            textRenderer,
                            Text.literal("✓").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                            checkboxX + 1,
                            checkboxY + 1,
                            CinnamonTheme.titleColor,
                            false
                        )
                    }

                    val textX = checkboxX + checkboxSize + 6
                    val textY = gridCurrentY + (itemBoxHeight - textRenderer.fontHeight) / 2
                    context.drawText(
                        textRenderer,
                        Text.literal(colorEnumEntry.friendlyName).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                        textX,
                        textY,
                        CinnamonTheme.primaryTextColor,
                        CinnamonTheme.enableTextShadow
                    )

                    currentX += itemBoxWidth + horizontalSpacing
                    itemsInCurrentRow++
                }

                if (allColors.isNotEmpty()) {
                    if (itemsInCurrentRow == 0 && rowsRenderedCount > 1) {
                        settingY = gridCurrentY - verticalSpacing
                    } else {
                        settingY = firstItemInRowY + itemBoxHeight
                    }
                    settingY += 5
                }
            }
            else -> {
                context.drawText(
                    textRenderer,
                    Text.literal("No settings available").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                    x,
                    settingY,
                    CinnamonTheme.secondaryTextColor,
                    CinnamonTheme.enableTextShadow
                )
            }
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
        return items.distinct()
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
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val moduleListY = contentY + 60 // This is a Y coordinate in the scaled space
        val moduleListHeight = contentHeight - 70 // This is a height in the scaled space

        // Check if the scaled mouse click is within the scaled module list area
        if (scaledMouseX >= contentX && scaledMouseX < contentX + contentWidth &&
            scaledMouseY >= moduleListY && scaledMouseY < moduleListY + moduleListHeight) {

            val items = getFilteredModules()
            var currentY = moduleListY - scrollOffset // currentY is in scaled space
            val cardX = contentX + 10 // cardX is in scaled space
            val cardWidth = contentWidth - 20 // cardWidth is in scaled space

            items.forEach { item ->
                val currentItemHeight = when (item) { // currentItemHeight is in scaled space
                    is Module -> if (expandedStates[item.name] == true) settingsModuleHeight else baseModuleHeight
                    is HudElement -> if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight
                    else -> 0
                }

                // Compare scaled mouse Y with scaled card Y bounds
                if (scaledMouseY >= currentY && scaledMouseY < currentY + currentItemHeight) {
                    if (item is Module) {
                        val bottomSectionYModule = if (expandedStates[item.name] == true) {
                            currentY + settingsModuleHeight - 30
                        } else {
                            currentY + baseModuleHeight - 30
                        }
                        val toggleXModule = cardX + cardWidth - 50
                        val toggleYModule = bottomSectionYModule + 6

                        // Compare scaled mouse with scaled toggle switch bounds
                        if (scaledMouseX >= toggleXModule && scaledMouseX < toggleXModule + 30 &&
                            scaledMouseY >= toggleYModule && scaledMouseY < toggleYModule + 16) {
                            ModuleManager.toggleModule(item.name)
                            return true
                        }

                        val expandButtonTextModule = if (expandedStates[item.name] == true) "-" else "+"
                        val expandButtonWidthModule = textRenderer.getWidth(expandButtonTextModule)
                        val expandButtonXModule = cardX + cardWidth - expandButtonWidthModule - 12
                        val expandButtonYModule = currentY + 8

                        // Compare scaled mouse with scaled expand button bounds
                        if (scaledMouseX >= expandButtonXModule && scaledMouseX < expandButtonXModule + expandButtonWidthModule &&
                            scaledMouseY >= expandButtonYModule && scaledMouseY < expandButtonYModule + textRenderer.fontHeight) {
                            expandedStates[item.name] = !(expandedStates[item.name] ?: false)
                            scrollOffset = min(scrollOffset, maxScrollOffset)
                            return true
                        }

                        if (expandedStates[item.name] == true && item is AutoclickerModule) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = currentY + 40 + 5
                            // Pass scaled mouse coordinates to settings handler
                            if (handleAutoClickerSettings(scaledMouseX, scaledMouseY, settingsContentX, settingsContentY, cardWidth - 24, item)) {
                                return true
                            }
                        } else if (expandedStates[item.name] == true && item is ChatPrefixModule) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = currentY + 40 + 5
                            // Pass scaled mouse coordinates to settings handler
                            if (handleChatPrefixSettingsClick(scaledMouseX, scaledMouseY, settingsContentX, settingsContentY, cardWidth - 24, item)) {
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

                        // Compare scaled mouse with scaled toggle switch bounds
                        if (scaledMouseX >= toggleXHud && scaledMouseX < toggleXHud + toggleWidth &&
                            scaledMouseY >= toggleYHud && scaledMouseY < toggleYHud + toggleHeight) {
                            item.isEnabled = !item.isEnabled
                            return true
                        }

                        // Compare scaled mouse with scaled expand button bounds
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
                            // Compare scaled mouse Y with scaled settings area Y bounds
                            if (scaledMouseY >= settingsContentY && scaledMouseY < settingsContentY + settingsAreaActualHeight) {
                                // Pass scaled mouse coordinates to settings handler
                                if (handleHudElementSettingsClick(scaledMouseX, scaledMouseY, settingsContentX, settingsContentY, settingsContentWidth, item)) {
                                    return true
                                }
                            }
                        }
                    }
                    return true // Click was handled within an item
                }
                currentY += currentItemHeight + moduleSpacing
            }
        }
        // Pass raw (original) mouse coordinates to super
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun handleAutoClickerSettings(scaledMouseX: Double, scaledMouseY: Double, settingsX: Int, settingsY: Int, settingsWidth: Int, module: AutoclickerModule): Boolean {
        var checkY = settingsY + 15
        val buttonWidth = 16
        val buttonHeight = 12
        val settingSpacing = 14
        val checkboxSize = 10

        val minCpsButtonsX = settingsX + settingsWidth - 40
        if (scaledMouseY >= checkY - 1 && scaledMouseY < checkY - 1 + buttonHeight) {
            if (scaledMouseX >= minCpsButtonsX && scaledMouseX < minCpsButtonsX + buttonWidth) {
                module.setMinCPS(module.minCPS - 1.0f)
                return true
            }
            if (scaledMouseX >= minCpsButtonsX + 20 && scaledMouseX < minCpsButtonsX + 20 + buttonWidth) {
                module.setMinCPS(module.minCPS + 1.0f)
                return true
            }
        }
        checkY += settingSpacing

        val maxCpsButtonsX = settingsX + settingsWidth - 40
        if (scaledMouseY >= checkY - 1 && scaledMouseY < checkY - 1 + buttonHeight) {
            if (scaledMouseX >= maxCpsButtonsX && scaledMouseX < maxCpsButtonsX + buttonWidth) {
                module.setMaxCPS(module.maxCPS - 1.0f)
                return true
            }
            if (scaledMouseX >= maxCpsButtonsX + 20 && scaledMouseX < maxCpsButtonsX + 20 + buttonWidth) {
                module.setMaxCPS(module.maxCPS + 1.0f)
                return true
            }
        }
        checkY += settingSpacing

        if (scaledMouseX >= settingsX && scaledMouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Humanization") &&
            scaledMouseY >= checkY - 1 && scaledMouseY < checkY - 1 + checkboxSize) {
            module.setHumanizationEnabled(!module.enableHumanization)
            return true
        }
        checkY += settingSpacing

        if (scaledMouseX >= settingsX && scaledMouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Burst Mode") &&
            scaledMouseY >= checkY - 1 && scaledMouseY < checkY - 1 + checkboxSize) {
            module.setBurstMode(!module.burstMode)
            return true
        }
        checkY += settingSpacing

        if (scaledMouseX >= settingsX && scaledMouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Left Click") &&
            scaledMouseY >= checkY - 1 && scaledMouseY < checkY - 1 + checkboxSize) {
            module.setLeftClickEnabled(!module.leftClickEnabled)
            return true
        }
        checkY += settingSpacing

        if (scaledMouseX >= settingsX && scaledMouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Right Click") &&
            scaledMouseY >= checkY - 1 && scaledMouseY < checkY - 1 + checkboxSize) {
            module.setRightClickEnabled(!module.rightClickEnabled)
            return true
        }

        return false
    }

    override fun close() {
        CinnamonGuiManager.openMainMenu()
        HudManager.markChangesForSave()
        HudManager.saveHudConfig()
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        val contentX = getContentX()
        val contentWidth = getContentWidth()

        val categoryAreaHeight = 50
        val moduleListY = getContentY() + categoryAreaHeight + 10 // Scaled Y
        val moduleListHeight = getContentHeight() - categoryAreaHeight - 20 // Scaled height

        // Compare scaled mouse coordinates with scaled list area
        if (scaledMouseX >= contentX && scaledMouseX < contentX + contentWidth &&
            scaledMouseY >= moduleListY && scaledMouseY < moduleListY + moduleListHeight) {

            if (maxScrollOffset > 0) {
                val scrollAmount = (verticalAmount * 20).toInt() // Keep standard scroll speed
                scrollOffset = max(0, min(maxScrollOffset, scrollOffset - scrollAmount))
                return true // Event handled
            }
        }
        // Pass raw (original) mouse coordinates to super
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
}