package code.cinnamon.gui

import net.minecraft.text.Style
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.gui.utils.GraphicsUtils
import net.minecraft.util.Identifier
import kotlin.math.max
import kotlin.math.min

abstract class CinnamonScreen(title: Text) : Screen(title) {
    protected val theme = CinnamonTheme
    protected val buttons = mutableListOf<CinnamonButton>()
    private var isSidebarOpen = false
    private val sidebarButtons = mutableListOf<CinnamonButton>()
    private var sidebarAnimationProgress = 0.0f
    private var lastAnimationTime = 0L


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
        const val CORNER_RADIUS = 8f
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
        initializeSidebarButtons()
        lastAnimationTime = System.currentTimeMillis()
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

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        this.init(client, width, height)
    }

    protected abstract fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float)

    protected open val shouldRenderDefaultGuiBox: Boolean = true

    // We render our own background, so we override this to do nothing.
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {}

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastAnimationTime) / 1000.0f
        lastAnimationTime = currentTime

        sidebarAnimationProgress = if (isSidebarOpen) min(1.0f, sidebarAnimationProgress + deltaTime * 6f)
        else max(0.0f, sidebarAnimationProgress - deltaTime * 6f)

        val scaleRatio = getScaleRatio()
        val scaledWidth = getEffectiveWidth()
        val scaledHeight = getEffectiveHeight()

        context.matrices.pushMatrix()
        context.matrices.scale(scaleRatio, scaleRatio)

        // Render our custom background
        renderBlurredBackground(context, scaledWidth, scaledHeight)

        val scaledMouseX = scaleMouseX(mouseX.toDouble()).toInt()
        val scaledMouseY = scaleMouseY(mouseY.toDouble()).toInt()

        // 1. Render the main panel and its content
        renderGuiBox(context, scaledMouseX, scaledMouseY, delta)

        // 2. Render all managed widgets.
        // This calls our empty renderBackground() and then renders the widgets inside our scaled matrix.
        // We pass the original mouseX/Y because the superclass expects them.
        super.render(context, mouseX, mouseY, delta)

        // 3. Render the sidebar on top of the content
        if (sidebarAnimationProgress > 0) {
            renderSidebar(context, scaledMouseX, scaledMouseY, delta)
        }

        // 4. Render the header on top of everything
        renderHeader(context, scaledMouseX, scaledMouseY, delta)

        // 5. Render non-widget buttons (e.g., footer buttons)
        buttons.forEach { it.render(context, scaledMouseX, scaledMouseY, delta) }

        context.matrices.popMatrix()
    }

    private fun renderBlurredBackground(context: DrawContext, scaledWidth: Int, scaledHeight: Int) {
        context.fill(0, 0, scaledWidth, scaledHeight, 0x80000000.toInt())
        context.fillGradient(0, 0, scaledWidth, scaledHeight, 0x40000000, 0x60000000)
    }

    private fun renderGuiBox(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        if (shouldRenderDefaultGuiBox) {
            GraphicsUtils.drawFilledRoundedRect(context, guiX.toFloat(), guiY.toFloat(), guiWidth.toFloat(), guiHeight.toFloat(), CORNER_RADIUS, theme.coreBackgroundPrimary)
        }

        renderFooter(context, scaledMouseX, scaledMouseY, delta)
        renderContent(context, scaledMouseX, scaledMouseY, delta)

        if (shouldRenderDefaultGuiBox) {
            GraphicsUtils.drawRoundedRectBorder(context, guiX.toFloat(), guiY.toFloat(), guiWidth.toFloat(), guiHeight.toFloat(), CORNER_RADIUS, theme.borderColor)
        }
    }

    protected open fun renderHeader(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val headerY = guiY

        renderHamburgerIcon(context, guiX + PADDING, headerY + (HEADER_HEIGHT - 20) / 2, 20, 20, scaledMouseX, scaledMouseY)

        val titleText = this.title.copy().setStyle(Style.EMPTY.withFont(theme.getCurrentFont()))
        val titleTextWidth = textRenderer.getWidth(titleText)
        val titleX = guiX + (guiWidth - titleTextWidth) / 2

        context.drawText(
            textRenderer,
            titleText,
            titleX,
            headerY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2,
            theme.titleColor,
            theme.enableTextShadow
        )

        context.fill(
            (guiX + CORNER_RADIUS).toInt(), headerY + HEADER_HEIGHT - 1,
            (guiX + guiWidth - CORNER_RADIUS).toInt(), headerY + HEADER_HEIGHT,
            theme.borderColor
        )

        val closeButtonSize = 16
        val closeButtonX = guiX + guiWidth - closeButtonSize - 8
        val closeButtonY = headerY + (HEADER_HEIGHT - closeButtonSize) / 2

        val isCloseHovered = scaledMouseX >= closeButtonX && scaledMouseX < closeButtonX + closeButtonSize &&
                scaledMouseY >= closeButtonY && scaledMouseY < closeButtonY + closeButtonSize

        val closeButtonColor = if (isCloseHovered) theme.errorColor else theme.secondaryTextColor


        val thickness = 2
        val padding = 3


        for (i in 0 until closeButtonSize - 2 * padding) {
            for (j in 0 until thickness) {
                val x = closeButtonX + padding + i
                val y = closeButtonY + padding + i + j
                if (y < closeButtonY + closeButtonSize - padding) {
                    context.fill(x, y, x + 1, y + 1, closeButtonColor)
                }
            }
        }


        for (i in 0 until closeButtonSize - 2 * padding) {
            for (j in 0 until thickness) {
                val x = closeButtonX + closeButtonSize - padding - 1 - i
                val y = closeButtonY + padding + i + j
                if (y < closeButtonY + closeButtonSize - padding) {
                    context.fill(x, y, x + 1, y + 1, closeButtonColor)
                }
            }
        }
    }

    protected open fun renderFooter(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val footerY = guiY + guiHeight - FOOTER_HEIGHT
        context.fill(
            (guiX + CORNER_RADIUS).toInt(), footerY,
            (guiX + guiWidth - CORNER_RADIUS).toInt(), footerY + 1,
            theme.borderColor
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        // 1. Handle hamburger menu click
        val hamburgerX = guiX + PADDING
        val hamburgerY = guiY + (HEADER_HEIGHT - 20) / 2
        if (scaledMouseX >= hamburgerX && scaledMouseX < hamburgerX + 20 && scaledMouseY >= hamburgerY && scaledMouseY < hamburgerY + 20) {
            isSidebarOpen = !isSidebarOpen
            return true
        }

        // 2. Handle close button click
        val closeButtonSize = 16
        val closeButtonX = guiX + guiWidth - closeButtonSize - 8
        val closeButtonY = guiY + (HEADER_HEIGHT - closeButtonSize) / 2
        if (scaledMouseX >= closeButtonX && scaledMouseX < closeButtonX + closeButtonSize && scaledMouseY >= closeButtonY && scaledMouseY < closeButtonY + closeButtonSize) {
            close()
            return true
        }

        // 3. Handle sidebar interactions if it's open
        if (isSidebarOpen && sidebarAnimationProgress > 0.1f) {
            val sidebarX = guiX
            val sidebarEndX = sidebarX + SIDEBAR_WIDTH
            if (scaledMouseX >= sidebarX && scaledMouseX < sidebarEndX && scaledMouseY >= guiY && scaledMouseY < guiY + guiHeight) {
                for (btn in sidebarButtons) {
                    if (btn.mouseClicked(scaledMouseX, scaledMouseY, button)) return true
                }
                // Absorb clicks on the sidebar background, but don't close it
                return true
            }
        }

        // 4. Delegate to widgets and other elements if the click is within the main GUI area
        if (scaledMouseX >= guiX && scaledMouseX < guiX + guiWidth && scaledMouseY >= guiY && scaledMouseY < guiY + guiHeight) {
            // Let the default screen handler deal with widgets (like search bar)
            if (super.mouseClicked(scaledMouseX, scaledMouseY, button)) return true

            // Handle footer buttons
            for (btn in buttons) {
                if (btn.mouseClicked(scaledMouseX, scaledMouseY, button)) return true
            }
        }

        // 5. If a click is outside the GUI, close the sidebar if it's open
        if (isSidebarOpen) {
            isSidebarOpen = false
            return true
        }

        // If click is outside GUI and sidebar is closed, do nothing (don't close the screen).
        // The ESC key will handle closing.
        return false
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        buttons.forEach { btn ->
            btn.mouseMoved(scaledMouseX, scaledMouseY)
        }
        super.mouseMoved(scaledMouseX, scaledMouseY)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)
        return super.mouseScrolled(scaledMouseX, scaledMouseY, horizontalAmount, verticalAmount)
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

    private fun renderHamburgerIcon(context: DrawContext, x: Int, y: Int, width: Int, height: Int, scaledMouseX: Int, scaledMouseY: Int) {
        val isHovered = scaledMouseX >= x && scaledMouseX < x + width && scaledMouseY >= y && scaledMouseY < y + height
        val color = if (isHovered) theme.accentColor else theme.primaryTextColor
        val lineY = y + (height - 8) / 2
        context.fill(x, lineY, x + width, lineY + 2, color)
        context.fill(x, lineY + 6, x + width, lineY + 8, color)
        context.fill(x, lineY + 12, x + width, lineY + 14, color)
    }

    private fun initializeSidebarButtons() {
        sidebarButtons.clear()
        val sidebarX = guiX
        val buttonWidth = SIDEBAR_WIDTH - PADDING * 2
        var currentY = guiY + HEADER_HEIGHT + PADDING

        sidebarButtons.add(CinnamonButton(
            sidebarX + PADDING,
            currentY,
            buttonWidth,
            CinnamonTheme.BUTTON_HEIGHT_LARGE,
            Text.literal("Modules"),
            { _, _ -> CinnamonGuiManager.openModulesScreen() }
        ))
        currentY += CinnamonTheme.BUTTON_HEIGHT_LARGE + PADDING

        sidebarButtons.add(CinnamonButton(
            sidebarX + PADDING,
            currentY,
            buttonWidth,
            CinnamonTheme.BUTTON_HEIGHT_LARGE,
            Text.literal("Keybindings"),
            { _, _ -> CinnamonGuiManager.openKeybindingsScreen() }
        ))
        currentY += CinnamonTheme.BUTTON_HEIGHT_LARGE + PADDING

        sidebarButtons.add(CinnamonButton(
            sidebarX + PADDING,
            currentY,
            buttonWidth,
            CinnamonTheme.BUTTON_HEIGHT_LARGE,
            Text.literal("HUD Editor"),
            { _, _ -> client?.setScreen(code.cinnamon.hud.HudScreen()) }
        ))
        currentY += CinnamonTheme.BUTTON_HEIGHT_LARGE + PADDING

        sidebarButtons.add(CinnamonButton(
            sidebarX + PADDING,
            currentY,
            buttonWidth,
            CinnamonTheme.BUTTON_HEIGHT_LARGE,
            Text.literal("Theme Manager"),
            { _, _ -> CinnamonGuiManager.openThemeManagerScreen() }
        ))
    }

    private fun renderSidebar(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val sidebarX = guiX
        val alpha = sidebarAnimationProgress

        val intAlpha = (alpha * 255).toInt()
        val backgroundColor = GraphicsUtils.withAlpha(theme.coreBackgroundPrimary, intAlpha)
        val borderColor = GraphicsUtils.withAlpha(theme.borderColor, intAlpha)

        GraphicsUtils.drawFilledRoundedRect(context, sidebarX.toFloat(), guiY.toFloat(), SIDEBAR_WIDTH.toFloat(), guiHeight.toFloat(), CORNER_RADIUS, backgroundColor)
        GraphicsUtils.drawRoundedRectBorder(context, sidebarX.toFloat(), guiY.toFloat(), SIDEBAR_WIDTH.toFloat(), guiHeight.toFloat(), CORNER_RADIUS, borderColor)

        sidebarButtons.forEach {
            it.alpha = alpha
            it.render(context, scaledMouseX, scaledMouseY, delta)
        }
    }
}