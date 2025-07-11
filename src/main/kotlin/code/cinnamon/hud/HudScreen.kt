package code.cinnamon.hud

import code.cinnamon.gui.CinnamonScreen // Import CinnamonScreen
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.theme.CinnamonTheme
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
// Screen import no longer needed if extending CinnamonScreen directly and it handles Screen import

class HudScreen : CinnamonScreen(Text.literal("HUD Editor").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))) {

    override fun init() {
        super.init() // This calls CinnamonScreen.init()
        HudManager.setEditMode(true)
    }

    // Force CinnamonScreen's GUI box to cover the entire effective screen
    // This prevents "click outside to close" behavior for a full-screen overlay.
    override fun getDesiredGuiWidth(effectiveScaledWidth: Int): Int {
        return effectiveScaledWidth
    }

    override fun getDesiredGuiHeight(effectiveScaledHeight: Int): Int {
        return effectiveScaledHeight
    }

    // Make the HudScreen background mostly transparent or just a dim overlay.
    override fun renderHeader(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) { // Match superclass
        // No header for HUD editor
    }

    override fun renderFooter(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) { // Match superclass
        // No footer for HUD editor
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // CinnamonScreen's render method calls renderBlurredBackground which provides a dim overlay.
        // We don't want CinnamonScreen's main rounded rect GUI box.
        // The getDesiredGuiWidth/Height overrides making the logical box full-screen, combined
        // with renderBlurredBackground, should achieve the desired effect without drawing
        // the default themed box on top.
        // If renderBlurredBackground is too opaque or unwanted, it could be conditionally skipped
        // in CinnamonScreen or overridden here if CinnamonScreen provides a hook.
        // For now, assume renderBlurredBackground is acceptable.
    }

    override val shouldRenderDefaultGuiBox: Boolean = false // Opt out of default GUI box

    // renderContent is called by CinnamonScreen.render (via its private renderGuiBox)
    // It now correctly receives scaledMouseX, scaledMouseY from CinnamonScreen's updated renderGuiBox.
    override fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        // HudManager.render will draw all HUD elements.
        // It needs to receive the scaled DrawContext.
        HudManager.render(context, delta) // HudManager will need to use this scaled context correctly.

        // Instruction text. This will also be scaled by CinnamonScreen's matrix.
        val instructionText = Text.literal("Drag elements to move them - Scroll on elements to scale them")
            .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

        // Centering based on the effective scaled width, which is CinnamonScreen.guiWidth now.
        context.drawCenteredTextWithShadow(
            textRenderer,
            instructionText,
            this.guiWidth / 2, // guiWidth is now the full effectiveScaledWidth
            15,                // Y position in scaled coordinates
            CinnamonTheme.primaryTextColor
        )
    }

    override fun initializeComponents() {
        // No CinnamonButtons needed for the HUD editor itself.
    }

    // Mouse methods receive raw screen coordinates from Minecraft.
    // We must scale them before passing to HudManager.
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

        // Give HudManager priority for clicks in edit mode.
        if (HudManager.onMouseClicked(scaledMouseX, scaledMouseY, button)) {
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        deltaX: Double,
        deltaY: Double
    ): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)
        val scaledDeltaX = deltaX / getScaleRatio()
        val scaledDeltaY = deltaY / getScaleRatio()

        // Pass this.guiWidth and this.guiHeight (which are the full effective scaled screen dimensions)
        // to HudManager for clamping purposes within HudElement.updateDragging.
        if (HudManager.onMouseDragged(scaledMouseX, scaledMouseY, button, scaledDeltaX, scaledDeltaY, this.guiWidth, this.guiHeight)) {
            return true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)
        if (HudManager.onMouseReleased(scaledMouseX, scaledMouseY, button)) {
            return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)
        if (HudManager.onMouseScrolled(scaledMouseX, scaledMouseY, verticalAmount)) {
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun close() {
        HudManager.onEditMenuClosed()
        HudManager.setEditMode(false)
        super.close()
    }

    override fun shouldCloseOnEsc(): Boolean = true
}