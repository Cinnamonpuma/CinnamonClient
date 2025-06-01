// NoFallModule.kt
package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.util.NoFallAccess
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NoFallModule : Module("NoFall", "Prevents fall damage using various advanced methods") {

    // Current configuration
    var method: NoFallAccess.NoFallMethod = NoFallAccess.NoFallMethod.PACKET
        private set
    
    // Method-specific settings
    var velocityReduction = 2.0
        private set
    var velocityMultiplier = 0.6
        private set
    var maxVelocity = 10.0
        private set
    
    var teleportThreshold = 3.0
        private set
    var teleportDistance = 0.1
        private set
    var resetVelocityAfterTeleport = true
        private set
    
    var minActivationHeight = 3.0
        private set
    var activationCooldown = 100L
        private set
    
    var hybridVelocityThreshold = 5.0
        private set
    var hybridTeleportThreshold = 12.0
        private set
    
    // Advanced settings
    var onlyWhenFalling = true
        private set
    var respectWater = true
        private set
    var respectLava = true
        private set
    var ignoreVoidFalls = false
        private set
    var maxActivationsPerSecond = 20
        private set
    
    // Performance monitoring
    var enablePerformanceMonitoring = true
        private set
    var enableVerboseLogging = false
        private set
    
    companion object {
        @JvmStatic
        var instance: NoFallModule? = null
            private set
        
        private const val VOID_Y_LEVEL = -64.0
        private const val MIN_SAFE_VELOCITY = -0.5
        private const val WATER_SAFE_VELOCITY = -2.0
        private const val LAVA_SAFE_VELOCITY = -1.5
    }
    
    init {
        instance = this
    }
    
    override fun onEnable() {
        println("NoFall: Module enabled with method: ${method.displayName}")
        
        try {
            // Apply current settings to access layer
            NoFallAccess.setEnabled(true)
            NoFallAccess.setMethod(method)
            applyCurrentSettings()
            
            // Reset statistics for new session
            NoFallAccess.resetStatistics()
            
            if (enableVerboseLogging) {
                println("NoFall: Configuration applied successfully")
                logCurrentConfiguration()
            }
            
        } catch (e: Exception) {
            println("NoFall: Error enabling module: ${e.message}")
            disable()
        }
    }
    
    override fun onDisable() {
        println("NoFall: Module disabled")
        
        try {
            NoFallAccess.setEnabled(false)
            NoFallAccess.clearGroundSpoofRequest()
            
            if (enablePerformanceMonitoring) {
                logSessionStatistics()
            }
            
        } catch (e: Exception) {
            println("NoFall: Error disabling module: ${e.message}")
        }
    }
    
    private fun applyCurrentSettings() {
        // Apply velocity method settings
        NoFallAccess.setVelocityReduction(velocityReduction)
        NoFallAccess.setVelocityMultiplier(velocityMultiplier)
        NoFallAccess.setMaxVelocity(maxVelocity)
        
        // Apply teleport method settings
        NoFallAccess.setTeleportThreshold(teleportThreshold)
        NoFallAccess.setTeleportDistance(teleportDistance)
        NoFallAccess.setResetVelocityAfterTeleport(resetVelocityAfterTeleport)
        
        // Apply smart method settings
        NoFallAccess.setMinActivationHeight(minActivationHeight)
        NoFallAccess.setActivationCooldown(activationCooldown)
        
        // Apply hybrid method settings
        NoFallAccess.setHybridVelocityThreshold(hybridVelocityThreshold)
        NoFallAccess.setHybridTeleportThreshold(hybridTeleportThreshold)
        
        // Apply advanced settings
        NoFallAccess.setOnlyWhenFalling(onlyWhenFalling)
        NoFallAccess.setRespectWater(respectWater)
        NoFallAccess.setRespectLava(respectLava)
        NoFallAccess.setIgnoreVoidFalls(ignoreVoidFalls)
        NoFallAccess.setMaxActivationsPerSecond(maxActivationsPerSecond)
    }
    
    private fun logCurrentConfiguration() {
        println("NoFall Configuration:")
        println("  Method: ${method.displayName}")
        println("  Description: ${method.description}")
        
        when (method) {
            NoFallAccess.NoFallMethod.VELOCITY -> {
                println("  Velocity Reduction: $velocityReduction")
                println("  Velocity Multiplier: $velocityMultiplier")
                println("  Max Velocity: $maxVelocity")
            }
            NoFallAccess.NoFallMethod.TELEPORT -> {
                println("  Teleport Threshold: $teleportThreshold")
                println("  Teleport Distance: $teleportDistance")
                println("  Reset Velocity: $resetVelocityAfterTeleport")
            }
            NoFallAccess.NoFallMethod.SMART -> {
                println("  Min Activation Height: $minActivationHeight")
                println("  Activation Cooldown: ${activationCooldown}ms")
            }
            NoFallAccess.NoFallMethod.HYBRID -> {
                println("  Velocity Threshold: $hybridVelocityThreshold")
                println("  Teleport Threshold: $hybridTeleportThreshold")
            }
            else -> { /* Packet and Damage Cancel don't need extra config display */ }
        }
        
        println("  Advanced Settings:")
        println("    Only When Falling: $onlyWhenFalling")
        println("    Respect Water: $respectWater")
        println("    Respect Lava: $respectLava")
        println("    Ignore Void Falls: $ignoreVoidFalls")
        println("    Max Activations/sec: $maxActivationsPerSecond")
    }
    
    private fun logSessionStatistics() {
        val sessionTime = NoFallAccess.getSessionTime()
        val totalActivations = NoFallAccess.getTotalActivations()
        val packetsSpoofed = NoFallAccess.getPacketsSpoofed()
        val damagePrevented = NoFallAccess.getTotalDamagePrevented()
        val activationsPerSecond = NoFallAccess.getActivationsPerSecond()
        
        println("NoFall Session Statistics:")
        println("  Session Time: %.1f seconds".format(sessionTime))
        println("  Total Activations: $totalActivations")
        println("  Packets Spoofed: $packetsSpoofed")
        println("  Damage Prevented: %.1f hearts".format(damagePrevented))
        println("  Activations/sec: %.2f".format(activationsPerSecond))
        
        if (activationsPerSecond > maxActivationsPerSecond) {
            println("  WARNING: Rate limit exceeded!")
        }
    }
    
    // === Method Configuration ===
    
    fun setMethod(newMethod: NoFallAccess.NoFallMethod) {
        val oldMethod = method
        method = newMethod
        
        if (isEnabled) {
            NoFallAccess.setMethod(newMethod)
        }
        
        println("NoFall: Method changed from ${oldMethod.displayName} to ${newMethod.displayName}")
        
        if (enableVerboseLogging) {
            println("NoFall: ${newMethod.description}")
        }
    }
    
    fun cycleMethod() {
        val methods = NoFallAccess.getAllMethods()
        val currentIndex = methods.indexOf(method)
        val nextIndex = (currentIndex + 1) % methods.size
        setMethod(methods[nextIndex])
    }
    
    // === Velocity Method Configuration ===
    
    fun setVelocityReduction(reduction: Double) {
        velocityReduction = reduction.coerceIn(0.1, 50.0)
        if (isEnabled) {
            NoFallAccess.setVelocityReduction(velocityReduction)
        }
        println("NoFall: Velocity reduction set to $velocityReduction")
    }
    
    fun setVelocityMultiplier(multiplier: Double) {
        velocityMultiplier = multiplier.coerceIn(0.1, 1.0)
        if (isEnabled) {
            NoFallAccess.setVelocityMultiplier(velocityMultiplier)
        }
        println("NoFall: Velocity multiplier set to $velocityMultiplier")
    }
    
    fun setMaxVelocity(maxVel: Double) {
        maxVelocity = maxVel.coerceIn(1.0, 100.0)
        if (isEnabled) {
            NoFallAccess.setMaxVelocity(maxVelocity)
        }
        println("NoFall: Max velocity set to $maxVelocity")
    }
    
    // === Teleport Method Configuration ===
    
    fun setTeleportThreshold(threshold: Double) {
        teleportThreshold = threshold.coerceIn(0.5, 50.0)
        if (isEnabled) {
            NoFallAccess.setTeleportThreshold(teleportThreshold)
        }
        println("NoFall: Teleport threshold set to $teleportThreshold")
    }
    
    fun setTeleportDistance(distance: Double) {
        teleportDistance = distance.coerceIn(0.01, 2.0)
        if (isEnabled) {
            NoFallAccess.setTeleportDistance(teleportDistance)
        }
        println("NoFall: Teleport distance set to $teleportDistance")
    }
    
    fun setResetVelocityAfterTeleport(reset: Boolean) {
        resetVelocityAfterTeleport = reset
        if (isEnabled) {
            NoFallAccess.setResetVelocityAfterTeleport(reset)
        }
        println("NoFall: Reset velocity after teleport: $reset")
    }
    
    // === Smart Method Configuration ===
    
    fun setMinActivationHeight(height: Double) {
        minActivationHeight = height.coerceIn(0.5, 50.0)
        if (isEnabled) {
            NoFallAccess.setMinActivationHeight(minActivationHeight)
        }
        println("NoFall: Min activation height set to $minActivationHeight")
    }
    
    fun setActivationCooldown(cooldown: Long) {
        activationCooldown = cooldown.coerceIn(10L, 5000L)
        if (isEnabled) {
            NoFallAccess.setActivationCooldown(activationCooldown)
        }
        println("NoFall: Activation cooldown set to ${activationCooldown}ms")
    }
    
    // === Hybrid Method Configuration ===
    
    fun setHybridVelocityThreshold(threshold: Double) {
        hybridVelocityThreshold = threshold.coerceIn(1.0, 50.0)
        if (isEnabled) {
            NoFallAccess.setHybridVelocityThreshold(hybridVelocityThreshold)
        }
        println("NoFall: Hybrid velocity threshold set to $hybridVelocityThreshold")
    }
    
    fun setHybridTeleportThreshold(threshold: Double) {
        hybridTeleportThreshold = threshold.coerceIn(5.0, 100.0)
        if (isEnabled) {
            NoFallAccess.setHybridTeleportThreshold(hybridTeleportThreshold)
        }
        println("NoFall: Hybrid teleport threshold set to $hybridTeleportThreshold")
    }
    
    // === Advanced Settings ===
    
    fun setOnlyWhenFalling(onlyWhenFalling: Boolean) {
        this.onlyWhenFalling = onlyWhenFalling
        if (isEnabled) {
            NoFallAccess.setOnlyWhenFalling(onlyWhenFalling)
        }
        println("NoFall: Only when falling: $onlyWhenFalling")
    }
    
    fun setRespectWater(respect: Boolean) {
        respectWater = respect
        if (isEnabled) {
            NoFallAccess.setRespectWater(respect)
        }
        println("NoFall: Respect water: $respect")
    }
    
    fun setRespectLava(respect: Boolean) {
        respectLava = respect
        if (isEnabled) {
            NoFallAccess.setRespectLava(respect)
        }
        println("NoFall: Respect lava: $respect")
    }
    
    fun setIgnoreVoidFalls(ignore: Boolean) {
        ignoreVoidFalls = ignore
        if (isEnabled) {
            NoFallAccess.setIgnoreVoidFalls(ignore)
        }
        println("NoFall: Ignore void falls: $ignore")
    }
    
    fun setMaxActivationsPerSecond(maxActivations: Int) {
        maxActivationsPerSecond = maxActivations.coerceIn(1, 100)
        if (isEnabled) {
            NoFallAccess.setMaxActivationsPerSecond(maxActivationsPerSecond)
        }
        println("NoFall: Max activations per second set to $maxActivationsPerSecond")
    }
    
    // === Utility Settings ===
    
    fun setPerformanceMonitoring(enabled: Boolean) {
        enablePerformanceMonitoring = enabled
        println("NoFall: Performance monitoring: $enabled")
    }
    
    fun setVerboseLogging(enabled: Boolean) {
        enableVerboseLogging = enabled
        println("NoFall: Verbose logging: $enabled")
    }
    
    // === Preset Configurations ===
    
    fun applyLegitPreset() {
        setMethod(NoFallAccess.NoFallMethod.PACKET)
        setMinActivationHeight(4.0)
        setActivationCooldown(200L)
        setOnlyWhenFalling(true)
        setRespectWater(true)
        setRespectLava(true)
        setMaxActivationsPerSecond(10)
        println("NoFall: Applied Legit preset")
    }
    
    fun applyGhostPreset() {
        setMethod(NoFallAccess.NoFallMethod.SMART)
        setMinActivationHeight(2.0)
        setActivationCooldown(50L)
        setVelocityMultiplier(0.8)
        setTeleportDistance(0.05)
        setOnlyWhenFalling(true)
        setMaxActivationsPerSecond(15)
        println("NoFall: Applied Ghost preset")
    }
    
    fun applyAggressivePreset() {
        setMethod(NoFallAccess.NoFallMethod.HYBRID)
        setMinActivationHeight(1.0)
        setActivationCooldown(10L)
        setVelocityMultiplier(0.5)
        setTeleportDistance(0.15)
        setHybridVelocityThreshold(3.0)
        setHybridTeleportThreshold(8.0)
        setMaxActivationsPerSecond(30)
        println("NoFall: Applied Aggressive preset")
    }
    
    fun applySafePreset() {
        setMethod(NoFallAccess.NoFallMethod.DAMAGE_CANCEL)
        setMinActivationHeight(6.0)
        setOnlyWhenFalling(true)
        setRespectWater(true)
        setRespectLava(true)
        setIgnoreVoidFalls(true)
        setMaxActivationsPerSecond(5)
        println("NoFall: Applied Safe preset")
    }
    
    // === Status and Information ===
    
    fun getCurrentStatus(): String {
        return buildString {
            append("NoFall: ${if (isEnabled) "ON" else "OFF"}")
            if (isEnabled) {
                append(" | Method: ${method.displayName}")
                
                val activations = NoFallAccess.getTotalActivations()
                val sessionTime = NoFallAccess.getSessionTime()
                val avgRate = if (sessionTime > 0) activations / sessionTime else 0.0
                
                append(" | Activations: $activations")
                append(" | Rate: %.1f/s".format(avgRate))
                
                if (method == NoFallAccess.NoFallMethod.PACKET) {
                    append(" | Spoofed: ${NoFallAccess.getPacketsSpoofed()}")
                }
                
                val damagePrevented = NoFallAccess.getTotalDamagePrevented()
                if (damagePrevented > 0) {
                    append(" | Saved: %.1f❤".format(damagePrevented))
                }
                
                if (NoFallAccess.isRateLimited()) {
                    append(" | RATE LIMITED")
                }
            }
        }
    }
    
    fun getDetailedStatus(): String {
        return buildString {
            appendLine("=== NoFall Status ===")
            appendLine("Enabled: $isEnabled")
            appendLine("Method: ${method.displayName}")
            appendLine("Description: ${method.description}")
            appendLine()
            
            if (isEnabled) {
                appendLine("=== Statistics ===")
                appendLine("Session Time: %.1f seconds".format(NoFallAccess.getSessionTime()))
                appendLine("Total Activations: ${NoFallAccess.getTotalActivations()}")
                appendLine("Packets Spoofed: ${NoFallAccess.getPacketsSpoofed()}")
                appendLine("Damage Prevented: %.1f hearts".format(NoFallAccess.getTotalDamagePrevented()))
                appendLine("Average Rate: %.2f activations/second".format(NoFallAccess.getActivationsPerSecond()))
                appendLine("Rate Limited: ${NoFallAccess.isRateLimited()}")
                appendLine()
            }
            
            appendLine("=== Current Configuration ===")
            appendLine("Method: ${method.displayName}")
            
            when (method) {
                NoFallAccess.NoFallMethod.VELOCITY -> {
                    appendLine("Velocity Settings:")
                    appendLine("  - Reduction Threshold: $velocityReduction")
                    appendLine("  - Velocity Multiplier: $velocityMultiplier")
                    appendLine("  - Max Velocity: $maxVelocity")
                }
                NoFallAccess.NoFallMethod.TELEPORT -> {
                    appendLine("Teleport Settings:")
                    appendLine("  - Activation Threshold: $teleportThreshold")
                    appendLine("  - Teleport Distance: $teleportDistance")
                    appendLine("  - Reset Velocity: $resetVelocityAfterTeleport")
                }
                NoFallAccess.NoFallMethod.SMART -> {
                    appendLine("Smart Settings:")
                    appendLine("  - Min Height: $minActivationHeight")
                    appendLine("  - Cooldown: ${activationCooldown}ms")
                }
                NoFallAccess.NoFallMethod.HYBRID -> {
                    appendLine("Hybrid Settings:")
                    appendLine("  - Velocity Threshold: $hybridVelocityThreshold")
                    appendLine("  - Teleport Threshold: $hybridTeleportThreshold")
                }
                else -> { /* Other methods don't need detailed config */ }
            }
            
            appendLine()
            appendLine("=== Advanced Settings ===")
            appendLine("Only When Falling: $onlyWhenFalling")
            appendLine("Respect Water: $respectWater")
            appendLine("Respect Lava: $respectLava")
            appendLine("Ignore Void Falls: $ignoreVoidFalls")
            appendLine("Max Activations/sec: $maxActivationsPerSecond")
            appendLine("Performance Monitoring: $enablePerformanceMonitoring")
            append("Verbose Logging: $enableVerboseLogging")
        }
    }
    
    // === Utility Methods ===
    
    fun resetStatistics() {
        NoFallAccess.resetStatistics()
        println("NoFall: Statistics reset")
    }
    
    fun resetToDefaults() {
        // Reset to default values
        method = NoFallAccess.NoFallMethod.PACKET
        velocityReduction = 2.0
        velocityMultiplier = 0.6
        maxVelocity = 10.0
        teleportThreshold = 3.0
        teleportDistance = 0.1
        resetVelocityAfterTeleport = true
        minActivationHeight = 3.0
        activationCooldown = 100L
        hybridVelocityThreshold = 5.0
        hybridTeleportThreshold = 12.0
        onlyWhenFalling = true
        respectWater = true
        respectLava = true
        ignoreVoidFalls = false
        maxActivationsPerSecond = 20
        enablePerformanceMonitoring = true
        enableVerboseLogging = false
        
        if (isEnabled) {
            NoFallAccess.resetToDefaults()
            NoFallAccess.setEnabled(true)
            applyCurrentSettings()
        }
        
        println("NoFall: Reset to default configuration")
    }
    
    fun getCurrentFallState(): String? {
        try {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return "No player"
            
            val velocity = player.velocity
            val isOnGround = player.isOnGround
            val isInWater = player.isTouchingWater
            val isInLava = player.isInLava
            val isFallFlying = player.isFallFlying()

            
            return buildString {
                append("Fall State: ")
                when {
                    isOnGround -> append("On Ground")
                    isInWater -> append("In Water")
                    isInLava -> append("In Lava")
                    isFallFlying -> append("Elytra Flying")
                    velocity.y > 0 -> append("Ascending (${String.format("%.2f", velocity.y)})")
                    velocity.y < -0.1 -> append("Falling (${String.format("%.2f", abs(velocity.y))})")
                    else -> append("Stable")
                }
                
                if (!isOnGround && !isInWater && !isInLava && !isFallFlying) {
                    append(" | Y: ${String.format("%.1f", player.y)}")
                    
                    val timeSinceLastActivation = System.currentTimeMillis() - NoFallAccess.getLastActivation()
                    if (timeSinceLastActivation < 1000) {
                        append(" | Last activation: ${timeSinceLastActivation}ms ago")
                    }
                }
            }
        } catch (e: Exception) {
            return "Error: ${e.message}"
        }
    }
    
    fun getAllMethods(): Array<NoFallAccess.NoFallMethod> {
        return NoFallAccess.getAllMethods()
    }
    
    fun getMethodDescription(method: NoFallAccess.NoFallMethod): String {
        return method.description
    }
}