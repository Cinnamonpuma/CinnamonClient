package code.cinnamon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.listener.PacketListener;
import code.cinnamon.util.PacketHandlerAPI;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(ClientConnection.class)
public class PacketHandlerMixin {
    // Static fields must be private!
    private static final Queue<Packet<? extends PacketListener>> packetQueue = PacketHandlerAPI.getPacketQueue();
    private static boolean packetBlocking = false;
    private static boolean safeClose = false;

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<? extends PacketListener> packet, CallbackInfo ci) {
        if (packetBlocking) {
            packetQueue.offer(packet);
            ci.cancel();
        }
        // If you want to process delayed packets, call your API here as needed
    }

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(CallbackInfo ci) {
        if (safeClose) {
            PacketHandlerAPI.flushPacketQueue();
            safeClose = false;
        }
    }

    // Only private static if needed, but ideally all logic should go in PacketHandlerAPI
}