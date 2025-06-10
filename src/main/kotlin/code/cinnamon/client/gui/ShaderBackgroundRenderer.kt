package code.cinnamon.client.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.nio.charset.StandardCharsets

class ShaderBackgroundRenderer {
    internal var programId = 0
    private var initialized = false

    private val vertId = Identifier.of("cinnamon", "shaders/wave_background.vert")
    private val fragId = Identifier.of("cinnamon", "shaders/wave_background.frag")

    fun init() {
        if (initialized) return
        val rm = MinecraftClient.getInstance().resourceManager

        try {
            val vertSrc = rm.getResource(vertId).orElseThrow().inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            val fragSrc = rm.getResource(fragId).orElseThrow().inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            
            val prog = GL20.glCreateProgram().also { require(it != 0) { "Failed to create program" } }
            val vs = compile(GL20.GL_VERTEX_SHADER, vertSrc)
            val fs = compile(GL20.GL_FRAGMENT_SHADER, fragSrc)
            
            GL20.glAttachShader(prog, vs)
            GL20.glAttachShader(prog, fs)
            GL20.glLinkProgram(prog)
            
            require(GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) != 0) {
                GL20.glGetProgramInfoLog(prog, 512).also { 
                    cleanupShaders(0, vs, fs, false)
                    GL20.glDeleteProgram(prog)
                }
            }
            
            programId = prog
            cleanupShaders(prog, vs, fs, true)
            initialized = true
            
        } catch (e: Exception) {
            e.printStackTrace()
            cleanup()
        }
    }

    private fun compile(type: Int, src: String): Int {
        val sid = GL20.glCreateShader(type).also { require(it != 0) { "Failed to create shader" } }
        GL20.glShaderSource(sid, src)
        GL20.glCompileShader(sid)
        require(GL20.glGetShaderi(sid, GL20.GL_COMPILE_STATUS) != 0) {
            GL20.glGetShaderInfoLog(sid, 512).also { GL20.glDeleteShader(sid) }
        }
        return sid
    }

    private fun cleanupShaders(prog: Int, vs: Int, fs: Int, detach: Boolean) {
        if (detach && prog != 0) {
            if (vs != 0) GL20.glDetachShader(prog, vs)
            if (fs != 0) GL20.glDetachShader(prog, fs)
        }
        if (vs != 0) GL20.glDeleteShader(vs)
        if (fs != 0) GL20.glDeleteShader(fs)
    }

    fun render(context: DrawContext, width: Int, height: Int, timeSec: Float) {
        if (!initialized || programId == 0) return
        
        val matrices = context.matrices

        try {
            // Obtain the tessellator and its BufferBuilder.
            val tessellator = Tessellator.getInstance()
            val buffer: BufferBuilder = tessellator.buffer

            // Activate our shader program.
            GL20.glUseProgram(programId)
            GL20.glUniform1f(GL20.glGetUniformLocation(programId, "time"), timeSec)
            GL20.glUniform2f(GL20.glGetUniformLocation(programId, "resolution"),
                width.toFloat(), height.toFloat())

            // Begin drawing a quad using GL11.GL_QUADS and the POSITION_COLOR vertex format.
            buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR)

            // Specify the quad coordinates in screen space.
            val x1 = 0f
            val y1 = 0f
            val x2 = width.toFloat()
            val y2 = height.toFloat()

            buffer.vertex(matrices.peek().positionMatrix, x1, y2, 0f)
            buffer.color(1f, 1f, 1f, 1f)
            buffer.next()

            buffer.vertex(matrices.peek().positionMatrix, x2, y2, 0f)
            buffer.color(1f, 1f, 1f, 1f)
            buffer.next()

            buffer.vertex(matrices.peek().positionMatrix, x2, y1, 0f)
            buffer.color(1f, 1f, 1f, 1f)
            buffer.next()

            buffer.vertex(matrices.peek().positionMatrix, x1, y1, 0f)
            buffer.color(1f, 1f, 1f, 1f)
            buffer.next()

            // Render the quad.
            tessellator.draw()

            // Deactivate the shader.
            GL20.glUseProgram(0)

        } catch (e: Exception) {
            throw RuntimeException("Error rendering shader background", e)
        }
    }

    fun cleanup() {
        if (programId != 0) {
            GL20.glDeleteProgram(programId)
            programId = 0
        }
        initialized = false
    }
}