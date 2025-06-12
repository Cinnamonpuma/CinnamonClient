package code.cinnamon.gui.theme

// Data class to hold all core colors for a theme
data class ThemeColors(
    val coreBackgroundPrimary: Int,
    val coreAccentPrimary: Int,
    val coreTextPrimary: Int,
    val coreBorder: Int,
    val coreButtonBackgroundDef: Int,
    // Status colors could also be part of the theme, but for now, keeping them as defined in CinnamonTheme
    // Or, if they should also change, add them to ThemeColors and update applyTheme accordingly
)

// Enum to define available themes
enum class Theme(val colors: ThemeColors) {
    DARK(
        ThemeColors(
            coreBackgroundPrimary = 0xE61a1a1a.toInt(),
            coreAccentPrimary = 0xFF00aaff.toInt(),
            coreTextPrimary = 0xFFe0e0e0.toInt(),
            coreBorder = 0xFF404040.toInt(),
            coreButtonBackgroundDef = 0xE6404040.toInt()
        )
    ),
    LIGHT(
        ThemeColors(
            coreBackgroundPrimary = 0xE6FAFAFA.toInt(),
            coreAccentPrimary = 0xFF007ACC.toInt(),
            coreTextPrimary = 0xFF202020.toInt(),
            coreBorder = 0xFFCCCCCC.toInt(),
            coreButtonBackgroundDef = 0xE6DDDDDD.toInt()
        )
    )
}

object CinnamonTheme {

    // 1. Core Color Properties (Mutable) - These will be updated by applyTheme
    var coreBackgroundPrimary = 0 // Placeholder, will be set by init -> applyTheme
    var coreAccentPrimary = 0 // Placeholder
    var coreTextPrimary = 0 // Placeholder
    var coreBorder = 0 // Placeholder
    var coreButtonBackground = 0 // Placeholder

    // Status colors remain directly mutable for now, not part of ThemeColors data class.
    // They could be added to ThemeColors if theme-specific status colors are desired.
    var coreStatusSuccess = 0xFF4caf50.toInt()         // Success Color
    var coreStatusWarning = 0xFFff9800.toInt()         // Warning Color
    var coreStatusError = 0xFFf44336.toInt()           // Error Color

    // Currently active theme
    var currentTheme: Theme = Theme.DARK // Default to Dark theme

    // 2. Independent Special Effect Colors (Mutable)
    var patternColor = 0x10ffffff.toInt()
    var overlayColor = 0x80000000.toInt()
    var glassHighlight = 0x20ffffff.toInt()
    var glassShadow = 0x40000000.toInt()

    // 3. Derived Color Properties (Mutable, updated by updateDependentColors)
    // These need to be 'var' because they are calculated and reassigned.
    // Initialize with a placeholder or calculation based on initial core defaults.
    // The init block will call updateDependentColors to set them correctly.
    var cardBackgroundHover: Int = 0
    var accentColorHover: Int = 0
    var accentColorPressed: Int = 0
    var primaryButtonBackground: Int = 0
    var primaryButtonBackgroundHover: Int = 0
    var primaryButtonBackgroundPressed: Int = 0
    var buttonBackgroundHover: Int = 0
    var buttonBackgroundPressed: Int = 0
    var moduleEnabledColor: Int = 0


    // --- Old Color Properties: Mapped to Core or Derived Colors via Getters ---

    // Backgrounds
    val guiBackground: Int get() = coreBackgroundPrimary
    val backgroundTop: Int get() = coreBackgroundPrimary 
    val backgroundBottom: Int get() = adjustBrightness(coreBackgroundPrimary, -0.05f) // Slight adjustment for depth
    
    val headerBackground: Int get() = coreBackgroundPrimary  // Changed from coreBackgroundSecondary
    val footerBackground: Int get() = coreBackgroundPrimary  // Changed from coreBackgroundSecondary
    val contentBackground: Int get() = coreBackgroundPrimary // This already points to primary
    val sidebarBackground: Int get() = coreBackgroundPrimary 
    
    // Content Area
    val cardBackground: Int get() = coreBackgroundPrimary 
    // cardBackgroundHover is a derived 'var'

    // Borders & Accents
    val borderColor: Int get() = coreBorder
    val accentColor: Int get() = coreAccentPrimary
    // accentColorHover, accentColorPressed are derived 'var's

    // Text
    val titleColor: Int get() = coreTextPrimary
    val primaryTextColor: Int get() = coreTextPrimary
    val secondaryTextColor: Int get() = coreTextPrimary
    val disabledTextColor: Int get() = adjustBrightness(coreTextPrimary, -0.3f)

    // Buttons
    val buttonBackground: Int get() = coreButtonBackground
    // buttonBackgroundHover, buttonBackgroundPressed are derived 'var's
    val buttonBackgroundDisabled: Int get() = adjustBrightness(coreButtonBackground, -0.3f)

    // Primary Buttons (Accent Colored)
    // primaryButtonBackground, primaryButtonBackgroundHover, primaryButtonBackgroundPressed are derived 'var's

    // Status Colors
    val successColor: Int get() = coreStatusSuccess
    val warningColor: Int get() = coreStatusWarning
    val errorColor: Int get() = coreStatusError
    val infoColor: Int get() = coreAccentPrimary // Mapped to primary accent

