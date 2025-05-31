package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.math.max
import kotlin.math.min

class AutoclickerModule : Module("Autoclicker", "Automatically clicks the mouse with configurable settings") {

    // Thread management
    private var executor: ScheduledExecutorService? = null
    private var clickTask: ScheduledFuture<*>? = null
    
    // Thread-safe state management
    private val isClicking = AtomicBoolean(false)
    private val isKeyPressed = AtomicBoolean(false)
    
    // Configuration options with better defaults
    private var clicksPerSecond: Float = 8.0f // More reasonable default CPS
    private var randomizeClicks: Boolean = true
    private var randomVariance: Float = 0.15f // 15% variance - less obvious
    private var clickHoldTimeMs: Long = 25 // Slightly longer hold time
    private var leftClickEnabled: Boolean = true
    private var rightClickEnabled: Boolean = false
    
    // New advanced settings
    private var onlyWhileHolding: Boolean = false // Only click while holding a key
    private var blockBreakMode: Boolean = false // Optimized for block breaking
    private var maxCPS: Float = 20.0f // Maximum allowed CPS
    private var minCPS: Float = 1.0f // Minimum allowed CPS
    
    // Statistics
    private var totalClicks: Long = 0
    private var sessionStartTime: Long = 0
    
    // Calculated values
    private val baseIntervalMs: Long
        get() = (1000.0f / clicksPerSecond).toLong()
    
    override fun onEnable() {
        println("Autoclicker: Module enabled with ${clicksPerSecond} CPS")
        sessionStartTime = System.currentTimeMillis()
        totalClicks = 0
        
        try {
            startAutoclicker()
            println("Autoclicker: Started successfully")
        } catch (e: Exception) {
            println("Autoclicker: Error starting module: ${e.message}")
            disable()
        }
    }
    
    override fun onDisable() {
        println("Autoclicker: Module disabled")
        
        try {
            stopAutoclicker()
            ensureKeysReleased()
            logSessionStats()
        } catch (e: Exception) {
            println("Autoclicker: Error stopping module: ${e.message}")
        }
    }
    
    private fun logSessionStats() {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000.0
        val avgCPS = if (sessionTime > 0) totalClicks / sessionTime else 0.0
        println("Autoclicker: Session stats - Total clicks: $totalClicks, Average CPS: %.2f".format(avgCPS))
    }
    
