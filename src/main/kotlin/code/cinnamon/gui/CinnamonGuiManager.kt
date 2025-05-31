package code.cinnamon.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import code.cinnamon.gui.screens.MainMenuScreen
import code.cinnamon.gui.screens.ModulesScreen
import code.cinnamon.gui.screens.KeybindingsScreen
import code.cinnamon.gui.screens.SettingsScreen
import code.cinnamon.modules.ModuleManager
import code.cinnamon.keybindings.KeybindingManager
import code.cinnamon.gui.screens.ThemeManagerScreen

object CinnamonGuiManager {
    private val client = MinecraftClient.getInstance()
    
    fun openMainMenu() {
        client.setScreen(MainMenuScreen())
    }
    fun openThemeManagerScreen() {
        client.setScreen(ThemeManagerScreen())
    }
    
    fun openModulesScreen() {
        client.setScreen(ModulesScreen())
    }
    
    fun openKeybindingsScreen() {
        client.setScreen(KeybindingsScreen())
    }
    
    fun openSettingsScreen() {
        client.setScreen(SettingsScreen())
    }
    
    fun closeCurrentScreen() {
        client.setScreen(null)
    }
    
    fun getCurrentScreen(): Screen? {
        return client.currentScreen
    }
    
    fun isGuiOpen(): Boolean {
        return client.currentScreen != null
    }
}