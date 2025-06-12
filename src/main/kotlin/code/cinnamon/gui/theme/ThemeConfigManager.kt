package code.cinnamon.gui.theme

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Paths

@Serializable
data class ThemeConfig(
    // Core Color Properties
    val coreBackgroundPrimary: Int = 0xE61a1a1a.toInt(),
    val coreAccentPrimary: Int = 0xFF00aaff.toInt(),
    val coreTextPrimary: Int = 0xFFe0e0e0.toInt(),
    val coreBorder: Int = 0xFF404040.toInt(),
    val coreButtonBackground: Int = 0xE6404040.toInt(), // Default to dark theme's button background
    val coreStatusSuccess: Int = 0xFF4caf50.toInt(),
    val coreStatusWarning: Int = 0xFFff9800.toInt(),
    val coreStatusError: Int = 0xFFf44336.toInt()
)

object ThemeConfigManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true } // Added ignoreUnknownKeys
    private val configDir = Paths.get("config", "cinnamon").toFile()
    private val themeFile = File(configDir, "theme.json")
    
    init {
        // Create config directory if it doesn't exist
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
                coreStatusError = CinnamonTheme.coreStatusError
            )

            // Detailed logging for the config object before serialization
            println("[ThemeConfigManager] Preparing to save theme configuration:")
            println("[ThemeConfigManager]   Core Background Primary: ${String.format("#%08X", config.coreBackgroundPrimary)}")
            println("[ThemeConfigManager]   Core Accent Primary: ${String.format("#%08X", config.coreAccentPrimary)}")
            println("[ThemeConfigManager]   Core Text Primary: ${String.format("#%08X", config.coreTextPrimary)}")
            // Add more logs as needed for other core colors if debugging specific ones
            
            val jsonString = json.encodeToString(config)
            themeFile.writeText(jsonString)
            println("[ThemeConfigManager] Theme saved successfully to ${themeFile.absolutePath}")
        } catch (e: Exception) {
            println("[ThemeConfigManager] Failed to save theme: ${e.message}")
            e.printStackTrace() // More detailed error for debugging
        }
    }
    
    fun loadTheme() {
        try {
            if (!themeFile.exists()) {
                println("[ThemeConfigManager] Theme file does not exist. Loading default theme values.")
                CinnamonTheme.resetToDefaults() // Ensure defaults are applied if no file
                return
            }
            
            val jsonString = themeFile.readText()
            val config = json.decodeFromString<ThemeConfig>(jsonString)
            
            // Apply loaded theme to core colors
            CinnamonTheme.coreBackgroundPrimary = config.coreBackgroundPrimary
            CinnamonTheme.coreAccentPrimary = config.coreAccentPrimary
            CinnamonTheme.coreTextPrimary = config.coreTextPrimary
            CinnamonTheme.coreBorder = config.coreBorder
            CinnamonTheme.coreButtonBackground = config.coreButtonBackground
            CinnamonTheme.coreStatusSuccess = config.coreStatusSuccess
            CinnamonTheme.coreStatusWarning = config.coreStatusWarning
            CinnamonTheme.coreStatusError = config.coreStatusError
            
            // Crucially, update all dependent colors in CinnamonTheme
            CinnamonTheme.updateDependentColors()
            println("[ThemeConfigManager] Theme loaded successfully from ${themeFile.absolutePath}")

        } catch (e: Exception) {
            println("[ThemeConfigManager] Failed to load theme: ${e.message}")
            e.printStackTrace() // More detailed error for debugging
            println("[ThemeConfigManager] Applying default theme values due to load failure.")
            CinnamonTheme.resetToDefaults() // Attempt to reset to a known good state
        }
    }
    
    // updateDependentColors() and adjustBrightness() are removed as this logic
    // is now centralized in CinnamonTheme.kt
}