    // Module Colors
    // moduleEnabledColor is a derived 'var'
    val moduleDisabledColor: Int get() = adjustBrightness(coreButtonBackground, -0.1f)
    val moduleBackgroundEnabled: Int get() = adjustBrightness(coreStatusSuccess, -0.3f) // Darker version of success
    val moduleBackgroundDisabled: Int get() = adjustBrightness(coreButtonBackground, -0.2f)


    // Store default values for reset functionality - This might need rethinking with themes.
    // For now, resetToDefaults will explicitly apply Theme.DARK.
    // private val defaultColors = mutableMapOf<String, Int>() // Keeping for now, but its role is diminished.

    init {
        // saveDefaults() // saveDefaults will be called by applyTheme if we want to save the applied theme's colors
        applyTheme(Theme.DARK) // Initialize with Dark Theme; this also calls updateDependentColors
    }

    // Removed saveDefaults() as its previous role is handled by applyTheme setting specific theme colors.
    // If we need to save *customizations* on top of a theme, this function might be re-introduced.

    fun applyTheme(theme: Theme) {
        currentTheme = theme

        coreBackgroundPrimary = theme.colors.coreBackgroundPrimary
        coreAccentPrimary = theme.colors.coreAccentPrimary
        coreTextPrimary = theme.colors.coreTextPrimary
        coreBorder = theme.colors.coreBorder
        coreButtonBackground = theme.colors.coreButtonBackgroundDef

        // Note: Status colors (success, warning, error) and special effect colors (patternColor, etc.)
        // are not part of the ThemeColors data class in this iteration.
        // They retain their values unless explicitly changed.
        // If they need to be themed, add them to ThemeColors and update here.

        updateDependentColors()
        // If we want 'resetToDefaults' to reset to the *last applied theme's* defaults,
        // then saveDefaults() should be called here.
        // For now, resetToDefaults explicitly applies Dark theme.
    }
    
    fun resetToDefaults() {
        // Reset to Dark Theme specifically, as per requirement.
        applyTheme(Theme.DARK)

        // Reset independent special effect colors and status colors to their initial defaults
        // as they are not part of the theme application logic in applyTheme.
        // If these were part of ThemeColors, applyTheme(Theme.DARK) would handle them.
        coreStatusSuccess = 0xFF4caf50.toInt()
        coreStatusWarning = 0xFFff9800.toInt()
        coreStatusError = 0xFFf44336.toInt()

        patternColor = 0x10ffffff.toInt()
        overlayColor = 0x80000000.toInt()
        glassHighlight = 0x20ffffff.toInt()
        glassShadow = 0x40000000.toInt()
        
        // updateDependentColors() is already called by applyTheme(Theme.DARK)
    }
    
    fun updateDependentColors() {
        // Update 'var' properties that depend on core colors
        cardBackgroundHover = adjustBrightness(coreBackgroundPrimary, 0.05f)
        
        accentColorHover = adjustBrightness(coreAccentPrimary, -0.1f)
        accentColorPressed = adjustBrightness(coreAccentPrimary, -0.2f)
        
        primaryButtonBackground = (coreAccentPrimary and 0x00FFFFFF) or 0xE6000000.toInt() // Retain original alpha logic
        primaryButtonBackgroundHover = accentColorHover 
        primaryButtonBackgroundPressed = accentColorPressed
        
        buttonBackgroundHover = adjustBrightness(coreButtonBackground, 0.1f)
        buttonBackgroundPressed = adjustBrightness(coreButtonBackground, -0.1f)
        
        moduleEnabledColor = coreStatusSuccess
        
        // Note: 'val' properties with 'get()' are automatically "updated" as they always read the current core color.
        // No need to list them here (e.g. guiBackground, borderColor, titleColor, etc.)
        // Also, specific derivations in getters (like disabledTextColor) are also always live.
    }
    
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val alpha = (color shr 24) and 0xFF
        val red = ((color shr 16) and 0xFF).toFloat()
        val green = ((color shr 8) and 0xFF).toFloat()
        val blue = (color and 0xFF).toFloat()
        
        val newRed = (red + (if (factor > 0) (255 - red) * factor else red * factor)).coerceIn(0f, 255f).toInt()
        val newGreen = (green + (if (factor > 0) (255 - green) * factor else green * factor)).coerceIn(0f, 255f).toInt()
        val newBlue = (blue + (if (factor > 0) (255 - blue) * factor else blue * factor)).coerceIn(0f, 255f).toInt()
        
        return (alpha shl 24) or (newRed shl 16) or (newGreen shl 8) or newBlue
    }
    
    // Animation durations (in milliseconds) - keep as constants
    const val ANIMATION_DURATION_SHORT = 150L
    const val ANIMATION_DURATION_MEDIUM = 250L
    const val ANIMATION_DURATION_LONG = 400L
    
    // Sizing constants - keep as constants
    const val BUTTON_HEIGHT = 32
    const val BUTTON_HEIGHT_SMALL = 24
    const val BUTTON_HEIGHT_LARGE = 40
    const val BORDER_RADIUS = 4
    const val CARD_PADDING = 16
    const val COMPONENT_SPACING = 8
}