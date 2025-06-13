package code.cinnamon.mixin.client;

import code.cinnamon.hud.HudManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(
        method = "onMouseButton",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onMouseButtonMixin(long window, int button, int action, int mods, CallbackInfo ci) {
        // action == 1 means press, action == 0 means release. We care about presses for clicks.
        if (action == 1) {
            MinecraftClient mc = MinecraftClient.getInstance();
            // It's important to check if mc.mouse is available, though it usually is here.
            if (mc.mouse != null && mc.getWindow() != null) {
                // Calculate scaled mouse coordinates
                double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
                double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

                // Call the static method from HudManager (which is a Kotlin object)
                // HudManager.INSTANCE.handleGlobalMouseClick(...) if HudManager is an object
                // Or HudManager.handleGlobalMouseClick(...) if it's a companion object method annotated with @JvmStatic
                // Assuming handleGlobalMouseClick is a @JvmStatic method in HudManager's companion object or HudManager is an object.
                // From previous steps, handleGlobalMouseClick is in HudManager (object). So it's HudManager.INSTANCE.
                // However, Kotlin objects' methods are typically accessed via INSTANCE if calling from Java, 
                // UNLESS the Kotlin compiler generates static methods, which it might for an object.
                // Let's assume direct call works due to how Kotlin objects are compiled or if it's in a companion with @JvmStatic.
                // The previous Kotlin mixin called HudManager.handleGlobalMouseClick directly.
                // For a Kotlin `object HudManager`, its methods are accessed via `HudManager.INSTANCE.method()` from Java.
                // Let's use INSTANCE for safety.

                if (HudManager.INSTANCE.handleGlobalMouseClick(mouseX, mouseY, button)) {
                    ci.cancel(); // Cancel vanilla processing if a HUD button handled the click
                }
            }
        }
    }
}
