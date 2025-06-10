package code.cinnamon.client.gui

import net.minecraft.client.gui.DrawContext

object GlobalBackgroundRenderer {
    
    private val htmlRenderer = HTMLBackgroundRenderer()
    private var lastWidth: Int = -1
    private var lastHeight: Int = -1
    private var initialized = false
    
    fun render(context: DrawContext, width: Int, height: Int) {
        // Initialize or resize if needed
        if (!initialized || width != lastWidth || height != lastHeight) {
            lastWidth = width
            lastHeight = height
            initialized = true
        }
        
        // Render HTML-style animated background
        htmlRenderer.render(context, width, height)
    }
    
    fun reset() {
        htmlRenderer.cleanup()
        initialized = false
    }
    
    // Configuration methods
    fun setBackgroundStyle(style: BackgroundStyle) {
        // You could add this functionality to HTMLBackgroundRenderer
    }
}