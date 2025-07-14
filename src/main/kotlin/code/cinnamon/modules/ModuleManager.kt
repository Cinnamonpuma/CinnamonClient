package code.cinnamon.modules

import code.cinnamon.modules.all.AutoclickerModule
import code.cinnamon.modules.all.ChatPrefixModule
import code.cinnamon.modules.all.FullbrightModule
import code.cinnamon.modules.all.CalculatorModule

object ModuleManager {
    private val modules = mutableListOf<Module>()

    fun registerModule(module: Module) {
        modules.add(module)
    }
    fun getModules(): List<Module> = modules.toList()
    fun getModule(name: String): Module? = modules.find { it.name == name }
    fun enableModule(name: String) = getModule(name)?.enable()
    fun disableModule(name: String) = getModule(name)?.disable()
    fun toggleModule(name: String) = getModule(name)?.toggle()
    fun getEnabledModules(): List<Module> = modules.filter { it.isEnabled }


    fun isModuleEnabled(name: String): Boolean = getModule(name)?.isEnabled ?: false

    fun initialize() {
        registerModule(AutoclickerModule())
        registerModule(ChatPrefixModule())
        registerModule(FullbrightModule())
        registerModule(CalculatorModule())
    }
}

abstract class Module(val name: String, val description: String) {
    val settings = mutableListOf<Setting<*>>()
    var isEnabled = false
        private set

    open fun enable(fromLoad: Boolean = false) {
        if (!isEnabled) {
            isEnabled = true
            onEnable()
            if (!fromLoad) {
                ModuleConfigManager.saveModules()
            }
        }
    }
    open fun disable(fromLoad: Boolean = false) {
        if (isEnabled) {
            isEnabled = false
            onDisable()
            if (!fromLoad) {
                ModuleConfigManager.saveModules()
            }
        }
    }
    fun toggle() {
        if (isEnabled) disable(fromLoad = false) else enable(fromLoad = false)
    }
    protected abstract fun onEnable()
    protected abstract fun onDisable()
}