// NoFallMixin.java
package code.cinnamon.mixin;

import code.cinnamon.util.NoFallAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class NoFallMixin {
    
    @Shadow
    private MinecraftClient client;
    
    @Shadow
    public abstract boolean isOnGround();
    
    @Shadow
    public abstract Vec3d getVelocity();
    
    @Shadow
    public abstract void setVelocity(Vec3d velocity);
    
    @Shadow
    public abstract double getY();
    
    @Shadow
    public abstract void setPos(double x, double y, double z);
    
    @Shadow
    public abstract boolean isFallFlying();
    
    @Shadow
    public abstract boolean isInLava();
    
    @Shadow
    public abstract boolean isTouchingWater();
    
    // Inject into the tick method to handle various NoFall methods
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        if (!NoFallAccess.isEnabled()) return;
        
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        
        try {
            // Handle different NoFall methods
            switch (NoFallAccess.getMethod()) {
                case PACKET:
                    handlePacketMethod(player);
                    break;
                case VELOCITY:
                    handleVelocityMethod(player);
                    break;
                case TELEPORT:
                    handleTeleportMethod(player);
                    break;
                case SMART:
                    handleSmartMethod(player);
                    break;
                case HYBRID:
                    handleHybridMethod(player);
                    break;
            }
        } catch (Exception e) {
            System.out.println("NoFall: Error in tick: " + e.getMessage());
        }
    }
    
    // Intercept fall damage calculations
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void onHandleFallDamage(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Boolean> cir) {
        if (!NoFallAccess.isEnabled()) return;
        
        try {
            if (NoFallAccess.shouldPreventFallDamage()) {
                // Cancel fall damage entirely for certain methods
                if (NoFallAccess.getMethod() == NoFallAccess.NoFallMethod.DAMAGE_CANCEL) {
                    NoFallAccess.incrementDamagePrevented(fallDistance);
                    cir.setReturnValue(false);
                    return;
                }
                
                // Smart damage reduction
                if (NoFallAccess.getMethod() == NoFallAccess.NoFallMethod.SMART && 
                    fallDistance > NoFallAccess.getMinActivationHeight()) {
                    NoFallAccess.incrementDamagePrevented(fallDistance);
                    cir.setReturnValue(false);
                }
            }
        } catch (Exception e) {
            System.out.println("NoFall: Error preventing fall damage: " + e.getMessage());
        }
    }
    
    // Modify outgoing movement packets for packet-based NoFall
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void onSendMovementPackets(CallbackInfo info) {
        if (!NoFallAccess.isEnabled()) return;
        if (NoFallAccess.getMethod() != NoFallAccess.NoFallMethod.PACKET) return;
        
        try {
            ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
            
            if (shouldActivatePacketMethod(player)) {
                NoFallAccess.requestGroundSpoof();
            }
        } catch (Exception e) {
            System.out.println("NoFall: Error in movement packet handling: " + e.getMessage());
        }
    }
    
    // Handle packet method - spoof being on ground
    private void handlePacketMethod(ClientPlayerEntity player) {
        if (!shouldActivatePacketMethod(player)) return;
        
        // The actual packet spoofing will be handled in the packet send mixin
        NoFallAccess.setLastActivation(System.currentTimeMillis());
        NoFallAccess.incrementActivations();
    }
    
    // Handle velocity method - reduce fall velocity
    private void handleVelocityMethod(ClientPlayerEntity player) {
        if (!shouldActivateVelocityMethod(player)) return;
        
        Vec3d velocity = player.getVelocity();
        double yVel = velocity.y;
        
        // Reduce downward velocity based on settings
        if (yVel < -NoFallAccess.getVelocityReduction()) {
            double newYVel = Math.max(yVel * NoFallAccess.getVelocityMultiplier(), 
                                    -NoFallAccess.getMaxVelocity());
            
            player.setVelocity(velocity.x, newYVel, velocity.z);
            NoFallAccess.setLastActivation(System.currentTimeMillis());
            NoFallAccess.incrementActivations();
        }
    }
    
    // Handle teleport method - micro teleport upward
    private void handleTeleportMethod(ClientPlayerEntity player) {
        if (!shouldActivateTeleportMethod(player)) return;
        
        Vec3d velocity = player.getVelocity();
        
        if (velocity.y < -NoFallAccess.getTeleportThreshold()) {
            double currentY = player.getY();
            double teleportDistance = NoFallAccess.getTeleportDistance();
            
            // Perform micro teleport
            player.setPos(player.getX(), currentY + teleportDistance, player.getZ());
            
            // Reset velocity to prevent rubber banding
            if (NoFallAccess.shouldResetVelocityAfterTeleport()) {
                player.setVelocity(velocity.x, velocity.y * 0.1, velocity.z);
            }
            
            NoFallAccess.setLastActivation(System.currentTimeMillis());
            NoFallAccess.incrementActivations();
        }
    }
    
    // Handle smart method - adaptive approach
    private void handleSmartMethod(ClientPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        double fallDistance = Math.abs(velocity.y);
        
        if (fallDistance < NoFallAccess.getMinActivationHeight()) return;
        
        // Choose method based on situation
        if (player.isOnGround() || player.isTouchingWater() || player.isInLava()) {
            return; // No need to activate
        }
        
        // Use packet method for small falls, velocity for medium, teleport for large
        if (fallDistance < 5.0) {
            if (shouldActivatePacketMethod(player)) {
                NoFallAccess.requestGroundSpoof();
            }
        } else if (fallDistance < 15.0) {
            handleVelocityMethod(player);
        } else {
            handleTeleportMethod(player);
        }
    }
    
    // Handle hybrid method - combine multiple approaches
    private void handleHybridMethod(ClientPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        
        if (shouldActivateHybridMethod(player)) {
            // Primary: Packet spoofing
            if (shouldActivatePacketMethod(player)) {
                NoFallAccess.requestGroundSpoof();
            }
            
            // Secondary: Velocity reduction for high speeds
            if (Math.abs(velocity.y) > NoFallAccess.getHybridVelocityThreshold()) {
                handleVelocityMethod(player);
            }
            
            // Tertiary: Emergency teleport for extreme falls
            if (Math.abs(velocity.y) > NoFallAccess.getHybridTeleportThreshold()) {
                handleTeleportMethod(player);
            }
            
            NoFallAccess.setLastActivation(System.currentTimeMillis());
            NoFallAccess.incrementActivations();
        }
    }
    
    // Activation condition checks
    private boolean shouldActivatePacketMethod(ClientPlayerEntity player) {
        if (player.isOnGround() || player.isFallFlying()) return false;
        if (player.isTouchingWater() || player.isInLava()) return false;
        
        Vec3d velocity = player.getVelocity();
        return velocity.y < -NoFallAccess.getMinActivationHeight() && 
               !hasActivatedRecently();
    }
    
    private boolean shouldActivateVelocityMethod(ClientPlayerEntity player) {
        if (player.isOnGround() || player.isFallFlying()) return false;
        if (player.isTouchingWater() || player.isInLava()) return false;
        
        return !hasActivatedRecently();
    }
    
    private boolean shouldActivateTeleportMethod(ClientPlayerEntity player) {
        if (player.isOnGround() || player.isFallFlying()) return false;
        if (player.isTouchingWater() || player.isInLava()) return false;
        
        return !hasActivatedRecently();
    }
    
    private boolean shouldActivateHybridMethod(ClientPlayerEntity player) {
        if (player.isOnGround() || player.isFallFlying()) return false;
        if (player.isTouchingWater() || player.isInLava()) return false;
        
        Vec3d velocity = player.getVelocity();
        return velocity.y < -NoFallAccess.getMinActivationHeight();
    }
    
    private boolean hasActivatedRecently() {
        long timeSinceLastActivation = System.currentTimeMillis() - NoFallAccess.getLastActivation();
        return timeSinceLastActivation < NoFallAccess.getActivationCooldown();
    }
}