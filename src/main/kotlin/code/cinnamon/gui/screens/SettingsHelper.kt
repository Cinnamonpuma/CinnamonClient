package code.cinnamon.gui.screens

import code.cinnamon.gui.components.CinnamonSlider
import code.cinnamon.gui.components.CinnamonTextField
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.modules.BooleanSetting
import code.cinnamon.modules.LookAtHudSetting
import code.cinnamon.modules.ColorSetting
import code.cinnamon.modules.DoubleSetting
import code.cinnamon.modules.ModeSetting
import code.cinnamon.modules.Setting
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
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
        delta: Float,
        sliders: MutableMap<DoubleSetting, CinnamonSlider>,
        textFields: MutableMap<DoubleSetting, CinnamonTextField>
    ): Int {
        var currentY = y
        for (setting in settings) {
            when (setting) {
                is LookAtHudSetting -> {
                    renderCheckbox(context, x, currentY, setting.name, setting.value)
                    currentY += 14
                }
                is BooleanSetting -> {
                    renderCheckbox(context, x, currentY, setting.name, setting.value)
                    currentY += 14
                }
                is DoubleSetting -> {
                    val slider = sliders.getOrPut(setting) {
                        CinnamonSlider(x, currentY + 12, width - 60, 16, setting.value, setting.min, setting.max, setting.step) {
                            setting.value = it
                            textFields[setting]?.text = "%.2f".format(it)
                        }
                    }
                    slider.setPosition(x, currentY + 12)
                    slider.render(context, scaledMouseX, scaledMouseY, delta)

                    val textField = textFields.getOrPut(setting) {
                        CinnamonTextField(mc.textRenderer, x + width - 50, currentY, 50, 16).apply {
                            text = "%.2f".format(setting.value)
                            setChangedListener {
                                it.toDoubleOrNull()?.let { newV ->
                                    setting.value = newV
                                    slider.setValue(newV)
                                }
                            }
                        }
                    }
                    textField.setPosition(x + width - 50, currentY)
                    textField.render(context, scaledMouseX, scaledMouseY, delta)

                    val text = "${setting.name}"
                    context.drawText(
                        mc.textRenderer,
                        Text.literal(text).setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
                        x,
                        currentY,
                        CinnamonTheme.primaryTextColor,
                        CinnamonTheme.enableTextShadow
                    )
                    currentY += 40
                }
                is ColorSetting -> {
                    val text = "${setting.name}: #${String.format("%08X", setting.value)}"
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
        return currentY
    }

    fun handleMouseClick(
        parent: Screen,
        mouseX: Double,
        mouseY: Double,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        settings: List<Setting<*>>,
        sliders: MutableMap<DoubleSetting, CinnamonSlider>,
        textFields: MutableMap<DoubleSetting, CinnamonTextField>
    ): Boolean {
        var currentY = y
        for (setting in settings) {
            when (setting) {
                is LookAtHudSetting -> {
                    val checkboxSize = 10
                    if (mouseX >= x && mouseX < x + checkboxSize + 6 + mc.textRenderer.getWidth(setting.name) &&
                        mouseY >= currentY && mouseY < currentY + checkboxSize
                    ) {
                        setting.value = !setting.value
                        return true
                    }
                    currentY += 14
                }
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
                    sliders[setting]?.let { if (it.mouseClicked(mouseX, mouseY, 0)) return true }
                    textFields[setting]?.let { if (it.mouseClicked(mouseX, mouseY, 0)) return true }
                    currentY += 40
                }
                is ColorSetting -> {
                    val buttonText = "[Set]"
                    val buttonWidth = mc.textRenderer.getWidth(buttonText)
                    val buttonX = x + width - buttonWidth
                    if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
                        mouseY >= currentY && mouseY < currentY + mc.textRenderer.fontHeight
                    ) {
                        mc.setScreen(ColorPickerScreen(
                            initialColor = setting.value,
                            onPick = { pickedColor ->
                                setting.set(pickedColor)
                                mc.setScreen(parent)
                            },
                            onCancel = { mc.setScreen(parent) }
                        ))
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

    fun handleMouseRelease(
        mouseX: Double,
        mouseY: Double,
        button: Int,
        sliders: Map<DoubleSetting, CinnamonSlider>
    ): Boolean {
        sliders.values.forEach { if (it.mouseReleased(mouseX, mouseY, button)) return true }
        return false
    }

    fun handleKeyPress(
        keyCode: Int,
        scanCode: Int,
        modifiers: Int,
        textFields: Map<DoubleSetting, CinnamonTextField>
    ): Boolean {
        textFields.values.forEach { if (it.keyPressed(keyCode, scanCode, modifiers)) return true }
        return false
    }

    fun handleCharTyped(
        chr: Char,
        modifiers: Int,
        textFields: Map<DoubleSetting, CinnamonTextField>
    ): Boolean {
        textFields.values.forEach { if (it.charTyped(chr, modifiers)) return true }
        return false
    }
}