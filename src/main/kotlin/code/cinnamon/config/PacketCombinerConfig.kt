// PacketCombinerConfig.kt
package code.cinnamon.config

import code.cinnamon.modules.all.JoinPacketCombinerModule
import code.cinnamon.util.PacketCombinerAccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException

@Serializable
data class PacketCombinerConfig(
    // Basic settings
    val proxyPort: Int = 25566,
    val targetServerHost: String = "localhost", 
    val targetServerPort: Int = 25565,
    val autoStartProxy: Boolean = false,
    
    // Combination settings
    val combinationMode: String = "PRIMARY_WINS", // PRIMARY_WINS, FUSE_DATA, RANDOM_SELECTION
    val primaryClientId: String = "",
    val maxWaitTimeMs: Long = 30000,
    val maxClients: Int = 2,
    
    // Advanced features
    val enablePacketMirror: Boolean = true,
    val mirrorGameplayPackets: Boolean = false,
    val logAllPackets: Boolean = false,
    val debugMode: Boolean = false,
    
    // Security settings
    val allowedClients: List<String> = emptyList(),
    val requireAuthentication: Boolean = false,
    val encryptCombinedData: Boolean = false,
    
    // Performance settings
    val bufferSize: Int = 8192,
    val workerThreads: Int = 2,
    val connectionTimeoutMs: Long = 10000,
    
    // Experimental features
    val enableSmartCombination: Boolean = false,
    val useHeuristicSelection: Boolean = false,
    val adaptiveTiming: Boolean = false
) {
    
    companion object {
        private const val CONFIG_FILE = "config/packet_combiner.json"
        private val json = Json { 
            prettyPrint = true
            ignoreUnknownKeys = true
        }
        
        fun load(): PacketCombinerConfig {
            return try {
                val file = File(CONFIG_FILE)
                if (file.exists()) {
                    val content = file.readText()
                    json.decodeFromString<PacketCombinerConfig>(content)
                } else {
                    val default = PacketCombinerConfig()
                    default.save()
                    default
                }
            } catch (e: Exception) {
                println("PacketCombiner: Error loading config, using defaults: ${e.message}")
                PacketCombinerConfig()
            }
        }
    }
    
    fun save() {
        try {
            val file = File(CONFIG_FILE)
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(this))
            println("PacketCombiner: Configuration saved to $CONFIG_FILE")
        } catch (e: IOException) {
            println("PacketCombiner: Error saving config: ${e.message}")
        }
    }
    
    fun applyToModule(module: JoinPacketCombinerModule) {
        try {
            module.setProxyPort(proxyPort)
            module.setTargetServer(targetServerHost, targetServerPort)
            module.setPrimaryClient(primaryClientId)
            module.setPacketMirrorEnabled(enablePacketMirror)
            
            // Set combination mode
            val mode = when (combinationMode.uppercase()) {
                "PRIMARY_WINS" -> JoinPacketCombinerModule.CombinationMode.PRIMARY_WINS
                "FUSE_DATA" -> JoinPacketCombinerModule.CombinationMode.FUSE_DATA
                "RANDOM_SELECTION" -> JoinPacketCombinerModule.CombinationMode.RANDOM_SELECTION
                else -> JoinPacketCombinerModule.CombinationMode.PRIMARY_WINS
            }
            module.setCombinationMode(mode)
            
            println("PacketCombiner: Configuration applied to module")
        } catch (e: Exception) {
            println("PacketCombiner: Error applying config: ${e.message}")
        }
    }
    
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (proxyPort < 1024 || proxyPort > 65535) {
            errors.add("Proxy port must be between 1024 and 65535")
        }
        
        if (targetServerPort < 1 || targetServerPort > 65535) {
            errors.add("Target server port must be between 1 and 65535")
        }
        
        if (targetServerHost.isBlank()) {
            errors.add("Target server host cannot be empty")
        }
        
        if (maxWaitTimeMs < 1000) {
            errors.add("Max wait time should be at least 1000ms")
        }
        
        if (maxClients < 1 || maxClients > 10) {
            errors.add("Max clients must be between 1 and 10")
        }
        
        if (bufferSize < 1024 || bufferSize > 1024 * 1024) {
            errors.add("Buffer size must be between 1KB and 1MB")
        }
        
        if (workerThreads < 1 || workerThreads > 16) {
            errors.add("Worker threads must be between 1 and 16")
        }
        
        return errors
    }
    
    fun isValid(): Boolean = validate().isEmpty()
    
    fun createQuickSetup(serverAddress: String): PacketCombinerConfig {
        val parts = serverAddress.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 25565 else 25565
        
        return copy(
            targetServerHost = host,
            targetServerPort = port,
            autoStartProxy = true,
            combinationMode = "PRIMARY_WINS",
            enablePacketMirror = true,
            debugMode = true
        )
    }
}

// Configuration manager with presets
object PacketCombinerConfigManager {
    
    private var currentConfig = PacketCombinerConfig.load()
    
    fun getConfig(): PacketCombinerConfig = currentConfig
    
