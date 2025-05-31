// AutoclickerModule.kt
package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.mixin.AutoClickerMixin
import net.minecraft.client.MinecraftClient
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.math.max

class AutoClickerModule : Module("AutoClicker", "Simulates real mouse clicks using mixins") {

    // Thread management
    private var executor: ScheduledExecutorService? = null
    private var clickTask: ScheduledFuture<*>? = null
    
    // Thread-safe state management
    private val isClicking = AtomicBoolean(false)
    
    // Configuration options
    var clicksPerSecond: Float = 8.0f
        private set
    var randomizeClicks: Boolean = true
        private set
    var randomVariance: Float = 0.15f // 15% variance
        private set
    var leftClickEnabled: Boolean = true
        private set
    var rightClickEnabled: Boolean = false
        private set
    var onlyWhileHolding: Boolean = false
        private set
    var maxCPS: Float = 20.0f
        private set
    var minCPS: Float = 1.0f
        private set
    
    // Statistics
    var totalClicks: Long = 0
        private set
    private var sessionStartTime: Long = 0
    
    companion object {
        @JvmStatic
        var instance: AutoClickerModule? = null
            private set
    }
    
    init {
        instance = this
    }
    
    private val baseIntervalMs: Long
        get() = (1000.0f / clicksPerSecond).toLong()
    
    override fun onEnable() {
        println("AutoClicker: Module enabled with ${clicksPerSecond} CPS")
        sessionStartTime = System.currentTimeMillis()
        totalClicks = 0
        
        try {
            startAutoClicker()
            println("AutoClicker: Started successfully")
        } catch (e: Exception) {
            println("AutoClicker: Error starting module: ${e.message}")
            disable()
        }
    }
    
    override fun onDisable() {
        println("AutoClicker: Module disabled")
        
        try {
            stopAutoClicker()
            logSessionStats()
        } catch (e: Exception) {
            println("AutoClicker: Error stopping module: ${e.message}")
        }
    }
    
    private fun logSessionStats() {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000.0
        val avgCPS = if (sessionTime > 0) totalClicks / sessionTime else 0.0
        println("AutoClicker: Session stats - Total clicks: $totalClicks, Average CPS: %.2f".format(avgCPS))
    }
    
    private fun startAutoClicker() {
        stopAutoClicker()
        
        executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "AutoClicker-Thread").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }
        
        isClicking.set(true)
        scheduleNextClick(0)
    }
    
    private fun scheduleNextClick(delay: Long) {
        if (!isClicking.get()) return
        
        clickTask = executor?.schedule({
            if (isClicking.get() && this.isEnabled) {
                performClick()
                
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
    
    private fun stopAutoClicker() {
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
    
    private fun performClick() {
        if (!this.isEnabled || !isClicking.get()) return
        
        try {
            val client = MinecraftClient.getInstance()
            
            if (!isValidGameState(client)) return
            
            // Check if we should only click while holding a key
            if (onlyWhileHolding && !isHoldingClickKey(client)) return
            
            client.execute {
                if (leftClickEnabled) {
                    AutoClickerMixin.simulateLeftClick()
                }
                
                if (rightClickEnabled) {
                    AutoClickerMixin.simulateRightClick()
                }
                
                totalClicks++
            }
            
        } catch (e: Exception) {
            println("AutoClicker: Error during click: ${e.message}")
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
    
    // Configuration methods
    fun setCPS(cps: Float) {
        val clampedCPS = cps.coerceIn(minCPS, maxCPS)
        if (clampedCPS != this.clicksPerSecond) {
            this.clicksPerSecond = clampedCPS
            println("AutoClicker: CPS updated to $clampedCPS")
        }
    }
    
    fun adjustCPS(delta: Float) {
        setCPS(clicksPerSecond + delta)
    }
    
    fun setRandomizeEnabled(enabled: Boolean) {
        this.randomizeClicks = enabled
        println("AutoClicker: Randomization ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setRandomVariance(variance: Float) {
        this.randomVariance = variance.coerceIn(0.0f, 0.5f)
        println("AutoClicker: Random variance set to ${(variance * 100).toInt()}%")
    }
    
    fun setLeftClickEnabled(enabled: Boolean) {
        this.leftClickEnabled = enabled
    }
    
    fun setRightClickEnabled(enabled: Boolean) {
        this.rightClickEnabled = enabled
    }
    
    fun setOnlyWhileHolding(enabled: Boolean) {
        this.onlyWhileHolding = enabled
        println("AutoClicker: Only while holding ${if (enabled) "enabled" else "disabled"}")
    }
    
    // Getters
    fun getSessionCPS(): Float {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000.0f
        return if (sessionTime > 0) totalClicks / sessionTime else 0.0f
    }
    
    fun resetStats() {
        totalClicks = 0
        sessionStartTime = System.currentTimeMillis()
        println("AutoClicker: Statistics reset")
    }
    
    fun getStatus(): String {
        return buildString {
            append("AutoClicker: ${if (isEnabled) "ON" else "OFF"}")
            if (isEnabled) {
                append(" | CPS: %.1f".format(clicksPerSecond))
                append(" | Clicks: $totalClicks")
                append(" | Avg: %.1f".format(getSessionCPS()))
            }
        }
    }
}