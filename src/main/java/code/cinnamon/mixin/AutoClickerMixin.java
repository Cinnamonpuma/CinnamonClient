package code.cinnamon.mixin;

import code.cinnamon.util.AutoClickerAccess;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class AutoClickerMixin {
    
    @Shadow
    private MinecraftClient client;
    
    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo info) {

    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        if (client == null) return;
        
        if (AutoClickerAccess.hasPendingLeftClick()) {
            AutoClickerAccess.clearPendingLeftClick();
            performLeftClick();
        }
        
        if (AutoClickerAccess.hasPendingRightClick()) {
            AutoClickerAccess.clearPendingRightClick();
            performRightClick();
        }
    }
    
    private void performLeftClick() {
        try {
            if (this.client == null) return;
            
            if (client.currentScreen == null) {
                if (client.crosshairTarget != null) {
                    HitResult.Type hitType = client.crosshairTarget.getType();
                    
                    if (hitType == HitResult.Type.ENTITY) {
                        EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
                        client.interactionManager.attackEntity(client.player, entityHit.getEntity());
                        client.player.swingHand(Hand.MAIN_HAND);
                    } else if (hitType == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
                        client.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                        client.player.swingHand(Hand.MAIN_HAND);
                    } else {
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("AutoClicker: Error performing left click: " + e.getMessage());
        }
    }
    
    private void performRightClick() {
        try {
            if (this.client == null) return;
            
            if (client.currentScreen == null) {
                if (client.crosshairTarget != null) {
                    HitResult.Type hitType = client.crosshairTarget.getType();
                    
                    if (hitType == HitResult.Type.BLOCK) {
                        BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
                        var result = client.interactionManager.interactBlock(
                            client.player, 
                            Hand.MAIN_HAND, 
                            blockHit
                        );
                        
                        if (result == null || !result.isAccepted()) {
                            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        }
                    } else if (hitType == HitResult.Type.ENTITY) {
                        EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
                        client.interactionManager.interactEntity(client.player, entityHit.getEntity(), Hand.MAIN_HAND);
                    } else {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("AutoClicker: Error performing right click: " + e.getMessage());
        }
    }
}