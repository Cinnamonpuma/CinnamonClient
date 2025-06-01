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
        sendMessage("Proxy: ${config.targetServerHost}:${config.targetServerPort} <- :${config.proxyPort}", Formatting.AQUA)
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
        
        sendMessage(PacketCombinerAccess.getDetailedStatus(), Formatting.WHITE)
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
            val result = PacketCombinerAccess.setEnabled(true)
            if (result) {
                sendMessage("PacketCombiner enabled", Formatting.GREEN)
            } else {
                sendMessage("Module already enabled or not available", Formatting.YELLOW)
            }
            true
        } catch (e: Exception) {
            sendMessage("Error enabling module: ${e.message}", Formatting.RED)
            false
        }
    }
    
    private fun disableModule(): Boolean {
        return try {
            val result = PacketCombinerAccess.setEnabled(false)
            if (result) {
                sendMessage("PacketCombiner disabled", Formatting.GREEN)
            } else {
                sendMessage("Module already disabled or not available", Formatting.YELLOW)
            }
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
            sendMessage("Usage: /combiner config <show|reset|export|import|<setting> <value>>", Formatting.YELLOW)
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
            "export" -> {
                val exported = PacketCombinerConfigManager.exportConfig()
                sendMessage("Configuration exported:", Formatting.GREEN)
                sendMessage(exported, Formatting.WHITE)
                true
            }
            "debug" -> {
                if (args.size < 2) {
                    sendMessage("Usage: /combiner config debug <on|off>", Formatting.YELLOW)
                    return false
                }
                when (args[1].lowercase()) {
                    "on" -> {
                        PacketCombinerConfigManager.enableDebugMode()
                        sendMessage("Debug mode enabled", Formatting.GREEN)
                        true
                    }
                    "off" -> {
                        PacketCombinerConfigManager.optimizeForPerformance()
                        sendMessage("Debug mode disabled, optimized for performance", Formatting.GREEN)
                        true
                    }
                    else -> {
                        sendMessage("Use 'on' or 'off'", Formatting.RED)
                        false
                    }
                }
            }
            else -> {
                sendMessage("Unknown config setting: ${args[0]}", Formatting.RED)
                sendMessage("Available: show, reset, export, debug", Formatting.YELLOW)
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
                try {
                    PacketCombinerConfigManager.quickSetupLocal(port)
                    sendMessage("Configured for local server on port $port", Formatting.GREEN)
                    true
                } catch (e: Exception) {
                    sendMessage("Error setting up local server: ${e.message}", Formatting.RED)
                    false
                }
            }
            "remote" -> {
                if (args.size < 2) {
                    sendMessage("Usage: /combiner setup remote <address>", Formatting.YELLOW)
                    return false
                }
                try {
                    PacketCombinerConfigManager.quickSetupRemote(args[1])
                    sendMessage("Configured for remote server ${args[1]}", Formatting.GREEN)
                    true
                } catch (e: Exception) {
                    sendMessage("Error setting up remote server: ${e.message}", Formatting.RED)
                    false
                }
            }
            else -> {
                sendMessage("Unknown setup type: ${args[0]}", Formatting.RED)
                sendMessage("Available: local, remote", Formatting.YELLOW)
                false
            }
        }
    }
    
    private fun handlePresetCommand(args: List<String>): Boolean {
        if (args.isEmpty()) {
            sendMessage("Available presets:", Formatting.YELLOW)
            sendMessage("  local - Local server preset", Formatting.WHITE)
            sendMessage("  hypixel - Hypixel server preset", Formatting.WHITE)
            sendMessage("  testing - Testing/debugging preset", Formatting.WHITE)
            sendMessage("  production - Production server preset", Formatting.WHITE)
            return true
        }
        
        return try {
            val config = when (args[0].lowercase()) {
                "local" -> PacketCombinerConfigManager.getLocalServerPreset()
                "hypixel" -> PacketCombinerConfigManager.getHypixelPreset()
                "testing" -> PacketCombinerConfigManager.getTestingPreset()
                "production" -> PacketCombinerConfigManager.getProductionPreset()
                else -> {
                    sendMessage("Unknown preset: ${args[0]}", Formatting.RED)
                    return false
                }
            }
            
            PacketCombinerConfigManager.updateConfig(config)
            sendMessage("Loaded ${args[0]} preset", Formatting.GREEN)
            true
        } catch (e: Exception) {
            sendMessage("Error loading preset: ${e.message}", Formatting.RED)
            false
        }
    }
    
    private fun handleDebugCommand(args: List<String>): Boolean {
        if (args.isEmpty()) {
            sendMessage("Usage: /combiner debug <on|off|packets|clear|stats>", Formatting.YELLOW)
            return false
        }
        
        return when (args[0].lowercase()) {
            "on" -> {
                PacketCombinerConfigManager.enableDebugMode()
                sendMessage("Debug mode enabled", Formatting.GREEN)
                true
            }
            "off" -> {
                PacketCombinerConfigManager.optimizeForPerformance()
                sendMessage("Debug mode disabled", Formatting.RED)
                true
            }
            "packets" -> {
                showPacketDebugInfo()
                true
            }
            "clear" -> {
                PacketCombinerAccess.clearPendingPackets()
                sendMessage("Cleared all pending packets", Formatting.GREEN)
                true
            }
            "stats" -> {
                sendMessage(PacketCombinerAccess.getStats(), Formatting.WHITE)
                true
            }
            else -> {
                sendMessage("Unknown debug command: ${args[0]}", Formatting.RED)
                false
            }
        }
    }
    
    private fun showPacketDebugInfo() {
        sendMessage("=== Packet Debug Information ===", Formatting.GOLD)
        sendMessage("Handshake Interception: ${PacketCombinerAccess.isInterceptingHandshake()}", Formatting.WHITE)
        sendMessage("Login Interception: ${PacketCombinerAccess.isInterceptingLogin()}", Formatting.WHITE)
        sendMessage("Module Active: ${PacketCombinerAccess.isActive()}", Formatting.WHITE)
        
        val pendingIntention = PacketCombinerAccess.getPendingIntention()
        val pendingLogin = PacketCombinerAccess.getPendingLogin()
        
        sendMessage("Pending Handshake: ${if (pendingIntention != null) "Yes" else "No"}", Formatting.WHITE)
        sendMessage("Pending Login: ${if (pendingLogin != null) "Yes" else "No"}", Formatting.WHITE)
        
        sendMessage("Detailed Status:", Formatting.YELLOW)
        sendMessage(PacketCombinerAccess.getDetailedStatus(), Formatting.WHITE)
    }
    
    // Utility method for sending messages to the player
    private fun sendMessage(message: String, formatting: Formatting) {
        client.player?.sendMessage(
            Text.literal(message).formatted(formatting),
            false
        )
    }
}