package code.cinnamon.modules

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory

object ModuleManager {
    private val modules = mutableListOf<Module>()
    private val logger = LoggerFactory.getLogger("cinnamon")

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
        val reflections = Reflections("code.cinnamon.modules.all", Scanners.SubTypes)
        val moduleClasses = reflections.getSubTypesOf(Module::class.java)

        for (moduleClass in moduleClasses) {
            try {
                val constructor = moduleClass.getConstructor()
                val module = constructor.newInstance()
                registerModule(module)
                logger.info("Registered module: ${module.name}")
            } catch (e: Exception) {
                logger.error("Failed to register module: ${moduleClass.simpleName}", e)
            }
        }
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