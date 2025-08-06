package code.cinnamon.modules

sealed class Setting<T>(val name: String, var value: T, val callback: ((T) -> Unit)? = null) {
    fun set(newValue: T) {
        this.value = newValue
        callback?.invoke(newValue)
    }
}

class BooleanSetting(name: String, value: Boolean, callback: ((Boolean) -> Unit)? = null) : Setting<Boolean>(name, value, callback)
class LookAtHudSetting(name: String, value: Boolean, callback: ((Boolean) -> Unit)? = null) : Setting<Boolean>(name, value, callback)
class DoubleSetting(name: String, value: Double, val min: Double, val max: Double, val step: Double, callback: ((Double) -> Unit)? = null) : Setting<Double>(name, value, callback)
class ColorSetting(name: String, value: Int, callback: ((Int) -> Unit)? = null) : Setting<Int>(name, value, callback)
class ModeSetting(name: String, value: String, val modes: List<String>, callback: ((String) -> Unit)? = null) : Setting<String>(name, value, callback)
