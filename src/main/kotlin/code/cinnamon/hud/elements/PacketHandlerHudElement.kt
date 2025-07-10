package code.cinnamon.hud.elements

import code.cinnamon.hud.HudElement
import code.cinnamon.hud.HudElementConfig
import code.cinnamon.gui.utils.GraphicsUtils
import code.cinnamon.util.PacketHandlerAPI
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.util.Identifier

class PacketHandlerHudElement(initialX: Float, initialY: Float) : HudElement(initialX, initialY), Element {

    companion object {
        private const val BUTTON_CORNER_RADIUS_BASE = 4f // Base radius
    }

    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val baseButtonHeight = 20 // Base height of a button
    private val baseButtonMargin = 2  // Base margin around/between buttons

    // Use new theme defaults
    var buttonColor: Int = 20987968
    var buttonTextColor: Int = 0xFFFFFFFF.toInt()
    var buttonTextShadowEnabled: Boolean = false
    var buttonHoverColor: Int = -6315615
    var buttonOutlineColor: Int = 2126605840.toInt()

    private fun createStyledText(text: String): Text =
        Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

    private data class HudButtonInternal( // Renamed to avoid conflict if HudButton is used elsewhere
        var text: () -> String,
        val action: () -> Unit
    )

    private val internalButtons = listOf( // Renamed
        HudButtonInternal({ "Close without packet" }) { client.setScreen(null) },
        HudButtonInternal({ "De-sync" }) {
            client.player?.let { player ->
                client.networkHandler?.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
            }
        },
        HudButtonInternal({ "Send packets: ${code.cinnamon.SharedVariables.packetSendingEnabled}" }) {
            code.cinnamon.SharedVariables.packetSendingEnabled = !code.cinnamon.SharedVariables.packetSendingEnabled
        },
        HudButtonInternal({ "Delay packets: ${PacketHandlerAPI.isPacketBlocking()}" }) {
            if (PacketHandlerAPI.isPacketBlocking()) {
                PacketHandlerAPI.stopPacketBlocking()
                client.networkHandler?.let { PacketHandlerAPI.flushPacketQueue() }
            } else {
                PacketHandlerAPI.startPacketBlocking()
            }
        },
        HudButtonInternal({ "Save GUI" }) {
            client.player?.let { player ->
                try {
                    val storedScreenField = code.cinnamon.SharedVariables::class.java.getDeclaredField("storedScreen")
                    val storedScreenHandlerField = code.cinnamon.SharedVariables::class.java.getDeclaredField("storedScreenHandler")
                    storedScreenField.isAccessible = true
                    storedScreenHandlerField.isAccessible = true
                    storedScreenField.set(null, client.currentScreen)
                    storedScreenHandlerField.set(null, player.currentScreenHandler)
                } catch (e: Exception) { e.printStackTrace() }
            }
        },
        HudButtonInternal({ "Disconnect and send packets" }) {
            if (PacketHandlerAPI.isPacketBlocking()) PacketHandlerAPI.stopPacketBlocking()
            client.networkHandler?.let { handler ->
                PacketHandlerAPI.flushPacketQueue()
                handler.connection.disconnect(Text.literal("Disconnecting (CINNAMON)"))
            }
        },
        HudButtonInternal({ "Copy GUI Title JSON" }) {
            try {
                val screen = client.currentScreen ?: throw IllegalStateException("No current screen")
                client.keyboard.setClipboard(screen.title.string)
            } catch (e: Exception) { e.printStackTrace() }
        }
    )


    fun applyConfig(config: HudElementConfig) {
        if (config.buttonColor != null) buttonColor = config.buttonColor
        if (config.buttonTextColor != null) buttonTextColor = config.buttonTextColor
        if (config.buttonTextShadowEnabled != null) buttonTextShadowEnabled = config.buttonTextShadowEnabled
        config.buttonHoverColor?.let { this.buttonHoverColor = it }
        config.buttonOutlineColor?.let { this.buttonOutlineColor = it }
        this.setX(config.x)
        this.setY(config.y)
        this.scale = config.scale // Element's overall scale
        this.isEnabled = config.isEnabled
        this.textColor = config.textColor // Not used by this element directly, but part of HudElement
        this.backgroundColor = config.backgroundColor // Not used by this element directly
        this.textShadowEnabled = config.textShadowEnabled // Not used by this element directly
    }

    fun toConfig(): HudElementConfig {
        return HudElementConfig(
            name = getName(),
            x = getX(),
            y = getY(),
            scale = scale, // Element's overall scale
            isEnabled = isEnabled,
            textColor = textColor,
            backgroundColor = backgroundColor,
            textShadowEnabled = textShadowEnabled,
            buttonColor = buttonColor,
            buttonTextColor = buttonTextColor,
            buttonTextShadowEnabled = buttonTextShadowEnabled,
            buttonHoverColor = this.buttonHoverColor,
            buttonOutlineColor = this.buttonOutlineColor
        )
    }

