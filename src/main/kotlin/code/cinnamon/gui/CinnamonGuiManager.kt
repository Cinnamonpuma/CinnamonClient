package code.cinnamon.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import code.cinnamon.gui.screens.MainMenuScreen
import code.cinnamon.gui.screens.ModulesScreen
import code.cinnamon.gui.screens.KeybindingsScreen
import code.cinnamon.modules.ModuleManager
import code.cinnamon.keybindings.KeybindingManager
import code.cinnamon.gui.screens.ThemeManagerScreen

object CinnamonGuiManager {
    private val client = MinecraftClient.getInstance()

    fun openMainMenu() {
        AnimatedScreenTransition.setCurrentScreen(MainMenuScreen())
    }
    fun openScreen(screen: Screen) {
        AnimatedScreenTransition.setCurrentScreen(screen)
    }
    fun openThemeManagerScreen() {
        AnimatedScreenTransition.setCurrentScreen(ThemeManagerScreen())
    }

    fun openModulesScreen() {
        AnimatedScreenTransition.setCurrentScreen(ModulesScreen())
    }

    fun openKeybindingsScreen() {
        AnimatedScreenTransition.setCurrentScreen(KeybindingsScreen())
    }

    fun closeCurrentScreen() {
        AnimatedScreenTransition.setCurrentScreen(null)
    }

    // getCurrentScreen should now reflect the screen being displayed by AnimatedScreenTransition
    fun getCurrentScreen(): Screen? {
        return AnimatedScreenTransition.getDisplayScreen()
    }

    // isGuiOpen should also reflect the state of AnimatedScreenTransition
    fun isGuiOpen(): Boolean {
        return AnimatedScreenTransition.getDisplayScreen() != null
    }
}