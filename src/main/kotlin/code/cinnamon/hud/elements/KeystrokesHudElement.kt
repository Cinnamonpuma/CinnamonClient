package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.gui.utils.GraphicsUtils
import code.cinnamon.hud.HudElement
import code.cinnamon.hud.HudElementConfig
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import kotlin.math.*

class KeystrokesHudElement(x: Float, y: Float) : HudElement(x, y) {
    var keypressedTextColor: Int = 0xFFFFFF
    var keypressedBackgroundColor: Int = 0xFFFFFF

    private val mc = MinecraftClient.getInstance()
    private val keySize = 32
    private val spacing = 4
    private val cornerRadius = 4f

    private var wPressed = false
    private var aPressed = false
    private var sPressed = false
    private var dPressed = false

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        updateKeyStates()

        val wX = keySize + spacing
        drawKey(context, "W", wX, 0, wPressed)
        drawKey(context, "A", 0, keySize + spacing, aPressed)
        drawKey(context, "S", keySize + spacing, keySize + spacing, sPressed)
        drawKey(context, "D", (keySize + spacing) * 2, keySize + spacing, dPressed)
        context.matrices.popMatrix()
    }

    private fun updateKeyStates() {
        wPressed = mc.options.forwardKey.isPressed
        aPressed = mc.options.leftKey.isPressed
        sPressed = mc.options.backKey.isPressed
        dPressed = mc.options.rightKey.isPressed
    }

    private fun drawKey(context: DrawContext, key: String, x: Int, y: Int, pressed: Boolean) {
        val currentBgColor = if (pressed) {
            this.keypressedBackgroundColor
        } else {
            this.backgroundColor
        }

        GraphicsUtils.drawFilledRoundedRect(context, x.toFloat(), y.toFloat(), keySize.toFloat(), keySize.toFloat(), cornerRadius, currentBgColor)

        val keyText = Text.literal(key).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val currentTextColor = if (pressed) {
            this.keypressedTextColor
        } else {
            this.textColor
        }

        val textWidth = mc.textRenderer.getWidth(keyText)
        val textHeight = mc.textRenderer.fontHeight
        val textX = x + (keySize - textWidth) / 2
        val textY = y + (keySize - textHeight) / 2

        if (!pressed && this.textShadowEnabled) {
            context.drawText(mc.textRenderer, keyText, textX + 1, textY + 1, 0x40000000, false)
        }
        context.drawText(mc.textRenderer, keyText, textX, textY, currentTextColor, false)
    }

    override fun getWidth(): Int = keySize * 3 + spacing * 2
    override fun getHeight(): Int = keySize * 2 + spacing
    override fun getName(): String = "Keystrokes"
    override val description: String = "Displays your movement keystrokes"

    fun toConfig(): HudElementConfig = HudElementConfig(
        name = getName(),
        x = getX(),
        y = getY(),
        scale = scale,
        isEnabled = isEnabled,
        textColor = textColor,
        backgroundColor = backgroundColor,
        textShadowEnabled = textShadowEnabled,
        keypressedTextColor = keypressedTextColor,
        keypressedBackgroundColor = keypressedBackgroundColor,
    )

    fun applyConfig(config: HudElementConfig) {
        setX(config.x)
        setY(config.y)
        scale = config.scale
        isEnabled = config.isEnabled
        textColor = config.textColor
        backgroundColor = config.backgroundColor
        textShadowEnabled = config.textShadowEnabled

        config.keypressedTextColor?.let { keypressedTextColor = it }
        config.keypressedBackgroundColor?.let { keypressedBackgroundColor = it }
    }
}
