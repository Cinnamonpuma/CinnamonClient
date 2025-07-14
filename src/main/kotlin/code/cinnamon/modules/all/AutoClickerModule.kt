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

import code.cinnamon.modules.BooleanSetting
import code.cinnamon.modules.DoubleSetting

class AutoclickerModule : Module("AutoClicker", "Simulates realistic mouse clicks using advanced timing patterns") {

    private var executor: ScheduledExecutorService? = null
    private var clickTask: ScheduledFuture<*>? = null

    private val isClicking = AtomicBoolean(false)

    private val minCPSSetting = DoubleSetting("Min CPS", 6.0, 0.5, 50.0, 0.5)
    private val maxCPSSetting = DoubleSetting("Max CPS", 10.0, 0.5, 50.0, 0.5)
    private val leftClickEnabledSetting = BooleanSetting("Left Click", true)
    private val rightClickEnabledSetting = BooleanSetting("Right Click", false)
    private val onlyWhileHoldingSetting = BooleanSetting("Only While Holding", false)
    private val enableHumanizationSetting = BooleanSetting("Enable Humanization", true)
    private val burstModeSetting = BooleanSetting("Burst Mode", false)
    private val fatigueEnabledSetting = BooleanSetting("Fatigue", true)
    private val microPausesSetting = BooleanSetting("Micro Pauses", true)
    private val timingVarianceSetting = DoubleSetting("Timing Variance", 0.25, 0.0, 1.0, 0.05)
    private val burstVarianceSetting = DoubleSetting("Burst Variance", 0.15, 0.0, 1.0, 0.05)
    private val fatigueRateSetting = DoubleSetting("Fatigue Rate", 0.02, 0.0, 1.0, 0.01)

    private var lastClickTime: Long = 0
    private var consecutiveClicks: Int = 0
    private var fatigueLevel: Float = 0.0f
    private var isInBurst: Boolean = false
    private var burstClicksRemaining: Int = 0
    private val clickHistory = mutableListOf<Long>()

    var totalClicks: Long = 0
        private set
    private var sessionStartTime: Long = 0

    companion object {
        @JvmStatic
        var instance: AutoclickerModule? = null
            private set

        private const val MAX_CLICK_HISTORY = 50
        private const val BURST_MIN_CLICKS = 3
        private const val BURST_MAX_CLICKS = 8
        private const val MICRO_PAUSE_CHANCE = 0.08f
        private const val MICRO_PAUSE_MIN_MS = 150L
        private const val MICRO_PAUSE_MAX_MS = 400L
        private const val FATIGUE_THRESHOLD = 100
        private const val MAX_FATIGUE = 1.0f
    }

    init {
        instance = this

        settings.add(minCPSSetting)
        settings.add(maxCPSSetting)
        settings.add(leftClickEnabledSetting)
        settings.add(rightClickEnabledSetting)
        settings.add(onlyWhileHoldingSetting)
        settings.add(enableHumanizationSetting)
        settings.add(burstModeSetting)
        settings.add(fatigueEnabledSetting)
        settings.add(microPausesSetting)
        settings.add(timingVarianceSetting)
        settings.add(burstVarianceSetting)
        settings.add(fatigueRateSetting)
    }

    private val currentTargetCPS: Float
        get() {
            if (minCPSSetting.value >= maxCPSSetting.value) return minCPSSetting.value.toFloat()

            val fatigueMultiplier = 1.0f - (fatigueLevel * 0.3f)
            val baseCPS = Random.nextFloat() * (maxCPSSetting.value - minCPSSetting.value) + minCPSSetting.value

            return (baseCPS * fatigueMultiplier).toFloat().coerceIn((minCPSSetting.value * 0.5f).toFloat(), maxCPSSetting.value.toFloat())
        }

