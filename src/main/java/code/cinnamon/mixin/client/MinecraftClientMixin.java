package code.cinnamon.mixin.client;

import code.cinnamon.modules.all.FakeItemsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    // Target the disconnect method - try common signatures
    @Inject(method = "disconnect()V", at = @At("HEAD"))
    private void onDisconnect(CallbackInfo ci) {
        System.out.println("[FakeItemsModule] Client disconnecting, clearing fake data");
        FakeItemsModule.INSTANCE.clearAllFakeData();
    }

    // Alternative disconnect method signature
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void onDisconnectWithScreen(Screen screen, CallbackInfo ci) {
        System.out.println("[FakeItemsModule] Client disconnecting with screen, clearing fake data");
        FakeItemsModule.INSTANCE.clearAllFakeData();
    }

    // Also clear on game close
    @Inject(method = "close()V", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        System.out.println("[FakeItemsModule] Client closing, clearing fake data");
        FakeItemsModule.INSTANCE.clearAllFakeData();
    }

    // Clear fake data when joining a world
    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        // Clear fake data when going to multiplayer screen or main menu
        if (screen != null) {
            String screenName = screen.getClass().getSimpleName();
            if (screenName.contains("MultiplayerScreen") || screenName.contains("TitleScreen")) {
                System.out.println("[FakeItemsModule] Returning to menu, clearing fake data");
                FakeItemsModule.INSTANCE.clearAllFakeData();
            }
        }
    }
}