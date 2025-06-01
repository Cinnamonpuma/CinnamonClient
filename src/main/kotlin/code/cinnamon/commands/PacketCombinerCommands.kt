// PacketCombinerCommands.kt
package code.cinnamon.commands

import code.cinnamon.modules.all.JoinPacketCombinerModule
import code.cinnamon.config.PacketCombinerConfigManager
import code.cinnamon.util.PacketCombinerAccess
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.Formatting

/**
 * Command interface for the PacketCombiner mod
 * Provides easy-to-use commands for configuration and control
 */
object PacketCombinerCommands {
    
    private val client: MinecraftClient
        get() = MinecraftClient.getInstance()
    
    // Main command dispatcher
    fun executeCommand(args: List<String>): Boolean {
        if (args.isEmpty()) {
            showHelp()
            return true
        }
        
        return when (args[0].lowercase()) {
            "help", "?" -> { showHelp(); true }
            "status", "info" -> { showStatus(); true }
            "enable", "on" -> { enableModule(); true }
            "disable", "off" -> { disableModule(); true }
            "start" -> { startProxy(); true }
            "stop" -> { stopProxy(); true }
            "config" -> { handleConfigCommand(args.drop(1)); true }
            "setup" -> { handleSetupCommand(args.drop(1)); true }
            "preset" -> { handlePresetCommand(args.drop(1)); true }
            "debug" -> { handleDebugCommand(args.drop(1)); true }
            "stats" -> { showStats(); true }
            "reset" -> { resetModule(); true }
            else -> { 
                sendMessage("Unknown command: ${args[0]}. Use 'combiner help' for available commands.", Formatting.RED)
                false 
            }
        }
    }
    
    private fun showHelp() {
        sendMessage("=== PacketCombiner Commands ===", Formatting.GOLD)
        sendMessage("Basic Controls:", Formatting.YELLOW)
        sendMessage("  /combiner enable/disable - Enable/disable the module", Formatting.WHITE)
        sendMessage("  /combiner start/stop - Start/stop the proxy server", Formatting.WHITE)
        sendMessage("  /combiner status - Show current status", Formatting.WHITE)
        sendMessage("  /combiner stats - Show detailed statistics", Formatting.WHITE)
        sendMessage("", Formatting.WHITE)
        
        sendMessage("Configuration:", Formatting.YELLOW)
        sendMessage("  /combiner config <setting> <value> - Change configuration", Formatting.WHITE)
        sendMessage("  /combiner config show - Show current configuration", Formatting.WHITE)
        sendMessage("  /combiner config reset - Reset to defaults", Formatting.WHITE)
        sendMessage("", Formatting.WHITE)
        
        sendMessage("Quick Setup:", Formatting.YELLOW)
        sendMessage("  /combiner setup local [port] - Setup for local server", Formatting.WHITE)
        sendMessage("  /combiner setup remote <address> - Setup for remote server", Formatting.WHITE)
        sendMessage("  /combiner preset <name> - Load configuration preset", Formatting.WHITE)
        sendMessage("", Formatting.WHITE)
        
        sendMessage("Debug:", Formatting.YELLOW)
        sendMessage("  /combiner debug on/off - Toggle debug mode", Formatting.WHITE)
        sendMessage("  /combiner debug packets - Show packet interception status", Formatting.WHITE)
        sendMessage("  /combiner debug clear - Clear pending packets", Formatting.WHITE)
    }
    
    private fun showStatus() {
        val module = JoinPacketCombinerModule.instance
        
        if (module == null) {
            sendMessage("PacketCombiner module not initialized", Formatting.RED)
            return
        }
        
        sendMessage("=== PacketCombiner Status ===", Formatting.GOLD)
        sendMessage("Module: ${if (module.isEnabled) "ENABLED" else "DISABLED"}", 
            if (module.isEnabled) Formatting.GREEN else Formatting.RED)
        
        val config = PacketCombinerConfigManager.getConfig()
        sendMessage("Proxy: ${config.targetServerHost}:${config.targetServerPort} <- :${config.proxyPort}", Formatting.CYAN)
        sendMessage("Mode: ${config.combinationMode}", Formatting.YELLOW)
        sendMessage("Connections: ${module.activeConnections}/2", Formatting.WHITE)
        sendMessage("Combined Logins: ${module.totalCombinedLogins}", Formatting.WHITE)
        
        if (PacketCombinerAccess.isIntercepting()) {
            sendMessage("Status: Intercepting packets", Formatting.YELLOW)
        } else {
            sendMessage("Status: Ready", Formatting.GREEN)
        }
    }
    
