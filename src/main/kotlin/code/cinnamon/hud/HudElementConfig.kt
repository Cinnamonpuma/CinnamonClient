package code.cinnamon.hud

import kotlinx.serialization.Serializable

@Serializable
data class HudElementConfig(
    val name: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val isEnabled: Boolean,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val backgroundColor: Int = 0x80000000.toInt(),
    val textShadowEnabled: Boolean = false,
    val keypressedTextColor: Int? = null,
    val keypressedBackgroundColor: Int? = null,
    val buttonColor: Int? = null,
    val buttonTextColor: Int? = null,
    val buttonTextShadowEnabled: Boolean? = null,
    val buttonHoverColor: Int? = null,
    val buttonOutlineColor: Int? = null,
)