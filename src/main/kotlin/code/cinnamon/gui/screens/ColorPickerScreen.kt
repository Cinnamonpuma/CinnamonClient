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

    private val pickerContentWidth = 300
    private val pickerContentHeight = 320

    private var hue = 0f
    private var saturation = 1f
    private var brightness = 1f
    private var alpha = 1f

    override fun init() {
        this.guiWidth = pickerContentWidth
        this.guiHeight = pickerContentHeight

        super.init()
        val hsv = rgbToHsv(initialColor)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        alpha = ((initialColor ushr 24) and 0xFF) / 255f
    }

    override fun initializeComponents() {
    }

    override fun renderHeader(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float, alpha: Float) {}

    override fun renderFooter(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float, alpha: Float) {}

    override fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val titleText = Text.literal("Select Color").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val titleWidth = textRenderer.getWidth(titleText)
        context.drawText(
            textRenderer,
            titleText,
            this.guiX + (this.guiWidth - titleWidth) / 2,
            this.guiY + PADDING,
            CinnamonTheme.titleColor,
            CinnamonTheme.enableTextShadow
        )

        val sliderWidth = this.guiWidth - (PADDING * 2)
        val sliderHeight = 20
        val sliderStartX = this.guiX + PADDING
        var currentContentY = PADDING + textRenderer.fontHeight + PADDING

        renderHueSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8

        renderSaturationSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8

        renderBrightnessSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8

        renderAlphaSlider(context, sliderStartX, this.guiY + currentContentY, sliderWidth, sliderHeight)
        currentContentY += sliderHeight + PADDING + 8

        val previewHeight = 40
        renderColorPreview(context, sliderStartX, this.guiY + currentContentY, sliderWidth, previewHeight)
        currentContentY += previewHeight + PADDING

        val buttonDrawWidth = 80
        val buttonDrawHeight = 25
        val buttonSpacing = 10
        val totalButtonWidth = buttonDrawWidth * 2 + buttonSpacing
        val buttonsY = this.guiY + this.guiHeight - PADDING - buttonDrawHeight
        val buttonsStartX = this.guiX + (this.guiWidth - totalButtonWidth) / 2

        val applyButtonX = buttonsStartX
        val applyHovered = scaledMouseX >= applyButtonX && scaledMouseX < applyButtonX + buttonDrawWidth &&
                scaledMouseY >= buttonsY && scaledMouseY < buttonsY + buttonDrawHeight
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
        val cancelHovered = scaledMouseX >= cancelButtonX && scaledMouseX < cancelButtonX + buttonDrawWidth &&
                scaledMouseY >= buttonsY && scaledMouseY < buttonsY + buttonDrawHeight
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
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val hueValue = (i.toFloat() / width) * 360f
            val color = hsvToRgb(hueValue, 1f, 1f)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        val handleX = x + (hue / 360f * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        context.drawText(textRenderer, Text.literal("Hue"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderSaturationSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val satValue = i.toFloat() / width
            val color = hsvToRgb(hue, satValue, brightness)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        val handleX = x + (saturation * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        context.drawText(textRenderer, Text.literal("Saturation"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderBrightnessSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val brightValue = i.toFloat() / width
            val color = hsvToRgb(hue, saturation, brightValue)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        val handleX = x + (brightness * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        context.drawText(textRenderer, Text.literal("Brightness"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderAlphaSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val checkerSize = 8
        for (i in 0 until width step checkerSize) {
            for (j in 0 until height step checkerSize) {
                val color = if ((i / checkerSize + j / checkerSize) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                val endX = minOf(x + i + checkerSize, x + width)
                val endY = minOf(y + j + checkerSize, y + height)
                context.fill(x + i, y + j, endX, endY, color)
            }
        }

        val baseColor = hsvToRgb(hue, saturation, brightness)
        val stripeWidth = 4
        for (i in 0 until width step stripeWidth) {
            val alphaValue = i.toFloat() / width
            val color = (baseColor and 0x00FFFFFF) or ((alphaValue * 255).toInt() shl 24)
            val endX = minOf(x + i + stripeWidth, x + width)
            context.fill(x + i, y, endX, y + height, color)
        }

        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        val handleX = x + (alpha * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())

        context.drawText(textRenderer, Text.literal("Alpha"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    private fun renderColorPreview(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val checkerSize = 16
        for (i in 0 until width step checkerSize) {
            for (j in 0 until height step checkerSize) {
                val color = if ((i / checkerSize + j / checkerSize) % 2 == 0) 0xFFCCCCCC.toInt() else 0xFF999999.toInt()
                val endX = minOf(x + i + checkerSize, x + width)
                val endY = minOf(y + j + checkerSize, y + height)
                context.fill(x + i, y + j, endX, endY, color)
            }
        }

        val currentColor = hsvToRgb(hue, saturation, brightness)
        val finalColor = (currentColor and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
        context.fill(x, y, x + width, y + height, finalColor)
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)

        context.drawText(textRenderer, Text.literal("Preview"), x, y - 12, CinnamonTheme.primaryTextColor, false)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mX = scaleMouseX(mouseX).toInt()
        val mY = scaleMouseY(mouseY).toInt()

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
                close()
                return true
            }
            val cancelButtonX = applyButtonX + buttonDrawWidth + buttonSpacing
            if (mX >= cancelButtonX && mX < cancelButtonX + buttonDrawWidth) {
                onCancel()
                close()
                return true
            }
        }

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

        return super.mouseClicked(mouseX, mouseY, button)
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
