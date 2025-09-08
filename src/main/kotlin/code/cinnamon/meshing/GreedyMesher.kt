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
                // The actual greedy meshing logic would go here.
                // For now, we are just ensuring the data structure is correct.
                // The buffer will be empty, but correctly processed.
            }

            if (!buffer.isEmpty) {
                val nativeBuffer = NativeBuffer.copy(buffer.slice())
                meshParts[pass] = BuiltSectionMeshParts(nativeBuffer, intArrayOf(buffer.count()))
                infoBuilder.addRenderPass(pass)
            } else {
                // For all other passes, create empty data with a valid vertex count array.
                val emptyBuffer = NativeBuffer.copy(ByteBuffer.allocate(0))
                meshParts[pass] = BuiltSectionMeshParts(emptyBuffer, intArrayOf(0))
                // Do not add the pass to the infoBuilder if it has no geometry
            }
        }

        val occlusionData = ChunkOcclusionData()
        infoBuilder.setOcclusionData(occlusionData)
        val builtInfo = infoBuilder.build()

        return ChunkBuildOutput(render, 0, null, builtInfo, meshParts)
    }
}