    fun updateConfig(newConfig: PacketCombinerConfig) {
        val errors = newConfig.validate()
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException("Configuration errors: ${errors.joinToString(", ")}")
        }
        
        currentConfig = newConfig
        currentConfig.save()
        
        // Apply to active module if available
        JoinPacketCombinerModule.instance?.let { module ->
            currentConfig.applyToModule(module)
        }
    }
    
    fun resetToDefaults() {
        currentConfig = PacketCombinerConfig()
        currentConfig.save()
    }
    
    // Preset configurations
    fun getLocalServerPreset(): PacketCombinerConfig {
        return PacketCombinerConfig(
            proxyPort = 25566,
            targetServerHost = "localhost",
            targetServerPort = 25565,
            combinationMode = "PRIMARY_WINS",
            enablePacketMirror = true,
            debugMode = true,
            autoStartProxy = true
        )
    }
    
    fun getHypixelPreset(): PacketCombinerConfig {
        return PacketCombinerConfig(
            proxyPort = 25566,
            targetServerHost = "mc.hypixel.net",
            targetServerPort = 25565,
            combinationMode = "FUSE_DATA",
            enablePacketMirror = false,
            debugMode = false,
            maxWaitTimeMs = 15000,
            requireAuthentication = true
        )
    }
    
    fun getTestingPreset(): PacketCombinerConfig {
        return PacketCombinerConfig(
            proxyPort = 25566,
            targetServerHost = "localhost",
            targetServerPort = 25565,
            combinationMode = "RANDOM_SELECTION",
            enablePacketMirror = true,
            mirrorGameplayPackets = true,
            logAllPackets = true,
            debugMode = true,
            enableSmartCombination = true,
            useHeuristicSelection = true,
            adaptiveTiming = true
        )
    }
    
    fun getProductionPreset(): PacketCombinerConfig {
        return PacketCombinerConfig(
            proxyPort = 25566,
            targetServerHost = "play.example.com",
            targetServerPort = 25565,
            combinationMode = "PRIMARY_WINS",
            enablePacketMirror = false,
            debugMode = false,
            requireAuthentication = true,
            encryptCombinedData = true,
            connectionTimeoutMs = 5000
        )
    }
    
    // Quick configuration methods
    fun quickSetupLocal(port: Int = 25565) {
        val config = getLocalServerPreset().copy(
            targetServerPort = port,
            autoStartProxy = true
        )
        updateConfig(config)
        println("PacketCombiner: Configured for local server on port $port")
    }
    
    fun quickSetupRemote(address: String) {
        val config = currentConfig.createQuickSetup(address)
        updateConfig(config)
        println("PacketCombiner: Configured for remote server $address")
    }
    
    fun enableDebugMode() {
        val config = currentConfig.copy(
            debugMode = true,
            logAllPackets = true,
            mirrorGameplayPackets = true
        )
        updateConfig(config)
        println("PacketCombiner: Debug mode enabled")
    }
    
    fun optimizeForPerformance() {
        val config = currentConfig.copy(
            debugMode = false,
            logAllPackets = false,
            mirrorGameplayPackets = false,
            bufferSize = 16384,
            workerThreads = 4,
            connectionTimeoutMs = 3000
        )
        updateConfig(config)
        println("PacketCombiner: Optimized for performance")
    }
    
    // Configuration validation and diagnostics
    fun validateCurrentConfig(): String {
        val errors = currentConfig.validate()
        return if (errors.isEmpty()) {
            "Configuration is valid"
        } else {
            "Configuration errors:\n" + errors.joinToString("\n") { "- $it" }
        }
    }
    
    fun getConfigSummary(): String {
        return buildString {
            appendLine("=== PacketCombiner Configuration ===")
            appendLine("Proxy Port: ${currentConfig.proxyPort}")
            appendLine("Target Server: ${currentConfig.targetServerHost}:${currentConfig.targetServerPort}")
            appendLine("Combination Mode: ${currentConfig.combinationMode}")
            appendLine("Max Clients: ${currentConfig.maxClients}")
            appendLine("Packet Mirroring: ${currentConfig.enablePacketMirror}")
            appendLine("Debug Mode: ${currentConfig.debugMode}")
            appendLine("Auto Start: ${currentConfig.autoStartProxy}")
            appendLine("Authentication Required: ${currentConfig.requireAuthentication}")
            if (currentConfig.primaryClientId.isNotEmpty()) {
                appendLine("Primary Client: ${currentConfig.primaryClientId}")
            }
            append("Status: ${if (currentConfig.isValid()) "Valid" else "Invalid"}")
        }
    }
    
    fun exportConfig(): String {
        return Json { prettyPrint = true }.encodeToString(currentConfig)
    }
    
    fun importConfig(jsonString: String): Boolean {
        return try {
            val config = Json.decodeFromString<PacketCombinerConfig>(jsonString)
            updateConfig(config)
            true
        } catch (e: Exception) {
            println("PacketCombiner: Error importing config: ${e.message}")
            false
        }
    }
}