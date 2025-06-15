package code.cinnamon.hud.elements

import code.cinnamon.hud.HudElement
import code.cinnamon.hud.HudElementConfig
import code.cinnamon.util.PacketHandlerAPI
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.hud.HudManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.util.Identifier

class PacketHandlerHudElement(initialX: Float, initialY: Float) : HudElement(initialX, initialY), Element {

    private val client: MinecraftClient = MinecraftClient.getInstance()
    private val buttonHeight = 20
    private val buttonMargin = 2
    private val CINNA_FONT: Identifier = CinnamonScreen.CINNA_FONT

    var buttonColor: Int = 0xFF222222.toInt()
    var buttonTextColor: Int = 0xFFFFFFFF.toInt()
    var buttonTextShadowEnabled: Boolean = true

    private fun createStyledText(text: String): Text =
        Text.literal(text).setStyle(Style.EMPTY.withFont(CINNA_FONT))

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
            buttonTextShadowEnabled = buttonTextShadowEnabled
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
        val mouseX = client.mouse.x.toInt()
        val mouseY = client.mouse.y.toInt()

        var currentY = hudY + buttonMargin
        for ((index, btn) in buttons.withIndex()) {
            val btnText = createStyledText(btn.text())
            val bx = hudX + buttonMargin
            val by = currentY
            val bw = getWidth() - 2 * buttonMargin
            val bh = buttonHeight

            val hovered = mouseX in bx until (bx + bw) && mouseY in by until (by + bh)
            drawCustomButton(context, bx, by, bw, bh, btnText, hovered)

            currentY += buttonHeight + buttonMargin
        }

        if (HudManager.isEditMode()) {
            context.drawBorder(
                hudX, hudY, getWidth(), getHeight(), 0xFFFF0000.toInt()
            )
        }
    }

    private fun drawCustomButton(
        context: DrawContext,
        x: Int, y: Int, width: Int, height: Int,
        text: Text, hovered: Boolean
    ) {
        context.fill(x, y, x + width, y + height, buttonColor)
        if (hovered) context.drawBorder(x, y, width, height, 0xFF00D0FF.toInt())
        val tr = client.textRenderer
        val textWidth = tr.getWidth(text)
        val textX = x + (width - textWidth) / 2
        val textY = y + (height - tr.fontHeight) / 2
        context.drawText(tr, text, textX, textY, buttonTextColor, buttonTextShadowEnabled)
    }

    override fun getWidth(): Int =
        buttons.maxOfOrNull { client.textRenderer.getWidth(createStyledText(it.text())) }?.plus(buttonMargin * 4) ?: 100

    override fun getHeight(): Int =
        buttons.size * (buttonHeight + buttonMargin) + buttonMargin

    override fun getName(): String = "PacketHandler"

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender() || !isMouseOver(mouseX, mouseY)) return false
        if (button == 1 && HudManager.isEditMode()) {
            startDragging(mouseX, mouseY)
            return true
        }
        val hudX = getX().toInt() + buttonMargin
        val hudY = getY().toInt() + buttonMargin
        val bw = getWidth() - 2 * buttonMargin
        for ((i, btn) in buttons.withIndex()) {
            val by = hudY + i * (buttonHeight + buttonMargin)
            if (mouseX in hudX.toDouble()..(hudX + bw).toDouble() &&
                mouseY in by.toDouble()..(by + buttonHeight).toDouble()
            ) {
                btn.action()
                return true
            }
        }
        return false
    }
    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!shouldRender()) return false
        if (HudManager.isEditMode()) {
            updateDragging(mouseX, mouseY)
            return true
        }
        return false
    }
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!shouldRender()) return false
        if (HudManager.isEditMode()) stopDragging()
        return false
    }
    override fun mouseMoved(mouseX: Double, mouseY: Double) {}
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean = false
    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = false
    override fun charTyped(chr: Char, modifiers: Int): Boolean = false
    override fun setFocused(focused: Boolean) {}
    override fun isFocused(): Boolean = false

    override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
        val hudX = getX().toDouble()
        val hudY = getY().toDouble()
        val w = getWidth().toDouble()
        val h = getHeight().toDouble()
        return mouseX >= hudX && mouseX <= hudX + w && mouseY >= hudY && mouseY <= hudY + h
    }
}