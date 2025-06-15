package code.cinnamon.hud.elements

import code.cinnamon.gui.CinnamonScreen
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
    private val cornerRadius = 4

    private var wPressed = false
    private var aPressed = false
    private var sPressed = false
    private var dPressed = false

    override fun render(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        updateKeyStates()

        context.matrices.push()
        context.matrices.scale(scale, scale, 1.0f)
        context.matrices.translate((getX() / scale).toDouble(), (getY() / scale).toDouble(), 0.0)

        val wX = keySize + spacing
        drawKey(context, "W", wX, 0, wPressed)
        drawKey(context, "A", 0, keySize + spacing, aPressed)
        drawKey(context, "S", keySize + spacing, keySize + spacing, sPressed)
        drawKey(context, "D", (keySize + spacing) * 2, keySize + spacing, dPressed)

        context.matrices.pop()
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

        drawRoundedRect(context, x, y, keySize, keySize, cornerRadius, currentBgColor)

        val keyText = Text.literal(key).setStyle(Style.EMPTY.withFont(CinnamonScreen.CINNA_FONT))
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

    private fun drawRoundedRect(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        color: Int
    ) {
        if (color == 0) return

        val r = minOf(radius, minOf(width / 2, height / 2))

        if (r <= 0) {
            context.fill(x, y, x + width, y + height, color)
            return
        }

        context.fill(x + r, y, x + width - r, y + height, color)
        context.fill(x, y + r, x + r, y + height - r, color)
        context.fill(x + width - r, y + r, x + width, y + height - r, color)

        drawRoundedCorner(context, x, y, r, color, 0) 
        drawRoundedCorner(context, x + width - r, y, r, color, 1) 
        drawRoundedCorner(context, x, y + height - r, r, color, 2)
        drawRoundedCorner(context, x + width - r, y + height - r, r, color, 3)
    }

    private fun drawRoundedCorner(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, corner: Int) {
        for (dy in 0 until radius) {
            for (dx in 0 until radius) {
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared <= radius * radius) {
                    val pixelX: Int
                    val pixelY: Int

                    when (corner) {
                        0 -> { pixelX = x + (radius - 1 - dx); pixelY = y + (radius - 1 - dy) }
                        1 -> { pixelX = x + dx; pixelY = y + (radius - 1 - dy) }
                        2 -> { pixelX = x + (radius - 1 - dx); pixelY = y + dy }
                        3 -> { pixelX = x + dx; pixelY = y + dy }
                        else -> continue
                    }

                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }

    override fun getWidth(): Int = keySize * 3 + spacing * 2
    override fun getHeight(): Int = keySize * 2 + spacing
    override fun getName(): String = "Keystrokes"

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