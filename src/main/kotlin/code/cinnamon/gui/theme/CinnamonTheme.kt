package code.cinnamon.gui.theme

data class ThemeColors(
    val coreBackgroundPrimary: Int,
    val coreAccentPrimary: Int,
    val coreTextPrimary: Int,
    val coreBorder: Int,
    val coreButtonBackgroundDef: Int,
)

enum class Theme(val colors: ThemeColors) {
    DARK(
        ThemeColors(
            coreBackgroundPrimary = 0xE61a1a1a.toInt(),
            coreAccentPrimary = 0xFF00aaff.toInt(),
            coreTextPrimary = 0xFFe0e0e0.toInt(),
            coreBorder = 0xFF404040.toInt(),
            coreButtonBackgroundDef = 0xFF404040.toInt()
        )
    ),
    LIGHT(
        ThemeColors(
            coreBackgroundPrimary = 0xE6FAFAFA.toInt(),
            coreAccentPrimary = 0xFF007ACC.toInt(),
            coreTextPrimary = 0xFF202020.toInt(),
            coreBorder = 0xFFCCCCCC.toInt(),
            coreButtonBackgroundDef = 0xFFDDDDDD.toInt()
        )
    )
}

object CinnamonTheme {

    var coreBackgroundPrimary = 0
    var coreAccentPrimary = 0
    var coreTextPrimary = 0
    var coreBorder = 0
    var coreButtonBackground = 0

    var coreStatusSuccess = 0xFF4caf50.toInt()
    var coreStatusWarning = 0xFFff9800.toInt()
    var coreStatusError = 0xFFf44336.toInt()
    var enableTextShadow: Boolean = true
    var useMinecraftFont: Boolean = true

    var currentTheme: Theme = Theme.DARK

    var patternColor = 0x10ffffff.toInt()
    var overlayColor = 0x80000000.toInt()
    var glassHighlight = 0x20ffffff.toInt()
    var glassShadow = 0x40000000.toInt()

    var cardBackgroundHover: Int = 0
    var accentColorHover: Int = 0
    var accentColorPressed: Int = 0
    var primaryButtonBackground: Int = 0
    var primaryButtonBackgroundHover: Int = 0
    var primaryButtonBackgroundPressed: Int = 0
    var buttonBackgroundHover: Int = 0
    var buttonBackgroundPressed: Int = 0
    var moduleEnabledColor: Int = 0


    var buttonOutlineColor: Int = 0xFF808080.toInt()
    var buttonOutlineHoverColor: Int = 0xFFA0A0A0.toInt()

    val guiBackground: Int get() = coreBackgroundPrimary
    val backgroundTop: Int get() = coreBackgroundPrimary
    val backgroundBottom: Int get() = adjustBrightness(coreBackgroundPrimary, -0.05f)

    val headerBackground: Int get() = coreBackgroundPrimary
    val footerBackground: Int get() = coreBackgroundPrimary
    val contentBackground: Int get() = coreBackgroundPrimary
    val sidebarBackground: Int get() = coreBackgroundPrimary

    val cardBackground: Int get() = coreBackgroundPrimary


    val borderColor: Int get() = coreBorder
    val accentColor: Int get() = coreAccentPrimary

    val titleColor: Int get() = coreTextPrimary
    val primaryTextColor: Int get() = coreTextPrimary
    val secondaryTextColor: Int get() = coreTextPrimary
    val disabledTextColor: Int get() = adjustBrightness(coreTextPrimary, -0.3f)


    val buttonBackground: Int get() = coreButtonBackground
    val buttonBackgroundDisabled: Int get() = adjustBrightness(coreButtonBackground, -0.3f)

    val successColor: Int get() = coreStatusSuccess
    val warningColor: Int get() = coreStatusWarning
    val errorColor: Int get() = coreStatusError
    val infoColor: Int get() = coreAccentPrimary


    val moduleDisabledColor: Int get() = adjustBrightness(coreButtonBackground, -0.1f)
    val moduleBackgroundEnabled: Int get() = adjustBrightness(coreStatusSuccess, -0.3f)
    val moduleBackgroundDisabled: Int get() = adjustBrightness(coreButtonBackground, -0.2f)

    init {
        resetToDefaults()
    }

    fun applyTheme(theme: Theme) {
        currentTheme = theme

        coreBackgroundPrimary = theme.colors.coreBackgroundPrimary
        coreAccentPrimary = theme.colors.coreAccentPrimary
        coreTextPrimary = theme.colors.coreTextPrimary
        coreBorder = theme.colors.coreBorder
        coreButtonBackground = theme.colors.coreButtonBackgroundDef

        buttonOutlineColor = coreAccentPrimary
        buttonOutlineHoverColor = adjustBrightness(coreAccentPrimary, -0.1f)

        updateDependentColors()
    }

    fun resetToDefaults() {
        coreBackgroundPrimary = -31647720
        coreAccentPrimary = 2117583233
        coreTextPrimary = -4605511
        coreBorder = 2117976462
        coreButtonBackground = 20987968

        coreStatusSuccess = 743210671
        coreStatusWarning = 452958208
        coreStatusError = 468992565

        buttonOutlineColor = 2114896241
        buttonOutlineHoverColor = -13524631

        enableTextShadow = false
        useMinecraftFont = true

        patternColor = 0x10ffffff.toInt()
        overlayColor = 0x80000000.toInt()
        glassHighlight = 0x20ffffff.toInt()
        glassShadow = 0x40000000.toInt()

        updateDependentColors()
    }

    fun updateDependentColors() {
        cardBackgroundHover = adjustBrightness(coreBackgroundPrimary, 0.05f)
        accentColorHover = adjustBrightness(coreAccentPrimary, -0.1f)
        accentColorPressed = adjustBrightness(coreAccentPrimary, -0.2f)
        primaryButtonBackground = (coreAccentPrimary and 0x00FFFFFF) or 0xFF000000.toInt()
        primaryButtonBackgroundHover = accentColorHover
        primaryButtonBackgroundPressed = accentColorPressed
        buttonBackgroundHover = adjustBrightness(coreButtonBackground, 0.1f)
        buttonBackgroundPressed = adjustBrightness(coreButtonBackground, -0.1f)
        moduleEnabledColor = coreStatusSuccess
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


    const val ANIMATION_DURATION_SHORT = 150L
    const val ANIMATION_DURATION_MEDIUM = 250L
    const val ANIMATION_DURATION_LONG = 400L

    const val BUTTON_HEIGHT = 32
    const val BUTTON_HEIGHT_SMALL = 24
    const val BUTTON_HEIGHT_LARGE = 40
    const val BORDER_RADIUS = 4
    const val CARD_PADDING = 16
    const val COMPONENT_SPACING = 8

    fun getCurrentFont(): net.minecraft.util.Identifier {
        return if (useMinecraftFont) {
            net.minecraft.util.Identifier.of("minecraft", "default")
        } else {
            code.cinnamon.gui.CinnamonScreen.CINNA_FONT
        }
    }
}
