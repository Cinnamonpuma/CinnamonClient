// AutoClickerModule.kt
package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.util.AutoClickerAccess
import net.minecraft.client.MinecraftClient
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

class AutoclickerModule : Module("AutoClicker", "Simulates realistic mouse clicks using advanced timing patterns") {

    // Thread management
    private var executor: ScheduledExecutorService? = null
    private var clickTask: ScheduledFuture<*>? = null
    
    // Thread-safe state management
    private val isClicking = AtomicBoolean(false)
    
    // Enhanced configuration options
    var minCPS: Float = 6.0f
        private set
    var maxCPS: Float = 10.0f
        private set
    var leftClickEnabled: Boolean = true
        private set
    var rightClickEnabled: Boolean = false
        private set
    var onlyWhileHolding: Boolean = false
        private set
    
    // Advanced humanization settings
    var enableHumanization: Boolean = true
        private set
    var burstMode: Boolean = false
        private set
    var fatigueEnabled: Boolean = true
        private set
    var microPauses: Boolean = true
        private set
    
    // Timing randomization parameters
    var timingVariance: Float = 0.25f // 25% base variance
        private set
    var burstVariance: Float = 0.15f // Additional variance during bursts
        private set
    var fatigueRate: Float = 0.02f // How quickly "fatigue" builds up
        private set
    
    // Internal state for humanization
    private var lastClickTime: Long = 0
    private var consecutiveClicks: Int = 0
    private var fatigueLevel: Float = 0.0f
    private var isInBurst: Boolean = false
    private var burstClicksRemaining: Int = 0
    private val clickHistory = mutableListOf<Long>()
    
    // Statistics
    var totalClicks: Long = 0
        private set
    private var sessionStartTime: Long = 0
    
    companion object {
        @JvmStatic
        var instance: AutoclickerModule? = null
            private set
            
        // Constants for humanization
        private const val MAX_CLICK_HISTORY = 50
        private const val BURST_MIN_CLICKS = 3
        private const val BURST_MAX_CLICKS = 8
        private const val MICRO_PAUSE_CHANCE = 0.08f // 8% chance
        private const val MICRO_PAUSE_MIN_MS = 150L
        private const val MICRO_PAUSE_MAX_MS = 400L
        private const val FATIGUE_THRESHOLD = 100
        private const val MAX_FATIGUE = 1.0f
    }
    
    init {
        instance = this
    }
    
    private val currentTargetCPS: Float
        get() {
            if (minCPS >= maxCPS) return minCPS
            
            // Apply fatigue effect
            val fatigueMultiplier = 1.0f - (fatigueLevel * 0.3f) // Max 30% reduction
            val baseCPS = Random.nextFloat() * (maxCPS - minCPS) + minCPS
            
            return (baseCPS * fatigueMultiplier).coerceIn(minCPS * 0.5f, maxCPS)
        }
    
    override fun onEnable() {
        println("AutoClicker: Module enabled with CPS range ${minCPS}-${maxCPS}")
        sessionStartTime = System.currentTimeMillis()
        totalClicks = 0
        resetInternalState()
        
        try {
            startAutoClicker()
            println("AutoClicker: Started successfully with humanization ${if (enableHumanization) "enabled" else "disabled"}")
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
            AutoClickerAccess.clearAllPendingClicks()
        } catch (e: Exception) {
            println("AutoClicker: Error stopping module: ${e.message}")
        }
    }
    
    private fun resetInternalState() {
        lastClickTime = 0
        consecutiveClicks = 0
        fatigueLevel = 0.0f
        isInBurst = false
        burstClicksRemaining = 0
        clickHistory.clear()
    }
    
    private fun logSessionStats() {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000.0
        val avgCPS = if (sessionTime > 0) totalClicks / sessionTime else 0.0
        val actualVariance = calculateActualVariance()
        println("AutoClicker: Session stats - Total clicks: $totalClicks, Average CPS: %.2f, Timing variance: %.1f%%"
            .format(avgCPS, actualVariance * 100))
    }
    
