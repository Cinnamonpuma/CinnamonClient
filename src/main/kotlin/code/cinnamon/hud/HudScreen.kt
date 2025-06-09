package code.cinnamon.hud

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import code.cinnamon.gui.CinnamonGuiManager

class HudScreen : Screen(Text.literal("HUD Editor").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {
    private val buttons = mutableListOf<CinnamonButton>()

    override fun init() {
        super.init()
        buttons.clear()
        initializeButtons()
        
        // Enable edit mode when screen opens
        HudManager.toggleEditMode()
    }
    
    private fun initializeButtons() {
        val centerX = width / 2
        val startY = 50
        val buttonWidth = 200
        val buttonHeight = CinnamonTheme.BUTTON_HEIGHT_LARGE
        val spacing = 50
        
        var currentY = startY
        
        // Add toggle buttons for each HUD element
        HudManager.getElements().forEach { element ->
            buttons.add(CinnamonButton(
                centerX - buttonWidth / 2,
                currentY,
                buttonWidth,
                buttonHeight,
                Text.literal("${element.getName()}: ${if (element.isEnabled) "ON" else "OFF"}").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
                { _, _ -> 
                    element.isEnabled = !element.isEnabled
                    refreshButtons()
                }
            ))
            currentY += spacing
        }
        
        // Add Done button
        buttons.add(CinnamonButton(
            centerX - buttonWidth / 2,
            currentY,
            buttonWidth,
            buttonHeight,
            Text.literal("Back").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            { _, _ -> CinnamonGuiManager.openMainMenu() }
        ))
    }
    
    private fun refreshButtons() {
        buttons.clear()
        initializeButtons()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Call super.render() FIRST to prevent blur/dimming effect
        super.render(context, mouseX, mouseY, delta)
        
        // Render semi-transparent background
        context.fill(0, 0, width, height, 0x80000000.toInt())
        
        // Render HUD elements so user can see them while editing
        HudManager.render(context, delta)
        
        // Render buttons
        buttons.forEach { button ->
            button.render(context, mouseX, mouseY, delta)
        }
        
        // Instructions at top
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("Drag elements to move them - Scroll on elements to scale them").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT)),
            width / 2,
            15,
            0xFFFFFF
        )
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // Check button clicks first
        buttons.forEach { btn ->
            if (btn.isMouseOver(mouseX, mouseY)) {
                btn.onClick(mouseX, mouseY)
                return true
            }
        }
        
        // Then check HUD element interactions
        return HudManager.onMouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        return HudManager.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        return HudManager.onMouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        return HudManager.onMouseScrolled(mouseX, mouseY, verticalAmount) || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }
    
    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        buttons.forEach { btn ->
            btn.setHovered(btn.isMouseOver(mouseX, mouseY))
        }
        super.mouseMoved(mouseX, mouseY)
    }

    override fun close() {
        // Disable edit mode when screen closes
        HudManager.toggleEditMode()
        super.close()
    }

    override fun shouldCloseOnEsc(): Boolean = true
}