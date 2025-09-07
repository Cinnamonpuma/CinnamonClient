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

        // --- Greedy Meshing Logic ---
        // (This part is simplified and would be filled in)
        // For now, we just create an empty mesh to prove the data flow.
        // ---

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
}