    private fun calculateActualVariance(): Float {
        if (clickHistory.size < 3) return 0.0f
        
        val intervals = mutableListOf<Long>()
        for (i in 1 until clickHistory.size) {
            intervals.add(clickHistory[i] - clickHistory[i-1])
        }
        
        val mean = intervals.average()
        val variance = intervals.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        return (standardDeviation / mean).toFloat()
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
        val currentTime = System.currentTimeMillis()
        
        // Handle micro pauses (realistic hesitations)
        if (enableHumanization && microPauses && Random.nextFloat() < MICRO_PAUSE_CHANCE) {
            val pauseTime = Random.nextLong(MICRO_PAUSE_MIN_MS, MICRO_PAUSE_MAX_MS + 1)
            println("AutoClicker: Micro pause for ${pauseTime}ms")
            return pauseTime
        }
        
        // Get base interval from current target CPS
        val targetCPS = currentTargetCPS
        val baseInterval = (1000.0f / targetCPS).toLong()
        
        if (!enableHumanization) {
            return max(10L, baseInterval)
        }
        
        // Apply sophisticated timing variance
        var finalInterval = baseInterval
        
        // Base timing variance using normal distribution simulation
        val normalVariance = generateNormalVariance() * timingVariance
        finalInterval = (finalInterval * (1.0f + normalVariance)).toLong()
        
        // Burst mode logic
        if (burstMode) {
            finalInterval = applyBurstTiming(finalInterval)
        }
        
        // Apply rhythm-based variance (humans have natural rhythm patterns)
        finalInterval = applyRhythmVariance(finalInterval, currentTime)
        
        // Update fatigue level
        updateFatigue()
        
        // Ensure minimum delay
        return max(10L, finalInterval)
    }
    
