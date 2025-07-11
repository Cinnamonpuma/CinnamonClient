package code.cinnamon.hud

import code.cinnamon.hud.elements.CoordinatesHudElement
import code.cinnamon.hud.elements.FpsHudElement
import code.cinnamon.hud.elements.PingHudElement
import code.cinnamon.hud.elements.KeystrokesHudElement
import code.cinnamon.hud.elements.PacketHandlerHudElement
import code.cinnamon.hud.elements.ArmorHudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import code.cinnamon.hud.HudScreen
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.theme.CinnamonTheme
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Paths

object HudManager {
    private var editMode: Boolean = false

    fun setEditMode(enabled: Boolean) {
        editMode = enabled
        if (!editMode) {
            selectedElement = null
            if (hasUnsavedChanges) {
                saveHudConfig()
                hasUnsavedChanges = false
            }
        }
    }

    fun isEditMode(): Boolean = editMode

    private val hudElements = mutableListOf<HudElement>()
    private var selectedElement: HudElement? = null
    private var hasUnsavedChanges = false

    val packetHandlerHudElement = PacketHandlerHudElement(10f, 90f)

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val configDir = Paths.get("config", "cinnamon").toFile()
    private val configFile = File(configDir, "hud.json")

    fun registerHudElement(element: HudElement) {
        hudElements.add(element)
    }

    fun init() {
        registerHudElement(FpsHudElement(10f, 10f))
        registerHudElement(PingHudElement(10f, 30f))
        registerHudElement(CoordinatesHudElement(20f, 60f))
        registerHudElement(KeystrokesHudElement(10f, 80f))
        registerHudElement(packetHandlerHudElement)
        registerHudElement(ArmorHudElement(10f, 120f))
        loadHudConfig()
    }

    fun render(context: DrawContext, tickDelta: Float) {
        hudElements.filter { it.isEnabled }.forEach { it.renderElement(context, tickDelta) }

        val mc = MinecraftClient.getInstance()
        val currentScreen = mc.currentScreen

        if (isEditMode() && currentScreen is HudScreen) {
            renderEditModeOverlay(context)
        }
    }

    private fun renderEditModeOverlay(context: DrawContext) {
        val mc = MinecraftClient.getInstance()
        val text = Text.literal("HUD Edit Mode - ESC to exit").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val x = (mc.window.scaledWidth - mc.textRenderer.getWidth(text)) / 2
        context.drawText(mc.textRenderer, text, x, 5, 0xFFFFFF, true)
    }

    fun enableElement(name: String) {
        hudElements.find { it.getName() == name }?.isEnabled = true
    }
    fun disableElement(name: String) {
        hudElements.find { it.getName() == name }?.isEnabled = false
    }
    fun toggleElement(name: String) {
        hudElements.find { it.getName() == name }?.let { it.isEnabled = !it.isEnabled }
    }

    fun onMouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isEditMode()) return false
        hudElements.firstOrNull { it.isMouseOver(mouseX, mouseY) }?.let { element ->
            selectedElement = element
            element.startDragging(mouseX, mouseY)
            return true
        }
        return false
    }

    fun onMouseDragged(scaledMouseX: Double, scaledMouseY: Double, button: Int, scaledDeltaX: Double, scaledDeltaY: Double, screenScaledWidth: Int, screenScaledHeight: Int): Boolean {
        if (!isEditMode()) return false
        selectedElement?.let {
            it.updateDragging(scaledMouseX, scaledMouseY, screenScaledWidth, screenScaledHeight)
            hasUnsavedChanges = true
        }
        return selectedElement != null
    }

    fun onMouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!isEditMode()) return false
        selectedElement?.let {
            it.stopDragging()
            selectedElement = null
            return true
        }
        return false
    }

    fun onMouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (!isEditMode()) return false
        hudElements.firstOrNull { it.isMouseOver(mouseX, mouseY) }?.let { element ->
            element.scale += (delta * 0.1).toFloat()
            hasUnsavedChanges = true
            return true
        }
        return false
    }

    fun toggleEditMode() {
        if (editMode && hasUnsavedChanges) {
            saveHudConfig()
            hasUnsavedChanges = false
        }
        editMode = !editMode
        if (!editMode) {
            selectedElement = null
        }
    }

    fun onEditMenuClosed() {
        if (hasUnsavedChanges) {
            saveHudConfig()
            hasUnsavedChanges = false
            println("[HudManager] HUD configuration saved on menu close")
        }
    }

    fun getElements(): List<HudElement> = hudElements.toList()

    fun markChangesForSave() {
        this.hasUnsavedChanges = true
    }

    fun saveHudConfig() {
        try {
            configDir.mkdirs()
            val configs = hudElements.map { element ->
                when (element) {
                    is PacketHandlerHudElement -> element.toConfig()
                    is KeystrokesHudElement -> element.toConfig()
                    else -> HudElementConfig(
                        name = element.getName(),
                        x = element.getX(),
                        y = element.getY(),
                        scale = element.scale,
                        isEnabled = element.isEnabled,
                        textColor = element.textColor,
                        backgroundColor = element.backgroundColor,
                        textShadowEnabled = element.textShadowEnabled
                    )
                }
            }
            val jsonString = json.encodeToString(configs)
            configFile.writeText(jsonString)
            println("[HudManager] HUD config saved successfully to ${configFile.absolutePath}")
        } catch (e: Exception) {
            println("[HudManager] Failed to save HUD config: ${e.message}")
        }
    }

    fun loadHudConfig() {
        if (!configFile.exists()) {
            println("[HudManager] HUD config file not found. Loading default HUD elements.")
            return
        }

        try {
            val jsonString = configFile.readText()
            val configs = json.decodeFromString<List<HudElementConfig>>(jsonString)

            configs.forEach { config ->
                hudElements.find { it.getName() == config.name }?.let { element ->
                    when (element) {
                        is PacketHandlerHudElement -> element.applyConfig(config)
                        is KeystrokesHudElement -> element.applyConfig(config)
                        else -> {
                            element.setX(config.x)
                            element.setY(config.y)
                            element.scale = config.scale
                            element.isEnabled = config.isEnabled
                            element.textColor = config.textColor
                            element.backgroundColor = config.backgroundColor
                            element.textShadowEnabled = config.textShadowEnabled
                        }
                    }
                }
            }
            println("[HudManager] HUD config loaded successfully from ${configFile.absolutePath}")
        } catch (e: Exception) {
            println("[HudManager] Failed to load HUD config: ${e.message}. Loading default HUD elements.")
        }
    }

    fun handleGlobalMouseClick(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (isEditMode()) {
            return false
        }

        val mc = MinecraftClient.getInstance()

        for (element in hudElements.reversed()) {
            if (element.isEnabled && element is Element) {
                if (element.mouseClicked(mouseX, mouseY, button)) {
                    return true
                }
            }
        }
        return false
    }
}