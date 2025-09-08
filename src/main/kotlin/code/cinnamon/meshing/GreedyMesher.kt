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
import java.nio.ByteBuffer
import java.util.*

object GreedyMesher {

    fun build(context: ChunkBuildContext, render: RenderSection, renderContext: ChunkRenderContext): ChunkBuildOutput {
        val infoBuilder = BuiltSectionInfo.Builder()
        val meshParts = mutableMapOf<TerrainRenderPass, BuiltSectionMeshParts>()

        for (pass in DefaultTerrainRenderPasses.ALL) {
            val buffer = context.buffers.get(pass).getVertexBuffer(ModelQuadFacing.POS_Y)
            buffer.start(render.sectionIndex)

            if (pass == DefaultTerrainRenderPasses.SOLID) {
                // Greedy meshing logic would go here
            }

            if (!buffer.isEmpty) {
                val nativeBuffer = NativeBuffer.copy(buffer.slice())
                meshParts[pass] = BuiltSectionMeshParts(nativeBuffer, intArrayOf(buffer.count()))
                infoBuilder.addRenderPass(pass)
            } else {
                val emptyBuffer = NativeBuffer.copy(ByteBuffer.allocate(0))
                // The vertex counts array needs to have a size equal to the number of facing directions.
                val vertexCounts = IntArray(ModelQuadFacing.DIRECTIONS)
                meshParts[pass] = BuiltSectionMeshParts(emptyBuffer, vertexCounts)
            }
        }

        val occlusionData = ChunkOcclusionData()
        infoBuilder.setOcclusionData(occlusionData)
        val builtInfo = infoBuilder.build()

        return ChunkBuildOutput(render, 0, null, builtInfo, meshParts)
    }
}
