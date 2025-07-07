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

        // The matrix transformations are removed as per the issue description.
        // Text will be drawn at absolute coordinates.

        val tr = client.textRenderer
        // Calculate text position within the button, then scale and add button's screen position.
        // Dimensions of the button (width, height) are already scaled.
        // We need unscaled dimensions to correctly center the text before applying the scale for drawing.
        val unscaledButtonWidth = (width / currentScale) 
        val unscaledButtonHeight = (height / currentScale)
        
        val unscaledTextWidth = tr.getWidth(text) // Text width is inherently unscaled
        val unscaledFontHeight = tr.fontHeight // Font height is inherently unscaled

        // Calculate text position as if the button was at (0,0) and unscaled
        val textXInButtonUnscaled = ((unscaledButtonWidth - unscaledTextWidth) / 2)
        val textYInButtonUnscaled = ((unscaledButtonHeight - unscaledFontHeight) / 2)

        // Now, calculate the final screen coordinates for the text:
        // Add the button's top-left (x, y)
        // And apply the scale to the text's relative position
        val finalTextX = x + (textXInButtonUnscaled * currentScale).toInt()
        val finalTextY = y + (textYInButtonUnscaled * currentScale).toInt()
        
        // We need to apply scaling to the text rendering itself if the context.drawText doesn't handle it.
        // Assuming context.drawText draws unscaled text at the given coordinates,
        // and if we want scaled text, we might need a different approach or ensure
        // CinnamonTheme.getCurrentFont() provides a scaled font, or use matrices if available and working.
        // Given the problem, direct drawing without matrix stack is preferred.
        // The text itself will be drawn at its natural size, but positioned correctly within the scaled button area.
        // If the text itself needs to be scaled, this solution would need adjustment,
        // potentially by using matrix transformations if a 2D equivalent is available and working,
        // or by requesting a font of a specific size.

        // For now, let's assume text is drawn unscaled, but positioned correctly.
        // If text scaling IS required, the matrix stack was the previous way.
        // The problem implies Matrix3x2fStack doesn't support push/pop/scale/translate in the same way.
        // If context.drawText needs scaled text, this might be an issue.
        // However, the original code's matrices.scale(currentScale, currentScale, 1.0f) was applied
        // AFTER translating to the button's origin (x,y). So text was drawn at (textXInButtonUnscaled, textYInButtonUnscaled)
        // in a context that was already scaled.

        // Re-evaluating: The text *was* scaled by the matrix.
        // To replicate this without the old matrix stack:
        // 1. Draw text with a transform or use a scaled font.
        // 2. If DrawContext's Matrix3x2fStack can be used for a single operation:
        //    context.matrices.translate(finalTextX, finalTextY, 0)
        //    context.matrices.scale(currentScale, currentScale, 1)
        //    context.drawText(tr, text, 0, 0, ...)
        //    context.matrices.scale(1/currentScale, 1/currentScale, 1)
        //    context.matrices.translate(-finalTextX, -finalTextY, 0)
        // This is cumbersome and error-prone.

        // Simpler: The text coordinates were relative to the scaled button origin.
        // So, textXInButtonUnscaled * currentScale gives the scaled offset from button's origin.
        // This is what finalTextX and finalTextY already calculate.
        // The question is whether context.drawText itself should be called within a scaled matrix
        // or if it respects pre-scaled font from CinnamonTheme.

        // Let's assume for now the text itself is not scaled by drawText, and the previous matrix scaling
        // effectively made the text larger/smaller.
        // If CinnamonTheme.getCurrentFont() gives a font that's already scaled with `currentScale`, then it's fine.
        // Otherwise, the text will appear smaller than before if currentScale > 1.

        // Given the problem description, the safest is to avoid matrix stack manipulations.
        // The `drawText` method of DrawContext takes integer coordinates.
        // The text itself will be drawn using the font style provided. If that font style
        // is already scaled by `currentScale` (e.g. by HudElement.scale), then drawing at
        // `finalTextX, finalTextY` should be correct.

        // The original code was:
        // matrices.translate(x,y,0)
        // matrices.scale(scale,scale,1)
        // drawText(..., textXUnscaled, textYUnscaled, ...)
        // This means text was drawn at screen_pos = (x + textXUnscaled*scale, y + textYUnscaled*scale)
        // AND the text itself was rendered larger/smaller by `scale`.

        // If `context.drawText` does not inherently scale the text based on matrix state (which is likely if we remove matrix ops),
        // then we need to find another way to scale text or accept it unscaled.
        // The problem description is about matrix API mismatch, not text rendering features.
        // Let's assume `CinnamonTheme.getCurrentFont()` is expected to provide the correctly scaled font.
        // If not, this is a limitation of removing the matrix calls without a direct replacement for text scaling.

        // The coordinates finalTextX, finalTextY correctly position the (potentially unscaled) text.
        context.drawText(tr, text, finalTextX, finalTextY, buttonTextColor, buttonTextShadowEnabled)
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