    private fun showStats() {
        val module = JoinPacketCombinerModule.instance
        
        if (module == null) {
            sendMessage("Module not available", Formatting.RED)
            return
        }
        
        sendMessage(module.getDetailedStatus(), Formatting.WHITE)
        sendMessage("", Formatting.WHITE)
        sendMessage("Packet Interception:", Formatting.YELLOW)
        sendMessage("  Handshake: ${if (PacketCombinerAccess.isInterceptingHandshake()) "Active" else "Idle"}", Formatting.WHITE)
        sendMessage("  Login: ${if (PacketCombinerAccess.isInterceptingLogin()) "Active" else "Idle"}", Formatting.WHITE)
        
        val config = PacketCombinerConfigManager.getConfig()
        sendMessage("", Formatting.WHITE)
        sendMessage("Configuration Validity: ${PacketCombinerConfigManager.validateCurrentConfig()}", 
            if (config.isValid()) Formatting.GREEN else Formatting.RED)
    }
    
    private fun enableModule(): Boolean {
        return try {
            val module = JoinPacketCombinerModule.instance
            if (module == null) {
                sendMessage("Module not available", Formatting.RED)
                return false
            }
            
            if (module.isEnabled) {
                sendMessage("Module is already enabled", Formatting.YELLOW)
                return true
            }
            
            module.enable()
            sendMessage("PacketCombiner enabled", Formatting.GREEN)
            true
        } catch (e: Exception) {
            sendMessage("Error enabling module: ${e.message}", Formatting.RED)
            false
        }
    }
    
    private fun disableModule(): Boolean {
        return try {
            val module = JoinPacketCombinerModule.instance
            if (module == null) {
                sendMessage("Module not available", Formatting.RED)
                return false
            }
            
            if (!module.isEnabled) {
                sendMessage("Module is already disabled", Formatting.YELLOW)
                return true
            }
            
            module.disable()
            sendMessage("PacketCombiner disabled", Formatting.RED)
            true
        } catch (e: Exception) {
            sendMessage("Error disabling module: ${e.message}", Formatting.RED)
            false
        }
    }
    
    private fun startProxy(): Boolean {
        return enableModule() // Starting proxy is same as enabling module
    }
    
    private fun stopProxy(): Boolean {
        return disableModule() // Stopping proxy is same as disabling module
    }
    
    private fun resetModule(): Boolean {
        return try {
            JoinPacketCombinerModule.instance?.resetStats()
            PacketCombinerAccess.emergencyCleanup()
            PacketCombinerConfigManager.resetToDefaults()
            sendMessage("PacketCombiner reset to defaults", Formatting.GREEN)
            true
        } catch (e: Exception) {
            sendMessage("Error resetting module: ${e.message}", Formatting.RED)
            false
        }
    }
    
    private fun handleConfigCommand(args: List<String>): Boolean {
        if (args.isEmpty()) {
            sendMessage("Usage: /combiner config <show|reset|<setting> <value>>", Formatting.YELLOW)
            return false
        }
        
        return when (args[0].lowercase()) {
            "show" -> { 
                sendMessage(PacketCombinerConfigManager.getConfigSummary(), Formatting.WHITE)
                true 
            }
            "reset" -> {
                PacketCombinerConfigManager.resetToDefaults()
                sendMessage("Configuration reset to defaults", Formatting.GREEN)
                true
            }
            "port" -> {
                if (args.size < 2) {
                    sendMessage("Usage: /combiner config port <port>", Formatting.YELLOW)
                    return false
                }
                setConfigPort(args[1])
            }
            "server" -> {
                if (args.size < 2) {
                    sendMessage("Usage: /combiner config server <host:port>", Formatting.YELLOW)
                    return false
                }
                setConfigServer(args[1])
            }
            "mode" -> {
                if (args.size < 2) {
                    sendMessage("Usage: /combiner config mode <PRIMARY_WINS|FUSE_DATA|RANDOM_SELECTION>", Formatting.YELLOW)
                    return false
                }
                setConfigMode(args[1])
            }
            "mirror" -> {
                if (args.size < 2) {
                    sendMessage("Usage: /combiner config mirror <true|false>", Formatting.YELLOW)
                    return false
                }
                setConfigMirror(args[1])
            }
            else -> {
                sendMessage("Unknown config setting: ${args[0]}", Formatting.RED)
                sendMessage("Available: port, server, mode, mirror", Formatting.YELLOW)
                false
            }
        }
    }
    
    private fun handleSetupCommand(args: List<String>): Boolean {
        if (args.isEmpty()) {
            sendMessage("Usage: /combiner setup <local|remote> [arguments]", Formatting.YELLOW)
            return false
        }
        
        return when (args[0].lowercase()) {
            "local" -> {
                val port = if (args.size > 1) args[1].toIntOrNull() ?: 25565 else 25565
                PacketCombinerConfigManager.quickSetupLocal(port)
                sendMessage("Configured for local server on port $port", Formatting.GREEN)
                true
            }
            "remote" -> {
                if (args.size < 2) {
                    sendMessage("Usage: /combiner setup remote <address>", Formatting.YELLOW)
                    return false
                }
                PacketCombinerConfigManager.quickSetupRemote(args[1])
                sendMessage("Configured for remote server ${args[1]}",