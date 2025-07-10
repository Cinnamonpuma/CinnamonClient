package code.cinnamon.hud

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudManager
import net.minecraft.client.gui.DrawContext
// import net.minecraft.client.gui.screen.Screen // No longer needed
import net.minecraft.text.Style
import net.minecraft.text.Text
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.theme.CinnamonTheme

class HudScreen : CinnamonScreen(Text.literal("HUD Editor").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))) {

    // CinnamonScreen's init will handle basic setup.
    // We want HudScreen to be mostly transparent, letting HudManager render elements.
    override fun init() {
        super.init() // This calls calculateGuiDimensions, initializeComponents
        HudManager.setEditMode(true)
    }

    // We don't need the default CinnamonScreen GUI box, header, or footer for the HUD editor.
    // The HUD editor should be a transparent overlay.
    override fun renderHeader(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {}
    override fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {}

    // Override renderGuiBox to prevent drawing the default rounded rectangle background,
    // making the screen transparent for HUD editing.
    // We will still call renderContent where HUD elements and instructions are drawn.
    private fun renderGuiBox(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Do not draw the default GUI box from CinnamonScreen.
        // Instead, directly call renderContent for our custom rendering.
        // mouseX and mouseY here are already scaled by CinnamonScreen.render
        renderContent(context, mouseX, mouseY, delta)
    }


    // render() is inherited from CinnamonScreen. It will:
    // 1. Call super.render() (from vanilla Screen - usually does nothing or a dim background)
    // 2. Set up matrix scaling based on TARGET_SCALE_FACTOR.
    // 3. Call this.renderBlurredBackground() (provides a dim overlay for the editor).
    // 4. Call this.renderShadow() (we might not want this, could override to be empty if needed for HUD screen).
    // 5. Call the overridden renderGuiBox above, which then calls renderContent.
    // 6. Render CinnamonButtons (if any were added via initializeComponents).
    // 7. Pop matrix.

    // We'll put the HUD rendering and instruction text into renderContent.
    // mouseX and mouseY passed here are already scaled by CinnamonScreen.
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // HudManager.render will draw all HUD elements.
        // It needs to be aware that it's now rendering within a scaled context.
        // For now, let it render as is; further changes to HudManager and HudElement will handle this.
        HudManager.render(context, delta)

        // Instruction text. This will also be scaled by CinnamonScreen's matrix.
        // We need to position it relative to the scaled screen dimensions.
        val instructionText = Text.literal("Drag elements to move them - Scroll on elements to scale them")
            .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())) // Use theme font

        val effectiveScaledWidth = getEffectiveWidth() // Use CinnamonScreen's helper for scaled width
        // val effectiveScaledHeight = getEffectiveHeight() // Use CinnamonScreen's helper for scaled height

        context.drawCenteredTextWithShadow(
            textRenderer,
            instructionText,
            effectiveScaledWidth / 2, // Center on the scaled screen width
            15, // Y position in scaled coordinates
            CinnamonTheme.primaryTextColor // Use theme color
        )
    }

    override fun initializeComponents() {
        // If we need any CinnamonButtons for the HudScreen (e.g., a "Done" button), add them here.
        // Their positions would use guiX, guiY, getContentX(), etc., from CinnamonScreen.
        // For now, no extra buttons seem to be defined in the original HudScreen.
    }


    // Mouse methods now receive mouseX, mouseY that are ALREADY SCALED by CinnamonScreen.
    // These are passed directly to HudManager, which will need to expect scaled coordinates.
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        // If CinnamonButtons were added, their click handling is done by CinnamonScreen.mouseClicked -> super.mouseClicked
        // We only need to call HudManager if no buttons were clicked or if we want HudManager to take precedence.
        if (super.mouseClicked(mouseX, mouseY, button)) return true // Handles CinnamonButtons first
        return HudManager.onMouseClicked(mouseX, mouseY, button) // Then pass to HudManager
    }

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double
    ): Boolean {
        // deltaX and deltaY are also in the scaled coordinate system.
        if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true
        return HudManager.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (super.mouseReleased(mouseX, mouseY, button)) return true
        return HudManager.onMouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double, // This is new in 1.20.5+
        verticalAmount: Double   // This is new in 1.20.5+
    ): Boolean {
        // In older versions, mouseScrolled had (mouseX, mouseY, amount)
        // Assuming verticalAmount is the primary scroll amount we need.
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true
        return HudManager.onMouseScrolled(mouseX, mouseY, verticalAmount) // Pass verticalAmount as 'delta'
    }

    // mouseMoved is fine, superclass handles it for buttons. HudManager doesn't use it directly.
    // override fun mouseMoved(mouseX: Double, mouseY: Double) {
    //     super.mouseMoved(mouseX, mouseY)
    // }

    override fun close() {
        HudManager.onEditMenuClosed()
        HudManager.setEditMode(false)
        // Assuming CinnamonGuiManager.openMainMenu() works as intended.
        CinnamonGuiManager.openMainMenu()
        // super.close() // CinnamonScreen doesn't have a close, vanilla Screen does.
        // CinnamonScreen.onClose is called by MinecraftClient.setScreen(null)
    }

    // shouldCloseOnEsc is inherited from CinnamonScreen (which defaults to true).
    // override fun shouldCloseOnEsc(): Boolean = true
}