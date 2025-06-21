package code.cinnamon.mixin.client;

import code.cinnamon.modules.all.FakeItemsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen; // For method signature if world is parameter
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    // Target the disconnect() method. Its signature is typically ()V or (Lnet/minecraft/client/gui/screen/Screen;)V
    // Common signature: disconnect(Lnet/minecraft/client/gui/screen/Screen;)V which is often used to show a disconnect screen.
    // Another simpler one is just disconnect()
    // Let's try the one without parameters first, if it fails, we'd try with Screen.
    // JNI signature for ()V is "()V"
    // JNI signature for (Lnet/minecraft/client/gui/screen/Screen;)V is "(Lnet/minecraft/client/gui/screen/Screen;)V"
    
    // Trying the simpler disconnect() first. If this is not found, the mixin will fail to apply.
    // A common one is also `public void disconnect(Screen screen)`
    // For this example, let's assume there's a parameterless version or a version where we don't need the screen parameter.
    // If the actual method is `disconnect(Screen screen)`, the method string should be:
    // method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V"
    // and the mixin method should also take `Screen screen, CallbackInfo ci`.
    
    // Let's target the version that takes a Screen, as it's common for handling disconnect screens.
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, CallbackInfo ci) {
        System.out.println("[FakeItemsModule] MinecraftClient.disconnect() called. Clearing fake data.");
        FakeItemsModule.INSTANCE.clearAllFakeData();
    }

    // As a fallback, or if there are multiple disconnect methods, also consider the parameterless one.
    // However, overloads with different signatures require distinct mixin methods or more complex @At targets.
    // For now, focusing on the one with Screen parameter.
    // If a parameter-less disconnect() is the actual target:
    // @Inject(method = "disconnect()V", at = @At("HEAD"))
    // private void onDisconnectParameterless(CallbackInfo ci) {
    //     System.out.println("[FakeItemsModule] MinecraftClient.disconnect() parameterless called. Clearing fake data.");
    //     FakeItemsModule.INSTANCE.clearAllFakeData();
    // }

     // Also, consider the close() method for game shutdown
    @Inject(method = "close()V", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        System.out.println("[FakeItemsModule] MinecraftClient.close() called. Clearing fake data.");
        FakeItemsModule.INSTANCE.clearAllFakeData();
    }
}
