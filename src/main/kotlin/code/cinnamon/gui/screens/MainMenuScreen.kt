package code.cinnamon.gui.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import net.minecraft.text.Style
import net.minecraft.util.Identifier
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudScreen


class MainMenuScreen : CinnamonScreen(Text.literal("Cinnamon Client").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))) {


    override fun initializeComponents() {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        val buttonWidth = 180
        val buttonHeight = CinnamonTheme.BUTTON_HEIGHT_LARGE
        val spacing = 45

        val logoAreaHeight = 80
        val buttonsStartYAnchor = contentY + logoAreaHeight
        val availableHeightForButtons = getContentHeight() - logoAreaHeight
        val totalButtonsHeight = (buttonHeight * 5) + (spacing * 4)
        val actualButtonsStartY = buttonsStartYAnchor + (availableHeightForButtons - totalButtonsHeight) / 2


        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY,
            buttonWidth,
            buttonHeight,
            Text.literal("Modules").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> CinnamonGuiManager.openModulesScreen() },
            false
        ))

        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing,
            buttonWidth,
            buttonHeight,
            Text.literal("Keybindings").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> CinnamonGuiManager.openKeybindingsScreen() }
        ))

        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 2,
            buttonWidth,
            buttonHeight,
            Text.literal("HUD Editor").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> client?.setScreen(HudScreen()) }
        ))

        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 3,
            buttonWidth,
            buttonHeight,
            Text.literal("Theme Manager").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> CinnamonGuiManager.openThemeManagerScreen() }
        ))

        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 4,
            buttonWidth,
            buttonHeight,
            Text.literal("Close").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont())),
            { _, _ -> CinnamonGuiManager.closeCurrentScreen() }
        ))
    }

    override fun renderContent(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
    }

    override fun renderFooter(context: DrawContext, scaledMouseX: Int, scaledMouseY: Int, delta: Float) {
        super.renderFooter(context, scaledMouseX, scaledMouseY, delta)

        val versionText = Text.literal("v1.5 - Minecraft 1.21.7").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val versionWidth = textRenderer.getWidth(versionText)
        context.drawText(
            textRenderer,
            versionText,
            guiX + guiWidth - versionWidth - PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.secondaryTextColor,
            CinnamonTheme.enableTextShadow
        )
        val statusText = Text.literal("Ready").fillStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        context.drawText(
            textRenderer,
            statusText,
            guiX + PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.successColor,
            CinnamonTheme.enableTextShadow
        )
    }
}