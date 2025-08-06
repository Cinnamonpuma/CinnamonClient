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
        private const val BUTTON_CORNER_RADIUS_BASE = 4f
    }

    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val baseButtonHeight = 20
    private val baseButtonMargin = 2

    var buttonColor: Int = 20987968
    var buttonHoverColor: Int = -6315615
    var buttonOutlineColor: Int = 2126605840.toInt()

    init {
        settings.add(code.cinnamon.modules.ColorSetting("Button Color", buttonColor) { value -> buttonColor = value })
        settings.add(code.cinnamon.modules.ColorSetting("Button Hover Color", buttonHoverColor) { value -> buttonHoverColor = value })
        settings.add(code.cinnamon.modules.ColorSetting("Button Outline Color", buttonOutlineColor) { value -> buttonOutlineColor = value })
    }

    private fun createStyledText(text: String): Text =
        Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

    private data class HudButtonInternal(
        var text: () -> String,
        val action: () -> Unit
    )

    private val internalButtons = listOf(
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
        HudButtonInternal({ "Chat Delay: ${PacketHandlerAPI.isGuiPacketBlocking()}" }) {
            PacketHandlerAPI.setGuiPacketBlocking(!PacketHandlerAPI.isGuiPacketBlocking())
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
        config.buttonHoverColor?.let { this.buttonHoverColor = it }
        config.buttonOutlineColor?.let { this.buttonOutlineColor = it }
        this.setX(config.x)
        this.setY(config.y)
        this.scale = config.scale
        this.isEnabled = config.isEnabled
        this.textColor = config.textColor
        this.backgroundColor = config.backgroundColor
        this.textShadowEnabled = config.textShadowEnabled
    }

    fun toConfig(): HudElementConfig {
        return HudElementConfig(
            name = getName(),
            x = getX(),
            y = getY(),
            scale = scale,
            isEnabled = isEnabled,
            textColor = textColor,
            backgroundColor = backgroundColor,
            textShadowEnabled = textShadowEnabled,
            buttonColor = buttonColor,
            buttonHoverColor = this.buttonHoverColor,
            buttonOutlineColor = this.buttonOutlineColor
        )
    }

    private fun shouldRender(): Boolean {
        val screen = client.currentScreen
        return code.cinnamon.SharedVariables.enabled &&
                (HudManager.isEditMode() || (screen != null && screen !is net.minecraft.client.gui.screen.GameMenuScreen))
    }

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!shouldRender() || !isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        var localUnscaledMouseX_forHover: Double = -1.0
        var localUnscaledMouseY_forHover: Double = -1.0

        if (this.scale != 0.0f) {
            val scaledMouseX = getCurrentMouseX()
            val scaledMouseY = getCurrentMouseY()

            val relativeScaledMouseX_forHover = scaledMouseX - getX()
            val relativeScaledMouseY_forHover = scaledMouseY - getY()

            localUnscaledMouseX_forHover = relativeScaledMouseX_forHover / this.scale
            localUnscaledMouseY_forHover = relativeScaledMouseY_forHover / this.scale
        }

        val unscaledButtonMargin = baseButtonMargin.toFloat()
        val unscaledButtonHeight = baseButtonHeight.toFloat()
        val unscaledContentWidth = getWidth().toFloat() - 2 * unscaledButtonMargin

        var currentUnscaledButtonTopY = unscaledButtonMargin

        for (btn in internalButtons) {
            val btnText = createStyledText(btn.text())
            val btnX = unscaledButtonMargin
            val btnY = currentUnscaledButtonTopY

            var isMouseOverButton = false
            if (this.scale != 0.0f) {
                isMouseOverButton = localUnscaledMouseX_forHover >= btnX &&
                        localUnscaledMouseX_forHover < btnX + unscaledContentWidth &&
                        localUnscaledMouseY_forHover >= btnY &&
                        localUnscaledMouseY_forHover < btnY + unscaledButtonHeight
            }

            drawCustomButton(context, btnX, btnY, unscaledContentWidth, unscaledButtonHeight, btnText, isMouseOverButton)

            currentUnscaledButtonTopY += unscaledButtonHeight + unscaledButtonMargin
        }

        if (HudManager.isEditMode()) {
            context.drawBorder(0, 0, getWidth(), getHeight(), 0xFFFF0000.toInt())
        }
        context.matrices.popMatrix()
    }

    private fun drawCustomButton(
        context: DrawContext,
        x: Float, y: Float, width: Float, height: Float,
        text: Text, hovered: Boolean
    ) {
        GraphicsUtils.drawFilledRoundedRect(
            context, x, y, width, height,
            BUTTON_CORNER_RADIUS_BASE,
            buttonColor
        )

        val currentOutlineColor = if (hovered) this.buttonHoverColor else this.buttonOutlineColor
        GraphicsUtils.drawRoundedRectBorder(
            context, x, y, width, height,
            BUTTON_CORNER_RADIUS_BASE,
            currentOutlineColor
        )

        val tr = client.textRenderer
        val textWidth = tr.getWidth(text)
        val fontHeight = tr.fontHeight

        val textX = x + (width - textWidth) / 2f
        val textY = y + (height - fontHeight) / 2f

        context.drawText(tr, text, textX.toInt(), textY.toInt(), textColor, textShadowEnabled)
    }

    override fun getWidth(): Int =
        internalButtons.maxOfOrNull { client.textRenderer.getWidth(createStyledText(it.text())) }?.plus(baseButtonMargin * 4) ?: 100

    override fun getHeight(): Int =
        internalButtons.size * (baseButtonHeight + baseButtonMargin) + baseButtonMargin

    override fun getName(): String = "PacketHandler"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender() || HudManager.isEditMode() || !isEnabled) return false

        val currentElementScale = this.scale
        if (currentElementScale == 0.0f) return false

        val relativeScaledMouseX = mouseX - getX()
        val relativeScaledMouseY = mouseY - getY()

        val localUnscaledMouseX = relativeScaledMouseX / currentElementScale
        val localUnscaledMouseY = relativeScaledMouseY / currentElementScale

        val unscaledButtonMargin = baseButtonMargin.toFloat()
        val unscaledButtonHeight = baseButtonHeight.toFloat()
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

    public override fun isMouseOver(scaledMouseX: Double, scaledMouseY: Double): Boolean {
        return super<HudElement>.isMouseOver(scaledMouseX, scaledMouseY)
    }
}