package code.cinnamon.mixin.client;

import code.cinnamon.modules.all.FakeItemsModule;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
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

    // Method signature based on common Fabric mappings (Yarn). Actual names/types can vary.
    // interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;
    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                 CallbackInfoReturnable<ActionResult> cir) {
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return; // Should not happen if interacting with a block

        ItemStack stackInHand = player.getStackInHand(hand);

        if (stackInHand.isEmpty() || !(stackInHand.getItem() instanceof BlockItem)) {
            return; // Not a block item, proceed with normal interaction
        }

        // Attempt to consume this item from our fake item list
        // This relies on FakeItemsModule.consumeFakeItem being able to identify if stackInHand
        // corresponds to an item in its fakeItems list.
        boolean consumed = FakeItemsModule.INSTANCE.consumeFakeItem(stackInHand);

        if (consumed) {
            // Item was found in FakeItemsModule and consumed
            BlockItem blockItem = (BlockItem) stackInHand.getItem();
            Block block = blockItem.getBlock();
            
            // Determine the position to place the block
            // BlockHitResult.getBlockPos() is the block that was hit.
            // We need to place it adjacent, often using hitResult.getSide().
            BlockPos placementPos = hitResult.getBlockPos().offset(hitResult.getSide());

            // Add to our list of placed fake blocks
            // We pass a copy of the stackInHand in case its count was > 1,
            // though consumeFakeItem should have handled the count.
            // Storing the original stack (or a representation of it) is good.
            ItemStack representativeStack = stackInHand.copy();
            representativeStack.setCount(1); // We are placing one block

            FakeItemsModule.INSTANCE.addPlacedFakeBlock(placementPos, block, representativeStack);

            // TODO: Play block placement sound locally?
            // client.world.playSound(player, placementPos, block.getSoundGroup(block.getDefaultState()).getPlaceSound(), 
            //                        SoundCategory.BLOCKS, 1.0f, 1.0f);


            // Prevent server-side placement and return SUCCESS or CONSUME
            cir.setReturnValue(ActionResult.SUCCESS); // Or ActionResult.CONSUME, depending on desired behavior
            // No need to call ci.cancel() explicitly when setReturnValue is used.
        }
        // If not consumed (not a fake item we manage), do nothing and let original method proceed.
    }
}
