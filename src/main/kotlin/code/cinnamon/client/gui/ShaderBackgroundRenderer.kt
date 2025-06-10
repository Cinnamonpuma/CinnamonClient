package code.cinnamon.client.gui

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgram // Using this for MC's shader management
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL20 // For direct GL calls if ShaderProgram abstraction is insufficient for setup
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class ShaderBackgroundRenderer {

    private var shaderProgram: ShaderProgram? = null // Keep for potential future use with MC's system
    private var programId: Int = 0 // Stores the raw OpenGL program ID

    private val vertexShaderPath = Identifier.of("cinnamon", "shaders/wave_background.vert")
    private val fragmentShaderPath = Identifier.of("cinnamon", "shaders/wave_background.frag")

    private var initialized = false

    fun init() {
        if (initialized) return

        val mc = MinecraftClient.getInstance()
        val resourceManager = mc.resourceManager

        try {
            // Read vertex shader source
            val vertResourceOpt = resourceManager.getResource(vertexShaderPath)
            if (!vertResourceOpt.isPresent) throw RuntimeException("Vertex shader not found: $vertexShaderPath")
            val vertSource = BufferedReader(InputStreamReader(vertResourceOpt.get().inputStream, StandardCharsets.UTF_8)).use { it.readText() }

            // Read fragment shader source
            val fragResourceOpt = resourceManager.getResource(fragmentShaderPath)
            if (!fragResourceOpt.isPresent) throw RuntimeException("Fragment shader not found: $fragmentShaderPath")
            val fragSource = BufferedReader(InputStreamReader(fragResourceOpt.get().inputStream, StandardCharsets.UTF_8)).use { it.readText() }
            
            val newProgramId = GL20.glCreateProgram()
            if (newProgramId == 0) throw RuntimeException("Could not create shader program.")

            val vertShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertSource)
            val fragShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragSource)

            GL20.glAttachShader(newProgramId, vertShaderId)
            GL20.glAttachShader(newProgramId, fragShaderId)
            GL20.glLinkProgram(newProgramId)

            if (GL20.glGetProgrami(newProgramId, GL20.GL_LINK_STATUS) == 0) {
                val log = GL20.glGetProgramInfoLog(newProgramId, 512)
                cleanupShaderObjects(0, vertShaderId, fragShaderId, false) // Program link failed, don't pass programId
                GL20.glDeleteProgram(newProgramId) // Delete the failed program
                throw RuntimeException("Shader program linking failed: $log")
            }
            
            this.programId = newProgramId
            cleanupShaderObjects(newProgramId, vertShaderId, fragShaderId, true) // Detach and delete shaders
            
            initialized = true
            // Simple log to confirm initialization path
            mc.logger.info("[Cinnamon/ShaderBackgroundRenderer] Initialized successfully with program ID: ${this.programId}")

        } catch (e: Exception) {
            mc.logger.error("[Cinnamon/ShaderBackgroundRenderer] Failed to initialize: ${e.message}", e)
            cleanup() 
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shaderId = GL20.glCreateShader(type)
        if (shaderId == 0) throw RuntimeException("Could not create shader of type $type.")
        GL20.glShaderSource(shaderId, source)
        GL20.glCompileShader(shaderId)
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            val log = GL20.glGetShaderInfoLog(shaderId, 512)
            GL20.glDeleteShader(shaderId)
            throw RuntimeException("Shader compilation failed (type $type): $log")
        }
        return shaderId
    }
    
    private fun cleanupShaderObjects(programIdToDetachFrom: Int, vertShaderId: Int, fragShaderId: Int, attached: Boolean) {
        if (attached && programIdToDetachFrom != 0) {
            if (vertShaderId != 0) GL20.glDetachShader(programIdToDetachFrom, vertShaderId)
            if (fragShaderId != 0) GL20.glDetachShader(programIdToDetachFrom, fragShaderId)
        }
        if (vertShaderId != 0) GL20.glDeleteShader(vertShaderId)
        if (fragShaderId != 0) GL20.glDeleteShader(fragShaderId)
    }

    fun render(width: Int, height: Int, timeSeconds: Float) {
        if (!initialized) {
            init() // Attempt to initialize
            if (!initialized) { // If still not initialized, bail
                // Log this issue, perhaps to Minecraft's logger if available
                // MinecraftClient.getInstance().logger.warn("[Cinnamon/ShaderBackgroundRenderer] Not initialized, cannot render.")
                return
            }
        }
        if (programId == 0) return // Not successfully initialized

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableCull()
        RenderSystem.depthMask(false)

        GL20.glUseProgram(this.programId)

        val timeUniformLoc = GL20.glGetUniformLocation(this.programId, "time")
        val resolutionUniformLoc = GL20.glGetUniformLocation(this.programId, "resolution")

        if (timeUniformLoc != -1) GL20.glUniform1f(timeUniformLoc, timeSeconds)
        if (resolutionUniformLoc != -1) GL20.glUniform2f(resolutionUniformLoc, width.toFloat(), height.toFloat())

        val tessellator = Tessellator.getInstance()
        val bufferBuilder = tessellator.buffer

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
        // The vertex shader expects clip-space coordinates.
        // Using 0 to width/height here means the vertex shader `gl_Position = vec4(in_Position, 0.0, 1.0);`
        // would need to be changed to project these coordinates into clip space.
        // For now, assuming the vertex shader is `in vec2 in_Position; void main() { gl_Position = vec4(in_Position, 0.0, 1.0); }`
        // this means `in_Position` must be clip space coords: -1 to 1.
        // Let's draw a quad that covers the entire screen in clip space.
        bufferBuilder.vertex(-1.0,  1.0, 0.0).texture(0.0f, 0.0f).next() // Top-left
        bufferBuilder.vertex( 1.0,  1.0, 0.0).texture(1.0f, 0.0f).next() // Top-right
        bufferBuilder.vertex( 1.0, -1.0, 0.0).texture(1.0f, 1.0f).next() // Bottom-right
        bufferBuilder.vertex(-1.0, -1.0, 0.0).texture(0.0f, 1.0f).next() // Bottom-left
        Tessellator.getInstance().draw() 

        GL20.glUseProgram(0) 

        RenderSystem.depthMask(true)
        RenderSystem.enableCull()
    }

    fun cleanup() {
        if (programId != 0) {
            // Ensure cleanup is on render thread if this object can be accessed from other threads
            // For now, assuming it's managed by a screen and thus on render thread.
            GL20.glDeleteProgram(programId)
            // MinecraftClient.getInstance().logger.info("[Cinnamon/ShaderBackgroundRenderer] Cleaned up program ID: ${this.programId}")
            programId = 0
        }
        shaderProgram?.close() 
        shaderProgram = null
        initialized = false
    }
}
