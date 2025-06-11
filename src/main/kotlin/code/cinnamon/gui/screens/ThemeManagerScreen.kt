package code.cinnamon.gui.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.util.Identifier
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import kotlin.math.*
import code.cinnamon.gui.theme.ThemeConfigManager

class ThemeManagerScreen : CinnamonScreen(Text.literal("Theme Manager").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {
    
    private var showColorPicker = false
    private var selectedColorType: ColorType? = null
    
    // Fixed color picker position (centered on screen)
    private val pickerWidth = 300
    private val pickerHeight = 350
    private var pickerX = 0
    private var pickerY = 0
    private var hexInputText = ""
    private var hexInputFocused = false
    
    // Color picker state
    private var hue = 0f
    private var saturation = 1f
    private var brightness = 1f
    private var alpha = 1f
    
    // Scroll offset for color list
    private var scrollOffset = 0
    private val itemHeight = 35
    private val maxVisibleItems = 12
    
    enum class ColorType(val displayName: String, val currentColor: () -> Int, val setter: (Int) -> Unit) {
        CORE_BACKGROUND_PRIMARY("Primary Background", { CinnamonTheme.coreBackgroundPrimary }, { color ->
            CinnamonTheme.coreBackgroundPrimary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_BACKGROUND_SECONDARY("Secondary Background", { CinnamonTheme.coreBackgroundSecondary }, { color ->
            CinnamonTheme.coreBackgroundSecondary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_ACCENT_PRIMARY("Primary Accent", { CinnamonTheme.coreAccentPrimary }, { color ->
            CinnamonTheme.coreAccentPrimary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_ACCENT_SECONDARY("Secondary Accent/Button", { CinnamonTheme.coreAccentSecondary }, { color ->
            CinnamonTheme.coreAccentSecondary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_TEXT_PRIMARY("Primary Text", { CinnamonTheme.coreTextPrimary }, { color ->
            CinnamonTheme.coreTextPrimary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_TEXT_SECONDARY("Secondary Text", { CinnamonTheme.coreTextSecondary }, { color ->
            CinnamonTheme.coreTextSecondary = color
            CinnamonTheme.updateDependentColors()
        }),
        CORE_BORDER("Border Color", { CinnamonTheme.coreBorder }, { color ->
            CinnamonTheme.coreBorder = color
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
        // The companion object containing adjustBrightness has been removed as it's no longer needed.
    }
    
    override fun init() {
        super.init()
        
        // Calculate fixed picker position (centered on screen)
        pickerX = (width - pickerWidth) / 2
        pickerY = (height - pickerHeight) / 2
    }
    
    override fun initializeComponents() {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        val buttonY = contentY + getContentHeight() - 45 // Fixed button position
        
        // Clear existing buttons to prevent duplicates
        clearButtons()
        
        // Back button - fixed position
        addButton(CinnamonButton(
            guiX + PADDING,
            buttonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            { _, _ -> CinnamonGuiManager.openMainMenu() } // Changed to openMainMenu
        ))
        
        // Reset to defaults button - fixed position
        addButton(CinnamonButton(
            centerX - 50,
            buttonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Reset").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            { _, _ -> resetToDefaults() }
        ))
        
        // Save theme button - fixed position
        addButton(CinnamonButton(
            guiX + guiWidth - PADDING - 100,
            buttonY,
            100,
            CinnamonTheme.BUTTON_HEIGHT,
            Text.literal("Save").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            { _, _ -> saveTheme() },
            false // Changed from true to false
        ))
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Always render the base screen
        super.render(context, mouseX, mouseY, delta)
        
        // Render color picker on top if open
        if (showColorPicker) {
            renderColorPicker(context, mouseX, mouseY)
        }
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Don't render content if color picker is open to prevent overlap
        if (showColorPicker) {
            return
        }
        
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        
        // Removed title, subtitle, and glow effect rendering as the screen title is handled by CinnamonScreen.renderHeader
        
        // Render color list
        renderColorList(context, mouseX, mouseY, contentY) // Pass contentY
    }
    
    private fun renderColorList(context: DrawContext, mouseX: Int, mouseY: Int, contentYPos: Int) { // Added parameter
        val listX = guiX + 40
        val listY = contentYPos + 20 // Use parameter
        val listWidth = guiWidth - 80
        val listHeight = getContentHeight() - 170 // Leave space for buttons
        
        // Background for color list
        context.fill(listX, listY, listX + listWidth, listY + listHeight, CinnamonTheme.contentBackground)
        context.drawBorder(listX, listY, listWidth, listHeight, CinnamonTheme.borderColor)

        // Enable scissor for clipping items within the list area
        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight)
        
        // Render color items
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

        // Disable scissor after rendering items
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
        
        // Item background
        context.fill(x, y, x + width, y + height, backgroundColor)
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)
        
        // Color preview square
        val colorSquareSize = height - 10
        val currentColor = colorType.currentColor()
        context.fill(x + 10, y + 5, x + 10 + colorSquareSize, y + 5 + colorSquareSize, currentColor)
        context.drawBorder(x + 10, y + 5, colorSquareSize, colorSquareSize, 0xFFFFFFFF.toInt())
        
        // Color name
        context.drawText(
            textRenderer,
            Text.literal(colorType.displayName).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            x + 20 + colorSquareSize,
            y + (height - textRenderer.fontHeight) / 2,
            CinnamonTheme.primaryTextColor,
            false
        )
        
        // Color hex value
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
    
    private fun renderColorPicker(context: DrawContext, mouseX: Int, mouseY: Int) {
        // Define consistent Y positions for rendering
        val titleY_render = pickerY + 5 // Adjusted for tighter packing
        val wheelY_render = pickerY + 20
        val wheelSize = 180 // Unchanged
        val sliderHeight = 20 // Unchanged
        val brightnessSliderY_render = wheelY_render + wheelSize + 5 // pickerY + 20 + 180 + 5 = pickerY + 205
        val alphaSliderY_render = brightnessSliderY_render + sliderHeight + 5 // pickerY + 205 + 20 + 5 = pickerY + 230
        val buttonsY_render = pickerY + 290 // Target Y for buttons (used by click handler)
        val previewBoxHeight = 30 // Unchanged
        val previewBoxY_render = buttonsY_render - previewBoxHeight - 5 // pickerY + 290 - 30 - 5 = pickerY + 255

        // Full screen overlay to block interaction with background
        context.fill(0, 0, width, height, 0xC0000000.toInt())
        
        // Picker background
        val pickerBg = 0xF0202020.toInt()
        context.fill(pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, pickerBg)
        context.drawBorder(pickerX, pickerY, pickerWidth, pickerHeight, CinnamonTheme.accentColor)
        
        // Title
        val title = selectedColorType?.displayName ?: "Color Picker"
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(
            textRenderer,
            Text.literal(title).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            pickerX + (pickerWidth - titleWidth) / 2,
            titleY_render, // Use new title Y
            CinnamonTheme.primaryTextColor,
            true
        )
        
        // Color wheel area
        // val wheelSize = 180 // Defined above
        val wheelX = pickerX + (pickerWidth - wheelSize) / 2 // X position remains the same
        // val wheelY_render = pickerY + 20 // Defined above
        
        renderColorWheel(context, wheelX, wheelY_render, wheelSize)
        
        // Brightness slider
        // val sliderHeight = 20 // Defined above
        // val brightnessSliderY_render = wheelY_render + wheelSize + 5 // Defined above
        renderBrightnessSlider(context, pickerX + 20, brightnessSliderY_render, pickerWidth - 40, sliderHeight)
        
        // Alpha slider
        // val alphaSliderY_render = brightnessSliderY_render + sliderHeight + 5 // Defined above
        renderAlphaSlider(context, pickerX + 20, alphaSliderY_render, pickerWidth - 40, sliderHeight)
        
        // Preview and hex input
        // val previewBoxHeight = 30 // Defined above
        // val previewBoxY_render = buttonsY_render - previewBoxHeight - 5 // Defined above
        renderColorPreview(context, pickerX + 20, previewBoxY_render, pickerWidth - 40, previewBoxHeight)
        
        // Buttons
        // val buttonsY_render = pickerY + 290 // Defined above
        val buttonWidth = 80
        val buttonSpacing = 20
        val totalButtonWidth = buttonWidth * 2 + buttonSpacing
        val buttonStartX = pickerX + (pickerWidth - totalButtonWidth) / 2
        
        // Apply button
        val applyHovered = mouseX >= buttonStartX && mouseX < buttonStartX + buttonWidth && 
                          mouseY >= buttonsY_render && mouseY < buttonsY_render + 25
        context.fill(
            buttonStartX, buttonsY_render, buttonStartX + buttonWidth, buttonsY_render + 25,
            if (applyHovered) CinnamonTheme.accentColorHover else CinnamonTheme.accentColor
        )
        context.drawText(
            textRenderer,
            Text.literal("Apply").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            buttonStartX + (buttonWidth - textRenderer.getWidth("Apply")) / 2,
            buttonsY_render + 8,
            0xFFFFFFFF.toInt(),
            false
        )
        
        // Cancel button
        val cancelX = buttonStartX + buttonWidth + buttonSpacing
        val cancelHovered = mouseX >= cancelX && mouseX < cancelX + buttonWidth && 
                           mouseY >= buttonsY_render && mouseY < buttonsY_render + 25
        context.fill(
            cancelX, buttonsY_render, cancelX + buttonWidth, buttonsY_render + 25,
            if (cancelHovered) CinnamonTheme.buttonBackgroundHover else CinnamonTheme.buttonBackground
        )
        context.drawText(
            textRenderer,
            Text.literal("Cancel").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            cancelX + (buttonWidth - textRenderer.getWidth("Cancel")) / 2,
            buttonsY_render + 8,
            CinnamonTheme.primaryTextColor,
            false
        )
    }
    
    private fun renderColorWheel(context: DrawContext, x: Int, y: Int, size: Int) {
        val centerX = x + size / 2
        val centerY = y + size / 2
        val radius = size / 2 - 5
        
        // Draw filled color wheel using small rectangles for better color coverage
        for (r in 0 until radius) {
            for (angle in 0 until 360 step 2) {
                val rad = Math.toRadians(angle.toDouble())
                val innerR = r
                val outerR = r + 1
                
                // Draw multiple points to create filled effect
                for (subR in innerR until outerR) {
                    val px = centerX + (cos(rad) * subR).toInt()
                    val py = centerY + (sin(rad) * subR).toInt()
                    
                    val sat = subR.toFloat() / radius
                    val hueColor = hsvToRgb(angle.toFloat(), sat, brightness)
                    
                    // Draw 2x2 pixel blocks for better fill
                    context.fill(px, py, px + 2, py + 2, hueColor)
                }
            }
        }
        
        // Draw white center circle
        val centerSize = 20
        context.fill(
            centerX - centerSize/2, centerY - centerSize/2, 
            centerX + centerSize/2, centerY + centerSize/2, 
            hsvToRgb(hue, 0f, brightness)
        )
        
        // Draw selection indicator
        val selRadius = saturation * radius
        val selAngle = Math.toRadians(hue.toDouble())
        val selX = centerX + (cos(selAngle) * selRadius).toInt()
        val selY = centerY + (sin(selAngle) * selRadius).toInt()
        
        // Larger, more visible selection indicator
        context.fill(selX - 6, selY - 6, selX + 6, selY + 6, 0xFFFFFFFF.toInt())
        context.fill(selX - 4, selY - 4, selX + 4, selY + 4, 0xFF000000.toInt())
        context.fill(selX - 2, selY - 2, selX + 2, selY + 2, 0xFFFFFFFF.toInt())
    }
    
    private fun renderBrightnessSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Gradient background
        for (i in 0 until width) {
            val brightnessVal = i.toFloat() / width
            val color = hsvToRgb(hue, saturation, brightnessVal)
            context.fill(x + i, y, x + i + 1, y + height, color)
        }
        
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)
        
        // Slider handle
        val handleX = x + (brightness * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())
    }
    
    private fun renderAlphaSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Checkerboard background
        val checkerSize = 8
        for (i in 0 until width step checkerSize) {
            for (j in 0 until height step checkerSize) {
                val color = if ((i / checkerSize + j / checkerSize) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                val endX = minOf(x + i + checkerSize, x + width)
                val endY = minOf(y + j + checkerSize, y + height)
                context.fill(x + i, y + j, endX, endY, color)
            }
        }
        
        // Alpha gradient
        val baseColor = hsvToRgb(hue, saturation, brightness)
        for (i in 0 until width) {
            val alphaVal = i.toFloat() / width
            val color = (baseColor and 0x00FFFFFF) or ((alphaVal * 255).toInt() shl 24)
            context.fill(x + i, y, x + i + 1, y + height, color)
        }
        
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)
        
        // Slider handle
        val handleX = x + (alpha * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())
    }
    
    private fun renderColorPreview(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val previewWidth = width / 2 - 5
        val inputWidth = width / 2 - 5
        
        // Color preview (left half)
        val checkerSize = 8
        for (i in 0 until previewWidth step checkerSize) {
            for (j in 0 until height step checkerSize) {
                val color = if ((i / checkerSize + j / checkerSize) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                val endX = minOf(x + i + checkerSize, x + previewWidth)
                val endY = minOf(y + j + checkerSize, y + height)
                context.fill(x + i, y + j, endX, endY, color)
            }
        }
        
        val currentColor = hsvToRgb(hue, saturation, brightness)
        val finalColor = (currentColor and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
        
        context.fill(x, y, x + previewWidth, y + height, finalColor)
        context.drawBorder(x, y, previewWidth, height, CinnamonTheme.borderColor)
        
        // Hex input field (right half)
        val inputX = x + previewWidth + 10
        val inputBgColor = if (hexInputFocused) CinnamonTheme.contentBackground else CinnamonTheme.cardBackground
        val inputBorderColor = if (hexInputFocused) CinnamonTheme.accentColor else CinnamonTheme.borderColor
        
        context.fill(inputX, y, inputX + inputWidth, y + height, inputBgColor)
        context.drawBorder(inputX, y, inputWidth, height, inputBorderColor)
        
        // Display hex text
        val displayText = if (hexInputText.isNotEmpty()) hexInputText else String.format("%08X", finalColor)
        val textY = y + (height - textRenderer.fontHeight) / 2
        val textColor = if (hexInputText.isNotEmpty()) CinnamonTheme.primaryTextColor else CinnamonTheme.secondaryTextColor
        
        context.drawText(
            textRenderer, 
            Text.literal("#$displayText").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)), 
            inputX + 5, textY, textColor, false
        )
        
        // Cursor for focused input
        if (hexInputFocused) {
            val cursorX = inputX + 5 + textRenderer.getWidth("#$displayText")
            context.fill(cursorX, y + 2, cursorX + 1, y + height - 2, CinnamonTheme.primaryTextColor)
        }
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (showColorPicker) {
            // println("[ThemeManagerScreen] mouseClicked detected while showColorPicker is true.") // <--- REMOVE THIS LINE
            val mX = mouseX.toInt()
            val mY = mouseY.toInt()

            // Check if the click occurred within the bounds of the color picker
            if (mX >= pickerX && mX < pickerX + pickerWidth && mY >= pickerY && mY < pickerY + pickerHeight) {
                // Click is INSIDE the color picker's main rectangle.
                // Let handleColorPickerClick manage interaction with picker components.
                return handleColorPickerClick(mX, mY, button)
            } else {
                // Click is OUTSIDE the color picker's main rectangle, but the overlay is active.
                // This means the user clicked on the overlay part that is not the picker itself.
                // Consume the event and close the picker.
                showColorPicker = false
                return true 
            }
        }
        
        // Handle color list clicks
        val listOuterX = guiX + 40
        val listOuterY = getContentY() + 20 // Matches renderColorList's contentYPos + 20
        val listContentWidth = guiWidth - 80
        val listContentHeight = getContentHeight() - 170 // Matches renderColorList

        // Check if click is within the general list scrollable area first
        if (mouseX >= listOuterX && mouseX < listOuterX + listContentWidth && 
            mouseY >= listOuterY && mouseY < listOuterY + listContentHeight) {

            val colors = ColorType.values()
            // Calculate the base Y for items within the list (where items start rendering, considering scrolling)
            // renderColorList renders items starting at listOuterY + 10 - scrollOffset
            // mouse Y needs to be adjusted by this same offset to find the relative click position
            
            val relativeMouseY = mouseY - (listOuterY + 10 - scrollOffset)
            
            if (relativeMouseY >= 0) { // Ensure click is not in the padding area above items
                val clickedIndex = (relativeMouseY / itemHeight).toInt()

                if (clickedIndex >= 0 && clickedIndex < colors.size) {
                    val colorType = colors[clickedIndex]
                    
                    // Calculate the specific item's rendering coordinates
                    // renderColorItem is called with x = listOuterX + 10, y = (listOuterY + 10 - scrollOffset) + clickedIndex * itemHeight
                    val itemX = listOuterX + 10 // This is the 'x' passed to renderColorItem
                    val itemY = (listOuterY + 10 - scrollOffset) + clickedIndex * itemHeight // This is the 'y' passed to renderColorItem
                    
                    // colorSquareSize = (itemHeight - 5) - 10 = 30 - 10 = 20
                    val colorSquareSize = (itemHeight - 5) - 10 
                    // squareRenderX = itemX + 10
                    val squareRenderX = itemX + 10
                    // squareRenderY = itemY + 5
                    val squareRenderY = itemY + 5

                    // Temporarily comment out precise check for diagnostics
                    // if (mouseX >= squareRenderX && mouseX < squareRenderX + colorSquareSize &&
                    //     mouseY >= squareRenderY && mouseY < squareRenderY + colorSquareSize) {
                    //     openColorPicker(colorType)
                    //     return true
                    // }

                    // Replace with broader click check for the entire item row
                    val itemRenderWidth = listContentWidth - 20 // listContentWidth is guiWidth - 80, width in renderColorItem is listWidth - 20
                    val itemRenderHeight = itemHeight - 5       // height in renderColorItem is itemHeight - 5

                    if (mouseX >= itemX && mouseX < itemX + itemRenderWidth &&
                        mouseY >= itemY && mouseY < itemY + itemRenderHeight) {
                        openColorPicker(colorType)
                        return true
                    }
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button)
    }
    
    private fun handleColorPickerClick(mouseX: Int, mouseY: Int, button: Int): Boolean {
        // Handle button clicks
        val buttonY = pickerY + 290
        val buttonWidth = 80
        val buttonSpacing = 20
        val totalButtonWidth = buttonWidth * 2 + buttonSpacing
        val buttonStartX = pickerX + (pickerWidth - totalButtonWidth) / 2
        
        if (mouseY >= buttonY && mouseY < buttonY + 25) {
            if (mouseX >= buttonStartX && mouseX < buttonStartX + buttonWidth) {
                // Apply button
                applyColor()
                ThemeConfigManager.saveTheme()
                showColorPicker = false
                return true
            } else if (mouseX >= buttonStartX + buttonWidth + buttonSpacing && mouseX < buttonStartX + buttonWidth * 2 + buttonSpacing) {
                // Cancel button
                showColorPicker = false
                return true
            }
        }
        
        // Handle hex input field click
        val previewBoxY = buttonY - 35
        val previewBoxHeight = 30
        val inputX = pickerX + 20 + (pickerWidth - 40) / 2 + 10
        val inputWidth = (pickerWidth - 40) / 2 - 5
        
        if (mouseX >= inputX && mouseX < inputX + inputWidth && 
            mouseY >= previewBoxY && mouseY < previewBoxY + previewBoxHeight) {
            hexInputFocused = true
            return true
        } else {
            hexInputFocused = false
        }
        
        // Handle color wheel, brightness, and alpha slider clicks
        handleColorControlClicks(mouseX, mouseY)
        
        return true
    }
    
    private fun handleColorControlClicks(mouseX: Int, mouseY: Int) {
        val wheelSize = 180
        val wheelX = pickerX + (pickerWidth - wheelSize) / 2
        val wheelY_click = pickerY + 20 // Consistent with wheelY_render
        val centerX = wheelX + wheelSize / 2
        val centerY = wheelY_click + wheelSize / 2
        val radius = wheelSize / 2 - 5
        
        // Color wheel click
        if (mouseX >= wheelX && mouseX < wheelX + wheelSize && mouseY >= wheelY_click && mouseY < wheelY_click + wheelSize) {
            val dx = mouseX - centerX
            val dy = mouseY - centerY
            val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            
            if (distance <= radius) {
                hue = (atan2(dy.toDouble(), dx.toDouble()) * 180 / PI).toFloat()
                if (hue < 0) hue += 360
                saturation = minOf(1f, distance / radius)
            }
        }
        
        val sliderHeight = 20 // Consistent with rendering
        // Brightness slider click
        val brightnessSliderY_click = pickerY + 205 // Consistent with brightnessSliderY_render
        if (mouseX >= pickerX + 20 && mouseX < pickerX + pickerWidth - 20 && mouseY >= brightnessSliderY_click && mouseY < brightnessSliderY_click + sliderHeight) {
            brightness = ((mouseX - pickerX - 20).toFloat() / (pickerWidth - 40)).coerceIn(0f, 1f)
        }
        
        // Alpha slider click
        val alphaSliderY_click = pickerY + 230 // Consistent with alphaSliderY_render
        if (mouseX >= pickerX + 20 && mouseX < pickerX + pickerWidth - 20 && mouseY >= alphaSliderY_click && mouseY < alphaSliderY_click + sliderHeight) {
            alpha = ((mouseX - pickerX - 20).toFloat() / (pickerWidth - 40)).coerceIn(0f, 1f)
        }
    }
    
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (!showColorPicker) {
            val listY = getContentY() + 80
            val listHeight = getContentHeight() - 170
            
            if (mouseY >= listY && mouseY < listY + listHeight) {
                val maxScroll = maxOf(0, ColorType.values().size * itemHeight - listHeight)
                scrollOffset = (scrollOffset - verticalAmount.toInt() * 20).coerceIn(0, maxScroll)
                return true
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
    
    private fun openColorPicker(colorType: ColorType) {
        selectedColorType = colorType
        showColorPicker = true
        hexInputText = ""
        hexInputFocused = false
        
        // Initialize color picker with current color
        val currentColor = colorType.currentColor()
        val hsv = rgbToHsv(currentColor)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        alpha = ((currentColor ushr 24) and 0xFF) / 255f
    }
    
    private fun applyColor() {
        selectedColorType?.let { colorType ->
            val finalColor = hsvToRgb(hue, saturation, brightness)
            val colorWithAlpha = (finalColor and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
            
            // Logging before setter
            println("[ThemeManagerScreen] Applying color for: ${colorType.displayName}")
            println("[ThemeManagerScreen] HSVA values: H=${hue}, S=${saturation}, V=${brightness}, A=${alpha}")
            println("[ThemeManagerScreen] Calculated RGBA: ${String.format("#%08X", colorWithAlpha)}")
            
            colorType.setter(colorWithAlpha)
            
            // Logging after setter
            println("[ThemeManagerScreen] Setter called for ${colorType.displayName}. Current theme value: ${String.format("#%08X", colorType.currentColor())}")

            this.initializeComponents() // Refresh UI components
        }
    }

    private fun parseHexColor(hex: String): Boolean {
        try {
            val cleanHex = hex.replace("#", "").uppercase()
            if (cleanHex.length == 6 || cleanHex.length == 8) {
                val color = if (cleanHex.length == 6) {
                    // RGB format - add full alpha
                    0xFF000000.toInt() or cleanHex.toLong(16).toInt()
                } else {
                    // RGBA format
                    cleanHex.toLong(16).toInt()
                }
                
                val hsv = rgbToHsv(color)
                hue = hsv[0]
                saturation = hsv[1]
                brightness = hsv[2]
                alpha = if (cleanHex.length == 8) {
                    ((color ushr 24) and 0xFF) / 255f
                } else {
                    1f
                }
                return true
            }
        } catch (e: NumberFormatException) {
            // Invalid hex format
        }
        return false
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (showColorPicker && hexInputFocused) {
            when (keyCode) {
                257 -> { // Enter key
                    if (parseHexColor(hexInputText)) {
                        hexInputText = ""
                    }
                    return true
                }
                259 -> { // Backspace
                    if (hexInputText.isNotEmpty()) {
                        hexInputText = hexInputText.dropLast(1)
                    }
                    return true
                }
                256 -> { // Escape
                    hexInputFocused = false
                    hexInputText = ""
                    return true
                }
            }
            return true
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(chr: Char, modifiers: Int): Boolean {
        if (showColorPicker && hexInputFocused) {
            if (chr.isLetterOrDigit() && hexInputText.length < 8) {
                val upperChar = chr.uppercaseChar()
                if (upperChar.isDigit() || upperChar in 'A'..'F') {
                    hexInputText += upperChar
                }
            }
            return true
        }
        return super.charTyped(chr, modifiers)
    }
    
    private fun resetToDefaults() {
        // Reset all colors to their default values
        CinnamonTheme.resetToDefaults()
    }
    
    private fun saveTheme() {
        // Save the current theme configuration to file
        ThemeConfigManager.saveTheme()
        
        // Optional: Add some visual feedback
        // You could show a toast or temporary status message here
    }
    
    // Color conversion utilities
    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - abs(((h / 60) % 2) - 1))
        val m = v - c
        
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        
        val red = ((r + m) * 255).toInt().coerceIn(0, 255)
        val green = ((g + m) * 255).toInt().coerceIn(0, 255)
        val blue = ((b + m) * 255).toInt().coerceIn(0, 255)
        
        return (255 shl 24) or (red shl 16) or (green shl 8) or blue
    }
    
    private fun rgbToHsv(color: Int): FloatArray {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        
        val h = when {
            delta == 0f -> 0f
            max == r -> 60 * (((g - b) / delta) % 6)
            max == g -> 60 * (((b - r) / delta) + 2)
            else -> 60 * (((r - g) / delta) + 4)
        }
        
        val s = if (max == 0f) 0f else delta / max
        val v = max
        
        return floatArrayOf(if (h < 0) h + 360 else h, s, v)
    }
    
    override fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Don't render footer if color picker is open
        if (showColorPicker) {
            return
        }
        
        super.renderFooter(context, mouseX, mouseY, delta)
        
        // Draw theme status
        val statusText = Text.literal("Theme Editor").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
        context.drawText(
            textRenderer,
            statusText,
            guiX + PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.infoColor,
            false
        )
        
        // Draw color count
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
    
    // Helper function to clear buttons (you may need to implement this in your base class)
    private fun clearButtons() {
        // This would clear the button list to prevent duplicates
        // Implementation depends on your CinnamonScreen base class
    }
}