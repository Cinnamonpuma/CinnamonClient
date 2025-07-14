package code.cinnamon.gui.screens

import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.modules.BooleanSetting
import code.cinnamon.modules.ColorSetting
import code.cinnamon.modules.DoubleSetting
import code.cinnamon.modules.ModeSetting
import code.cinnamon.modules.Setting
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style

object SettingsHelper {
    fun renderSettings(
        context: DrawContext,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        settings: List<Setting<*>>,
        scaledMouseX: Int,
        scaledMouseY: Int,
        delta: Float
    ) {
        var currentY = y
        for (setting in settings) {
            when (setting) {
                is BooleanSetting -> {
                    renderCheckbox(context, x, currentY, setting.name, setting.value)
                    currentY += 14
                }
                is DoubleSetting -> {
                    val text = "${setting.name}: %.1f".format(setting.value)
                    context.drawText(
                        mc.textRenderer,
                        Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                        x,
                        currentY,
                        CinnamonTheme.primaryTextColor,
                        CinnamonTheme.enableTextShadow
                    )
                    val buttonWidth = 16
                    val buttonHeight = 12
                    val buttonsX = x + width - 40
                    drawSettingButton(context, buttonsX, currentY - 1, buttonWidth, buttonHeight, "-", false)
                    drawSettingButton(context, buttonsX + 20, currentY - 1, buttonWidth, buttonHeight, "+", false)
                    currentY += 14
                }
                is ColorSetting -> {
                    val text = "${setting.name}: #${String.format("%06X", setting.value)}"
                    context.drawText(
                        mc.textRenderer,
                        Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                        x,
                        currentY,
                        CinnamonTheme.primaryTextColor,
                        CinnamonTheme.enableTextShadow
                    )
                    val buttonText = "[Set]"
                    val buttonWidth = mc.textRenderer.getWidth(buttonText)
                    context.drawText(
                        mc.textRenderer,
                        Text.literal(buttonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                        x + width - buttonWidth,
                        currentY,
                        CinnamonTheme.accentColor,
                        false
                    )
                    currentY += 14
                }
                is ModeSetting -> {
                    if (setting.name == "Prefix Color") {
                        val allColors = code.cinnamon.util.MinecraftColorCodes.entries
                        val itemBoxHeight = 20
                        val itemBoxWidth = 110
                        val horizontalSpacing = 4
                        val verticalSpacing = 4
                        val itemsPerRow = kotlin.math.max(1, (width + horizontalSpacing) / (itemBoxWidth + horizontalSpacing))
                        var currentX = x
                        var itemsInCurrentRow = 0
                        for (colorEnumEntry in allColors) {
                            if (itemsInCurrentRow >= itemsPerRow) {
                                currentX = x
                                currentY += itemBoxHeight + verticalSpacing
                                itemsInCurrentRow = 0
                            }
                            val isSelected = (setting.value == colorEnumEntry.code)
                            val checkboxX = currentX + 2
                            val checkboxY = currentY + (itemBoxHeight - 10) / 2
                            val checkboxSize = 10
                            val checkboxBg = if (isSelected) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
                            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, checkboxBg)
                            context.drawBorder(checkboxX, checkboxY, checkboxSize, checkboxSize, CinnamonTheme.borderColor)
                            if (isSelected) {
                                context.drawText(
                                    mc.textRenderer,
                                    Text.literal("x").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                                    checkboxX + 1,
                                    checkboxY + 1,
                                    CinnamonTheme.titleColor,
                                    false
                                )
                            }
                            val textX = checkboxX + checkboxSize + 6
                            val textY = currentY + (itemBoxHeight - mc.textRenderer.fontHeight) / 2
                            context.drawText(
                                mc.textRenderer,
                                Text.literal(colorEnumEntry.friendlyName).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                                textX,
                                textY,
                                CinnamonTheme.primaryTextColor,
                                CinnamonTheme.enableTextShadow
                            )
                            currentX += itemBoxWidth + horizontalSpacing
                            itemsInCurrentRow++
                        }
                        currentY += itemBoxHeight + verticalSpacing
                    } else {
                        val text = "${setting.name}: ${setting.value}"
                        context.drawText(
                            mc.textRenderer,
                            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                            x,
                            currentY,
                            CinnamonTheme.primaryTextColor,
                            CinnamonTheme.enableTextShadow
                        )
                        val buttonText = "[Change]"
                        val buttonWidth = mc.textRenderer.getWidth(buttonText)
                        context.drawText(
                            mc.textRenderer,
                            Text.literal(buttonText).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                            x + width - buttonWidth,
                            currentY,
                            CinnamonTheme.accentColor,
                            false
                        )
                        currentY += 14
                    }
                }
            }
        }
    }

