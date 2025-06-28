package code.cinnamon.modules

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Paths

@Serializable
data class ModuleState(
    val name: String,
    val isEnabled: Boolean
)

@Serializable
data class ModulesConfig(
    val modules: List<ModuleState>
)

object ModuleConfigManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val configDir = Paths.get("config", "cinnamon").toFile()
    private val modulesFile = File(configDir, "modules.json")

    init {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }

    fun saveModules() {
        try {
            val moduleStates = ModuleManager.getModules().map { module ->
                ModuleState(module.name, module.isEnabled)
            }
            val config = ModulesConfig(moduleStates)
            
            val jsonString = json.encodeToString(config)
            modulesFile.writeText(jsonString)
            println("[ModuleConfigManager] Modules state saved successfully to ${modulesFile.absolutePath}")
        } catch (e: Exception) {
            println("[ModuleConfigManager] Failed to save modules state: ${e.message}")
            e.printStackTrace()
        }
    }

    fun loadModules() {
        try {
            if (!modulesFile.exists()) {
                println("[ModuleConfigManager] Modules file does not exist. No module states loaded.")
                return
            }

            val jsonString = modulesFile.readText()
            val config = json.decodeFromString<ModulesConfig>(jsonString)

            config.modules.forEach { moduleState ->
                val module = ModuleManager.getModule(moduleState.name)
                if (module != null) {
                    val wasEnabled = module.isEnabled 
                    if (moduleState.isEnabled) {
                        module.enable(fromLoad = true) 
                    } else {
                        module.disable(fromLoad = true)
                    }
                    if (moduleState.isEnabled) {
                        module.enable(fromLoad = true)
                    } else {
                        module.disable(fromLoad = true)
                    }
                } else {
                    println("[ModuleConfigManager] Found state for unknown module: ${moduleState.name}")
                }
            }
            println("[ModuleConfigManager] Modules state loaded successfully from ${modulesFile.absolutePath}")
        } catch (e: Exception) {
            println("[ModuleConfigManager] Failed to load modules state: ${e.message}")
            e.printStackTrace()
            println("[ModuleConfigManager] Applying default module states due to load failure.")

        }
    }
}
