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
        }),
        BUTTON_OUTLINE_COLOR("Button Outline", { CinnamonTheme.buttonOutlineColor }, { color ->
            CinnamonTheme.buttonOutlineColor = color

        }),
        BUTTON_OUTLINE_HOVER_COLOR("Button Hover Outline", { CinnamonTheme.buttonOutlineHoverColor }, { color ->
            CinnamonTheme.buttonOutlineHoverColor = color
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

    override fun renderContent(context: DrawContext, rawMouseX: Int, rawMouseY: Int, delta: Float) {
        val scaledMouseX = scaleMouseX(rawMouseX.toDouble()).toInt()
        val scaledMouseY = scaleMouseY(rawMouseY.toDouble()).toInt()

        val contentY = getContentY() // This is a scaled Y coordinate
        renderColorList(context, scaledMouseX, scaledMouseY, contentY)
    }

    private fun renderColorList(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, contentYPos: Int) {
        val dims = getListDimensions() // dims are in scaled coordinates

        context.drawBorder(dims.x, dims.y, dims.width, dims.height, CinnamonTheme.borderColor)

        context.enableScissor(dims.x, dims.y, dims.x + dims.width, dims.y + dims.height)
        val colors = ColorType.values()
        var currentY = dims.y + 10 - scrollOffset // currentY is a scaled Y
        for ((index, colorType) in colors.withIndex()) {
            if (currentY > dims.y + dims.height) break
            if (currentY + itemHeight < dims.y) { // itemHeight is scaled
                currentY += itemHeight
                continue
            }
            // Pass scaled mouse coordinates to renderColorItem
            renderColorItem(context, colorType, dims.x + 10, currentY, dims.width - 20, itemHeight - 5, scaledMouseX, scaledMouseY)
            currentY += itemHeight
        }
        context.disableScissor()
    }

    private fun renderColorItem(context: DrawContext, colorType: ColorType, x: Int, y: Int, width: Int, height: Int, scaledMouseX: Int, scaledMouseY: Int) {
        // Compare scaled mouse coordinates with scaled item bounds (x, y, width, height are scaled)
        val isHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= y && scaledMouseY < y + height
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

    override fun mouseClicked(rawMouseX: Double, rawMouseY: Double, button: Int): Boolean {
        // First, let CinnamonButtons handle their clicks. Pass raw coordinates.
        // CinnamonScreen.mouseClicked will scale them for CinnamonButton checks.
        if (super.mouseClicked(rawMouseX, rawMouseY, button)) {
            return true
        }

        // Scale mouse coordinates for custom hit detection below
        val scaledMouseX = scaleMouseX(rawMouseX)
        val scaledMouseY = scaleMouseY(rawMouseY)

        val dims = getListDimensions() // Scaled dimensions of the list area

        // Compare scaled mouse coordinates with scaled list area bounds
        if (scaledMouseX >= dims.x && scaledMouseX < dims.x + dims.width &&
            scaledMouseY >= dims.y && scaledMouseY < dims.y + dims.height) {

            val colors = ColorType.values()
            val listContentY = dims.y + 10 // Scaled Y of the first item's potential top
            // Calculate adjustedMouseY based on scaledMouseY relative to the scrollable content
            val adjustedMouseYInList = scaledMouseY - listContentY + scrollOffset

            if (adjustedMouseYInList >= 0) {
                val clickedIndex = (adjustedMouseYInList / itemHeight).toInt() // itemHeight is scaled

                if (clickedIndex >= 0 && clickedIndex < colors.size) {
                    // No need to re-check if item is visible, scissor rect handles that.
                    // The click is within the logical list area and an item was indexed.
                    val colorType = colors[clickedIndex]
                    openColorPicker(colorType)
                    return true
                }
            }
        }
        // If no custom element handled the click, and super did not (CinnamonButtons),
        // then the click was not handled by this screen's specific logic.
        // Screen.mouseClicked (vanilla) default is false, so this is fine.
        return false
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

    override fun mouseScrolled(rawMouseX: Double, rawMouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        // Scale mouse coordinates for the area check
        val scaledMouseX = scaleMouseX(rawMouseX) // Though not used in current logic, good practice if X mattered
        val scaledMouseY = scaleMouseY(rawMouseY)

        val dims = getListDimensions() // Scaled dimensions

        // Compare scaled mouse Y with scaled list area bounds
        if (scaledMouseY >= dims.y && scaledMouseY < dims.y + dims.height) {
            val totalContentHeight = ColorType.values().size * itemHeight + 20 // Scaled total height
            val maxScroll = maxOf(0, totalContentHeight - dims.height)
            scrollOffset = (scrollOffset - verticalAmount.toInt() * 20).coerceIn(0, maxScroll)
            return true // Event handled
        }
        // Pass raw (original) mouse coordinates to super
        return super.mouseScrolled(rawMouseX, rawMouseY, horizontalAmount, verticalAmount)
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