    private fun generateNormalVariance(): Float {
        // Box-Muller transform to generate normal distribution
        val u1 = Random.nextFloat()
        val u2 = Random.nextFloat()
        val z0 = kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1.toDouble())) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
        return (z0 * 0.5).toFloat().coerceIn(-2.0f, 2.0f) // Clamp to reasonable range
    }
    
    private fun applyBurstTiming(baseInterval: Long): Long {
        if (!isInBurst && Random.nextFloat() < 0.15f) { // 15% chance to start burst
            isInBurst = true
            burstClicksRemaining = Random.nextInt(BURST_MIN_CLICKS, BURST_MAX_CLICKS + 1)
            println("AutoClicker: Starting burst of $burstClicksRemaining clicks")
        }
        
        if (isInBurst) {
            burstClicksRemaining--
            if (burstClicksRemaining <= 0) {
                isInBurst = false
            }
            
            // Faster clicks during burst with additional variance
            val burstMultiplier = 0.6f + (Random.nextFloat() * burstVariance)
            return (baseInterval * burstMultiplier).toLong()
        }
        
        return baseInterval
    }
    
    private fun applyRhythmVariance(baseInterval: Long, currentTime: Long): Long {
        // Simulate natural rhythm by looking at recent click patterns
        if (clickHistory.size >= 3) {
            val recentIntervals = mutableListOf<Long>()
            for (i in max(0, clickHistory.size - 4) until clickHistory.size - 1) {
                recentIntervals.add(clickHistory[i + 1] - clickHistory[i])
            }
            
            if (recentIntervals.isNotEmpty()) {
                val avgRecentInterval = recentIntervals.average()
                val rhythmInfluence = 0.15f // How much rhythm affects next click
                val rhythmVariance = ((avgRecentInterval - baseInterval) * rhythmInfluence).toLong()
                return baseInterval + rhythmVariance
            }
        }
        
        return baseInterval
    }
    
    private fun updateFatigue() {
        if (!fatigueEnabled) return
        
        consecutiveClicks++
        
        if (consecutiveClicks > FATIGUE_THRESHOLD) {
            fatigueLevel = min(MAX_FATIGUE, fatigueLevel + fatigueRate)
        } else {
            // Slowly recover from fatigue during normal operation
            fatigueLevel = max(0.0f, fatigueLevel - (fatigueRate * 0.2f))
        }
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
            
            if (onlyWhileHolding && !isHoldingClickKey(client)) return
            
            val currentTime = System.currentTimeMillis()
            
            client.execute {
                if (leftClickEnabled) {
                    AutoClickerAccess.triggerLeftClick()
                }
                
                if (rightClickEnabled) {
                    AutoClickerAccess.triggerRightClick()
                }
                
                totalClicks++
                
                // Update click history for analysis
                clickHistory.add(currentTime)
                if (clickHistory.size > MAX_CLICK_HISTORY) {
                    clickHistory.removeAt(0)
                }
                
                lastClickTime = currentTime
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
    fun setCPSRange(minCPS: Float, maxCPS: Float) {
        this.minCPS = minCPS.coerceIn(0.5f, 50.0f)
        this.maxCPS = max(this.minCPS, maxCPS.coerceIn(0.5f, 50.0f))
        resetInternalState() // Reset to avoid weird transitions
        println("AutoClicker: CPS range updated to ${this.minCPS}-${this.maxCPS}")
    }
    
    fun setMinCPS(cps: Float) {
        val newMin = cps.coerceIn(0.5f, 50.0f)
        this.minCPS = newMin
        if (this.maxCPS < newMin) {
            this.maxCPS = newMin
        }
        println("AutoClicker: Min CPS set to $newMin")
    }
    
    fun setMaxCPS(cps: Float) {
        val newMax = cps.coerceIn(0.5f, 50.0f)
        this.maxCPS = newMax
        if (this.minCPS > newMax) {
            this.minCPS = newMax
        }
        println("AutoClicker: Max CPS set to $newMax")
    }
    
    fun setHumanizationEnabled(enabled: Boolean) {
        this.enableHumanization = enabled
        println("AutoClicker: Humanization ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setBurstMode(enabled: Boolean) {
        this.burstMode = enabled
        println("AutoClicker: Burst mode ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setFatigueEnabled(enabled: Boolean) {
        this.fatigueEnabled = enabled
        if (!enabled) fatigueLevel = 0.0f
        println("AutoClicker: Fatigue simulation ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setMicroPauses(enabled: Boolean) {
        this.microPauses = enabled
        println("AutoClicker: Micro pauses ${if (enabled) "enabled" else "disabled"}")
    }
    
    fun setTimingVariance(variance: Float) {
        this.timingVariance = variance.coerceIn(0.0f, 1.0f)
        println("AutoClicker: Timing variance set to ${(variance * 100).toInt()}%")
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
    fun getCurrentCPS(): Float = currentTargetCPS
    
    fun getSessionCPS(): Float {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000.0f
        return if (sessionTime > 0) totalClicks / sessionTime else 0.0f
    }
    
    fun getFatigueLevel(): Float = fatigueLevel
    
    fun isInBurstMode(): Boolean = isInBurst
    
    fun resetStats() {
        totalClicks = 0
        sessionStartTime = System.currentTimeMillis()
        resetInternalState()
        println("AutoClicker: Statistics and state reset")
    }
    
    fun getStatus(): String {
        return buildString {
            append("AutoClicker: ${if (isEnabled) "ON" else "OFF"}")
            if (isEnabled) {
                append(" | Range: %.1f-%.1f CPS".format(minCPS, maxCPS))
                append(" | Current: %.1f CPS".format(getCurrentCPS()))
                append(" | Clicks: $totalClicks")
                append(" | Avg: %.1f CPS".format(getSessionCPS()))
                if (enableHumanization) {
                    append(" | Fatigue: %.0f%%".format(fatigueLevel * 100))
                    if (isInBurst) append(" | BURST")
                }
            }
        }
    }
    
    fun getDetailedStatus(): String {
        return buildString {
            appendLine("=== AutoClicker Status ===")
            appendLine("Enabled: ${isEnabled}")
            appendLine("CPS Range: $minCPS - $maxCPS")
            appendLine("Current Target CPS: %.2f".format(getCurrentCPS()))
            appendLine("Total Clicks: $totalClicks")
            appendLine("Session Average CPS: %.2f".format(getSessionCPS()))
            appendLine("Humanization: $enableHumanization")
            if (enableHumanization) {
                appendLine("  - Timing Variance: ${(timingVariance * 100).toInt()}%")
                appendLine("  - Burst Mode: $burstMode")
                appendLine("  - Fatigue: $fatigueEnabled (Level: ${(fatigueLevel * 100).toInt()}%)")
                appendLine("  - Micro Pauses: $microPauses")
                appendLine("  - Currently in burst: $isInBurst")
                appendLine("  - Actual variance: %.1f%%".format(calculateActualVariance() * 100))
            }
            appendLine("Click Types: L:$leftClickEnabled R:$rightClickEnabled")
            append("Only while holding: $onlyWhileHolding")
        }
    }
}