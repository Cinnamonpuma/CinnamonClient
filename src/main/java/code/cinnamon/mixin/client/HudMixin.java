package code.cinnamon.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.cinnamon.hud.HudManager;
import code.cinnamon.SharedVariables;

@Mixin(code.cinnamon.hud.HudScreen.class)
public abstract class HudMixin {
    private static code.cinnamon.hud.elements.PacketHandlerHudElement getHudElement() {
        return HudManager.INSTANCE.getPacketHandlerHudElement();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedVariables.enabled && MinecraftClient.getInstance().player != null && getHudElement().isEnabled()) {
            getHudElement().render(context, delta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}