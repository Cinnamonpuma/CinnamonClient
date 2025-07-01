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
    private val buttonHeight = 20
    private val buttonMargin = 2

    var buttonColor: Int = 0xFF222222.toInt()
    var buttonTextColor: Int = 0xFFFFFFFF.toInt()
    var buttonTextShadowEnabled: Boolean = true
    var buttonHoverColor: Int = 0xFF00D0FF.toInt() 
    var buttonOutlineColor: Int = CinnamonTheme.buttonOutlineColor 

    private fun createStyledText(text: String): Text =
        Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))

    private data class HudButton(
        var text: () -> String,
        val action: () -> Unit
    )

    private val buttons = listOf(
        HudButton({ "Close without packet" }) { client.setScreen(null) },
        HudButton({ "De-sync" }) {
            client.player?.let { player ->
                client.networkHandler?.sendPacket(CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId))
            }
        },
        HudButton({ "Send packets: ${code.cinnamon.SharedVariables.packetSendingEnabled}" }) {
            code.cinnamon.SharedVariables.packetSendingEnabled = !code.cinnamon.SharedVariables.packetSendingEnabled
        },
        HudButton({ "Delay packets: ${PacketHandlerAPI.isPacketBlocking()}" }) {
            if (PacketHandlerAPI.isPacketBlocking()) {
                PacketHandlerAPI.stopPacketBlocking()
                client.networkHandler?.let { PacketHandlerAPI.flushPacketQueue() }
            } else {
                PacketHandlerAPI.startPacketBlocking()
            }
        },
        HudButton({ "Save GUI" }) {
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
        HudButton({ "Disconnect and send packets" }) {
            if (PacketHandlerAPI.isPacketBlocking()) PacketHandlerAPI.stopPacketBlocking()
            client.networkHandler?.let { handler ->
                PacketHandlerAPI.flushPacketQueue()
                handler.connection.disconnect(Text.literal("Disconnecting (CINNAMON)"))
            }
        },
        HudButton({ "Copy GUI Title JSON" }) {
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
            buttonTextColor = buttonTextColor,
            buttonTextShadowEnabled = buttonTextShadowEnabled,
            buttonHoverColor = this.buttonHoverColor,
            buttonOutlineColor = this.buttonOutlineColor
        )
    }

    private fun shouldRender(): Boolean {
        val screen = client.currentScreen
        return code.cinnamon.SharedVariables.enabled &&
               (HudManager.isEditMode() ||
                (screen != null && screen !is net.minecraft.client.gui.screen.GameMenuScreen))
    }

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!shouldRender() || !isEnabled) return

        val hudX = getX().toInt() 
        val hudY = getY().toInt() 
        val scaleFactor = client.window.scaleFactor
        val guiMouseX = (client.mouse.x / scaleFactor).toInt()
        val guiMouseY = (client.mouse.y / scaleFactor).toInt()

        val scaledButtonHeight = (buttonHeight * scale).toInt()
        val scaledButtonMargin = (buttonMargin * scale).toInt()
        val scaledContentWidth = (getWidth() * scale).toInt() - 2 * scaledButtonMargin

        var currentY = hudY + scaledButtonMargin 
        for ((index, btn) in buttons.withIndex()) {
            val btnText = createStyledText(btn.text())
            val bx = hudX + scaledButtonMargin 
            val by = currentY                 
            val bw = scaledContentWidth       
            val bh = scaledButtonHeight       

            
            val isMouseOverButton = guiMouseX in bx until (bx + bw) && guiMouseY in by until (by + bh)
            drawCustomButton(context, bx, by, bw, bh, btnText, isMouseOverButton, scale)

            currentY += scaledButtonHeight + scaledButtonMargin
        }

        if (HudManager.isEditMode()) {
            context.drawBorder(
                hudX, hudY, (getWidth() * scale).toInt(), (getHeight() * scale).toInt(), 0xFFFF0000.toInt()
            )
        }
    }

    private fun drawCustomButton(
        context: DrawContext,
        x: Int, y: Int, width: Int, height: Int,
        text: Text, hovered: Boolean, currentScale: Float
    ) {
        val scaledRadius = (BUTTON_CORNER_RADIUS_BASE * currentScale).coerceAtLeast(1f)

        GraphicsUtils.drawFilledRoundedRect(
            context,
            x.toFloat(), y.toFloat(),
            width.toFloat(), height.toFloat(),
            scaledRadius,
            buttonColor
        )

        val currentOutlineColor = if (hovered) {
            this.buttonHoverColor 
        } else {
            this.buttonOutlineColor
        }

        GraphicsUtils.drawRoundedRectBorder(
            context,
            x.toFloat(), y.toFloat(),
            width.toFloat(), height.toFloat(),
            scaledRadius,
            currentOutlineColor
        )

        val matrices = context.matrices
        matrices.push()
        
        matrices.translate(x.toFloat(), y.toFloat(), 0f)
        
        matrices.scale(currentScale, currentScale, 1.0f)

        val tr = client.textRenderer
        val unscaledButtonWidth = (width / currentScale)
        val unscaledButtonHeight = (height / currentScale)
        val unscaledTextWidth = tr.getWidth(text)
        val unscaledFontHeight = tr.fontHeight

        val textXInButtonUnscaled = ((unscaledButtonWidth - unscaledTextWidth) / 2).toInt()
        val textYInButtonUnscaled = ((unscaledButtonHeight - unscaledFontHeight) / 2).toInt()

        context.drawText(tr, text, textXInButtonUnscaled, textYInButtonUnscaled, buttonTextColor, buttonTextShadowEnabled)
        matrices.pop()
    }

    override fun getWidth(): Int =
        buttons.maxOfOrNull { client.textRenderer.getWidth(createStyledText(it.text())) }?.plus(buttonMargin * 4) ?: 100

    override fun getHeight(): Int =
        buttons.size * (buttonHeight + buttonMargin) + buttonMargin

    override fun getName(): String = "PacketHandler"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false 

        if (HudManager.isEditMode()) {
            return false
        }


        if (!isMouseOver(mouseX, mouseY)) return false

        val scaledButtonHeight = (buttonHeight * scale).toInt()
        val scaledButtonMargin = (buttonMargin * scale).toInt()
        
        
        val elementX = getX().toInt() 
        val elementY = getY().toInt() 

        
        val buttonsAreaX = elementX + scaledButtonMargin 
        val buttonsAreaWidth = (getWidth() * scale).toInt() - 2 * scaledButtonMargin
        
        var currentButtonY = elementY + scaledButtonMargin 
        
        for (btn in buttons) {
            val buttonTop = currentButtonY
            val buttonBottom = currentButtonY + scaledButtonHeight
            val buttonLeft = buttonsAreaX
            val buttonRight = buttonsAreaX + buttonsAreaWidth

            if (mouseX >= buttonLeft && mouseX < buttonRight && 
                mouseY >= buttonTop && mouseY < buttonBottom) {
                btn.action()
                return true
            }
            currentButtonY += scaledButtonHeight + scaledButtonMargin
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