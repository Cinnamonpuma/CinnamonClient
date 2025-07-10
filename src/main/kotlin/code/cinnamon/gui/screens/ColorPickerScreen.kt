package code.cinnamon.gui.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style
import code.cinnamon.gui.CinnamonScreen
import kotlin.math.*
import code.cinnamon.gui.theme.CinnamonTheme

class ColorPickerScreen(
    private val initialColor: Int,
    private val onPick: (Int) -> Unit,
    private val onCancel: () -> Unit = {}
) : CinnamonScreen(Text.literal("Color Picker").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {

    // These define the size of the color picker content area, in scaled units
    private val pickerContentWidth = 300
    private val pickerContentHeight = 320

    private var hue = 0f
    private var saturation = 1f
    private var brightness = 1f
    private var alpha = 1f

    override fun init() {
        // Set the desired overall GUI size for CinnamonScreen to use
        this.guiWidth = pickerContentWidth
        this.guiHeight = pickerContentHeight

        super.init() // This will call calculateGuiDimensions() which now respects our guiWidth/Height preferences
        // and centers it on the effective (scaled) screen.

        val hsv = rgbToHsv(initialColor)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        alpha = ((initialColor ushr 24) and 0xFF) / 255f
    }

    override fun initializeComponents() {
        // No CinnamonButtons are used in this screen, components are custom drawn.
    }

    // We don't want the standard header from CinnamonScreen for this custom dialog UI.
    override fun renderHeader(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {}

    // We don't want the standard footer from CinnamonScreen for this custom dialog UI.
    override fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {}

    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // The CinnamonScreen.render() method has already set up the scaled matrix.
        // It has also called renderBlurredBackground().
        // It has also drawn the main GUI box based on this.guiX, this.guiY, this.guiWidth, this.guiHeight.
        // So, coordinates here are relative to the scaled screen, and (0,0) for content is effectively this.guiX, this.guiY.
        // However, to make positioning clear, we'll use this.guiX and this.guiY as the origin for drawing.

        // Background for the picker area (already drawn by CinnamonScreen's renderGuiBox using theme.coreBackgroundPrimary)
        // If a different background is needed for the content area itself:
        // context.fill(this.guiX, this.guiY, this.guiX + this.guiWidth, this.guiY + this.guiHeight, 0xF0202020.toInt())
        // context.drawBorder(this.guiX, this.guiY, this.guiWidth, this.guiHeight, CinnamonTheme.accentColor)

        val titleText = Text.literal("Select Color").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val titleWidth = textRenderer.getWidth(titleText)
        context.drawText(
            textRenderer,
            titleText,
            this.guiX + (this.guiWidth - titleWidth) / 2,
            this.guiY + PADDING, // Use PADDING from CinnamonScreen for consistency
            CinnamonTheme.titleColor, // Use theme's title color
            CinnamonTheme.enableTextShadow
        )

        val sliderWidth = this.guiWidth - (PADDING * 2) // Sliders span content width with padding
        val sliderHeight = 20
        val sliderStartX = this.guiX + PADDING
        var currentContentY = PADDING + textRenderer.fontHeight + PADDING // Start Y below title

        // Hue slider
        renderHueSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8 // Extra space for label

        // Saturation slider
        renderSaturationSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8

        // Brightness slider
        renderBrightnessSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8

        // Alpha slider
        renderAlphaSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8

        // Color preview
        val previewHeight = 40
        renderColorPreview(context, sliderStartX, this.guiY + currentContentY, sliderWidth, previewHeight)
        currentContentY += previewHeight + PADDING

        // Buttons
        val buttonDrawWidth = 80
        val buttonDrawHeight = 25 // Consistent button height
        val buttonSpacing = 10
        val totalButtonWidth = buttonDrawWidth * 2 + buttonSpacing
        val buttonsY = this.guiY + this.guiHeight - PADDING - buttonDrawHeight
        val buttonsStartX = this.guiX + (this.guiWidth - totalButtonWidth) / 2

        val applyButtonX = buttonsStartX
        val applyHovered = mouseX >= applyButtonX && mouseX < applyButtonX + buttonDrawWidth &&
                mouseY >= buttonsY && mouseY < buttonsY + buttonDrawHeight
        context.fill(
            applyButtonX, buttonsY, applyButtonX + buttonDrawWidth, buttonsY + buttonDrawHeight,
            if (applyHovered) CinnamonTheme.accentColorHover else CinnamonTheme.accentColor
        )
        val applyText = Text.literal("Apply").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        context.drawText(
            textRenderer,
            applyText,
            applyButtonX + (buttonDrawWidth - textRenderer.getWidth(applyText)) / 2,
            buttonsY + (buttonDrawHeight - textRenderer.fontHeight) / 2,
            0xFFFFFFFF.toInt(),
            CinnamonTheme.enableTextShadow
        )

        val cancelButtonX = applyButtonX + buttonDrawWidth + buttonSpacing
        val cancelHovered = mouseX >= cancelButtonX && mouseX < cancelButtonX + buttonDrawWidth &&
                mouseY >= buttonsY && mouseY < buttonsY + buttonDrawHeight
        context.fill(
            cancelButtonX, buttonsY, cancelButtonX + buttonDrawWidth, buttonsY + buttonDrawHeight,
            if (cancelHovered) CinnamonTheme.buttonBackgroundHover else CinnamonTheme.buttonBackground
        )
        val cancelText = Text.literal("Cancel").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        context.drawText(
            textRenderer,
            cancelText,
            cancelButtonX + (buttonDrawWidth - textRenderer.getWidth(cancelText)) / 2,
            buttonsY + (buttonDrawHeight - textRenderer.fontHeight) / 2,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )
    }

    private fun renderHueSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Draw hue gradient using vertical stripes (much more efficient)
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val hueValue = (i.toFloat() / width) * 360f
            val color = hsvToRgb(hueValue, 1f, 1f)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        // Draw handle
        val handleX = x + (hue / 360f * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        // Label
        context.drawText(textRenderer, Text.literal("Hue"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderSaturationSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Draw saturation gradient using vertical stripes
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val satValue = i.toFloat() / width
            val color = hsvToRgb(hue, satValue, brightness)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        // Draw handle
        val handleX = x + (saturation * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        // Label
        context.drawText(textRenderer, Text.literal("Saturation"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderBrightnessSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Draw brightness gradient using vertical stripes
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val brightValue = i.toFloat() / width
            val color = hsvToRgb(hue, saturation, brightValue)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        // Draw handle
        val handleX = x + (brightness * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        // Label
        context.drawText(textRenderer, Text.literal("Brightness"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderAlphaSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Draw checkerboard background
        val checkerSize = 8
        for (i in 0 until width step checkerSize) {
            for (j in 0 until height step checkerSize) {
                val color = if ((i / checkerSize + j / checkerSize) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                val endX = minOf(x + i + checkerSize, x + width)
                val endY = minOf(y + j + checkerSize, y + height)
                context.fill(x + i, y + j, endX, endY, color)
            }
        }

        // Draw alpha gradient using vertical stripes
        val baseColor = hsvToRgb(hue, saturation, brightness)
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val alphaValue = i.toFloat() / width
            val color = (baseColor and 0x00FFFFFF) or ((alphaValue * 255).toInt() shl 24)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        // Draw handle
        val handleX = x + (alpha * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        // Label
        context.drawText(textRenderer, Text.literal("Alpha"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderColorPreview(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        // Draw checkerboard background
        val checkerSize = 16
        for (i in 0 until width step checkerSize) {
            for (j in 0 until height step checkerSize) {
                val color = if ((i / checkerSize + j / checkerSize) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                val endX = minOf(x + i + checkerSize, x + width)
                val endY = minOf(y + j + checkerSize, y + height)
                context.fill(x + i, y + j, endX, endY, color)
            }
        }

        // Draw current color
        val currentColor = hsvToRgb(hue, saturation, brightness)
        val finalColor = (currentColor and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
        context.fill(x, y, x + width, y + height, finalColor)
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        // Label
        context.drawText(textRenderer, Text.literal("Preview"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    // Removed the override fun render(...) as CinnamonScreen.render() handles the main render loop and calls renderContent.

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // mouseX and mouseY are already scaled by CinnamonScreen.
        val mX = mouseX.toInt()
        val mY = mouseY.toInt()

        // Button handling - coordinates must be relative to the overall scaled screen, like in renderContent
        val buttonDrawWidth = 80
        val buttonDrawHeight = 25
        val buttonSpacing = 10
        val totalButtonWidth = buttonDrawWidth * 2 + buttonSpacing
        val buttonsY = this.guiY + this.guiHeight - PADDING - buttonDrawHeight
        val buttonsStartX = this.guiX + (this.guiWidth - totalButtonWidth) / 2

        val applyButtonX = buttonsStartX
        if (mY >= buttonsY && mY < buttonsY + buttonDrawHeight) {
            if (mX >= applyButtonX && mX < applyButtonX + buttonDrawWidth) {
                val color = (hsvToRgb(hue, saturation, brightness) and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
                onPick(color)
                close() // Close the color picker screen
                return true
            }
            val cancelButtonX = applyButtonX + buttonDrawWidth + buttonSpacing
            if (mX >= cancelButtonX && mX < cancelButtonX + buttonDrawWidth) {
                onCancel()
                close() // Close the color picker screen
                return true
            }
        }

        // Slider handling - coordinates must be relative to the overall scaled screen
        val sliderWidth = this.guiWidth - (PADDING * 2)
        val sliderHeight = 20
        val sliderStartX = this.guiX + PADDING
        var currentContentY = PADDING + textRenderer.fontHeight + PADDING

        val hueSliderY = this.guiY + currentContentY
        if (mX >= sliderStartX && mX < sliderStartX + sliderWidth && mY >= hueSliderY && mY < hueSliderY + sliderHeight) {
            hue = ((mX - sliderStartX).toFloat() / sliderWidth * 360f).coerceIn(0f, 360f)
            return true
        }
        currentContentY += sliderHeight + PADDING + 8

        val satSliderY = this.guiY + currentContentY
        if (mX >= sliderStartX && mX < sliderStartX + sliderWidth && mY >= satSliderY && mY < satSliderY + sliderHeight) {
            saturation = ((mX - sliderStartX).toFloat() / sliderWidth).coerceIn(0f, 1f)
            return true
        }
        currentContentY += sliderHeight + PADDING + 8

        val brightSliderY = this.guiY + currentContentY
        if (mX >= sliderStartX && mX < sliderStartX + sliderWidth && mY >= brightSliderY && mY < brightSliderY + sliderHeight) {
            brightness = ((mX - sliderStartX).toFloat() / sliderWidth).coerceIn(0f, 1f)
            return true
        }
        currentContentY += sliderHeight + PADDING + 8

        val alphaSliderY = this.guiY + currentContentY
        if (mX >= sliderStartX && mX < sliderStartX + sliderWidth && mY >= alphaSliderY && mY < alphaSliderY + sliderHeight) {
            alpha = ((mX - sliderStartX).toFloat() / sliderWidth).coerceIn(0f, 1f)
            return true
        }

        // If click is outside the main gui picker area, but still within CinnamonScreen's general interaction area,
        // allow CinnamonScreen to handle it (e.g. its own close button if header was visible).
        // However, since we are overriding header/footer to be empty, this picker is modal.
        // A click outside the picker should ideally close it if it's a modal dialog.
        // The current CinnamonScreen.mouseClicked already closes the screen if the click is outside guiX/guiY/guiWidth/guiHeight.
        // So, this behavior is inherited. We just need to ensure our interactions return true if handled.

        return super.mouseClicked(mouseX, mouseY, button) // Let CinnamonScreen handle if not interacting with picker elements.
    }

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
}