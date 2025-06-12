package code.cinnamon.gui.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.gui.theme.ThemeConfigManager

class ThemeManagerScreen : CinnamonScreen(Text.literal("Theme Manager").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {

    private var scrollOffset = 0
    private val itemHeight = 35
    private val maxVisibleItems = 12

    enum class ColorType(val displayName: String, val currentColor: () -> Int, val setter: (Int) -> Unit) {
        CORE_BACKGROUND_PRIMARY("Primary Background", { CinnamonTheme.coreBackgroundPrimary }, { color ->
            CinnamonTheme.coreBackgroundPrimary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_ACCENT_PRIMARY("Primary Accent", { CinnamonTheme.coreAccentPrimary }, { color ->
            CinnamonTheme.coreAccentPrimary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_TEXT_PRIMARY("Primary Text", { CinnamonTheme.coreTextPrimary }, { color ->
            CinnamonTheme.coreTextPrimary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_BORDER("Border Color", { CinnamonTheme.coreBorder }, { color ->
            CinnamonTheme.coreBorder = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_BUTTON_BACKGROUND("Button Background", { CinnamonTheme.coreButtonBackground }, { color ->
            CinnamonTheme.coreButtonBackground = color
            CinnamonTheme.updateDependentColors() // Important to update dependents if button visuals rely on it
        }),
        CORE_STATUS_SUCCESS("Success Color", { CinnamonTheme.coreStatusSuccess }, { color ->
            CinnamonTheme.coreStatusSuccess = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_STATUS_WARNING("Warning Color", { CinnamonTheme.coreStatusWarning }, { color ->
            CinnamonTheme.coreStatusWarning = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_STATUS_ERROR("Error Color", { CinnamonTheme.coreStatusError }, { color ->
            CinnamonTheme.coreStatusError = color
            CinnamonTheme.updateDependentColors()
        });
    }

    override fun initializeComponents() {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        val buttonY = contentY + getContentHeight() - 45
        clearButtons()
        addButton(CinnamonButton(
            guiX + PADDING,
            buttonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            { _, _ -> CinnamonGuiManager.openMainMenu() }
        ))
        addButton(CinnamonButton(
            centerX - 50,
            buttonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Reset").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            { _, _ -> resetToDefaults() }
        ))
        addButton(CinnamonButton(
            guiX + guiWidth - PADDING - 100,
            buttonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Save").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            { _, _ -> saveTheme() },
            false
        ))
    }

    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        renderColorList(context, mouseX, mouseY, contentY)
    }

    private fun renderColorList(context: DrawContext, mouseX: Int, mouseY: Int, contentYPos: Int) {
        val listX = guiX + 40
        val listY = contentYPos + 20
        val listWidth = guiWidth - 80
        val listHeight = getContentHeight() - 170

        context.fill(listX, listY, listX + listWidth, listY + listHeight, CinnamonTheme.contentBackground)
        context.drawBorder(listX, listY, listWidth, listHeight, CinnamonTheme.borderColor)

        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight)
        val colors = ColorType.values()
        var currentY = listY + 10 - scrollOffset
        for ((index, colorType) in colors.withIndex()) {
            if (currentY > listY + listHeight) break
            if (currentY + itemHeight < listY) {
                currentY += itemHeight
                continue
            }
            renderColorItem(context, colorType, listX + 10, currentY, listWidth - 20, itemHeight - 5, mouseX, mouseY)
            currentY += itemHeight
        }
        context.disableScissor()

        // Scroll indicators
        if (scrollOffset > 0) {
            context.drawText(textRenderer, Text.literal("▲").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), listX + listWidth - 20, listY + 5, CinnamonTheme.accentColor, false)
        }
        if (colors.size * itemHeight > listHeight + scrollOffset) {
            context.drawText(textRenderer, Text.literal("▼").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), listX + listWidth - 20, listY + listHeight - 15, CinnamonTheme.accentColor, false)
        }
    }

    private fun renderColorItem(context: DrawContext, colorType: ColorType, x: Int, y: Int, width: Int, height: Int, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
        val backgroundColor = if (isHovered) CinnamonTheme.cardBackgroundHover else CinnamonTheme.cardBackground
        context.fill(x, y, x + width, y + height, backgroundColor)
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)
        val colorSquareSize = height - 10
        val currentColor = colorType.currentColor()
        context.fill(x + 10, y + 5, x + 10 + colorSquareSize, y + 5 + colorSquareSize, currentColor)
        context.drawBorder(x + 10, y + 5, colorSquareSize, colorSquareSize, 0xFFFFFFFF.toInt())
        context.drawText(
            textRenderer,
            Text.literal(colorType.displayName).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + 20 + colorSquareSize,
            y + (height - textRenderer.fontHeight) / 2,
            CinnamonTheme.primaryTextColor,
            false
        )
        val hexValue = String.format("#%08X", currentColor)
        val hexWidth = textRenderer.getWidth(hexValue)
        context.drawText(
            textRenderer,
            Text.literal(hexValue).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + width - hexWidth - 10,
            y + (height - textRenderer.fontHeight) / 2,
            CinnamonTheme.secondaryTextColor,
            false
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Color list click for color picker
        val listOuterX = guiX + 40
        val listOuterY = getContentY() + 20
        val listContentWidth = guiWidth - 80
        val listContentHeight = getContentHeight() - 170

        if (mouseX >= listOuterX && mouseX < listOuterX + listContentWidth &&
            mouseY >= listOuterY && mouseY < listOuterY + listContentHeight) {

            val colors = ColorType.values()
            val relativeMouseY = mouseY - (listOuterY + 10 - scrollOffset)
            if (relativeMouseY >= 0) {
                val clickedIndex = (relativeMouseY / itemHeight).toInt()
                if (clickedIndex >= 0 && clickedIndex < colors.size) {
                    val colorType = colors[clickedIndex]
                    openColorPicker(colorType)
                    return true
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun openColorPicker(colorType: ColorType) {
        CinnamonGuiManager.openScreen(
            ColorPickerScreen(
                initialColor = colorType.currentColor(),
                onPick = { pickedColor ->
                    colorType.setter(pickedColor)
                    ThemeConfigManager.saveTheme()
                    CinnamonGuiManager.openScreen(this)
                },
                onCancel = {
                    CinnamonGuiManager.openScreen(this)
                }
            )
        )
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val listY = getContentY() + 80
        val listHeight = getContentHeight() - 170
        if (mouseY >= listY && mouseY < listY + listHeight) {
            val maxScroll = maxOf(0, ColorType.values().size * itemHeight - listHeight)
            scrollOffset = (scrollOffset - verticalAmount.toInt() * 20).coerceIn(0, maxScroll)
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    private fun resetToDefaults() {
        CinnamonTheme.resetToDefaults()
    }

    private fun saveTheme() {
        ThemeConfigManager.saveTheme()
    }

    override fun close() {
        // Auto-save theme config when this screen is closed
        ThemeConfigManager.saveTheme()
        // Instead of calling super.close(), open the main menu (just like HudScreen)
        CinnamonGuiManager.openMainMenu()
    }

    override fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderFooter(context, mouseX, mouseY, delta)
        val statusText = Text.literal("Theme Editor").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        context.drawText(
            textRenderer,
            statusText,
            guiX + PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.infoColor,
            false
        )
        val colorCountText = Text.literal("${ColorType.values().size} Colors Available").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        val colorCountWidth = textRenderer.getWidth(colorCountText)
        context.drawText(
            textRenderer,
            colorCountText,
            guiX + guiWidth - colorCountWidth - PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.secondaryTextColor,
            false
        )
    }

    // Helper function to clear buttons (implement as needed in your base class)
    private fun clearButtons() {
        // Implementation depends on your CinnamonScreen base class
    }
}