    fun handleMouseClick(
        mouseX: Double,
        mouseY: Double,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        settings: List<Setting<*>>
    ): Boolean {
        var currentY = y
        for (setting in settings) {
            when (setting) {
                is BooleanSetting -> {
                    val checkboxSize = 10
                    if (mouseX >= x && mouseX < x + checkboxSize + 6 + mc.textRenderer.getWidth(setting.name) &&
                        mouseY >= currentY && mouseY < currentY + checkboxSize
                    ) {
                        setting.value = !setting.value
                        return true
                    }
                    currentY += 14
                }
                is DoubleSetting -> {
                    val buttonWidth = 16
                    val buttonHeight = 12
                    val buttonsX = x + width - 40
                    if (mouseY >= currentY - 1 && mouseY < currentY - 1 + buttonHeight) {
                        if (mouseX >= buttonsX && mouseX < buttonsX + buttonWidth) {
                            setting.value = (setting.value - setting.step).coerceIn(setting.min, setting.max)
                            return true
                        }
                        if (mouseX >= buttonsX + 20 && mouseX < buttonsX + 20 + buttonWidth) {
                            setting.value = (setting.value + setting.step).coerceIn(setting.min, setting.max)
                            return true
                        }
                    }
                    currentY += 14
                }
                is ColorSetting -> {
                    val buttonText = "[Set]"
                    val buttonWidth = mc.textRenderer.getWidth(buttonText)
                    val buttonX = x + width - buttonWidth
                    if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                        mouseY >= currentY && mouseY < currentY + mc.textRenderer.fontHeight
                    ) {
                        return true
                    }
                    currentY += 14
                }
                is ModeSetting -> {
                    if (setting.name == "Prefix Color") {
                        val allColors = code.cinnamon.util.MinecraftColorCodes.entries
                        val itemBoxHeight = 20
                        val itemBoxWidth = 110
                        val horizontalSpacing = 4
                        val verticalSpacing = 4
                        val itemsPerRow = kotlin.math.max(1, (width + horizontalSpacing) / (itemBoxWidth + horizontalSpacing))
                        var currentX = x
                        var itemsInCurrentRow = 0
                        for (colorEnumEntry in allColors) {
                            if (itemsInCurrentRow >= itemsPerRow) {
                                currentX = x
                                currentY += itemBoxHeight + verticalSpacing
                                itemsInCurrentRow = 0
                            }
                            val itemHitboxX = currentX
                            val itemHitboxY = currentY
                            val itemHitboxEndX = itemHitboxX + itemBoxWidth
                            val itemHitboxEndY = itemHitboxY + itemBoxHeight
                            if (mouseX >= itemHitboxX && mouseX < itemHitboxEndX &&
                                mouseY >= itemHitboxY && mouseY < itemHitboxEndY
                            ) {
                                setting.value = colorEnumEntry.code
                                return true
                            }
                            currentX += itemBoxWidth + horizontalSpacing
                            itemsInCurrentRow++
                        }
                        currentY += itemBoxHeight + verticalSpacing
                    } else {
                        val buttonText = "[Change]"
                        val buttonWidth = mc.textRenderer.getWidth(buttonText)
                        val buttonX = x + width - buttonWidth
                        if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                            mouseY >= currentY && mouseY < currentY + mc.textRenderer.fontHeight
                        ) {
                            val currentIndex = setting.modes.indexOf(setting.value)
                            val nextIndex = (currentIndex + 1) % setting.modes.size
                            setting.value = setting.modes[nextIndex]
                            return true
                        }
                        currentY += 14
                    }
                }
            }
        }
        return false
    }

    private fun renderCheckbox(context: DrawContext, x: Int, y: Int, text: String, checked: Boolean) {
        val checkboxSize = 10
        val checkboxBg = if (checked) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
        context.fill(x, y, x + checkboxSize, y + checkboxSize, checkboxBg)
        context.drawBorder(x, y, checkboxSize, checkboxSize, CinnamonTheme.borderColor)
        if (checked) {
            context.drawText(
                mc.textRenderer,
                Text.literal("x").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                x + 1,
                y + 1,
                CinnamonTheme.titleColor,
                false
            )
        }
        context.drawText(
            mc.textRenderer,
            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + checkboxSize + 6,
            y + 1,
            CinnamonTheme.primaryTextColor,
            CinnamonTheme.enableTextShadow
        )
    }

    private fun drawSettingButton(context: DrawContext, x: Int, y: Int, width: Int, height: Int, text: String, pressed: Boolean) {
        val bgColor = if (pressed) CinnamonTheme.accentColor else CinnamonTheme.buttonBackground
        val textColor = if (pressed) CinnamonTheme.titleColor else CinnamonTheme.primaryTextColor
        context.fill(x, y, x + width, y + height, bgColor)
        context.drawBorder(x, y, width, height, CinnamonTheme.borderColor)
        val textWidth = mc.textRenderer.getWidth(text)
        context.drawText(
            mc.textRenderer,
            Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            x + (width - textWidth) / 2,
            y + 2,
            textColor,
            false
        )
    }

    private val mc
        get() = net.minecraft.client.MinecraftClient.getInstance()
}
