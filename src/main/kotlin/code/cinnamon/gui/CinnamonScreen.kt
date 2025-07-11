package code.cinnamon.gui

import net.minecraft.text.Style
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import net.minecraft.util.Identifier
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.RenderPipelines
import code.cinnamon.gui.AnimatedScreenTransition
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

abstract class CinnamonScreen(title: Text) : Screen(title) {
    protected val theme = CinnamonTheme
    protected val buttons = mutableListOf<CinnamonButton>()

    protected var guiWidth = 600
    protected var guiHeight = 450
    protected var guiX = 0
    protected var guiY = 0

    companion object {
        val CINNA_FONT: Identifier = Identifier.of("cinnamon", "cinna")
        const val HEADER_HEIGHT = 50
        const val FOOTER_HEIGHT = 35
        const val SIDEBAR_WIDTH = 180
        const val PADDING = 15
        const val CORNER_RADIUS = 8
        const val SHADOW_SIZE = 4
        private val LOGO_TEXTURE = Identifier.of("cinnamon", "textures/gui/logo.png")

        const val MIN_GUI_WIDTH = 400
        const val MAX_GUI_WIDTH = 800
        const val MIN_GUI_HEIGHT = 300
        const val MAX_GUI_HEIGHT = 600

        const val TARGET_SCALE_FACTOR = 2f
    }

    override fun init() {
        super.init()
        buttons.clear()
        calculateGuiDimensions()
        initializeComponents()
    }

    protected open fun getDesiredGuiWidth(effectiveScaledWidth: Int): Int {
        return max(MIN_GUI_WIDTH, min(MAX_GUI_WIDTH, (effectiveScaledWidth * 0.7f).toInt()))
    }

    protected open fun getDesiredGuiHeight(effectiveScaledHeight: Int): Int {
        return max(MIN_GUI_HEIGHT, min(MAX_GUI_HEIGHT, (effectiveScaledHeight * 0.8f).toInt()))
    }

    private fun calculateGuiDimensions() {
        val scaledWidth = getEffectiveWidth()
        val scaledHeight = getEffectiveHeight()

        guiWidth = getDesiredGuiWidth(scaledWidth)
        guiHeight = getDesiredGuiHeight(scaledHeight)

        guiX = (scaledWidth - guiWidth) / 2
        guiY = (scaledHeight - guiHeight) / 2
    }

    internal fun getEffectiveWidth(): Int {
        val currentScale = client!!.window.scaleFactor.toFloat()
        return (width * currentScale / TARGET_SCALE_FACTOR).toInt()
    }

    private fun getEffectiveHeight(): Int {
        val currentScale = client!!.window.scaleFactor.toFloat()
        return (height * currentScale / TARGET_SCALE_FACTOR).toInt()
    }

    protected fun getScaleRatio(): Float {
        val currentScale = client!!.window.scaleFactor.toFloat()
        return TARGET_SCALE_FACTOR / currentScale
    }

    protected fun scaleMouseX(mouseX: Double): Double = mouseX / getScaleRatio()
    protected fun scaleMouseY(mouseY: Double): Double = mouseY / getScaleRatio()

