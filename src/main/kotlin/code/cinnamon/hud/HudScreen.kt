package code.cinnamon.hud

import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class HudScreen : Screen(Text.literal("HUD Editor")) {

    override fun init() {
        super.init()

        // Add buttons for HUD element toggles
        var y = 20
        HudManager.getElements().forEach { element ->
            addDrawableChild(
                ButtonWidget.builder(
                    Text.literal("${element.getName()}: ${if (element.isEnabled) "ON" else "OFF"}"),
                ) { button ->
                    element.isEnabled = !element.isEnabled
                    button.message = Text.literal("${element.getName()}: ${if (element.isEnabled) "ON" else "OFF"}")
                }.dimensions(width - 120, y, 100, 20).build()
            )
            y += 25
        }

        // Done button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Done")) { 
                client?.setScreen(null) 
            }.dimensions(width / 2 - 50, height - 30, 100, 20).build()
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        // Render HUD elements
        HudManager.render(context, delta)

        super.render(context, mouseX, mouseY, delta)

        // Instructions
        context.drawCenteredTextWithShadow(textRenderer, "Drag elements to move them", width / 2, 10, 0xFFFFFF)
        context.drawCenteredTextWithShadow(textRenderer, "Scroll on elements to scale them", width / 2, height - 50, 0xFFFFFF)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
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

    override fun close() {
        HudManager.toggleEditMode()
        super.close()
    }

    override fun shouldCloseOnEsc(): Boolean = true
}