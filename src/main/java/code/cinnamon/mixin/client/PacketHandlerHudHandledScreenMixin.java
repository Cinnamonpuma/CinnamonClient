package code.cinnamon.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.cinnamon.hud.elements.PacketHandlerHudElement;
import code.cinnamon.SharedVariables;

@Mixin(HandledScreen.class)
public abstract class PacketHandlerHudHandledScreenMixin {
    private static final PacketHandlerHudElement packetHandlerHudElement = new PacketHandlerHudElement(10.0f, 10.0f);

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (SharedVariables.enabled && client.player != null) {
            packetHandlerHudElement.render(context, delta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (SharedVariables.enabled && client.player != null && packetHandlerHudElement.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    // REMOVED: no such method in HandledScreen in 1.21+
    // @Inject(method = "mouseMoved", at = @At("HEAD"))
    // private void onMouseMoved(double mouseX, double mouseY, CallbackInfo ci) {
    //     MinecraftClient client = MinecraftClient.getInstance();
    //     if (SharedVariables.enabled && client.player != null) {
    //         packetHandlerHudElement.mouseMoved(mouseX, mouseY);
    //     }
    // }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (SharedVariables.enabled && client.player != null && packetHandlerHudElement.mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (SharedVariables.enabled && client.player != null && packetHandlerHudElement.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}