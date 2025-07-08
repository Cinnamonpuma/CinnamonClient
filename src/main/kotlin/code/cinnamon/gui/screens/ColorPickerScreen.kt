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

    private val pickerWidth = 300
    private val pickerHeight = 320
    private var pickerX = 0
    private var pickerY = 0

    private var hue = 0f
    private var saturation = 1f
    private var brightness = 1f
    private var alpha = 1f

    override fun init() {
        super.init()
        pickerX = (width - pickerWidth) / 2
        pickerY = (height - pickerHeight) / 2

        val hsv = rgbToHsv(initialColor)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        alpha = ((initialColor ushr 24) and 0xFF) / 255f
    }

    override fun initializeComponents() {

    }

    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fill(0, 0, width, height, 0xC0000000.toInt())
        context.fill(pickerX, pickerY, pickerX + pickerWidth, pickerY + pickerHeight, 0xF0202020.toInt())
        context.drawBorder(pickerX, pickerY, pickerWidth, pickerHeight, CinnamonTheme.accentColor)

        val title = "Color Picker"
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(
            textRenderer,
            Text.literal(title).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            pickerX + (pickerWidth - titleWidth) / 2,
            pickerY + 8,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow 
        )

        val sliderWidth = pickerWidth - 40
        val sliderHeight = 20
        val sliderX = pickerX + 20
        var currentY = pickerY + 35

        // Hue slider (rainbow gradient)
        renderHueSlider(context, sliderX, currentY, sliderWidth, sliderHeight)
        currentY += sliderHeight + 16

        // Saturation slider (white to current hue)
        renderSaturationSlider(context, sliderX, currentY, sliderWidth, sliderHeight)
        currentY += sliderHeight + 16

        // Brightness slider (black to current color)
        renderBrightnessSlider(context, sliderX, currentY, sliderWidth, sliderHeight)
        currentY += sliderHeight + 16

        // Alpha slider (transparent to opaque)
        renderAlphaSlider(context, sliderX, currentY, sliderWidth, sliderHeight)
        currentY += sliderHeight + 20

        // Color preview
        renderColorPreview(context, sliderX, currentY, sliderWidth, 40)
        currentY += 50

        // Buttons
        val buttonWidth = 80
        val buttonSpacing = 24
        val totalButtonWidth = buttonWidth * 2 + buttonSpacing
        val buttonY = pickerY + pickerHeight - 32
        val buttonStartX = pickerX + (pickerWidth - totalButtonWidth) / 2

        val applyHovered = mouseX in buttonStartX until (buttonStartX + buttonWidth) &&
            mouseY in buttonY until (buttonY + 30)
        context.fill(
            buttonStartX, buttonY, buttonStartX + buttonWidth, buttonY + 30,
            if (applyHovered) CinnamonTheme.accentColorHover else CinnamonTheme.accentColor
        )
        context.drawText(
            textRenderer,
            Text.literal("Apply").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            buttonStartX + (buttonWidth - textRenderer.getWidth("Apply")) / 2,
            buttonY + 10,
            0xFFFFFFFF.toInt(),
            CinnamonTheme.enableTextShadow 
        )

        val cancelX = buttonStartX + buttonWidth + buttonSpacing
        val cancelHovered = mouseX in cancelX until (cancelX + buttonWidth) &&
            mouseY in buttonY until (buttonY + 30)
        context.fill(
            cancelX, buttonY, cancelX + buttonWidth, buttonY + 30,
            if (cancelHovered) CinnamonTheme.buttonBackgroundHover else CinnamonTheme.buttonBackground
        )
        context.drawText(
            textRenderer,
            Text.literal("Cancel").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            cancelX + (buttonWidth - textRenderer.getWidth("Cancel")) / 2,
            buttonY + 10,
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

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        renderContent(context, mouseX, mouseY, delta)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mX = mouseX.toInt()
        val mY = mouseY.toInt()

        // Button handling
        val buttonWidth = 80
        val buttonSpacing = 24
        val totalButtonWidth = buttonWidth * 2 + buttonSpacing
        val buttonY = pickerY + pickerHeight - 32
        val buttonStartX = pickerX + (pickerWidth - totalButtonWidth) / 2

        if (mY in buttonY until (buttonY + 30)) {
            if (mX in buttonStartX until (buttonStartX + buttonWidth)) {
                val color = (hsvToRgb(hue, saturation, brightness) and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
                onPick(color)
                return true
            }
            if (mX in (buttonStartX + buttonWidth + buttonSpacing) until (buttonStartX + buttonWidth * 2 + buttonSpacing)) {
                onCancel()
                return true
            }
        }

        // Slider handling
        val sliderWidth = pickerWidth - 40
        val sliderHeight = 20
        val sliderX = pickerX + 20
        var currentY = pickerY + 35

        // Hue slider
        if (mX in sliderX until (sliderX + sliderWidth) && mY in currentY until (currentY + sliderHeight)) {
            hue = ((mX - sliderX).toFloat() / sliderWidth * 360f).coerceIn(0f, 360f)
            return true
        }
        currentY += sliderHeight + 16

        // Saturation slider
        if (mX in sliderX until (sliderX + sliderWidth) && mY in currentY until (currentY + sliderHeight)) {
            saturation = ((mX - sliderX).toFloat() / sliderWidth).coerceIn(0f, 1f)
            return true
        }
        currentY += sliderHeight + 16

        // Brightness slider
        if (mX in sliderX until (sliderX + sliderWidth) && mY in currentY until (currentY + sliderHeight)) {
            brightness = ((mX - sliderX).toFloat() / sliderWidth).coerceIn(0f, 1f)
            return true
        }
        currentY += sliderHeight + 16

        // Alpha slider
        if (mX in sliderX until (sliderX + sliderWidth) && mY in currentY until (currentY + sliderHeight)) {
            alpha = ((mX - sliderX).toFloat() / sliderWidth).coerceIn(0f, 1f)
            return true
        }

        return true
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