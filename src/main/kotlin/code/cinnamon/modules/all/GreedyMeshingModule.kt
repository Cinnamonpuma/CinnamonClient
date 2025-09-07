package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import org.slf4j.LoggerFactory

/**
 * A module to enable or disable Greedy Meshing.
 * This feature can significantly boost FPS but may cause visual artifacts.
 */
class GreedyMeshingModule : Module(
    "Greedy Meshing",
    "A highly experimental rendering mode that merges block faces to boost FPS. May cause visual issues."
) {
    private val logger = LoggerFactory.getLogger("cinnamon-greedy-meshing")

    init {
        // Disabled by default as it is experimental.
        this.disable()
    }

    override fun onEnable() {
        logger.info("Greedy Meshing has been enabled. The world will need to be reloaded.")
        // A world reload would be needed to see the effects.
    }

    override fun onDisable() {
        logger.info("Greedy Meshing has been disabled. The world will need to be reloaded.")
    }
}
