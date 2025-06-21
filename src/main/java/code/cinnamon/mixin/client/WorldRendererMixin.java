package code.cinnamon.mixin.client;

import code.cinnamon.modules.all.FakeItemsModule;
import com.mojang.blaze3d.systems.RenderSystem; // For GL state if needed, though often managed by renderBlockAsEntity
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager; // Keep for original signature, might be unused with new one
import net.minecraft.client.render.OverlayTexture; // For new rendering method
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer; // For new rendering method
import net.minecraft.client.render.VertexConsumerProvider; 
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedSimpleModel; // Changed from BakedModel
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.GameRenderer; // Added for class_757
import net.minecraft.client.render.Camera; // Added for class_4184
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow; // Keep @Shadow client
import kotlin.Triple;
// Removed class_4184 and class_757 direct imports as they are replaced by proper classes.
// The JNI signature in @Inject will still use Lnet/minecraft/class_4184; and Lnet/minecraft/class_757;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow private MinecraftClient client;
    @Shadow private net.minecraft.client.render.BufferBuilderStorage bufferBuilders; // Assuming this is the correct field for bufferBuilders based on method_22710 context
    @Shadow private net.minecraft.client.world.ClientWorld world; // Shadow the world

    // World Rendering Logic Temporarily Commented Out
    // @Inject(
    //     // The JNI signature in 'method' MUST use the mapped names (class_xxxx) if that's what the mapping provides for the target.
    //     method = "method_22710(Lnet/minecraft/class_9922;Lnet/minecraft/class_9779;ZLnet/minecraft/class_4184;Lnet/minecraft/class_757;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
    //     at = @At("TAIL")
    // )
    // private void onRenderWorldTail(
    //     ObjectAllocator allocator,
    //     RenderTickCounter tickCounter,
    //     boolean renderBlockOutline,
    //     Camera camera, // Updated from class_4184
    //     GameRenderer gameRenderer, // Updated from class_757
    //     Matrix4f positionMatrix,
    //     Matrix4f projectionMatrix,
    //     CallbackInfo ci) {
        
    //     if (this.client == null || this.world == null) {
    //         return;
    //     }

    //     var fakeBlocksToRender = FakeItemsModule.INSTANCE.getPlacedFakeBlocks();
    //     if (fakeBlocksToRender.isEmpty()) {
    //         return;
    //     }

    //     BlockRenderManager blockRenderManager = this.client.getBlockRenderManager();
    //     VertexConsumerProvider.Immediate vertexConsumerProvider = this.bufferBuilders.getEntityVertexConsumers();

    //     double cameraX = camera.getPos().x;
    //     double cameraY = camera.getPos().y;
    //     double cameraZ = camera.getPos().z;
        
    //     MatrixStack matrices = new MatrixStack(); 

    //     for (Triple<BlockPos, Block, net.minecraft.item.ItemStack> fakeBlockData : fakeBlocksToRender) {
    //         BlockPos blockPos = fakeBlockData.getFirst();
    //         Block block = fakeBlockData.getSecond();
    //         BlockState blockState = block.getDefaultState();

    //         matrices.push();
    //         matrices.translate(blockPos.getX() - cameraX, blockPos.getY() - cameraY, blockPos.getZ() - cameraZ);

    //         try {
    //             BlockModelRenderer blockModelRenderer = blockRenderManager.getModelRenderer();
    //             // Changed type to BakedSimpleModel and added cast
    //             net.minecraft.client.render.model.BakedSimpleModel bakedModel = (net.minecraft.client.render.model.BakedSimpleModel) blockRenderManager.getModel(blockState);
    //             VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(RenderLayer.getSolid());

    //             blockModelRenderer.render(
    //                 (BlockRenderView)this.world,
    //                 bakedModel, // Use BakedSimpleModel type
    //                 blockState,
    //                 blockPos,
    //                 matrices,
    //                 vertexConsumer,
    //                 true, // cull
    //                 Random.create(),
    //                 0L, // seed
    //                 OverlayTexture.DEFAULT_UV
    //             );

    //         } catch (Exception e) {
    //             System.err.println("[FakeItemsModule] Error rendering fake block at " + blockPos + ": " + e.getMessage());
    //         } finally {
    //             matrices.pop();
    //         }
    //     }
        
    //     vertexConsumerProvider.draw(); 
    // }
}
