package code.cinnamon.hud

import kotlinx.serialization.Serializable

@Serializable
data class HudElementConfig(
    val name: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val isEnabled: Boolean,
    val textColor: Int = 0xFFFFFF,
    val backgroundColor: Int = 0x80000000.toInt(),
    val textShadowEnabled: Boolean = true
)
