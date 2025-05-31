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

class AutoclickerModule : Module("Autoclicker", "Automatically clicks the mouse") {

    // Thread management
    private var executor: ScheduledExecutorService? = null
    private var clickTask: ScheduledFuture<*>? = null
    
    // Thread-safe state management
    private val isClicking = AtomicBoolean(false)
    private val isKeyPressed = AtomicBoolean(false)
    
    // Configuration options
    private var clicksPerSecond: Float = 10.0f // CPS (clicks per second) - Fixed: Float instead of Double
    private var randomizeClicks: Boolean = true // Add randomization to avoid detection
    private var randomVariance: Float = 0.2f // 20% variance in timing - Fixed: Float instead of Double
    private var clickHoldTimeMs: Long = 20 // How long to hold the click
    private var leftClickEnabled: Boolean = true
    private var rightClickEnabled: Boolean = false
    
    // Calculated values
    private val baseIntervalMs: Long
        get() = (1000.0f / clicksPerSecond).toLong() // Fixed: Use Float arithmetic
    
    override fun onEnable() {
        println("Autoclicker module enabled!")
        
        try {
            startAutoclicker()
            println("Autoclicker: Started with ${clicksPerSecond} CPS")
        } catch (e: Exception) {
            println("Autoclicker: Error starting module: ${e.message}")
            disable() // Fixed: use disable method
        }
    }
    
    override fun onDisable() {
        println("Autoclicker module disabled!")
        
        try {
            stopAutoclicker()
            ensureKeysReleased()
            println("Autoclicker: Stopped successfully")
        } catch (e: Exception) {
            println("Autoclicker: Error stopping module: ${e.message}")
        }
    }
    
