package code.cinnamon.hud

import code.cinnamon.hud.elements.FpsHudElement
import code.cinnamon.hud.elements.PingHudElement
import code.cinnamon.hud.elements.KeystrokesHudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import code.cinnamon.hud.HudScreen

object HudManager {
    private val hudElements = mutableListOf<HudElement>()
    var isEditMode = false
        private set
    private var selectedElement: HudElement? = null
    
    fun init() {
        hudElements.apply {
            add(FpsHudElement(10f, 10f))
            add(PingHudElement(10f, 30f))
            add(KeystrokesHudElement(10f, 60f))
        }
    }
    
    fun render(context: DrawContext, tickDelta: Float) {
        hudElements.filter { it.isEnabled }.forEach { it.render(context, tickDelta) }
        
        // Get the current screen
        val mc = MinecraftClient.getInstance()
        val currentScreen = mc.currentScreen
        
        // Only render edit mode overlay if isEditMode is true AND current screen is HudScreen
        if (isEditMode && currentScreen is HudScreen) {
            renderEditModeOverlay(context)
        }
    }
    
    private fun renderEditModeOverlay(context: DrawContext) {
        val mc = MinecraftClient.getInstance()
        val text = Text.literal("HUD Edit Mode - ESC to exit").setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))

        val x = (mc.window.scaledWidth - mc.textRenderer.getWidth(text)) / 2
        context.drawText(mc.textRenderer, text, x, 5, 0xFFFFFF, true)
    }
    
    fun onMouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isEditMode) return false
        
        hudElements.firstOrNull { it.isMouseOver(mouseX, mouseY) }?.let { element ->
            selectedElement = element
            element.startDragging(mouseX, mouseY)
            return true
        }
        return false
    }
    
    fun onMouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!isEditMode) return false
        
        selectedElement?.updateDragging(mouseX, mouseY)
        return selectedElement != null
    }
    
    fun onMouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isEditMode) return false
        
        selectedElement?.let {
            it.stopDragging()
            selectedElement = null
            return true
        }
        return false
    }
    
    fun onMouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (!isEditMode) return false
        
        hudElements.firstOrNull { it.isMouseOver(mouseX, mouseY) }?.let { element ->
            element.scale += (delta * 0.1).toFloat()
            return true
        }
        return false
    }
    
    fun toggleEditMode() {
        isEditMode = !isEditMode
        if (!isEditMode) {
            selectedElement = null
        }
    }
    
    fun getElements(): List<HudElement> = hudElements.toList()
}