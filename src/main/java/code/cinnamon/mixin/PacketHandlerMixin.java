package code.cinnamon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import code.cinnamon.util.PacketHandlerAPI;
import code.cinnamon.SharedVariables;

@Mixin(ClientConnection.class)
public class PacketHandlerMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<? extends PacketListener> packet, CallbackInfo ci) {
        if (isInventoryActionPacket(packet)) {
            if (PacketHandlerAPI.isPacketBlocking()) {
                PacketHandlerAPI.getPacketQueue().offer(packet);
                ci.cancel();
            } else if (!SharedVariables.packetSendingEnabled) {
                ci.cancel();
            }
        }
    }

    private boolean isInventoryActionPacket(Packet<?> packet) {
        if (PacketHandlerAPI.isGuiPacketBlocking()) {
            return true;
        }
        return packet instanceof ClickSlotC2SPacket
                || packet instanceof UpdateSelectedSlotC2SPacket;
    }

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onDisconnect(CallbackInfo ci) {
        if (PacketHandlerAPI.isSafeCloseEnabled()) {
            PacketHandlerAPI.flushPacketQueue();
            PacketHandlerAPI.disableSafeClose();
        }
    }
}