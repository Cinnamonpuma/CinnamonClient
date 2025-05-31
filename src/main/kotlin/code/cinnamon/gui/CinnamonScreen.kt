package code.cinnamon.gui

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import net.minecraft.util.Identifier // Ensure Identifier is imported
import net.minecraft.client.render.RenderLayer
import kotlin.math.max
import kotlin.math.min

abstract class CinnamonScreen(title: Text) : Screen(title) {
    protected val theme = CinnamonTheme
    protected val buttons = mutableListOf<CinnamonButton>()
    
    // GUI Box dimensions and positioning
    protected var guiWidth = 600
    protected var guiHeight = 450
    protected var guiX = 0
    protected var guiY = 0
    
    companion object {
        const val HEADER_HEIGHT = 50
        const val FOOTER_HEIGHT = 35
        const val SIDEBAR_WIDTH = 180
        const val PADDING = 15
        const val CORNER_RADIUS = 8
        const val SHADOW_SIZE = 4
        private val LOGO_TEXTURE = Identifier.of("cinnamon", "textures/gui/logo.png")
        
        // Minimum and maximum GUI sizes
        const val MIN_GUI_WIDTH = 400
        const val MAX_GUI_WIDTH = 800
        const val MIN_GUI_HEIGHT = 300
        const val MAX_GUI_HEIGHT = 600
    }
    
    override fun init() {
        super.init()
        buttons.clear()
        calculateGuiDimensions()
        initializeComponents()
    }
    
    private fun calculateGuiDimensions() {
        // Calculate GUI size based on screen size
        guiWidth = max(MIN_GUI_WIDTH, min(MAX_GUI_WIDTH, (width * 0.7f).toInt()))
        guiHeight = max(MIN_GUI_HEIGHT, min(MAX_GUI_HEIGHT, (height * 0.8f).toInt()))
        
        // Center the GUI
        guiX = (width - guiWidth) / 2
        guiY = (height - guiHeight) / 2
    }
    
    abstract fun initializeComponents()
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        
        // Render blurred background
        renderBlurredBackground(context, mouseX, mouseY, delta)
        
        // Render shadow
        renderShadow(context)
        
        // Render main GUI box
        renderGuiBox(context, mouseX, mouseY, delta)
        
