// NoFallPacketMixin.java
package code.cinnamon.mixin;

import code.cinnamon.util.NoFallAccess;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class NoFallPacketMixin {
    
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo info) {
        if (!NoFallAccess.isEnabled()) return;
        
        try {
            // Handle packet spoofing for movement packets
            if (packet instanceof PlayerMoveC2SPacket) {
                PlayerMoveC2SPacket movePacket = (PlayerMoveC2SPacket) packet;
                
                if (NoFallAccess.shouldSpoofGround()) {
                    // Create a new packet with onGround set to true
                    PlayerMoveC2SPacket spoofedPacket = createSpoofedPacket(movePacket);
                    
                    if (spoofedPacket != null) {
                        // Cancel the original packet
                        info.cancel();
                        
                        // Send the spoofed packet instead
                        ((ClientPlayNetworkHandler) (Object) this).sendPacket(spoofedPacket);
                        
                        NoFallAccess.clearGroundSpoofRequest();
                        NoFallAccess.incrementPacketsSpoofed();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("NoFall: Error spoofing packet: " + e.getMessage());
        }
    }
    
    private PlayerMoveC2SPacket createSpoofedPacket(PlayerMoveC2SPacket original) {
        try {
            // Create appropriate spoofed packet based on the original type
            if (original instanceof PlayerMoveC2SPacket.Full) {
                PlayerMoveC2SPacket.Full fullPacket = (PlayerMoveC2SPacket.Full) original;
                return new PlayerMoveC2SPacket.Full(
                    fullPacket.getX(),
                    fullPacket.getY(),
                    fullPacket.getZ(),
                    fullPacket.getYaw(),
                    fullPacket.getPitch(),
                    true, // Force onGround to true
                    fullPacket.horizontalCollision()
                );
            } else if (original instanceof PlayerMoveC2SPacket.PositionAndOnGround) {
                PlayerMoveC2SPacket.PositionAndOnGround posPacket = (PlayerMoveC2SPacket.PositionAndOnGround) original;
                return new PlayerMoveC2SPacket.PositionAndOnGround(
                    posPacket.getX(),
                    posPacket.getY(),
                    posPacket.getZ(),
                    true, // Force onGround to true
                    posPacket.horizontalCollision()
                );
            } else if (original instanceof PlayerMoveC2SPacket.LookAndOnGround) {
                PlayerMoveC2SPacket.LookAndOnGround lookPacket = (PlayerMoveC2SPacket.LookAndOnGround) original;
                return new PlayerMoveC2SPacket.LookAndOnGround(
                    lookPacket.getYaw(),
                    lookPacket.getPitch(),
                    true, // Force onGround to true
                    lookPacket.horizontalCollision()
                );
            } else if (original instanceof PlayerMoveC2SPacket.OnGroundOnly) {
                // Already only contains onGround, create new one with true
                PlayerMoveC2SPacket.OnGroundOnly groundPacket = (PlayerMoveC2SPacket.OnGroundOnly) original;
                return new PlayerMoveC2SPacket.OnGroundOnly(
                    true, // Force onGround to true
                    groundPacket.horizontalCollision()
                );
            }
        } catch (Exception e) {
            System.out.println("NoFall: Error creating spoofed packet: " + e.getMessage());
        }
        
        return null;
    }
}