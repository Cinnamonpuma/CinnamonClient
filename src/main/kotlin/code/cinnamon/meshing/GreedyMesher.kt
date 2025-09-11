package code.cinnamon.meshing

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass
import net.caffeinemc.mods.sodium.client.util.NativeBuffer
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext
import net.minecraft.client.render.chunk.ChunkOcclusionData
import java.nio.ByteBuffer
import java.util.*

object GreedyMesher {

    fun build(context: ChunkBuildContext, render: RenderSection, renderContext: ChunkRenderContext): ChunkBuildOutput {
        val infoBuilder = BuiltSectionInfo.Builder()
        val meshParts = mutableMapOf<TerrainRenderPass, BuiltSectionMeshParts>()

        for (pass in DefaultTerrainRenderPasses.ALL) {
            // This is the version that fixed the crashes. It produces empty meshes but is stable.
            val emptyBuffer = NativeBuffer.copy(ByteBuffer.allocate(0))
            val vertexCounts = IntArray(ModelQuadFacing.COUNT)
            meshParts[pass] = BuiltSectionMeshParts(emptyBuffer, vertexCounts)
        }

        val occlusionData = ChunkOcclusionData()
        infoBuilder.setOcclusionData(occlusionData)
        val builtInfo = infoBuilder.build()

        return ChunkBuildOutput(render, 0, null, builtInfo, meshParts)
    }
}