        // Render components
        buttons.forEach { button ->
            button.render(context, mouseX, mouseY, delta)
        }
    }
    
    private fun renderBlurredBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Semi-transparent dark overlay
        context.fill(0, 0, width, height, 0x80000000.toInt())
        
        // Optional: Add a subtle gradient effect
        context.fillGradient(
            0, 0, width, height,
            0x40000000, 0x60000000
        )
    }
    
    private fun renderShadow(context: DrawContext) {
        val shadowColor = 0x40000000
        val fadeColor = 0x00000000
        
        // Bottom shadow - positioned below the GUI box
        context.fillGradient(
            guiX, guiY + guiHeight,
            guiX + guiWidth, guiY + guiHeight + SHADOW_SIZE,
            shadowColor, fadeColor
        )
        
        // Right shadow - positioned to the right of the GUI box
        context.fillGradient(
            guiX + guiWidth, guiY,
            guiX + guiWidth + SHADOW_SIZE, guiY + guiHeight,
            shadowColor, fadeColor
        )
        
        // Bottom-right corner shadow - fills the corner gap
        context.fillGradient(
            guiX + guiWidth, guiY + guiHeight,
            guiX + guiWidth + SHADOW_SIZE, guiY + guiHeight + SHADOW_SIZE,
            shadowColor, fadeColor
        )
    }
    
    private fun renderGuiBox(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Main GUI background with rounded corners - REMOVED
        // drawRoundedRect(context, guiX, guiY, guiWidth, guiHeight, theme.guiBackground)
        
        // Header
        renderHeader(context, mouseX, mouseY, delta)
        
        // Fill the background of the content area (between header and footer) - Moved before renderContent
        context.fill(guiX, guiY + HEADER_HEIGHT, guiX + guiWidth, guiY + guiHeight - FOOTER_HEIGHT, theme.coreBackgroundPrimary)

        // Main content area
        renderContent(context, mouseX, mouseY, delta)
        
        // Footer
        renderFooter(context, mouseX, mouseY, delta)

        // GUI border (drawn last to be on top of all sections)
        drawRoundedBorder(context, guiX, guiY, guiWidth, guiHeight, theme.borderColor) 
    }
    
    protected open fun renderHeader(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val headerY = guiY
        
        // Header background with top rounded corners
        drawRoundedRect(
            context, guiX, headerY, guiWidth, HEADER_HEIGHT,
            theme.headerBackground, true, false
        )

        // Define Logo Size and Position
        val logoPadding = PADDING / 2 
        val desiredLogoHeight = HEADER_HEIGHT - 2 * logoPadding 
        val desiredLogoWidth = desiredLogoHeight 
        
        val logoX = guiX + PADDING
        val logoY = guiY + (HEADER_HEIGHT - desiredLogoHeight) / 2 

        // Render logo using drawTexture with RenderLayer
        context.drawTexture(
            RenderLayer::getGuiTextured,
            LOGO_TEXTURE, 
            logoX, logoY, 
            0f, 0f, 
            desiredLogoWidth, desiredLogoHeight, 
            desiredLogoWidth, desiredLogoHeight
        )
        
        // Title (Adjusted Position)
        val titleText = this.title 
        val titleTextWidth = textRenderer.getWidth(titleText)
       
        // Center the title in the entire header width
        val titleX = guiX + (guiWidth - titleTextWidth) / 2
       
        context.drawText(
            textRenderer,
            titleText,
            titleX, // New X
            headerY + (HEADER_HEIGHT - textRenderer.fontHeight) / 2, // Y remains the same
            theme.titleColor,
            true
        )
        
        // Header border
        context.fill(
            guiX + CORNER_RADIUS, headerY + HEADER_HEIGHT - 1,
            guiX + guiWidth - CORNER_RADIUS, headerY + HEADER_HEIGHT,
            theme.borderColor
        )
        
        // Close button (X) in top right
        val closeButtonSize = 16 // Standard size for the close button visual
        val closeButtonX = guiX + guiWidth - closeButtonSize - 8 
        val closeButtonY = headerY + (HEADER_HEIGHT - closeButtonSize) / 2
        
        val isCloseHovered = mouseX >= closeButtonX && mouseX < closeButtonX + closeButtonSize &&
                            mouseY >= closeButtonY && mouseY < closeButtonY + closeButtonSize
        
        val closeButtonColor = if (isCloseHovered) theme.errorColor else theme.secondaryTextColor
        
        // Draw X
        context.drawHorizontalLine(closeButtonX + 3, closeButtonX + 13, closeButtonY + 3, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 4, closeButtonX + 12, closeButtonY + 4, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 5, closeButtonX + 11, closeButtonY + 5, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 6, closeButtonX + 10, closeButtonY + 6, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 7, closeButtonX + 9, closeButtonY + 7, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 6, closeButtonX + 10, closeButtonY + 9, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 5, closeButtonX + 11, closeButtonY + 10, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 4, closeButtonX + 12, closeButtonY + 11, closeButtonColor)
        context.drawHorizontalLine(closeButtonX + 3, closeButtonX + 13, closeButtonY + 12, closeButtonColor)
    }
    
    protected abstract fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float)
    
    protected open fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val footerY = guiY + guiHeight - FOOTER_HEIGHT
        
        // Footer background with bottom rounded corners
        drawRoundedRect(
            context, guiX, footerY, guiWidth, FOOTER_HEIGHT,
            theme.footerBackground, false, true
        )
        
        // Footer border
        context.fill(
            guiX + CORNER_RADIUS, footerY,
            guiX + guiWidth - CORNER_RADIUS, footerY + 1,
            theme.borderColor
        )
    }
    
    private fun drawRoundedRect(
        context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int,
        topRounded: Boolean = true, bottomRounded: Boolean = true
    ) {
        // Main rectangle
        context.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + height, color)
        
        if (topRounded) {
            context.fill(x, y + CORNER_RADIUS, x + width, y + height, color)
            // Top corners
            drawCorner(context, x, y, CORNER_RADIUS, color, true, true)
            drawCorner(context, x + width - CORNER_RADIUS, y, CORNER_RADIUS, color, false, true)
        } else {
            context.fill(x, y, x + width, y + height, color)
        }
        
        if (bottomRounded && !topRounded) {
            context.fill(x, y, x + width, y + height - CORNER_RADIUS, color)
            // Bottom corners
            drawCorner(context, x, y + height - CORNER_RADIUS, CORNER_RADIUS, color, true, false)
            drawCorner(context, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, color, false, false)
        }
    }
    
    private fun drawRoundedBorder(context: DrawContext, x: Int, y: Int, width: Int, height: Int, color: Int) {
        // Top border
        context.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + 1, color)
        // Bottom border  
        context.fill(x + CORNER_RADIUS, y + height - 1, x + width - CORNER_RADIUS, y + height, color)
        // Left border
        context.fill(x, y + CORNER_RADIUS, x + 1, y + height - CORNER_RADIUS, color)
        // Right border
        context.fill(x + width - 1, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, color)
        
        // Corner borders (simplified)
        drawCornerBorder(context, x, y, CORNER_RADIUS, color, true, true)
        drawCornerBorder(context, x + width - CORNER_RADIUS, y, CORNER_RADIUS, color, false, true)
        drawCornerBorder(context, x, y + height - CORNER_RADIUS, CORNER_RADIUS, color, true, false)
        drawCornerBorder(context, x + width - CORNER_RADIUS, y + height - CORNER_RADIUS, CORNER_RADIUS, color, false, false)
    }
    
    private fun drawCorner(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, left: Boolean, top: Boolean) {
        // Simplified rounded corner using pixels
        for (i in 0 until radius) {
            for (j in 0 until radius) {
                val distance = kotlin.math.sqrt((i * i + j * j).toDouble())
                if (distance <= radius) {
                    val pixelX = if (left) x + radius - 1 - i else x + i
                    val pixelY = if (top) y + radius - 1 - j else y + j
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }
    
    private fun drawCornerBorder(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, left: Boolean, top: Boolean) {
        // Simplified rounded corner border
        for (i in 0 until radius) {
            for (j in 0 until radius) {
                val distance = kotlin.math.sqrt((i * i + j * j).toDouble())
                if (distance >= radius - 1 && distance <= radius) {
                    val pixelX = if (left) x + radius - 1 - i else x + i
                    val pixelY = if (top) y + radius - 1 - j else y + j
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }
    
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Check if click is on close button
        val closeButtonSize = 16
        val closeButtonX = guiX + guiWidth - closeButtonSize - 8
        val closeButtonY = guiY + (HEADER_HEIGHT - closeButtonSize) / 2
        
        if (mouseX >= closeButtonX && mouseX < closeButtonX + closeButtonSize &&
            mouseY >= closeButtonY && mouseY < closeButtonY + closeButtonSize) {
            close()
            return true
        }
        
        // Only process clicks within GUI bounds
        if (mouseX >= guiX && mouseX < guiX + guiWidth &&
            mouseY >= guiY && mouseY < guiY + guiHeight) {
            
            buttons.forEach { btn ->
                if (btn.isMouseOver(mouseX, mouseY)) {
                    btn.onClick(mouseX, mouseY)
                    return true
                }
            }
            return super.mouseClicked(mouseX, mouseY, button)
        }
        
        // Click outside GUI closes it
        close()
        return true
    }
    
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        buttons.forEach { btn ->
            btn.setHovered(btn.isMouseOver(mouseX, mouseY))
        }
        super.mouseMoved(mouseX, mouseY)
    }
    
    protected fun addButton(button: CinnamonButton) {
        buttons.add(button)
    }
    
    // Helper methods for positioning elements within the GUI box
    protected fun getContentX(): Int = guiX + PADDING
    protected fun getContentY(): Int = guiY + HEADER_HEIGHT + PADDING
    protected fun getContentWidth(): Int = guiWidth - (PADDING * 2)
    protected fun getContentHeight(): Int = guiHeight - HEADER_HEIGHT - FOOTER_HEIGHT - (PADDING * 2)
    protected fun getFooterY(): Int = guiY + guiHeight - FOOTER_HEIGHT
    
    override fun shouldCloseOnEsc(): Boolean = true
}