    private fun startAutoclicker() {
        stopAutoclicker()
        
        executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "Autoclicker-Thread").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1 // Slightly lower priority
            }
        }
        
        isClicking.set(true)
        
        // Use variable delay for better randomization
        scheduleNextClick(0)
    }
    
    private fun scheduleNextClick(delay: Long) {
        if (!isClicking.get()) return
        
        clickTask = executor?.schedule({
            if (isClicking.get() && this.isEnabled) {
                performClickCycle()
                
                // Calculate next delay with randomization
                val nextDelay = calculateNextDelay()
                scheduleNextClick(nextDelay)
            }
        }, delay, TimeUnit.MILLISECONDS)
    }
    
    private fun calculateNextDelay(): Long {
        var delay = baseIntervalMs
        
        if (randomizeClicks && randomVariance > 0) {
            val variance = (baseIntervalMs.toFloat() * randomVariance).toLong()
            val randomOffset = Random.nextLong(-variance, variance + 1)
            delay = max(10L, delay + randomOffset)
        }
        
        return delay
    }
    
    private fun stopAutoclicker() {
        isClicking.set(false)
        
        clickTask?.cancel(true)
        clickTask = null
        
        executor?.let { exec ->
            exec.shutdown()
            try {
                if (!exec.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    exec.shutdownNow()
                }
            } catch (e: InterruptedException) {
                exec.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        executor = null
    }
    
    private fun performClickCycle() {
        if (!this.isEnabled || !isClicking.get()) return
        
        try {
            val client = MinecraftClient.getInstance()
            
            if (!isValidGameState(client)) return
            
            // Check if we should only click while holding a key
            if (onlyWhileHolding && !isHoldingClickKey(client)) return
            
            executeClick(client)
            totalClicks++
            
        } catch (e: Exception) {
            println("Autoclicker: Error during click cycle: ${e.message}")
        }
    }
    
    private fun isValidGameState(client: MinecraftClient): Boolean {
        return client.player != null && 
               client.world != null && 
               client.currentScreen == null &&
               client.mouse.isCursorLocked
    }
    
    private fun isHoldingClickKey(client: MinecraftClient): Boolean {
        return (leftClickEnabled && client.options.attackKey.isPressed) ||
               (rightClickEnabled && client.options.useKey.isPressed)
    }
    
    private fun executeClick(client: MinecraftClient) {
        client.execute {
            try {
                if (this.isEnabled && isClicking.get()) {
                    if (leftClickEnabled) {
                        performLeftClick(client)
                    }
                    
                    if (rightClickEnabled) {
                        performRightClick(client)
                    }
                }
            } catch (e: Exception) {
                println("Autoclicker: Error executing click: ${e.message}")
            }
        }
    }
    
    private fun performLeftClick(client: MinecraftClient) {
        try {
            if (blockBreakMode) {
                // For block breaking, we want continuous holding rather than clicking
                client.options.attackKey.setPressed(true)
                
                // Release after hold time for next click cycle
                executor?.schedule({
                    client.execute {
                        client.options.attackKey.setPressed(false)
                    }
                }, clickHoldTimeMs, TimeUnit.MILLISECONDS)
            } else {
                // Normal clicking behavior
                client.options.attackKey.setPressed(true)
                isKeyPressed.set(true)
                
                executor?.schedule({
                    client.execute {
                        client.options.attackKey.setPressed(false)
                        isKeyPressed.set(false)
                    }
                }, clickHoldTimeMs, TimeUnit.MILLISECONDS)
            }
        } catch (e: Exception) {
            println("Autoclicker: Error performing left click: ${e.message}")
        }
    }
    
    private fun performRightClick(client: MinecraftClient) {
        try {
            client.options.useKey.setPressed(true)
            
            executor?.schedule({
                client.execute {
                    client.options.useKey.setPressed(false)
                }
            }, clickHoldTimeMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            println("Autoclicker: Error performing right click: ${e.message}")
        }
    }
    
    private fun ensureKeysReleased() {
        try {
            val client = MinecraftClient.getInstance()
            client.execute {
                client.options.attackKey.setPressed(false)
                client.options.useKey.setPressed(false)
                isKeyPressed.set(false)
            }
        } catch (e: Exception) {
            println("Autoclicker: Error releasing keys: ${e.message}")
        }
    }
    
    // Configuration methods with proper validation
    fun setCPS(cps: Float) {
        val clampedCPS = cps.coerceIn(minCPS, maxCPS)
        if (clampedCPS != this.clicksPerSecond) {
            this.clicksPerSecond = clampedCPS
            if (this.isEnabled) {
                // Restart is not needed, just let the new timing take effect
                println("Autoclicker: CPS updated to $clampedCPS")
            }
        }
    }
    
    fun adjustCPS(delta: Float) {
        setCPS(clicksPerSecond + delta)
    }
    
    fun setRandomizeEnabled(enabled: Boolean) {
        this.randomizeClicks = enabled
        println("Autoclicker: Randomization ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setRandomVariance(variance: Float) {
        this.randomVariance = variance.coerceIn(0.0f, 0.5f) // Max 50% variance
        println("Autoclicker: Random variance set to ${(variance * 100).toInt()}%")
    }
    
    fun setClickHoldTime(ms: Long) {
        this.clickHoldTimeMs = ms.coerceIn(5, 500) // 5ms to 500ms
        println("Autoclicker: Hold time set to ${this.clickHoldTimeMs}ms")
    }
    
    fun adjustHoldTime(deltaMs: Long) {
        setClickHoldTime(clickHoldTimeMs + deltaMs)
    }
    
    fun setLeftClickEnabled(enabled: Boolean) {
        this.leftClickEnabled = enabled
        if (!enabled && this.isEnabled) {
            // Ensure left click is released if we're disabling it
            MinecraftClient.getInstance().execute {
                MinecraftClient.getInstance().options.attackKey.setPressed(false)
            }
        }
    }
    
    fun setRightClickEnabled(enabled: Boolean) {
        this.rightClickEnabled = enabled
        if (!enabled && this.isEnabled) {
            MinecraftClient.getInstance().execute {
                MinecraftClient.getInstance().options.useKey.setPressed(false)
            }
        }
    }
    
    fun setOnlyWhileHolding(enabled: Boolean) {
        this.onlyWhileHolding = enabled
        println("Autoclicker: Only while holding ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setBlockBreakMode(enabled: Boolean) {
        this.blockBreakMode = enabled
        println("Autoclicker: Block break mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    // Getters
    fun getClicksPerSecond(): Float = clicksPerSecond
    fun isRandomizeClicksEnabled(): Boolean = randomizeClicks
    fun getRandomVariance(): Float = randomVariance
    fun getClickHoldTimeMs(): Long = clickHoldTimeMs
    fun isLeftClickEnabled(): Boolean = leftClickEnabled
    fun isRightClickEnabled(): Boolean = rightClickEnabled
    fun isOnlyWhileHolding(): Boolean = onlyWhileHolding
    fun isBlockBreakMode(): Boolean = blockBreakMode
    fun getTotalClicks(): Long = totalClicks
    fun getSessionCPS(): Float {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000.0f
        return if (sessionTime > 0) totalClicks / sessionTime else 0.0f
    }
    
    // Utility methods
    fun resetStats() {
        totalClicks = 0
        sessionStartTime = System.currentTimeMillis()
        println("Autoclicker: Statistics reset")
    }
    
    fun getStatus(): String {
        return buildString {
            append("Autoclicker: ${if (isEnabled) "ON" else "OFF"}")
            if (isEnabled) {
                append(" | CPS: %.1f".format(clicksPerSecond))
                append(" | Clicks: $totalClicks")
                append(" | Avg: %.1f".format(getSessionCPS()))
            }
        }
    }
}