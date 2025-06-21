package code.cinnamon.mixin.client;

import code.cinnamon.modules.all.FakeItemsModule;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                 CallbackInfoReturnable<ActionResult> cir) {
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }

        ItemStack stackInHand = player.getStackInHand(hand);
        if (stackInHand.isEmpty() || !(stackInHand.getItem() instanceof BlockItem)) {
            return; // Not a block item
        }

        // Try to consume from fake inventory
        boolean consumed = FakeItemsModule.INSTANCE.consumeFakeItem(stackInHand);
        
        if (consumed) {
            BlockItem blockItem = (BlockItem) stackInHand.getItem();
            Block block = blockItem.getBlock();
            
            // Calculate placement position
            BlockPos placementPos = hitResult.getBlockPos().offset(hitResult.getSide());
            
            // Check if placement position is valid (not occupied)
            if (!client.world.getBlockState(placementPos).isReplaceable()) {
                // Position is occupied, don't place but still consume the item
                cir.setReturnValue(ActionResult.FAIL);
                return;
            }

            // Add to fake placed blocks
            ItemStack representativeStack = stackInHand.copy();
            representativeStack.setCount(1);
            FakeItemsModule.INSTANCE.addPlacedFakeBlock(placementPos, block, representativeStack);

            // Play placement sound locally
            try {
                client.world.playSound(
                    client.player, 
                    placementPos, 
                    block.getDefaultState().getSoundGroup().getPlaceSound(), 
                    SoundCategory.BLOCKS, 
                    1.0f, 
                    1.0f
                );
            } catch (Exception e) {
                // Sound playing failed, continue anyway
                System.err.println("[FakeItemsModule] Failed to play placement sound: " + e.getMessage());
            }

            // Return success to indicate the interaction was handled
            cir.setReturnValue(ActionResult.SUCCESS);
        }
        // If not consumed (not a fake item), let normal placement proceed
    }
}