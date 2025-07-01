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

    private var textShadowButton: CinnamonButton? = null
    private var fontToggleButton: CinnamonButton? = null
    private var backButton: CinnamonButton? = null
    private var resetButton: CinnamonButton? = null

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
            CinnamonTheme.updateDependentColors()
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
    private fun getListDimensions(): ListDimensions {
        val listX = guiX + 40
        val contentY = getContentY()
        val listY = contentY + 20
        val listWidth = guiWidth - 80
        val listHeight = getContentHeight() - (2 * CinnamonTheme.BUTTON_HEIGHT) - 85
        return ListDimensions(listX, listY, listWidth, listHeight)
    }

    data class ListDimensions(val x: Int, val y: Int, val width: Int, val height: Int)

    override fun initializeComponents() {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        val bottomControlsY = contentY + getContentHeight() - 45
        clearButtons()

        val buttonStartY = bottomControlsY - (CinnamonTheme.BUTTON_HEIGHT * 2) - 20 


        textShadowButton = CinnamonButton(
            guiX + PADDING, 
            buttonStartY + 42,
            150,
            CinnamonTheme.BUTTON_HEIGHT,
            getTextShadowButtonText(),
            { _, _ -> 
                CinnamonTheme.enableTextShadow = !CinnamonTheme.enableTextShadow
                ThemeConfigManager.saveTheme()
                textShadowButton?.text = getTextShadowButtonText() 
            }
        )
        addButton(textShadowButton!!)

        val fontToggleButtonY = buttonStartY + CinnamonTheme.BUTTON_HEIGHT + 10
        fontToggleButton = CinnamonButton(
            guiX + PADDING, 
            fontToggleButtonY + 42,
            150,
            CinnamonTheme.BUTTON_HEIGHT,
            getFontToggleButtonText(),
            { mouseX: Double, mouseY: Double -> 
                CinnamonTheme.useMinecraftFont = !CinnamonTheme.useMinecraftFont
                ThemeConfigManager.saveTheme()
                fontToggleButton?.text = getFontToggleButtonText()
                textShadowButton?.text = getTextShadowButtonText() 
                backButton?.text = Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
                resetButton?.text = Text.literal("Reset").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            }
        )
        addButton(fontToggleButton!!)

        val backAndResetButtonY = bottomControlsY 
        backButton = CinnamonButton(
            centerX - 50, 
            backAndResetButtonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> CinnamonGuiManager.openMainMenu() }
        )
        addButton(backButton!!)
        
        resetButton = CinnamonButton(
            guiX + guiWidth - PADDING - 100,
            backAndResetButtonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Reset").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> 
                this.resetToDefaults() 
                initializeComponents() 
            }
        )
        addButton(resetButton!!)
    }

    private fun getTextShadowButtonText(): Text {
        val status = if (CinnamonTheme.enableTextShadow) "Enabled" else "Disabled"
        return Text.literal("Text Shadow: $status").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
    }

    private fun getFontToggleButtonText(): Text {
        val fontName = if (CinnamonTheme.useMinecraftFont) "Minecraft" else "Custom"
        return Text.literal("Font: $fontName").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
    }

    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val contentY = getContentY()
        renderColorList(context, mouseX, mouseY, contentY)
    }

    private fun renderColorList(context: DrawContext, mouseX: Int, mouseY: Int, contentYPos: Int) {
        val dims = getListDimensions()
        
        context.drawBorder(dims.x, dims.y, dims.width, dims.height, CinnamonTheme.borderColor)

        context.enableScissor(dims.x, dims.y, dims.x + dims.width, dims.y + dims.height)
        val colors = ColorType.values()
        var currentY = dims.y + 10 - scrollOffset
        for ((index, colorType) in colors.withIndex()) {
            if (currentY > dims.y + dims.height) break
            if (currentY + itemHeight < dims.y) {
                currentY += itemHeight
                continue
            }
            renderColorItem(context, colorType, dims.x + 10, currentY, dims.width - 20, itemHeight - 5, mouseX, mouseY)
            currentY += itemHeight
        }
        context.disableScissor()
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
            Text.literal(colorType.displayName).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + 20 + colorSquareSize,
            y + (height - textRenderer.fontHeight) / 2,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )
        val hexValue = String.format("#%08X", currentColor)
        val hexWidth = textRenderer.getWidth(hexValue)
        context.drawText(
            textRenderer,
            Text.literal(hexValue).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + width - hexWidth - 10,
            y + (height - textRenderer.fontHeight) / 2,
            CinnamonTheme.secondaryTextColor,
            CinnamonTheme.enableTextShadow 
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val dims = getListDimensions()
        
        
        if (mouseX >= dims.x && mouseX < dims.x + dims.width &&
            mouseY >= dims.y && mouseY < dims.y + dims.height) {

            val colors = ColorType.values()
            
            
            val listContentY = dims.y + 10 
            val adjustedMouseY = mouseY + scrollOffset - listContentY
            
            if (adjustedMouseY >= 0) {
                val clickedIndex = (adjustedMouseY / itemHeight).toInt()
                
                
                if (clickedIndex >= 0 && clickedIndex < colors.size) {
                    val itemY = listContentY + (clickedIndex * itemHeight) - scrollOffset
                    
                    
                    if (itemY + itemHeight > dims.y && itemY < dims.y + dims.height) {
                        val colorType = colors[clickedIndex]
                        openColorPicker(colorType)
                        return true
                    }
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
        val dims = getListDimensions()
        
        if (mouseY >= dims.y && mouseY < dims.y + dims.height) {
            val totalContentHeight = ColorType.values().size * itemHeight + 20 
            val maxScroll = maxOf(0, totalContentHeight - dims.height)
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
        ThemeConfigManager.saveTheme()
        CinnamonGuiManager.openMainMenu()
    }

    override fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderFooter(context, mouseX, mouseY, delta)
        val statusText = Text.literal("Theme Editor").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        context.drawText(
            textRenderer,
            statusText,
            guiX + PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.infoColor,
            CinnamonTheme.enableTextShadow 
        )
        val colorCountText = Text.literal("${ColorType.values().size} Colors Available").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val colorCountWidth = textRenderer.getWidth(colorCountText)
        context.drawText(
            textRenderer,
            colorCountText,
            guiX + guiWidth - colorCountWidth - PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.secondaryTextColor,
            CinnamonTheme.enableTextShadow 
        )
    }
    
    private fun clearButtons() {
        textShadowButton = null
        fontToggleButton = null
        backButton = null
        resetButton = null
    }
}