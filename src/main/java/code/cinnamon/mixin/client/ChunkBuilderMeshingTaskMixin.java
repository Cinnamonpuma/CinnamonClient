package code.cinnamon.mixin.client;

import code.cinnamon.meshing.GreedyMesher;
import code.cinnamon.modules.Module;
import code.cinnamon.modules.ModuleManager;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
public abstract class ChunkBuilderMeshingTaskMixin extends ChunkBuilderTask<ChunkBuildOutput> {

    // Dummy constructor to satisfy the parent class
    private ChunkBuilderMeshingTaskMixin(RenderSection render, int flags, Vector3dc cameraPos) {
        super(render, flags, cameraPos);
    }

    @Accessor("renderContext")
    public abstract ChunkRenderContext getRenderContext();

    /**
     * Injects into the start of Sodium's meshing task execution.
     * If our Greedy Meshing module is enabled, this mixin will cancel the original
     * method and run our own mesher instead.
     */
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    public void execute(ChunkBuildContext context, CancellationToken cancellationToken, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        Module module = ModuleManager.INSTANCE.getModule("Greedy Meshing");

        if (module != null && module.isEnabled()) {
            // Run our custom greedy mesher and return its result
            cir.setReturnValue(GreedyMesher.INSTANCE.build(context, this.render, this.getRenderContext()));
        }
    }
}