    /**
     * Start the autoclicker functionality
     */
    private fun startAutoclicker() {
        // Stop any existing tasks
        stopAutoclicker()
        
        // Create new executor
        executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "Autoclicker-Thread").apply {
                isDaemon = true
            }
        }
        
        isClicking.set(true)
        
        // Schedule the clicking task
        clickTask = executor?.scheduleWithFixedDelay({
            performClickCycle()
        }, 0, baseIntervalMs, TimeUnit.MILLISECONDS)
    }
    
    /**
     * Stop the autoclicker functionality
     */
    private fun stopAutoclicker() {
        isClicking.set(false)
        
        // Cancel the clicking task
        clickTask?.cancel(false)
        clickTask = null
        
        // Shutdown executor
        executor?.let { exec ->
            exec.shutdown()
            try {
                if (!exec.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    exec.shutdownNow()
                    if (!exec.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                        println("Autoclicker: Executor did not terminate cleanly")
                    }
                }
            } catch (e: InterruptedException) {
                exec.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        executor = null
    }
    
    /**
     * Perform a complete click cycle (press and release)
     */
    private fun performClickCycle() {
        if (!this.isEnabled || !isClicking.get()) return // Fixed: use property
        
        try {
            val client = MinecraftClient.getInstance()
            
            // Validate game state
            if (!isValidGameState(client)) return
            
            // Apply randomization if enabled
            if (randomizeClicks) {
                applyRandomDelay()
            }
            
            // Perform the click
            executeClick(client)
            
        } catch (e: Exception) {
            println("Autoclicker: Error during click cycle: ${e.message}")
        }
    }
    
    /**
     * Check if the game state is valid for clicking
     */
    private fun isValidGameState(client: MinecraftClient): Boolean {
        return client.player != null && 
               client.world != null && 
               client.currentScreen == null && // No GUI open
               client.mouse.isCursorLocked // Mouse is captured
    }
    
    /**
     * Apply random delay to avoid detection patterns
     */
    private fun applyRandomDelay() {
        if (randomVariance > 0) {
            val variance = (baseIntervalMs.toFloat() * randomVariance).toLong() // Fixed: Explicit Float conversion
            val randomDelay = Random.nextLong(-variance, variance + 1L) // Fixed: Explicit Long literal
            val adjustedDelay = maxOf(10L, randomDelay) // Minimum 10ms, Fixed: Long literal
            
            if (adjustedDelay > 0) {
                try {
                    Thread.sleep(adjustedDelay)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }
    
    /**
     * Execute the actual click on the main thread
     */
    private fun executeClick(client: MinecraftClient) {
        client.execute {
            try {
                if (this.isEnabled && isClicking.get()) { // Fixed: use property
                    // Perform left click
                    if (leftClickEnabled) {
                        performLeftClick(client)
                    }
                    
                    // Perform right click
                    if (rightClickEnabled) {
                        performRightClick(client)
                    }
                }
            } catch (e: Exception) {
                println("Autoclicker: Error executing click: ${e.message}")
            }
        }
    }
    
    /**
     * Perform left click action
     */
    private fun performLeftClick(client: MinecraftClient) {
        // Simulate left click using Minecraft's input handling
        try {
            // Method 1: Use the key binding's press method
            client.options.attackKey.setPressed(true)
            
            // Schedule release
            executor?.schedule({
                client.execute {
                    try {
                        client.options.attackKey.setPressed(false)
                        isKeyPressed.set(false)
                    } catch (e: Exception) {
                        println("Autoclicker: Error releasing left click: ${e.message}")
                    }
                }
            }, clickHoldTimeMs, TimeUnit.MILLISECONDS)
            
            isKeyPressed.set(true)
        } catch (e: Exception) {
            println("Autoclicker: Error performing left click: ${e.message}")
        }
    }
    
    /**
     * Perform right click action  
     */
    private fun performRightClick(client: MinecraftClient) {
        // Simulate right click using Minecraft's input handling
        try {
            client.options.useKey.setPressed(true)
            
            // Schedule release
            executor?.schedule({
                client.execute {
                    try {
                        client.options.useKey.setPressed(false)
                    } catch (e: Exception) {
                        println("Autoclicker: Error releasing right click: ${e.message}")
                    }
                }
            }, clickHoldTimeMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            println("Autoclicker: Error performing right click: ${e.message}")
        }
    }
    
    /**
     * Ensure all keys are released when stopping
     */
    private fun ensureKeysReleased() {
        try {
            val client = MinecraftClient.getInstance()
            client.execute {
                // Release attack key
                if (leftClickEnabled) {
                    client.options.attackKey.setPressed(false)
                }
                
                // Release use key
                if (rightClickEnabled) {
                    client.options.useKey.setPressed(false)
                }
                
                isKeyPressed.set(false)
                println("Autoclicker: All keys released")
            }
        } catch (e: Exception) {
            println("Autoclicker: Error releasing keys: ${e.message}")
        }
    }
    
    /**
     * Configuration methods
     */
    fun setClicksPerSecond(cps: Float) { // FIXED: Changed parameter type from Double to Float
        if (cps > 0 && cps <= 50) { // Reasonable limits
            this.clicksPerSecond = cps
            if (this.isEnabled) { // Fixed: use property
                // Restart with new settings
                stopAutoclicker()
                startAutoclicker()
            }
            println("Autoclicker: CPS set to $cps")
        }
    }
    
    fun setRandomization(enabled: Boolean, variance: Float = 0.2f) { // FIXED: Changed parameter type from Double to Float
        this.randomizeClicks = enabled
        this.randomVariance = variance.coerceIn(0.0f, 1.0f) // FIXED: Use Float values
        println("Autoclicker: Randomization ${if (enabled) "enabled" else "disabled"} with ${variance * 100}% variance")
    }
    
    fun setClickModes(leftClick: Boolean, rightClick: Boolean) {
        this.leftClickEnabled = leftClick
        this.rightClickEnabled = rightClick
        println("Autoclicker: Left click: $leftClick, Right click: $rightClick")
    }

    // New individual setters
    fun setCPS(cps: Float) {
        setClicksPerSecond(cps) // Delegates to existing clamped and side-effecting method
    }

    fun setRandomizeEnabled(enabled: Boolean) {
        setRandomization(enabled, this.randomVariance) // Keep current variance
    }

    fun setRandomVariance(variance: Float) {
        setRandomization(this.randomizeClicks, variance) // Keep current enabled state
    }

    fun setClickHoldTime(ms: Long) {
        // Clamp to reasonable values, e.g., 1ms to 1000ms
        this.clickHoldTimeMs = ms.coerceIn(1, 1000)
        println("Autoclicker: Click hold time set to ${this.clickHoldTimeMs}ms")
        // No need to restart the autoclicker for this change if it's already running,
        // as it's read on each click cycle by the scheduled executor.
    }

    fun setLeftClickEnabled(enabled: Boolean) {
        this.leftClickEnabled = enabled
        println("Autoclicker: Left click ${if (enabled) "enabled" else "disabled"}")
        // Consider if ensureKeysReleased() or other logic needs to run if a click type is disabled while active.
        // For now, direct update. If module is active and this is disabled, current click might complete.
    }

    fun setRightClickEnabled(enabled: Boolean) {
        this.rightClickEnabled = enabled
        println("Autoclicker: Right click ${if (enabled) "enabled" else "disabled"}")
        // Similar considerations as setLeftClickEnabled.
    }

    /**
     * Public getters for configuration options
     */
    fun getClicksPerSecond(): Float = clicksPerSecond
    fun isRandomizeClicksEnabled(): Boolean = randomizeClicks
    fun getClickHoldTimeMs(): Long = clickHoldTimeMs
    fun isLeftClickEnabled(): Boolean = leftClickEnabled
    fun isRightClickEnabled(): Boolean = rightClickEnabled
    
    /**
     * Get current status information
     */
    fun getStatus(): String {
        return "Autoclicker: ${if (this.isEnabled) "Enabled" else "Disabled"} | " + // Fixed: use property
               "CPS: $clicksPerSecond | " +
               "Randomized: $randomizeClicks | " +
               "Left: $leftClickEnabled | Right: $rightClickEnabled | " +
               "Clicking: ${isClicking.get()}"
    }
}