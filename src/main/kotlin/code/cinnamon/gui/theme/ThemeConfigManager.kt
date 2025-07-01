package code.cinnamon.gui.theme

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Paths

@Serializable
data class ThemeConfig(
    val coreBackgroundPrimary: Int = 0xE61a1a1a.toInt(),
    val coreAccentPrimary: Int = 0xFF00aaff.toInt(),
    val coreTextPrimary: Int = 0xFFe0e0e0.toInt(),
    val coreBorder: Int = 0xFF404040.toInt(),
    val coreButtonBackground: Int = 0xE6404040.toInt(), 
    val coreStatusSuccess: Int = 0xFF4caf50.toInt(),
    val coreStatusWarning: Int = 0xFFff9800.toInt(),
    val coreStatusError: Int = 0xFFf44336.toInt(),
    val buttonOutlineColor: Int = 0xFF808080.toInt(),
    val buttonOutlineHoverColor: Int = 0xFFA0A0A0.toInt(),
    val enableTextShadow: Boolean = true,
    val useMinecraftFont: Boolean = false
)

object ThemeConfigManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val configDir = Paths.get("config", "cinnamon").toFile()
    private val themeFile = File(configDir, "theme.json")
    
    init {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }
    
    fun saveTheme() {
        try {
            val config = ThemeConfig(
                coreBackgroundPrimary = CinnamonTheme.coreBackgroundPrimary,
                coreAccentPrimary = CinnamonTheme.coreAccentPrimary,
                coreTextPrimary = CinnamonTheme.coreTextPrimary,
                coreBorder = CinnamonTheme.coreBorder,
                coreButtonBackground = CinnamonTheme.coreButtonBackground,
                coreStatusSuccess = CinnamonTheme.coreStatusSuccess,
                coreStatusWarning = CinnamonTheme.coreStatusWarning,
                coreStatusError = CinnamonTheme.coreStatusError,
                buttonOutlineColor = CinnamonTheme.buttonOutlineColor,
                buttonOutlineHoverColor = CinnamonTheme.buttonOutlineHoverColor,
                enableTextShadow = CinnamonTheme.enableTextShadow,
                useMinecraftFont = CinnamonTheme.useMinecraftFont
            )

            println("[ThemeConfigManager] Preparing to save theme configuration:")
            println("[ThemeConfigManager]   Core Background Primary: ${String.format("#%08X", config.coreBackgroundPrimary)}")
            println("[ThemeConfigManager]   Core Accent Primary: ${String.format("#%08X", config.coreAccentPrimary)}")
            println("[ThemeConfigManager]   Core Text Primary: ${String.format("#%08X", config.coreTextPrimary)}")
            println("[ThemeConfigManager]   Button Outline Color: ${String.format("#%08X", config.buttonOutlineColor)}")
            println("[ThemeConfigManager]   Button Outline Hover Color: ${String.format("#%08X", config.buttonOutlineHoverColor)}")
            
            val jsonString = json.encodeToString(config)
            themeFile.writeText(jsonString)
            println("[ThemeConfigManager] Theme saved successfully to ${themeFile.absolutePath}")
        } catch (e: Exception) {
            println("[ThemeConfigManager] Failed to save theme: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun loadTheme() {
        try {
            if (!themeFile.exists()) {
                println("[ThemeConfigManager] Theme file does not exist. Loading default theme values.")
                CinnamonTheme.resetToDefaults()
                return
            }
            
            val jsonString = themeFile.readText()
            val config = json.decodeFromString<ThemeConfig>(jsonString)
            
            CinnamonTheme.coreBackgroundPrimary = config.coreBackgroundPrimary
            CinnamonTheme.coreAccentPrimary = config.coreAccentPrimary
            CinnamonTheme.coreTextPrimary = config.coreTextPrimary
            CinnamonTheme.coreBorder = config.coreBorder
            CinnamonTheme.coreButtonBackground = config.coreButtonBackground
            CinnamonTheme.coreStatusSuccess = config.coreStatusSuccess
            CinnamonTheme.coreStatusWarning = config.coreStatusWarning
            CinnamonTheme.coreStatusError = config.coreStatusError
            CinnamonTheme.buttonOutlineColor = config.buttonOutlineColor
            CinnamonTheme.buttonOutlineHoverColor = config.buttonOutlineHoverColor
            CinnamonTheme.enableTextShadow = config.enableTextShadow
            CinnamonTheme.useMinecraftFont = config.useMinecraftFont
            
            CinnamonTheme.updateDependentColors()
            println("[ThemeConfigManager] Theme loaded successfully from ${themeFile.absolutePath}")

        } catch (e: Exception) {
            println("[ThemeConfigManager] Failed to load theme: ${e.message}")
            e.printStackTrace() 
            println("[ThemeConfigManager] Applying default theme values due to load failure.")
            CinnamonTheme.resetToDefaults()
        }
    }
}