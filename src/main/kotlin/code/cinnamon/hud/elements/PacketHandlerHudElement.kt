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

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices) // Apply element's own scale

        // Calculate mouse position in local unscaled coordinates for hover detection
        var localUnscaledMouseX_forHover: Double = -1.0 // Default to off-element
        var localUnscaledMouseY_forHover: Double = -1.0

        if (this.scale != 0.0f) {
            val mcGuiScale = client.window.scaleFactor
            // Screen mouse coordinates in Minecraft's virtual pixels (already affected by mcGuiScale)
            val screenMouseX = client.mouse.x / mcGuiScale
            val screenMouseY = client.mouse.y / mcGuiScale

            // Mouse position relative to this element's scaled top-left
            val relativeScaledMouseX_forHover = screenMouseX - getX()
            val relativeScaledMouseY_forHover = screenMouseY - getY()

            // Convert to local unscaled coordinates
            localUnscaledMouseX_forHover = relativeScaledMouseX_forHover / this.scale
            localUnscaledMouseY_forHover = relativeScaledMouseY_forHover / this.scale
        }

        // All drawing from here is relative to (0,0) for this element, using its base unscaled dimensions.
        val unscaledButtonMargin = baseButtonMargin.toFloat()
        val unscaledButtonHeight = baseButtonHeight.toFloat()
        // getWidth() provides the base, unscaled width of the element.
        // unscaledContentWidth is the base width available for buttons inside margins.
        val unscaledContentWidth = getWidth().toFloat() - 2 * unscaledButtonMargin

        var currentUnscaledButtonTopY = unscaledButtonMargin

        for (btn in internalButtons) {
            val btnText = createStyledText(btn.text())
            val btnX = unscaledButtonMargin // Button's X position in unscaled local coords
            val btnY = currentUnscaledButtonTopY // Button's Y position in unscaled local coords

            var isMouseOverButton = false
            if (this.scale != 0.0f) { // Only check hover if scale is valid
                isMouseOverButton = localUnscaledMouseX_forHover >= btnX &&
                        localUnscaledMouseX_forHover < btnX + unscaledContentWidth &&
                        localUnscaledMouseY_forHover >= btnY &&
                        localUnscaledMouseY_forHover < btnY + unscaledButtonHeight
            }

            drawCustomButton(context, btnX, btnY, unscaledContentWidth, unscaledButtonHeight, btnText, isMouseOverButton)

            currentUnscaledButtonTopY += unscaledButtonHeight + unscaledButtonMargin
        }

        if (HudManager.isEditMode()) {
            // Draw border around the base getWidth()/getHeight()
            // These are unscaled dimensions, will be scaled by the matrix.
            context.drawBorder(0, 0, getWidth(), getHeight(), 0xFFFF0000.toInt())
        }
        context.matrices.popMatrix()
    }

    private fun drawCustomButton(
        context: DrawContext,
        x: Float, y: Float, width: Float, height: Float, // Base (unscaled) dimensions/positions
        text: Text, hovered: Boolean
    ) {
        // All drawing of the button itself is using base dimensions.
        // The element's overall 'scale' (from this.scale) is already applied to the context.matrixStack.
        GraphicsUtils.drawFilledRoundedRect(
            context, x, y, width, height,
            BUTTON_CORNER_RADIUS_BASE, // Use base radius, it will be scaled by matrix
            buttonColor
        )

        val currentOutlineColor = if (hovered) this.buttonHoverColor else this.buttonOutlineColor
        GraphicsUtils.drawRoundedRectBorder(
            context, x, y, width, height,
            BUTTON_CORNER_RADIUS_BASE, // Use base radius, it will be scaled by matrix
            currentOutlineColor
        )

        // Text is drawn using unscaled coordinates. The matrix will scale it.
        val tr = client.textRenderer
        val textWidth = tr.getWidth(text) // This should give unscaled text width
        val fontHeight = tr.fontHeight // This is unscaled font height

        val textX = x + (width - textWidth) / 2f
        val textY = y + (height - fontHeight) / 2f

        context.drawText(tr, text, textX.toInt(), textY.toInt(), buttonTextColor, buttonTextShadowEnabled)
    }

    override fun getWidth(): Int =
        internalButtons.maxOfOrNull { client.textRenderer.getWidth(createStyledText(it.text())) }?.plus(baseButtonMargin * 4) ?: 100

    override fun getHeight(): Int =
        internalButtons.size * (baseButtonHeight + baseButtonMargin) + baseButtonMargin

    override fun getName(): String = "PacketHandler"

    // mouseX, mouseY are in CinnamonScreen's scaled coordinate system (i.e., after Minecraft's global GUI scale).
    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender() || HudManager.isEditMode() || !isEnabled) return false

        // Element's own scale factor. Avoid division by zero.
        val currentElementScale = this.scale
        if (currentElementScale == 0.0f) return false

        // Convert mouseX, mouseY (which are in CinnamonScreen-scaled system, relative to screen top-left)
        // to coordinates relative to this element's scaled top-left corner.
        val relativeScaledMouseX = mouseX - getX()
        val relativeScaledMouseY = mouseY - getY()

        // Transform to this element's local, unscaled coordinate system.
        val localUnscaledMouseX = relativeScaledMouseX / currentElementScale
        val localUnscaledMouseY = relativeScaledMouseY / currentElementScale

        // Now, localUnscaledMouseX/Y are relative to the element's unscaled top-left (0,0).
        // All button dimensions and positions will be in this local unscaled space.

        val unscaledButtonMargin = baseButtonMargin.toFloat()
        val unscaledButtonHeight = baseButtonHeight.toFloat()
        // getWidth() returns the base, unscaled width of the element.
        // unscaledContentWidth is the base width available for buttons inside margins.
        val unscaledContentWidth = getWidth().toFloat() - 2 * unscaledButtonMargin

        var currentUnscaledButtonTopY = unscaledButtonMargin

        for (btn in internalButtons) {
            val unscaledButtonLeftX = unscaledButtonMargin

            if (localUnscaledMouseX >= unscaledButtonLeftX && localUnscaledMouseX < unscaledButtonLeftX + unscaledContentWidth &&
                localUnscaledMouseY >= currentUnscaledButtonTopY && localUnscaledMouseY < currentUnscaledButtonTopY + unscaledButtonHeight) {
                btn.action()
                return true
            }
            currentUnscaledButtonTopY += unscaledButtonHeight + unscaledButtonMargin
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

    public override fun isMouseOver(scaledMouseX: Double, scaledMouseY: Double): Boolean { // Match super
        return super<HudElement>.isMouseOver(scaledMouseX, scaledMouseY)
    }
}