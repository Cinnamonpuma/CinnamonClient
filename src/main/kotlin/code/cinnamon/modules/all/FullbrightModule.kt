package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

class FullbrightModule : Module("Fullbright", "Illuminates the world to maximum brightness (ClearColor Method).") {
    private val logger = LoggerFactory.getLogger("cinnamon-fullbright")

    private fun requestLightmapUpdate() {
        MinecraftClient.getInstance().gameRenderer?.lightmapTextureManager?.let { lightmapManager ->
            lightmapManager.tick() 
            logger.debug("Lightmap tick() requested due to Fullbright module state change (ClearColor Method).")
        }
    }

    override fun onEnable() {
        logger.info("Fullbright enabled (ClearColor Method)")
        requestLightmapUpdate() 
    }

    override fun onDisable() {
        logger.info("Fullbright disabled (ClearColor Method)")
        requestLightmapUpdate() 
    }

    /**
     * Optional: Method to manually request a lightmap update.
     */
    fun refreshLightmap() {
        logger.debug("Manual lightmap update request (ClearColor Method).")
        requestLightmapUpdate()
    }
}
