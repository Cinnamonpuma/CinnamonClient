package code.cinnamon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.listener.PacketListener;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Mixin(ClientConnection.class)
public class PacketHandlerMixin {
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final Queue<DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();
    private static final Queue<Packet<? extends PacketListener>> packetQueue = new ConcurrentLinkedQueue<>();
    private static boolean packetBlocking = false;
    private static boolean safeClose = false;
    
    private static class DelayedPacket {
        final Packet<? extends PacketListener> packet;
        final long sendTime;
        
        DelayedPacket(Packet<? extends PacketListener> packet, long delay) {
            this.packet = packet;
            this.sendTime = System.currentTimeMillis() + delay;
        }
    }
    
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<? extends PacketListener> packet, CallbackInfo ci) {
        if (packetBlocking) {
            packetQueue.offer(packet);
            ci.cancel();
            return;
        }
        
        // Process delayed packets
        processDelayedPackets();
    }
    
    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(CallbackInfo ci) {
        if (safeClose) {
            // Send all queued packets before disconnecting
            flushPacketQueue();
            processDelayedPackets();
            safeClose = false;
        }
    }
    
    private static void processDelayedPackets() {
        long currentTime = System.currentTimeMillis();
        DelayedPacket delayed;
        
        while ((delayed = delayedPackets.peek()) != null && delayed.sendTime <= currentTime) {
            delayedPackets.poll();
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                MinecraftClient.getInstance().getNetworkHandler().getConnection().send(delayed.packet);
            }
        }
    }
    
    private static void flushPacketQueue() {
        Packet<? extends PacketListener> packet;
        while ((packet = packetQueue.poll()) != null) {
            if (MinecraftClient.getInstance().getNetworkHandler() != null) {
                MinecraftClient.getInstance().getNetworkHandler().getConnection().send(packet);
            }
        }
    }
    
    // Public API methods
    public static void delayPacket(Packet<? extends PacketListener> packet, long delayMs) {
        delayedPackets.offer(new DelayedPacket(packet, delayMs));
    }
    
    public static void sendPacket(Packet<? extends PacketListener> packet) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            MinecraftClient.getInstance().getNetworkHandler().getConnection().send(packet);
        }
    }
    
    public static void startPacketBlocking() {
        packetBlocking = true;
    }
    
    public static void stopPacketBlocking() {
        packetBlocking = false;
        flushPacketQueue();
    }
    
    public static void enableSafeClose() {
        safeClose = true;
    }
    
    public static void clearQueues() {
        packetQueue.clear();
        delayedPackets.clear();
    }
    
    public static int getQueuedPacketCount() {
        return packetQueue.size();
    }
    
    public static int getDelayedPacketCount() {
        return delayedPackets.size();
    }
    
    public static boolean isPacketBlocking() {
        return packetBlocking;
    }
    
    public static boolean isSafeCloseEnabled() {
        return safeClose;
    }
    
    public static ScheduledExecutorService getScheduler() {
        return scheduler;
    }
    
    public static void shutdownScheduler() {
        scheduler.shutdown();
    }
}

// Utility class for advanced packet operations
class PacketUtils {
    
    public static void delayPacketByTicks(Packet<? extends PacketListener> packet, int ticks) {
        long delayMs = ticks * 50; // 50ms per tick
        PacketHandlerMixin.delayPacket(packet, delayMs);
    }
    
    public static void sendPacketAfterDelay(Packet<? extends PacketListener> packet, long delayMs) {
        PacketHandlerMixin.getScheduler().schedule(() -> {
            PacketHandlerMixin.sendPacket(packet);
        }, delayMs, TimeUnit.MILLISECONDS);
    }
    
    public static void sendRepeatingPacket(Packet<? extends PacketListener> packet, long intervalMs, int count) {
        for (int i = 0; i < count; i++) {
            final int index = i;
            PacketHandlerMixin.getScheduler().schedule(() -> {
                PacketHandlerMixin.sendPacket(packet);
            }, intervalMs * index, TimeUnit.MILLISECONDS);
        }
    }
    
    public static void batchSendPackets(List<Packet<? extends PacketListener>> packets, long intervalMs) {
        for (int i = 0; i < packets.size(); i++) {
            final Packet<? extends PacketListener> packet = packets.get(i);
            PacketHandlerMixin.getScheduler().schedule(() -> {
                PacketHandlerMixin.sendPacket(packet);
            }, intervalMs * i, TimeUnit.MILLISECONDS);
        }
    }
    
    public static void sendCommand(String command) {
        if (MinecraftClient.getInstance().player != null) {
            if (command.startsWith("/")) {
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command.substring(1));
            } else {
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);
            }
        }
    }
    
    public static void sendChatMessage(String message) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.networkHandler.sendChatMessage(message);
        }
    }
    
    public static void shutdown() {
        PacketHandlerMixin.shutdownScheduler();
    }
}