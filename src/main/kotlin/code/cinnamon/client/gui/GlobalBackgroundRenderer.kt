package code.cinnamon.client.gui

import code.cinnamon.client.wave.WaveSystem
import net.minecraft.client.gui.DrawContext

object GlobalBackgroundRenderer {
    
    private val waveSystem = WaveSystem()
    private var lastWidth: Int = -1
    private var lastHeight: Int = -1
    private var initialized = false
    
    fun render(context: DrawContext, width: Int, height: Int) {
        // Initialize or resize if needed
        if (!initialized || width != lastWidth || height != lastHeight) {
            waveSystem.resize(width, height)
            lastWidth = width
            lastHeight = height
            initialized = true
        }
        
        // Update and render the wave system
        waveSystem.update()
        waveSystem.render(context, width, height)
    }
    
    fun reset() {
        initialized = false
    }
}