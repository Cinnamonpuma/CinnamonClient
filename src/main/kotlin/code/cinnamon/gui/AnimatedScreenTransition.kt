package code.cinnamon.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import kotlin.math.max
import kotlin.math.min

object AnimatedScreenTransition {
    private var currentScreen: Screen? = null
    private var nextScreen: Screen? = null
    private var animationProgress = 0f
    private var transitioning = false
    private const val animationDuration = 0.3f // seconds

    fun setCurrentScreen(screen: Screen?) {
        if (screen == currentScreen) return

        if (currentScreen != null && screen != null) {
            nextScreen = screen
            animationProgress = 0f
            transitioning = true
        } else {
            currentScreen = screen
            nextScreen = null
            animationProgress = 1f
            transitioning = false
            MinecraftClient.getInstance().setScreen(currentScreen)
        }
    }

    fun update(delta: Float) {
        if (!transitioning) return

        animationProgress += delta / animationDuration
        animationProgress = min(animationProgress, 1f)

        if (animationProgress >= 1f) {
            transitioning = false
            currentScreen = nextScreen
            nextScreen = null
            MinecraftClient.getInstance().setScreen(currentScreen)
        }
    }

    fun getAlpha(): Float {
        return if (transitioning) {
            // Fade out current, fade in next
            if (animationProgress < 0.5f) {
                1f - (animationProgress * 2) // Fading out current
            } else {
                (animationProgress - 0.5f) * 2 // Fading in next
            }
        } else {
            1f // Not transitioning or fully transitioned
        }
    }

    fun getDisplayScreen(): Screen? {
        return if (transitioning && animationProgress >= 0.5f && nextScreen != null) {
            nextScreen
        } else {
            currentScreen
        }
    }

    fun isTransitioning(): Boolean = transitioning

    fun getProgress(): Float = animationProgress
}
