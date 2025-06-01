// PacketCombinerMixin.java
package code.cinnamon.mixin;

import code.cinnamon.util.PacketCombinerAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.ClientIntentionPacket;
import net.minecraft.network.packet.c2s.login.LoginStartPacket;
import net.minecraft.network.packet.s2c.login.LoginSuccessPacket;
import net.minecraft.network.packet.s2c.login.LoginDisconnectPacket;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientConnection.class)
public class PacketCombinerMixin {
    
    @Shadow
    private PacketListener packetListener;
    
    @Shadow
    private NetworkSide side;
    
    private String clientId = null;
    private boolean isInCombinerMode = false;
    private long connectionStartTime = 0;
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConnectionInit(NetworkSide side, CallbackInfo info) {
        this.connectionStartTime = System.currentTimeMillis();
        this.clientId = "client_" + System.currentTimeMillis() + "_" + hashCode();
        this.isInCombinerMode = PacketCombinerAccess.isActive();
        
        if (this.isInCombinerMode) {
            System.out.println("PacketCombiner: New connection initialized - " + clientId);
        }
    }
    
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onPacketSend(Packet<?> packet, CallbackInfo info) {
        if (!PacketCombinerAccess.isActive()) return;
        
        try {
            boolean intercepted = false;
            
            // Intercept handshake packets
            if (packet instanceof ClientIntentionPacket) {
                ClientIntentionPacket handshake = (ClientIntentionPacket) packet;
                intercepted = PacketCombinerAccess.interceptHandshake(handshake);
                
                if (intercepted) {
                    System.out.println("PacketCombiner: Intercepted handshake from " + clientId);
                    System.out.println("  Target: " + handshake.getAddress() + ":" + handshake.getPort());
                    System.out.println("  Intent: " + handshake.getIntention());
                }
            }
            
            // Intercept login start packets
            else if (packet instanceof LoginStartPacket) {
                LoginStartPacket login = (LoginStartPacket) packet;
                intercepted = PacketCombinerAccess.interceptLogin(login, clientId);
                
                if (intercepted) {
                    System.out.println("PacketCombiner: Intercepted login from " + clientId);
                    System.out.println("  Username: " + login.getName());
                    System.out.println("  Profile ID: " + login.getProfileId());
                }
            }
            
            // Cancel the packet if it was intercepted
            if (intercepted) {
                info.cancel();
            }
            
        } catch (Exception e) {
            System.out.println("PacketCombiner: Error intercepting outbound packet: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void onPacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo info) {
        if (!PacketCombinerAccess.isActive()) return;
        
        try {
            // Monitor incoming packets for combination feedback
            if (packet instanceof LoginSuccessPacket) {
                LoginSuccessPacket success = (LoginSuccessPacket) packet;
                System.out.println("PacketCombiner: Login success received for " + clientId);
                System.out.println("  Username: " + success.getProfile().getName());
                System.out.println("  UUID: " + success.getProfile().getId());
                
                // Notify the combiner of successful authentication
                onLoginSuccess(success);
            }
            
            else if (packet instanceof LoginDisconnectPacket) {
                LoginDisconnectPacket disconnect = (LoginDisconnectPacket) packet;
                System.out.println("PacketCombiner: Login failed for " + clientId);
                System.out.println("  Reason: " + disconnect.getReason().getString());
                
                // Handle login failure
                onLoginFailure(disconnect);
            }
            
        } catch (Exception e) {
            System.out.println("PacketCombiner: Error processing inbound packet: " + e.getMessage());
        }
    }
    
    @Inject(method = "setPacketListener", at = @At("HEAD"))
    private void onPacketListenerChange(PacketListener listener, CallbackInfo info) {
        if (!PacketCombinerAccess.isActive()) return;
        
        String listenerType = listener.getClass().getSimpleName();
        System.out.println("PacketCombiner: Packet listener changed to " + listenerType + " for " + clientId);
        
        // Track connection state changes
        if (listener instanceof ClientLoginNetworkHandler) {
            System.out.println("PacketCombiner: Entering login phase for " + clientId);
        } else if (listener instanceof ClientPlayNetworkHandler) {
            System.out.println("PacketCombiner: Entering play phase for " + clientId);
        }
    }
    
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(CallbackInfo info) {
        if (!PacketCombinerAccess.isActive()) return;
        
        System.out.println("PacketCombiner: Connection disconnecting for " + clientId);
        
        // Clean up any pending packets for this client
        if (PacketCombinerAccess.isIntercepting()) {
            PacketCombinerAccess.clearPendingPackets();
        }
    }
    
    // Handle successful login
    private void onLoginSuccess(LoginSuccessPacket packet) {
        try {
            // Implementation would notify the combiner module about successful authentication
            // This allows the module to handle packet mirroring and connection management
            System.out.println("PacketCombiner: Processing login success for client " + clientId);
            
            // In a full implementation, this would:
            // 1. Notify other connected clients about the successful login
            // 2. Set up packet mirroring if enabled
            // 3. Update connection state in the module
            
        } catch (Exception e) {
            System.out.println("PacketCombiner: Error handling login success: " + e.getMessage());
        }
    }
    
    // Handle login failure
    private void onLoginFailure(LoginDisconnectPacket packet) {
        try {
            System.out.println("PacketCombiner: Processing login failure for client " + clientId);
            
            // Clean up state and potentially retry with different combination strategy
            PacketCombinerAccess.clearPendingPackets();
            
        } catch (Exception e) {
            System.out.println("PacketCombiner: Error handling login failure: " + e.getMessage());
        }
    }
    
    // Getter methods for debugging
    public String getClientId() {
        return clientId;
    }
    
    public boolean isInCombinerMode() {
        return isInCombinerMode;
    }
    
    public long getConnectionAge() {
        return System.currentTimeMillis() - connectionStartTime;
    }
}

// Additional Mixin for handling specific network events
@Mixin(ClientLoginNetworkHandler.class)
abstract class LoginNetworkHandlerMixin {
    
    @Shadow
    private MinecraftClient client;
    
    @Shadow
    private ClientConnection connection;
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onLoginHandlerInit(ClientConnection connection, MinecraftClient client, CallbackInfo info) {
        if (PacketCombinerAccess.isActive()) {
            System.out.println("PacketCombiner: Login network handler initialized");
        }
    }
    
    @Inject(method = "onLoginSuccess", at = @At("HEAD"))
    private void onLoginSuccessHandler(LoginSuccessPacket packet, CallbackInfo info) {
        if (PacketCombinerAccess.isActive()) {
            System.out.println("PacketCombiner: Login success handler called");
            System.out.println("  Profile: " + packet.getProfile().getName());
            
            // This is where we can implement client synchronization
            // Both clients should receive the same success packet
        }
    }
    
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onLoginDisconnectHandler(LoginDisconnectPacket packet, CallbackInfo info) {
        if (PacketCombinerAccess.isActive()) {
            System.out.println("PacketCombiner: Login disconnect handler called");
            System.out.println("  Reason: " + packet.getReason().getString());
            
            // Handle disconnection during login phase
            PacketCombinerAccess.emergencyCleanup();
        }
    }
}

// Mixin for handling the play network handler
@Mixin(ClientPlayNetworkHandler.class)
abstract class PlayNetworkHandlerMixin {
    
    @Shadow
    private MinecraftClient client;
    
    @Shadow
    private ClientConnection connection;
    
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onPlayHandlerInit(MinecraftClient client, ClientConnection connection, CallbackInfo info) {
        if (PacketCombinerAccess.isActive()) {
            System.out.println("PacketCombiner: Play network handler initialized - client is now in game");
            
            // At this point, the combination was successful and the client is in the game
            // We can now set up packet mirroring for gameplay packets if needed
        }
    }
    
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onPlayDisconnectHandler(CallbackInfo info) {
        if (PacketCombinerAccess.isActive()) {
            System.out.println("PacketCombiner: Play disconnect handler called - cleaning up");
            PacketCombinerAccess.emergencyCleanup();
        }
    }
    
    // Optional: Intercept specific gameplay packets for mirroring
    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void onGameplayPacketSend(Packet<?> packet, CallbackInfo info) {
        if (PacketCombinerAccess.isActive()) {
            // Here you could implement selective packet mirroring
            // For example, mirror player movement, chat, or interaction packets
            // to keep both clients synchronized during gameplay
            
            // Example implementation would check packet type and forward to other clients
            // This is where the "dual client" experience would be maintained
        }
    }
}