    abstract fun initializeComponents()
    protected abstract fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float)

    protected open val shouldRenderDefaultGuiBox: Boolean = true

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        val scaleRatio = getScaleRatio()
        val scaledWidth = getEffectiveWidth()
        val scaledHeight = getEffectiveHeight()
        val alpha = AnimatedScreenTransition.getAlpha()

        context.matrices.pushMatrix()
        context.matrices.scale(scaleRatio, scaleRatio, context.matrices)

        renderBlurredBackground(context, scaledWidth, scaledHeight, alpha)
        renderShadow(context, alpha)

        val scaledMouseX = scaleMouseX(mouseX.toDouble()).toInt()
        val scaledMouseY = scaleMouseY(mouseY.toDouble()).toInt()

        renderGuiBox(context, scaledMouseX, scaledMouseY, delta, alpha)

        buttons.forEach { button ->
            // TODO: Consider if buttons should also fade, might need to pass alpha to button.render
            button.render(context, scaledMouseX, scaledMouseY, delta)
        }

        context.matrices.popMatrix()
    }

    private fun renderBlurredBackground(context: DrawContext, scaledWidth: Int, scaledHeight: Int, alpha: Float) {
        val baseAlpha = 0x80
        val gradientStartAlpha = 0x40
        val gradientEndAlpha = 0x60

        val finalBaseAlpha = (baseAlpha * alpha).toInt()
        val finalGradientStartAlpha = (gradientStartAlpha * alpha).toInt()
        val finalGradientEndAlpha = (gradientEndAlpha * alpha).toInt()

        context.fill(0, 0, scaledWidth, scaledHeight, (finalBaseAlpha shl 24) or 0x000000)
        context.fillGradient(0, 0, scaledWidth, scaledHeight, (finalGradientStartAlpha shl 24) or 0x000000, (finalGradientEndAlpha shl 24) or 0x000000)
    }

    private fun renderShadow(context: DrawContext, alpha: Float) {
        val baseShadowAlpha = 0x40
        val finalShadowAlpha = (baseShadowAlpha * alpha).toInt()
        val shadowColor = (finalShadowAlpha shl 24) or 0x000000
        val fadeColor = 0x00000000 // Transparent, no alpha needed here as it's the target

        context.fillGradient(guiX, guiY + guiHeight, guiX + guiWidth, guiY + guiHeight + SHADOW_SIZE, shadowColor, fadeColor)
        context.fillGradient(guiX + guiWidth, guiY, guiX + guiWidth + SHADOW_SIZE, guiY + guiHeight, shadowColor, fadeColor)
        context.fillGradient(guiX + guiWidth, guiY + guiHeight, guiX + guiWidth + SHADOW_SIZE, guiY + guiHeight + SHADOW_SIZE, shadowColor, fadeColor)
    }

    private fun renderGuiBox(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float, alpha: Float) {
        if (shouldRenderDefaultGuiBox) {
            drawRoundedRect(context, guiX, guiY, guiWidth, guiHeight, theme.coreBackgroundPrimary, alpha = alpha)
        }

        renderHeader(context, scaledMouseX, scaledMouseY, delta, alpha)
        renderFooter(context, scaledMouseX, scaledMouseY, delta, alpha)
        renderContent(context, scaledMouseX, scaledMouseY, delta) // Content usually has its own alpha logic or is fine

        if (shouldRenderDefaultGuiBox) {
            drawRoundedBorder(context, guiX, guiY, guiWidth, guiHeight, theme.borderColor, alpha = alpha)
        }
    }

    protected open fun renderHeader(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float, alpha: Float) {
        val headerY = guiY

        val logoPadding = PADDING / 2
        val desiredLogoHeight = HEADER_HEIGHT - 2 * logoPadding
        val desiredLogoWidth = desiredLogoHeight

        val logoX = guiX + PADDING
        val logoY = guiY + (HEADER_HEIGHT - desiredLogoHeight) / 2

        // Apply alpha to logo
        val logoColorWithAlpha = ((alpha * 255).toInt() shl 24) or 0x00FFFFFF // White with variable alpha
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            LOGO_TEXTURE,
            logoX, logoY, // screen x, y
            0f, 0f,   // u, v
            desiredLogoWidth, desiredLogoHeight, // width, height to draw on screen
            desiredLogoWidth, desiredLogoHeight,  // texture sheet width, texture sheet height
            logoColorWithAlpha // ARGB color tint
        )

        val titleText = this.title.copy().setStyle(Style.EMPTY.withFont(theme.getCurrentFont()))
        val titleTextWidth = textRenderer.getWidth(titleText)
        val titleX = guiX + (guiWidth - titleTextWidth) / 2

        val titleColorWithAlpha = applyAlphaToColor(theme.titleColor, alpha)

        context.drawText(
            textRenderer,
            titleText,
            titleX,
            headerY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2,
            titleColorWithAlpha,
            theme.enableTextShadow
        )

        val borderColorWithAlpha = applyAlphaToColor(theme.borderColor, alpha)
        context.fill(
            guiX + CORNER_RADIUS, headerY + HEADER_HEIGHT - 1,
            guiX + guiWidth - CORNER_RADIUS, headerY + HEADER_HEIGHT,
            borderColorWithAlpha
        )

        val closeButtonSize = 16
        val closeButtonX = guiX + guiWidth - closeButtonSize - 8
        val closeButtonY = headerY + (HEADER_HEIGHT - closeButtonSize) / 2

        val isCloseHovered = scaledMouseX >= closeButtonX && scaledMouseX < closeButtonX + closeButtonSize &&
                scaledMouseY >= closeButtonY && scaledMouseY < closeButtonY + closeButtonSize

        val baseCloseButtonColor = if (isCloseHovered) theme.errorColor else theme.secondaryTextColor
        val closeButtonColorWithAlpha = applyAlphaToColor(baseCloseButtonColor, alpha)

        context.drawHorizontalLine(closeButtonX + 3, closeButtonX + 13, closeButtonY + 3, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 4, closeButtonX + 12, closeButtonY + 4, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 5, closeButtonX + 11, closeButtonY + 5, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 6, closeButtonX + 10, closeButtonY + 6, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 7, closeButtonX + 9, closeButtonY + 7, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 6, closeButtonX + 10, closeButtonY + 9, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 5, closeButtonX + 11, closeButtonY + 10, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 4, closeButtonX + 12, closeButtonY + 11, closeButtonColorWithAlpha)
        context.drawHorizontalLine(closeButtonX + 3, closeButtonX + 13, closeButtonY + 12, closeButtonColorWithAlpha)
    }

    protected open fun renderFooter(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float, alpha: Float) {
        val footerY = guiY + guiHeight - FOOTER_HEIGHT
        val borderColorWithAlpha = applyAlphaToColor(theme.borderColor, alpha)
        context.fill(
            guiX + CORNER_RADIUS, footerY,
            guiX + guiWidth - CORNER_RADIUS, footerY + 1,
            borderColorWithAlpha
        )
    }

    fun applyAlphaToColor(color: Int, alphaFactor: Float): Int {
        val originalAlpha = (color shr 24 and 0xFF).toFloat()
        val newAlpha = (originalAlpha * alphaFactor).toInt().coerceIn(0, 255)
        return (newAlpha shl 24) or (color and 0x00FFFFFF)
    }

    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int, topRounded: Boolean = true, bottomRounded: Boolean = true, alpha: Float = 1.0f) {
        val radius = CORNER_RADIUS
        val colorWithAlpha = applyAlphaToColor(color, alpha)
        val centerY = if (topRounded) y + radius else y
        val centerHeight = height - (if (topRounded) radius else 0) - (if (bottomRounded) radius else 0)

        context.fill(x, centerY, x + width, centerY + centerHeight, colorWithAlpha)

        if (topRounded) context.fill(x + radius, y, x + width - radius, y + radius, colorWithAlpha)
        else context.fill(x, y, x + width, y + radius, colorWithAlpha)

        if (bottomRounded) context.fill(x + radius, y + height - radius, x + width - radius, y + height, colorWithAlpha)
        else context.fill(x, y + height - radius, x + width, y + height, colorWithAlpha)

        if (topRounded) {
            drawRoundedCorner(context, x, y, radius, colorWithAlpha, true, true, alpha)
            drawRoundedCorner(context, x + width - radius, y, radius, colorWithAlpha, false, true, alpha)
        }

        if (bottomRounded) {
            drawRoundedCorner(context, x, y + height - radius, radius, colorWithAlpha, true, false, alpha)
            drawRoundedCorner(context, x + width - radius, y + height - radius, radius, colorWithAlpha, false, false, alpha)
        }
    }

    private fun drawRoundedCorner(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, left: Boolean, top: Boolean, alpha: Float = 1.0f) {
        val colorWithAlpha = applyAlphaToColor(color, alpha)
        for (i in 0 until radius) {
            for (j in 0 until radius) {
                if (sqrt((i * i + j * j).toDouble()) <= radius - 0.5) {
                    val pixelX = if (left) x + radius - 1 - i else x + i
                    val pixelY = if (top) y + radius - 1 - j else y + j
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, colorWithAlpha)
                }
            }
        }
    }

    private fun drawRoundedBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int, alpha: Float = 1.0f) {
        val colorWithAlpha = applyAlphaToColor(color, alpha)
        context.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + 1, colorWithAlpha)
        context.fill(x + CORNER_RADIUS, y + height - 1, x + width - CORNER_RADIUS, y + height, colorWithAlpha)
        context.fill(x, y + CORNER_RADIUS, x + 1, y + height - CORNER_RADIUS, colorWithAlpha)
        context.fill(x + width - 1, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, colorWithAlpha)

        drawCornerBorder(context, x, y, CORNER_RADIUS, colorWithAlpha, true, true, alpha)
        drawCornerBorder(context, x + width - CORNER_RADIUS, y, CORNER_RADIUS, colorWithAlpha, false, true, alpha)
        drawCornerBorder(context, x, y + height - CORNER_RADIUS, CORNER_RADIUS, colorWithAlpha, true, false, alpha)
        drawCornerBorder(context, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, colorWithAlpha, false, false, alpha)
    }

    private fun drawCornerBorder(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, left: Boolean, top: Boolean, alpha: Float = 1.0f) {
        val colorWithAlpha = applyAlphaToColor(color, alpha)
        for (i in 0 until radius) {
            for (j in 0 until radius) {
                val distance = sqrt((i * i + j * j).toDouble())
                if (distance >= radius - 1.5 && distance <= radius - 0.5) {
                    val pixelX = if (left) x + radius - 1 - i else x + i
                    val pixelY = if (top) y + radius - 1 - j else y + j
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, colorWithAlpha)
                }
            }
        }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        val closeButtonSize = 16
        val closeButtonX = guiX + guiWidth - closeButtonSize - 8
        val closeButtonY = guiY + (HEADER_HEIGHT - closeButtonSize) / 2

        if (scaledMouseX >= closeButtonX && scaledMouseX < closeButtonX + closeButtonSize &&
            scaledMouseY >= closeButtonY && scaledMouseY < closeButtonY + closeButtonSize) {
            close()
            return true
        }

        if (scaledMouseX >= guiX && scaledMouseX < guiX + guiWidth &&
            scaledMouseY >= guiY && scaledMouseY < guiY + guiHeight) {

            buttons.forEach { btn ->
                if (btn.isMouseOver(scaledMouseX, scaledMouseY)) {
                    btn.mouseClicked(scaledMouseX, scaledMouseY, button)
                    return true
                }
            }
            return super.mouseClicked(mouseX, mouseY, button)
        }

        close()
        return true
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        buttons.forEach { btn ->
            btn.mouseMoved(scaledMouseX, scaledMouseY)
        }
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    protected fun addButton(button: CinnamonButton) {
        buttons.add(button)
    }

    protected fun getContentX(): Int = guiX + PADDING
    protected fun getContentY(): Int = guiY + HEADER_HEIGHT + PADDING
    protected fun getContentWidth(): Int = guiWidth - (PADDING * 2)
    protected fun getContentHeight(): Int = guiHeight - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2)
    protected fun getFooterY(): Int = guiY + guiHeight - FOOTER_HEIGHT

    override fun shouldCloseOnEsc(): Boolean = true
}
