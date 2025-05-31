package code.cinnamon.gui.screens

import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import code.cinnamon.gui.CinnamonScreen
import code.cinnamon.gui.CinnamonGuiManager
import code.cinnamon.gui.components.CinnamonButton
import code.cinnamon.gui.theme.CinnamonTheme

class SettingsScreen : CinnamonScreen(Text.literal("Settings")) {
    
    override fun initializeComponents() {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        val buttonWidth = 160
        val buttonHeight = CinnamonTheme.BUTTON_HEIGHT
        val spacing = 35
        
        // New calculation for button block positioning
        val infoTextTopPadding = 20 
        val infoTextFontHeight = 9 // Assuming textRenderer.fontHeight is 9
        val spaceBelowInfoText = 20 
        val topOffset = infoTextTopPadding + infoTextFontHeight + spaceBelowInfoText
        val buttonsAvailableY = contentY + topOffset
        val availableHeightForButtons = getContentHeight() - topOffset
        val totalButtonsHeight = (buttonHeight * 5) + (spacing * 4)
        val actualButtonsStartY = buttonsAvailableY + (availableHeightForButtons - totalButtonsHeight) / 2
        
        // Settings category buttons
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("General"),
            { _, _ -> /* TODO: Open general settings */ },
            false // Changed from true to false
        ))
        
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Performance"),
            { _, _ -> /* TODO: Open performance settings */ }
        ))
        
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 2, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Interface"),
            { _, _ -> /* TODO: Open interface settings */ }
        ))
        
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 3, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Advanced"),
            { _, _ -> /* TODO: Open advanced settings */ }
        ))
        
        // Back button at bottom
        addButton(CinnamonButton(
            centerX - buttonWidth / 2,
            actualButtonsStartY + spacing * 4, // Use new startY
            buttonWidth,
            buttonHeight,
            Text.literal("Back"),
            { _, _ -> CinnamonGuiManager.openMainMenu() } // Changed to openMainMenu
        ))
    }
    
    override fun renderContent(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        val centerX = guiX + guiWidth / 2
        val contentY = getContentY()
        
        // Removed "SETTINGS" title, subtitle, and glow effect.
        // Screen title is handled by CinnamonScreen.renderHeader().
        
        // Draw settings info text
        val infoText = Text.literal("Select a category to configure")
        val infoWidth = textRenderer.getWidth(infoText)
        context.drawText(
            textRenderer,
            infoText,
            centerX - infoWidth / 2,
            contentY + 20, // Adjusted Y position
            CinnamonTheme.secondaryTextColor,
            false
        )
    }
    
    override fun renderFooter(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderFooter(context, mouseX, mouseY, delta)
        
        // Draw configuration status in footer area
        val configText = Text.literal("Configuration Status")
        context.drawText(
            textRenderer,
            configText,
            guiX + PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.infoColor,
            false
        )
        
        // Draw save status indicator
        val saveText = Text.literal("Auto-Save: Enabled")
        val saveWidth = textRenderer.getWidth(saveText)
        context.drawText(
            textRenderer,
            saveText,
            guiX + guiWidth - saveWidth - PADDING,
            getFooterY() + (FOOTER_HEIGHT - textRenderer.fontHeight) / 2,
            CinnamonTheme.successColor,
            false
        )
    }
}