    override fun onEnable() {
        println("AutoClicker: Module enabled with CPS range ${minCPSSetting.value}-${maxCPSSetting.value}")
        sessionStartTime = System.currentTimeMillis()
        totalClicks = 0
        resetInternalState()

        try {
            startAutoClicker()
            println("AutoClicker: Started successfully with humanization ${if (enableHumanizationSetting.value) "enabled" else "disabled"}")
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

        if (enableHumanizationSetting.value && microPausesSetting.value && Random.nextFloat() < MICRO_PAUSE_CHANCE) {
            val pauseTime = Random.nextLong(MICRO_PAUSE_MIN_MS, MICRO_PAUSE_MAX_MS + 1)
            println("AutoClicker: Micro pause for ${pauseTime}ms")
            return pauseTime
        }

        val targetCPS = currentTargetCPS
        val baseInterval = (1000.0f / targetCPS).toLong()

        if (!enableHumanizationSetting.value) {
            return max(10L, baseInterval)
        }

        var finalInterval = baseInterval

        val normalVariance = generateNormalVariance() * timingVarianceSetting.value
        finalInterval = (finalInterval * (1.0f + normalVariance)).toLong()

        if (burstModeSetting.value) {
            finalInterval = applyBurstTiming(finalInterval)
        }

        finalInterval = applyRhythmVariance(finalInterval, currentTime)

        updateFatigue()

        return max(10L, finalInterval)
    }

    private fun generateNormalVariance(): Float {
        val u1 = Random.nextFloat()
        val u2 = Random.nextFloat()
        val z0 = kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1.toDouble())) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
        return (z0 * 0.5).toFloat().coerceIn(-2.0f, 2.0f)
    }

    private fun applyBurstTiming(baseInterval: Long): Long {
        if (!isInBurst && Random.nextFloat() < 0.15f) {
            isInBurst = true
            burstClicksRemaining = Random.nextInt(BURST_MIN_CLICKS, BURST_MAX_CLICKS + 1)
            println("AutoClicker: Starting burst of $burstClicksRemaining clicks")
        }

        if (isInBurst) {
            burstClicksRemaining--
            if (burstClicksRemaining <= 0) {
                isInBurst = false
            }

            val burstMultiplier = 0.6f + (Random.nextFloat() * burstVarianceSetting.value)
            return (baseInterval * burstMultiplier).toLong()
        }

        return baseInterval
    }

    private fun applyRhythmVariance(baseInterval: Long, currentTime: Long): Long {
        if (clickHistory.size >= 3) {
            val recentIntervals = mutableListOf<Long>()
            for (i in max(0, clickHistory.size - 4) until clickHistory.size - 1) {
                recentIntervals.add(clickHistory[i + 1] - clickHistory[i])
            }

            if (recentIntervals.isNotEmpty()) {
                val avgRecentInterval = recentIntervals.average()
                val rhythmInfluence = 0.15f
                val rhythmVariance = ((avgRecentInterval - baseInterval) * rhythmInfluence).toLong()
                return baseInterval + rhythmVariance
            }
        }

        return baseInterval
    }

    private fun updateFatigue() {
        if (!fatigueEnabledSetting.value) return

        consecutiveClicks++

        if (consecutiveClicks > FATIGUE_THRESHOLD) {
            fatigueLevel = min(MAX_FATIGUE, fatigueLevel + fatigueRateSetting.value.toFloat())
        } else {
            fatigueLevel = max(0.0f, fatigueLevel - (fatigueRateSetting.value.toFloat() * 0.2f))
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

            if (onlyWhileHoldingSetting.value && !isHoldingClickKey(client)) return

            val currentTime = System.currentTimeMillis()

            client.execute {
                if (leftClickEnabledSetting.value) {
                    AutoClickerAccess.triggerLeftClick()
                }

                if (rightClickEnabledSetting.value) {
                    AutoClickerAccess.triggerRightClick()
                }

                totalClicks++

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
        return (leftClickEnabledSetting.value && client.options.attackKey.isPressed) ||
                (rightClickEnabledSetting.value && client.options.useKey.isPressed)
    }


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
                append(" | Range: %.1f-%.1f CPS".format(minCPSSetting.value, maxCPSSetting.value))
                append(" | Current: %.1f CPS".format(getCurrentCPS()))
                append(" | Clicks: $totalClicks")
                append(" | Avg: %.1f CPS".format(getSessionCPS()))
                if (enableHumanizationSetting.value) {
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
            appendLine("CPS Range: ${minCPSSetting.value} - ${maxCPSSetting.value}")
            appendLine("Current Target CPS: %.2f".format(getCurrentCPS()))
            appendLine("Total Clicks: $totalClicks")
            appendLine("Session Average CPS: %.2f".format(getSessionCPS()))
            appendLine("Humanization: ${enableHumanizationSetting.value}")
            if (enableHumanizationSetting.value) {
                appendLine("  - Timing Variance: ${(timingVarianceSetting.value * 100).toInt()}%")
                appendLine("  - Burst Mode: ${burstModeSetting.value}")
                appendLine("  - Fatigue: ${fatigueEnabledSetting.value} (Level: ${(fatigueLevel * 100).toInt()}%)")
                appendLine("  - Micro Pauses: ${microPausesSetting.value}")
                appendLine("  - Currently in burst: $isInBurst")
                appendLine("  - Actual variance: %.1f%%".format(calculateActualVariance() * 100))
            }
            appendLine("Click Types: L:${leftClickEnabledSetting.value} R:${rightClickEnabledSetting.value}")
            append("Only while holding: ${onlyWhileHoldingSetting.value}")
        }
    }
}