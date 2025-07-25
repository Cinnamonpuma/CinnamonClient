package code.cinnamon.util;

import code.cinnamon.SharedVariables;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PacketHandlerAPI {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final Queue<DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();
    private static final Queue<Packet<? extends PacketListener>> packetQueue = new ConcurrentLinkedQueue<>();
    private static boolean packetBlocking = false;
    private static boolean guiPacketBlocking = false;
    private static boolean safeClose = false;

    private static class DelayedPacket {
        final Packet<? extends PacketListener> packet;
        final long sendTime;

        DelayedPacket(Packet<? extends PacketListener> packet, long delay) {
            this.packet = packet;
            this.sendTime = System.currentTimeMillis() + delay;
        }
    }

    static {
        scheduler.scheduleAtFixedRate(PacketHandlerAPI::processDelayedPackets, 50, 50, TimeUnit.MILLISECONDS);
    }

    public static void processDelayedPackets() {
        long currentTime = System.currentTimeMillis();
        DelayedPacket delayed;
        while ((delayed = delayedPackets.peek()) != null && delayed.sendTime <= currentTime) {
            delayedPackets.poll();
            if (MinecraftClient.getInstance().getNetworkHandler() != null && SharedVariables.packetSendingEnabled) {
                MinecraftClient.getInstance().getNetworkHandler().getConnection().send(delayed.packet);
            }
        }
    }

    public static void flushPacketQueue() {
        Packet<? extends PacketListener> packet;
        while ((packet = packetQueue.poll()) != null) {
            if (MinecraftClient.getInstance().getNetworkHandler() != null && SharedVariables.packetSendingEnabled) {
                MinecraftClient.getInstance().getNetworkHandler().getConnection().send(packet);
            }
        }
    }

    public static void delayPacket(Packet<? extends PacketListener> packet, long delayMs) {
        delayedPackets.offer(new DelayedPacket(packet, delayMs));
    }

    public static void sendPacket(Packet<? extends PacketListener> packet) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null && SharedVariables.packetSendingEnabled) {
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

    public static void disableSafeClose() {
        safeClose = false;
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

    public static void setGuiPacketBlocking(boolean block) {
        guiPacketBlocking = block;
    }

    public static boolean isGuiPacketBlocking() {
        return guiPacketBlocking;
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

    public static Queue<Packet<? extends PacketListener>> getPacketQueue() {
        return packetQueue;
    }
}