package code.cinnamon.modules

sealed class Setting<T>(val name: String, var value: T)

class BooleanSetting(name: String, value: Boolean) : Setting<Boolean>(name, value)
class LookAtHudSetting(name: String, value: Boolean) : Setting<Boolean>(name, value)
class DoubleSetting(name: String, value: Double, val min: Double, val max: Double, val step: Double) : Setting<Double>(name, value)
class ColorSetting(name: String, value: Int) : Setting<Int>(name, value)
class ModeSetting(name: String, value: String, val modes: List<String>) : Setting<String>(name, value)