    private fun shouldRender(): Boolean {
        val screen = client.currentScreen
        // Render if Cinnamon is enabled AND (edit mode is active OR current screen is not GameMenuScreen)
        return code.cinnamon.SharedVariables.enabled &&
                (HudManager.isEditMode() || (screen != null && screen !is net.minecraft.client.gui.screen.GameMenuScreen))
    }

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!shouldRender() || !isEnabled) return

        // HudManager has applied translation to getX/getY and scaling by element.scale.
        // All drawing is relative to (0,0) using base dimensions.

        // Mouse coordinates for hover check, relative to this element's (0,0) and NOT scaled by element.scale yet.
        // This requires mouseX, mouseY to be transformed by HudManager if they are screen coords.
        // However, for rendering, mouse coords are only for hover effect.
        // The actual click is handled in mouseClicked with already scaled coords.
        val mcScaleFactor = client.window.scaleFactor
        val screenMouseX = client.mouse.x / mcScaleFactor
        val screenMouseY = client.mouse.y / mcScaleFactor

        // Convert screen mouse to this element's local, unscaled coordinate system
        // This is complex because getX/Y are already scaled by CinnamonScreen's logic.
        // For simplicity in renderElement, we'll assume mouse coords for hover are tricky to get perfectly
        // without more context from HudManager about how it passes mouse coords for rendering.
        // The click logic in mouseClicked is more reliable as it uses CinnamonScreen-scaled coords.
        // For visual hover, we can approximate or rely on HudScreen providing relative mouse.
        // Let's assume for now mouseX, mouseY are NOT passed to renderElement for hover.

        var currentRelativeY = baseButtonMargin.toFloat()
        for (btn in internalButtons) {
            val btnText = createStyledText(btn.text())
            val btnX = baseButtonMargin.toFloat()
            val btnY = currentRelativeY
            // getWidth() is base width of the whole element. contentWidth is for button area.
            val contentWidth = getWidth() - 2 * baseButtonMargin
            val btnHeight = baseButtonHeight.toFloat()

            // Simplified: isMouseOverButton check is complex here due to nested scaling.
            // Rely on mouseClicked for actual interaction. Hover effect might be slightly off if not careful.
            // For now, pass false for hovered, or implement a more robust local mouse check.
            val isMouseOverButton = false // Placeholder: mouseClicked handles actual clicks accurately.
            // To get proper hover for rendering, HudManager would need to pass
            // mouse coordinates relative to the unscaled element at (0,0).

            drawCustomButton(context, btnX, btnY, contentWidth.toFloat(), btnHeight, btnText, isMouseOverButton)

            currentRelativeY += baseButtonHeight + baseButtonMargin
        }

        if (HudManager.isEditMode()) {
            // Draw border around the base getWidth()/getHeight()
            context.drawBorder(0, 0, getWidth(), getHeight(), 0xFFFF0000.toInt())
        }
    }

    private fun drawCustomButton(
        context: DrawContext,
        x: Float, y: Float, width: Float, height: Float, // Base dimensions/positions
        text: Text, hovered: Boolean
    ) {
        // All drawing of the button itself is using base dimensions.
        // The element's overall 'scale' is applied by HudManager.
        GraphicsUtils.drawFilledRoundedRect(
            context, x, y, width, height,
            BUTTON_CORNER_RADIUS_BASE, // Use base radius
            buttonColor
        )

        val currentOutlineColor = if (hovered) this.buttonHoverColor else this.buttonOutlineColor
        GraphicsUtils.drawRoundedRectBorder(
            context, x, y, width, height,
            BUTTON_CORNER_RADIUS_BASE, // Use base radius
            currentOutlineColor
        )

        // Text is drawn unscaled. HudManager's scaling of the element will scale the text.
        val tr = client.textRenderer
        val textWidth = tr.getWidth(text)
        val fontHeight = tr.fontHeight

        val textX = x + (width - textWidth) / 2f
        val textY = y + (height - fontHeight) / 2f

        context.drawText(tr, text, textX.toInt(), textY.toInt(), buttonTextColor, buttonTextShadowEnabled)
    }

    override fun getWidth(): Int =
        internalButtons.maxOfOrNull { client.textRenderer.getWidth(createStyledText(it.text())) }?.plus(baseButtonMargin * 4) ?: 100

    override fun getHeight(): Int =
        internalButtons.size * (baseButtonHeight + baseButtonMargin) + baseButtonMargin

    override fun getName(): String = "PacketHandler"

    // mouseX, mouseY are in CinnamonScreen's scaled coordinate system.
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender() || HudManager.isEditMode() || !isEnabled) return false

        // Convert mouseX, mouseY (which are in CinnamonScreen-scaled system, relative to screen top-left)
        // to coordinates relative to this element's top-left corner (also in CinnamonScreen-scaled system).
        val localMouseX = mouseX - getX()
        val localMouseY = mouseY - getY()

        // Now, localMouseX/Y are relative to the element's scaled top-left.
        // We need to check against button positions which are also effectively in this scaled space
        // (base_pos * element.scale).

        val currentElementScale = this.scale
        val actualButtonMargin = baseButtonMargin * currentElementScale
        val actualButtonHeight = baseButtonHeight * currentElementScale

        // The content width of the button area, in element's scaled units
        val actualContentWidth = (getWidth() - 2 * baseButtonMargin) * currentElementScale

        var currentButtonTopY = actualButtonMargin // Relative to element's scaled top

        for (btn in internalButtons) {
            val buttonLeftX = actualButtonMargin // Relative to element's scaled left

            if (localMouseX >= buttonLeftX && localMouseX < buttonLeftX + actualContentWidth &&
                localMouseY >= currentButtonTopY && localMouseY < currentButtonTopY + actualButtonHeight) {
                btn.action()
                return true
            }
            currentButtonTopY += actualButtonHeight + actualButtonMargin
        }
        return false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!shouldRender()) return false
        return if (HudManager.isEditMode()) {
            false
        } else {
            false
        }
    }
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false
        return if (HudManager.isEditMode()) {
            false
        } else {
            false
        }
    }
    override fun mouseMoved(mouseX: Double, mouseY: Double) {}
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = false
    override fun setFocused(focused: Boolean) {}
    override fun isFocused(): Boolean = false

    public override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        return super<HudElement>.isMouseOver(mouseX, mouseY)
    }
}