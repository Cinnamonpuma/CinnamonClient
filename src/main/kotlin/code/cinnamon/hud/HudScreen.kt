package code.cinnamon.hud

import code.cinnamon.gui.CinnamonScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import code.cinnamon.gui.CinnamonGuiManager

class HudScreen : Screen(Text.literal("HUD Editor").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {

    override fun init() {
        super.init()
        
        // Enable edit mode when screen opens
        HudManager.toggleEditMode()
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Call super.render() FIRST to prevent blur/dimming effect
        super.render(context, mouseX, mouseY, delta)
        
        // Render semi-transparent background
        context.fill(0, 0, width, height, 0x80000000.toInt())
        
        // Render HUD elements so user can see them while editing
        HudManager.render(context, delta)
        
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
        super.mouseMoved(mouseX, mouseY)
    }

    override fun close() {
        HudManager.onEditMenuClosed()
        // Instead of calling super.close(), open the main menu
        CinnamonGuiManager.openMainMenu()
    }

    override fun shouldCloseOnEsc(): Boolean = true
}