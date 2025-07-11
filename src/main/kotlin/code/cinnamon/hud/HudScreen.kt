package code.cinnamon.hud

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.theme.CinnamonTheme
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text


class HudScreen : CinnamonScreen(Text.literal("HUD Editor").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))) {

    override fun init() {
        super.init()
        HudManager.setEditMode(true)
    }

    override fun getDesiredGuiWidth(effectiveScaledWidth: Int): Int {
        return effectiveScaledWidth
    }

    override fun getDesiredGuiHeight(effectiveScaledHeight: Int): Int {
        return effectiveScaledHeight
    }

    override fun renderHeader(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
    }

    override fun renderFooter(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    }

    override val shouldRenderDefaultGuiBox: Boolean = false

    override fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        HudManager.render(context, delta)

        val instructionText = Text.literal("Drag elements to move them - Scroll on elements to scale them")
            .setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

        context.drawCenteredTextWithShadow(
            textRenderer,
            instructionText,
            this.guiWidth / 2,
            15,
            CinnamonTheme.primaryTextColor
        )
    }

    override fun initializeComponents() {
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val scaledMouseX = scaleMouseX(mouseX)
        val scaledMouseY = scaleMouseY(mouseY)

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