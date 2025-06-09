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

class MainMenuScreen : CinnamonScreen(Text.literal("Cinnamon Client").fillStyle(Style.EMPTY.withFont(Identifier.of("cinnamon", "cinna")))) {
    
    // Create a reusable font identifier
    companion object {
        private val CINNA_FONT = Identifier.of("cinnamon", "cinna")
    }
    
    override fun initializeComponents() {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        val buttonWidth = 180
        val buttonHeight = CinnamonTheme.BUTTON_HEIGHT_LARGE
        val spacing = 45
        
        // New calculation for startY for the first button:
        val logoAreaHeight = 80 // Configurable estimate for logo and subtitle area
        val buttonsStartYAnchor = contentY + logoAreaHeight
        val availableHeightForButtons = getContentHeight() - logoAreaHeight
        val totalButtonsHeight = (buttonHeight * 5) + (spacing * 4) // buttonHeight is CinnamonTheme.BUTTON_HEIGHT_LARGE
        val actualButtonsStartY = buttonsStartYAnchor + (availableHeightForButtons - totalButtonsHeight) / 2
        
        // Main navigation buttons
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Modules").fillStyle(Style.EMPTY.withFont(CINNA_FONT)),
            { _, _ -> CinnamonGuiManager.openModulesScreen() },
            false // Changed from true to false
        ))
        
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Keybindings").fillStyle(Style.EMPTY.withFont(CINNA_FONT)),
            { _, _ -> CinnamonGuiManager.openKeybindingsScreen() }
        ))
        
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 2, // New Y position for HUD Editor
            buttonWidth,
            buttonHeight,
            Text.literal("HUD Editor").fillStyle(Style.EMPTY.withFont(CINNA_FONT)),
            { _, _ -> client?.setScreen(HudScreen()) } // Action to open HudScreen
        ))
        
        // New Theme Manager button
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 3, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Theme Manager").fillStyle(Style.EMPTY.withFont(CINNA_FONT)),
            { _, _ -> CinnamonGuiManager.openThemeManagerScreen() }
        ))
        
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 4, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Close").fillStyle(Style.EMPTY.withFont(CINNA_FONT)),
            { _, _ -> CinnamonGuiManager.closeCurrentScreen() }
        ))
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        
    }
    
    override fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderFooter(context, mouseX, mouseY, delta)
        
        // Draw version info in footer area
        val versionText = Text.literal("v1.0.0 - Minecraft 1.21.5").fillStyle(Style.EMPTY.withFont(CINNA_FONT))
        val versionWidth = textRenderer.getWidth(versionText)
        context.drawText(
            textRenderer,
            versionText,
            guiX + guiWidth - versionWidth - PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.secondaryTextColor,
            false
        )
        
        // Draw status indicator
        val statusText = Text.literal("Ready").fillStyle(Style.EMPTY.withFont(CINNA_FONT))
        context.drawText(
            textRenderer,
            statusText,
            guiX + PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.successColor,
            false
        )
    }
}