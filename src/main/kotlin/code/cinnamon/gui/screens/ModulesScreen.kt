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
    private val settingsHudElementHeight = 148
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
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
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
                Text.literal(category).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
                { _, _ -> selectCategory(category) },
                this.selectedCategory == category
            )
            this.categoryButtons.add(catButton)
            addButton(catButton)
        }
    }

    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val categoryAreaHeight = 50
        val moduleListY = contentY + categoryAreaHeight + 10
        val moduleListHeight = contentHeight - categoryAreaHeight - 20

        renderModuleList(context, contentX + 10, moduleListY, contentWidth - 20, moduleListHeight, mouseX, mouseY, delta)

        if (maxScrollOffset > 0) {
            renderScrollbar(context, contentX + contentWidth - 8, moduleListY, 6, moduleListHeight)
        }
    }

    private fun renderModuleList(context: DrawContext, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int, delta: Float) {
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
                    renderModuleCard(context, x, currentY, width, itemHeight, item, mouseX, mouseY, delta)
                } else if (item is HudElement) {
                    renderHudElementCard(context, x, currentY, width, itemHeight, item, mouseX, mouseY, delta)
                }
            }
            currentY += itemHeight + moduleSpacing
        }
        context.disableScissor()
    }

    private fun renderHudElementCard(context: DrawContext, x: Int, y: Int, width: Int, height: Int, element: HudElement, mouseX: Int, mouseY: Int, delta: Float) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
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
            Text.literal(element.getName()).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + 12,
            y + 8,
            if (element.isEnabled) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor,
            true
        )

        val toggleWidth = 30
        val toggleHeight = 16
        val headerContentHeight = baseModuleHeight
        val toggleY = y + (headerContentHeight - toggleHeight) / 2

        val expandButtonText = if (expandedStates[element.getName()] == true) "-" else "+"
        val expandButtonWidth = textRenderer.getWidth(expandButtonText)
        val expandButtonX = x + width - expandButtonWidth - 12
        val toggleX = expandButtonX - toggleWidth - 8

        renderToggleSwitch(context, toggleX, toggleY, toggleWidth, toggleHeight, element.isEnabled, mouseX, mouseY)

        context.drawText(
            textRenderer,
            Text.literal(expandButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            expandButtonX,
            y + 8,
            CinnamonTheme.primaryTextColor,
            true
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
                mouseX,
                mouseY,
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
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        var currentY = y

        context.drawText(
            textRenderer,
            Text.literal("Settings").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x, currentY, CinnamonTheme.titleColor, true
        )
        currentY += 15

        if (element !is PacketHandlerHudElement) {
            val textColorText = "Text Color: ${element.textColor.toRGBHexString()}"
            context.drawText(textRenderer, Text.literal(textColorText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, currentY, CinnamonTheme.primaryTextColor, false)
            val setTextColorButtonText = "[Set]"
            val setTextColorButtonWidth = textRenderer.getWidth(setTextColorButtonText)
            context.drawText(textRenderer, Text.literal(setTextColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x + width - setTextColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            val bgColorText = "Background: ${element.backgroundColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(bgColorText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, currentY, CinnamonTheme.primaryTextColor, false)
            val setBgColorButtonText = "[Set]"
            val setBgColorButtonWidth = textRenderer.getWidth(setBgColorButtonText)
            context.drawText(textRenderer, Text.literal(setBgColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x + width - setBgColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            drawCheckbox(context, x, currentY, "Enable Text Shadow", element.textShadowEnabled)
            currentY += 14
        }

        if (element is KeystrokesHudElement) {
            val keyPressedTextColorText = "Pressed Text: ${element.keypressedTextColor.toRGBHexString()}"
            context.drawText(textRenderer, Text.literal(keyPressedTextColorText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, currentY, CinnamonTheme.primaryTextColor, false)
            val setKeyPressedTextColorButtonText = "[Set]"
            val setKeyPressedTextColorButtonWidth = textRenderer.getWidth(setKeyPressedTextColorButtonText)
            context.drawText(textRenderer, Text.literal(setKeyPressedTextColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x + width - setKeyPressedTextColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            val keyPressedBgColorText = "Pressed Background: ${element.keypressedBackgroundColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(keyPressedBgColorText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, currentY, CinnamonTheme.primaryTextColor, false)
            val setKeyPressedBgColorButtonText = "[Set]"
            val setKeyPressedBgColorButtonWidth = textRenderer.getWidth(setKeyPressedBgColorButtonText)
            context.drawText(textRenderer, Text.literal(setKeyPressedBgColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x + width - setKeyPressedBgColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14
        }

        if (element is PacketHandlerHudElement) {
            val buttonColorText = "Button Color: ${element.buttonColor.toRGBAHexString()}"
            context.drawText(textRenderer, Text.literal(buttonColorText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, currentY, CinnamonTheme.primaryTextColor, false)
            val setButtonColorButtonText = "[Set]"
            val setButtonColorButtonWidth = textRenderer.getWidth(setButtonColorButtonText)
            context.drawText(textRenderer, Text.literal(setButtonColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x + width - setButtonColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            val buttonTextColorText = "Button Text: ${element.buttonTextColor.toRGBHexString()}"
            context.drawText(textRenderer, Text.literal(buttonTextColorText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, currentY, CinnamonTheme.primaryTextColor, false)
            val setButtonTextColorButtonText = "[Set]"
            val setButtonTextColorButtonWidth = textRenderer.getWidth(setButtonTextColorButtonText)
            context.drawText(textRenderer, Text.literal(setButtonTextColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x + width - setButtonTextColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14

            drawCheckbox(context, x, currentY, "Button Text Shadow", element.buttonTextShadowEnabled)
            currentY += 14

            val buttonHoverColorText = "Button Hover: ${element.buttonHoverColor.toRGBAHexString()}" 
            context.drawText(textRenderer, Text.literal(buttonHoverColorText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, currentY, CinnamonTheme.primaryTextColor, false)
            val setButtonHoverColorButtonText = "[Set]"
            val setButtonHoverColorButtonWidth = textRenderer.getWidth(setButtonHoverColorButtonText)
            context.drawText(textRenderer, Text.literal(setButtonHoverColorButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x + width - setButtonHoverColorButtonWidth, currentY, CinnamonTheme.accentColor, false)
            currentY += 14
        }
    }

    private fun handleHudElementSettingsClick(
        mouseX: Double,
        mouseY: Double,
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
            if (mouseX >= setTextColorButtonX && mouseX < setTextColorButtonX + setTextColorButtonWidth &&
                mouseY >= setTextColorButtonY && mouseY < setTextColorButtonY + textElementHeight
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
            if (mouseX >= setBgColorButtonX && mouseX < setBgColorButtonX + setBgColorButtonWidth &&
                mouseY >= setBgColorButtonY && mouseY < setBgColorButtonY + textElementHeight
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
            if (mouseX >= shadowCheckboxX && mouseX < shadowCheckboxX + checkboxSize + 6 + shadowCheckboxTextWidth &&
                mouseY >= shadowCheckboxY && mouseY < shadowCheckboxY + checkboxSize
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
            if (mouseX >= setKeyPressedTextColorButtonX && mouseX < setKeyPressedTextColorButtonX + setKeyPressedTextColorButtonWidth &&
                mouseY >= setKeyPressedTextColorButtonY && mouseY < setKeyPressedTextColorButtonY + textElementHeight
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
            if (mouseX >= setKeyPressedBgColorButtonX && mouseX < setKeyPressedBgColorButtonX + setKeyPressedBgColorButtonWidth &&
                mouseY >= setKeyPressedBgColorButtonY && mouseY < setKeyPressedBgColorButtonY + textElementHeight
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
            if (mouseX >= setButtonColorButtonX && mouseX < setButtonColorButtonX + setButtonColorButtonWidth &&
                mouseY >= setButtonColorButtonY && mouseY < setButtonColorButtonY + textElementHeight
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
            if (mouseX >= setButtonTextColorButtonX && mouseX < setButtonTextColorButtonX + setButtonTextColorButtonWidth &&
                mouseY >= setButtonTextColorButtonY && mouseY < setButtonTextColorButtonY + textElementHeight
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
            if (mouseX >= buttonShadowCheckboxX && mouseX < buttonShadowCheckboxX + checkboxSize + 6 + buttonShadowCheckboxTextWidth &&
                mouseY >= buttonShadowCheckboxY && mouseY < buttonShadowCheckboxY + checkboxSize
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
            if (mouseX >= setButtonHoverColorButtonX && mouseX < setButtonHoverColorButtonX + setButtonHoverColorButtonWidth &&
                mouseY >= setButtonHoverColorButtonY && mouseY < setButtonHoverColorButtonY + textElementHeight
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
        }
        return false
    }

    private fun handleChatPrefixSettingsClick(mouseX: Double, mouseY: Double, settingsX: Int, settingsY: Int, settingsWidth: Int, module: ChatPrefixModule): Boolean {
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
        val availableGridHeight = (settingsY + effectiveSettingsContentHeight) - gridCurrentY - 5 

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

            if (mouseX >= itemHitboxX && mouseX < itemHitboxEndX &&
                mouseY >= itemHitboxY && mouseY < itemHitboxEndY) {
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
                Text.literal("✓").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
                x + 1,
                y + 1,
                CinnamonTheme.titleColor,
                false
            )
        }
        context.drawText(
            textRenderer,
            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + checkboxSize + 6,
            y + 1,
            CinnamonTheme.primaryTextColor,
            false
        )
    }

    private fun renderModuleCard(context: DrawContext, x: Int, y: Int, width: Int, moduleHeight: Int, module: Module, mouseX: Int, mouseY: Int, delta: Float) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + moduleHeight
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
            Text.literal(module.name).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + 12,
            y + 8,
            if (module.isEnabled) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor,
            true
        )

        val expandButtonText = if (expandedStates[module.name] == true) "-" else "+"
        val expandButtonWidth = textRenderer.getWidth(expandButtonText)
        context.drawText(
            textRenderer,
            Text.literal(expandButtonText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + width - expandButtonWidth - 12,
            y + 8,
            CinnamonTheme.primaryTextColor,
            true
        )

        context.drawText(
            textRenderer,
            Text.literal(module.description).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + 12,
            y + 22,
            CinnamonTheme.secondaryTextColor,
            false
        )

        val bottomSectionY = if (expandedStates[module.name] == true) {
            y + settingsModuleHeight - 30
        } else {
            y + baseModuleHeight - 30
        }

        renderToggleSwitch(context, x + width - 50, bottomSectionY + 6, 30, 16, module.isEnabled, mouseX, mouseY)
        val statusColor = if (module.isEnabled) CinnamonTheme.successColor else CinnamonTheme.moduleDisabledColor
        context.fill(x + 12, bottomSectionY + 18, x + 20, bottomSectionY + 26, statusColor)

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
                context.drawText(textRenderer, Text.literal("Settings").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, settingY, CinnamonTheme.titleColor, true)
                settingY += 15
                val buttonWidth = 16
                val buttonHeight = 12
                val settingSpacing = 14
                val minCpsText = "Min CPS: %.1f".format(module.minCPS)
                context.drawText(textRenderer, Text.literal(minCpsText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, settingY, CinnamonTheme.primaryTextColor, false)
                val minCpsButtonsX = x + width - 40
                drawSettingButton(context, minCpsButtonsX, settingY - 1, buttonWidth, buttonHeight, "-", false)
                drawSettingButton(context, minCpsButtonsX + 20, settingY - 1, buttonWidth, buttonHeight, "+", false)
                settingY += settingSpacing
                val maxCpsText = "Max CPS: %.1f".format(module.maxCPS)
                context.drawText(textRenderer, Text.literal(maxCpsText).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, settingY, CinnamonTheme.primaryTextColor, false)
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
                context.drawText(textRenderer, Text.literal("Settings").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), x, settingY, CinnamonTheme.titleColor, true)
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
                            Text.literal("✓").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
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
                        Text.literal(colorEnumEntry.friendlyName).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
                        textX,
                        textY,
                        CinnamonTheme.primaryTextColor,
                        false
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
        context.fill(x, y, x + width, y + height, bgColor)
        context.fill(x, y, x + width, y + 1, CinnamonTheme.borderColor)
        context.fill(x, y + height - 1, x + width, y + height, CinnamonTheme.borderColor)
        context.fill(x, y, x + 1, y + height, CinnamonTheme.borderColor)
        context.fill(x + width - 1, y, x + width, y + height, CinnamonTheme.borderColor)
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

    private fun renderToggleSwitch(context: DrawContext, x: Int, y: Int, width: Int, height: Int, enabled: Boolean, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
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
        val contentX = getContentX()
        val contentY = getContentY()
        val contentWidth = getContentWidth()
        val contentHeight = getContentHeight()
        val moduleListY = contentY + 60
        val moduleListHeight = contentHeight - 70

        if (mouseX >= contentX && mouseX < contentX + contentWidth &&
            mouseY >= moduleListY && mouseY < moduleListY + moduleListHeight) {

            val items = getFilteredModules()
            var currentY = moduleListY - scrollOffset
            val cardX = contentX + 10
            val cardWidth = contentWidth - 20

            items.forEach { item ->
                val currentItemHeight = when (item) {
                    is Module -> if (expandedStates[item.name] == true) settingsModuleHeight else baseModuleHeight
                    is HudElement -> if (expandedStates[item.getName()] == true) settingsHudElementHeight else baseModuleHeight
                    else -> 0
                }

                if (mouseY >= currentY && mouseY < currentY + currentItemHeight) {
                    if (item is Module) {
                        val bottomSectionYModule = if (expandedStates[item.name] == true) {
                            currentY + settingsModuleHeight - 30
                        } else {
                            currentY + baseModuleHeight - 30
                        }
                        val toggleXModule = cardX + cardWidth - 50
                        val toggleYModule = bottomSectionYModule + 6

                        if (mouseX >= toggleXModule && mouseX < toggleXModule + 30 &&
                            mouseY >= toggleYModule && mouseY < toggleYModule + 16) {
                            ModuleManager.toggleModule(item.name)
                            return true
                        }

                        val expandButtonTextModule = if (expandedStates[item.name] == true) "-" else "+"
                        val expandButtonWidthModule = textRenderer.getWidth(expandButtonTextModule)
                        val expandButtonXModule = cardX + cardWidth - expandButtonWidthModule - 12
                        val expandButtonYModule = currentY + 8

                        if (mouseX >= expandButtonXModule && mouseX < expandButtonXModule + expandButtonWidthModule &&
                            mouseY >= expandButtonYModule && mouseY < expandButtonYModule + textRenderer.fontHeight) {
                            expandedStates[item.name] = !(expandedStates[item.name] ?: false)
                            scrollOffset = min(scrollOffset, maxScrollOffset)
                            return true
                        }

                        if (expandedStates[item.name] == true && item is AutoclickerModule) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = currentY + 40 + 5
                            if (handleAutoClickerSettings(mouseX, mouseY, settingsContentX, settingsContentY, cardWidth - 24, item)) {
                                return true
                            }
                        } else if (expandedStates[item.name] == true && item is ChatPrefixModule) { 
                            val settingsContentX = cardX + 12
                            val settingsContentY = currentY + 40 + 5 
                            if (handleChatPrefixSettingsClick(mouseX, mouseY, settingsContentX, settingsContentY, cardWidth - 24, item)) {
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

                        if (mouseX >= toggleXHud && mouseX < toggleXHud + toggleWidth &&
                            mouseY >= toggleYHud && mouseY < toggleYHud + toggleHeight) {
                            item.isEnabled = !item.isEnabled
                            return true
                        }

                        if (mouseX >= expandButtonXHud && mouseX < expandButtonXHud + expandButtonWidthHud &&
                            mouseY >= expandButtonYHud && mouseY < expandButtonYHud + textRenderer.fontHeight) {
                            expandedStates[item.getName()] = !(expandedStates[item.getName()] ?: false)
                            scrollOffset = min(scrollOffset, maxScrollOffset)
                            return true
                        }

                        if (expandedStates[item.getName()] == true) {
                            val settingsContentX = cardX + 12
                            val settingsContentY = currentY + baseModuleHeight
                            val settingsContentWidth = cardWidth - 24
                            val settingsAreaActualHeight = settingsHudElementHeight - baseModuleHeight - 8
                            if (mouseY >= settingsContentY && mouseY < settingsContentY + settingsAreaActualHeight) {
                                if (handleHudElementSettingsClick(mouseX, mouseY, settingsContentX, settingsContentY, settingsContentWidth, item)) {
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

    private fun handleAutoClickerSettings(mouseX: Double, mouseY: Double, settingsX: Int, settingsY: Int, settingsWidth: Int, module: AutoclickerModule): Boolean {
        var checkY = settingsY + 15
        val buttonWidth = 16
        val buttonHeight = 12
        val settingSpacing = 14
        val checkboxSize = 10

        val minCpsButtonsX = settingsX + settingsWidth - 40
        if (mouseY >= checkY - 1 && mouseY < checkY - 1 + buttonHeight) {
            if (mouseX >= minCpsButtonsX && mouseX < minCpsButtonsX + buttonWidth) {
                module.setMinCPS(module.minCPS - 1.0f)
                return true
            }
            if (mouseX >= minCpsButtonsX + 20 && mouseX < minCpsButtonsX + 20 + buttonWidth) {
                module.setMinCPS(module.minCPS + 1.0f)
                return true
            }
        }
        checkY += settingSpacing

        val maxCpsButtonsX = settingsX + settingsWidth - 40
        if (mouseY >= checkY - 1 && mouseY < checkY - 1 + buttonHeight) {
            if (mouseX >= maxCpsButtonsX && mouseX < maxCpsButtonsX + buttonWidth) {
                module.setMaxCPS(module.maxCPS - 1.0f)
                return true
            }
            if (mouseX >= maxCpsButtonsX + 20 && mouseX < maxCpsButtonsX + 20 + buttonWidth) {
                module.setMaxCPS(module.maxCPS + 1.0f)
                return true
            }
        }
        checkY += settingSpacing

        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Humanization") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
            module.setHumanizationEnabled(!module.enableHumanization)
            return true
        }
        checkY += settingSpacing

        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Burst Mode") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
            module.setBurstMode(!module.burstMode)
            return true
        }
        checkY += settingSpacing

        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Left Click") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
            module.setLeftClickEnabled(!module.leftClickEnabled)
            return true
        }
        checkY += settingSpacing

        if (mouseX >= settingsX && mouseX < settingsX + checkboxSize + 6 + textRenderer.getWidth("Right Click") &&
            mouseY >= checkY - 1 && mouseY < checkY - 1 + checkboxSize) {
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
        val contentX = getContentX()
        val contentWidth = getContentWidth()

        val categoryAreaHeight = 50
        val moduleListY = getContentY() + categoryAreaHeight + 10
        val moduleListHeight = getContentHeight() - categoryAreaHeight - 20

        if (mouseX >= contentX && mouseX < contentX + contentWidth &&
            mouseY >= moduleListY && mouseY < moduleListY + moduleListHeight) {

            if (maxScrollOffset > 0) {
                val scrollAmount = (verticalAmount * 20).toInt()
                scrollOffset = max(0, min(maxScrollOffset, scrollOffset - scrollAmount))
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
}