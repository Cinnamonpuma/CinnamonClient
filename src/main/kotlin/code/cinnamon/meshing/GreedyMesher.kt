package code.cinnamon.meshing

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder
import net.caffeinemc.mods.sodium.client.util.NativeBuffer
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext
import net.minecraft.client.render.chunk.ChunkOcclusionData
import net.minecraft.util.math.BlockPos
import java.util.*

object GreedyMesher {

    fun build(context: ChunkBuildContext, render: RenderSection, renderContext: ChunkRenderContext): ChunkBuildOutput {
        val infoBuilder = BuiltSectionInfo.Builder()
        val meshParts = mutableMapOf<TerrainRenderPass, BuiltSectionMeshParts>()

        val solidPass = DefaultTerrainRenderPasses.SOLID
        val buffer = context.buffers.get(solidPass).getVertexBuffer(ModelQuadFacing.POS_Y)

        buffer.start(render.sectionIndex)

        val sectionIndex = renderContext.origin.y
        if (sectionIndex >= 0 && sectionIndex < renderContext.sections.size) {
            val section = renderContext.sections[sectionIndex]
            if (section != null) {
                val blockData = section.blockData
                if (blockData != null) {
                    val visited = BitSet(16 * 16 * 16)
                    for (y in 0 until 16) {
                        for (z in 0 until 16) {
                            for (x in 0 until 16) {
                                val index = x + z * 16 + y * 256
                                if (visited[index]) {
                                    continue
                                }

                                val state = blockData.get(x, y, z)
                                if (state.isAir) {
                                    continue
                                }

                                val upState = if (y + 1 < 16) blockData.get(x, y + 1, z) else null
                                if (upState == null || upState.isAir) {
                                    var w = 1
                                    while (x + w < 16 && !visited[index + w] && blockData.get(x + w, y, z) == state) {
                                        val neighborUpState = if (y + 1 < 16) blockData.get(x + w, y + 1, z) else null
                                        if (neighborUpState != null && !neighborUpState.isAir) break
                                        w++
                                    }

                                    var h = 1
                                    var canExpandHeight = true
                                    while (z + h < 16 && canExpandHeight) {
                                        for (i in 0 until w) {
                                            if (visited[index + i + h * 16] || blockData.get(x + i, y, z + h) != state) {
                                                canExpandHeight = false
                                                break
                                            }
                                            val neighborUpState = if (y + 1 < 16) blockData.get(x + i, y + 1, z + h) else null
                                            if (neighborUpState != null && !neighborUpState.isAir) {
                                                canExpandHeight = false
                                                break
                                            }
                                        }
                                        if (canExpandHeight) {
                                            h++
                                        }
                                    }

                                    for (i in 0 until w) {
                                        for (j in 0 until h) {
                                            visited.set(index + i + j * 16)
                                        }
                                    }

                                    addQuad(buffer, BlockPos(x, y + section.position.minY, z), w, h)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!buffer.isEmpty) {
            val nativeBuffer = NativeBuffer.copy(buffer.slice())
            meshParts[solidPass] = BuiltSectionMeshParts(nativeBuffer, intArrayOf(buffer.count()))
            infoBuilder.addRenderPass(solidPass)
        }

        val occlusionData = ChunkOcclusionData()
        infoBuilder.setOcclusionData(occlusionData)
        val builtInfo = infoBuilder.build()

        return ChunkBuildOutput(render, 0, null, builtInfo, meshParts)
    }

    private fun addQuad(
        buffer: ChunkMeshBufferBuilder,
        pos: BlockPos,
        width: Int,
        height: Int
    ) {
        val x1 = pos.x.toFloat()
        val y1 = pos.y.toFloat()
        val z1 = pos.z.toFloat()
        val x2 = x1 + width
        val y2 = y1 + 1
        val z2 = z1 + height

        val vertices = ChunkVertexEncoder.Vertex.uninitializedQuad()

        val v1 = vertices[0]; v1.x = x1; v1.y = y2; v1.z = z1; v1.u = 0f; v1.v = 0f
        val v2 = vertices[1]; v2.x = x1; v2.y = y2; v2.z = z2; v2.u = 0f; v2.v = height.toFloat()
        val v3 = vertices[2]; v3.x = x2; v3.y = y2; v3.z = z2; v3.u = width.toFloat(); v3.v = height.toFloat()
        val v4 = vertices[3]; v4.x = x2; v4.y = y2; v4.z = z1; v4.u = width.toFloat(); v4.v = 0f

        for (v in vertices) {
            v.color = -1
            v.light = 15728880
        }

        buffer.push(vertices, 0)
    }
}
