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
    private val pickerHeight = 370
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
            Text.literal(title).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            pickerX + (pickerWidth - titleWidth) / 2,
            pickerY + 8,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow 
        )

        val wheelSize = 180
        val wheelX = pickerX + (pickerWidth - wheelSize) / 2
        val wheelY = pickerY + 35
        renderColorWheel(context, wheelX, wheelY, wheelSize)

        val sliderHeight = 20
        val brightnessSliderY = wheelY + wheelSize + 16
        renderBrightnessSlider(context, pickerX + 20, brightnessSliderY, pickerWidth - 40, sliderHeight)

        val alphaSliderY = brightnessSliderY + sliderHeight + 12
        renderAlphaSlider(context, pickerX + 20, alphaSliderY, pickerWidth - 40, sliderHeight)

        val previewBoxY = alphaSliderY + sliderHeight + 18
        renderColorPreview(context, pickerX + 20, previewBoxY, pickerWidth - 40, 32)

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
            Text.literal("Apply").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
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
            Text.literal("Cancel").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            cancelX + (buttonWidth - textRenderer.getWidth("Cancel")) / 2,
            buttonY + 10,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow 
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        renderContent(context, mouseX, mouseY, delta)
    }

    private fun renderColorWheel(context: DrawContext, x: Int, y: Int, size: Int) {
        val centerX = x + size / 2
        val centerY = y + size / 2
        val radius = size / 2 - 1

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val dist = sqrt((dx * dx + dy * dy).toDouble())
                if (dist <= radius) {
                    val angle = atan2(dy.toDouble(), dx.toDouble())
                    val hue = ((Math.toDegrees(angle) + 360) % 360).toFloat()
                    val sat = (dist / radius).toFloat()
                    val color = hsvToRgb(hue, sat, brightness)
                    context.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, color)
                }
            }
        }

        val selRadius = saturation * radius
        val selAngle = Math.toRadians(hue.toDouble())
        val selX = centerX + (cos(selAngle) * selRadius).toInt()
        val selY = centerY + (sin(selAngle) * selRadius).toInt()
        context.fill(selX - 6, selY - 6, selX + 6, selY + 6, 0xFFFFFFFF.toInt())
        context.fill(selX - 4, selY - 4, selX + 4, selY + 4, 0xFF000000.toInt())
        context.fill(selX - 2, selY - 2, selX + 2, selY + 2, 0xFFFFFFFF.toInt())
    }

    private fun renderBrightnessSlider(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        for (i in 0 until width) {
            val b = i.toFloat() / width
            val color = hsvToRgb(hue, saturation, b)
            context.fill(x + i, y, x + i + 1, y + height, color)
        }
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)
        val handleX = x + (brightness * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())
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
        for (i in 0 until width) {
            val alphaVal = i.toFloat() / width
            val color = (baseColor and 0x00FFFFFF) or ((alphaVal * 255).toInt() shl 24)
            context.fill(x + i, y, x + i + 1, y + height, color)
        }
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)
        val handleX = x + (alpha * width).toInt() - 2
        context.fill(handleX, y - 2, handleX + 4, y + height + 2, 0xFFFFFFFF.toInt())
        context.drawBorder(handleX - 1, y - 3, 6, height + 4, 0xFF000000.toInt())
    }

    private fun renderColorPreview(context: DrawContext, x: Int, y: Int, width: Int, height: Int) {
        val checkerSize = 8
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
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val mX = mouseX.toInt()
        val mY = mouseY.toInt()

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

        val wheelSize = 180
        val wheelX = pickerX + (pickerWidth - wheelSize) / 2
        val wheelY = pickerY + 35
        val centerX = wheelX + wheelSize / 2
        val centerY = wheelY + wheelSize / 2
        val radius = wheelSize / 2 - 1

        if (mX in wheelX until (wheelX + wheelSize) && mY in wheelY until (wheelY + wheelSize)) {
            val dx = mX - centerX
            val dy = mY - centerY
            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            if (dist <= radius) {
                hue = (atan2(dy.toDouble(), dx.toDouble()) * 180 / PI).toFloat()
                if (hue < 0) hue += 360
                saturation = minOf(1f, dist / radius)
                return true
            }
        }

        val sliderHeight = 20
        val brightnessSliderY = wheelY + wheelSize + 16
        if (mX in (pickerX + 20) until (pickerX + pickerWidth - 20) &&
            mY in brightnessSliderY until (brightnessSliderY + sliderHeight)) {
            brightness = ((mX - pickerX - 20).toFloat() / (pickerWidth - 40)).coerceIn(0f, 1f)
            return true
        }


        val alphaSliderY = brightnessSliderY + sliderHeight + 12
        if (mX in (pickerX + 20) until (pickerX + pickerWidth - 20) &&
            mY in alphaSliderY until (alphaSliderY + sliderHeight)) {
            alpha = ((mX - pickerX - 20).toFloat() / (pickerWidth - 40)).coerceIn(0f, 1f)
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