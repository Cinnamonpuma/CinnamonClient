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

    // Inject into renderContent method since that's where HUD rendering happens
    @Inject(method = "renderContent(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    private void onRenderContent(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (SharedVariables.enabled && MinecraftClient.getInstance().player != null && getHudElement().isEnabled()) {
            getHudElement().renderElement(context, delta);
        }
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged(DDIDD)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!HudManager.INSTANCE.isEditMode() && SharedVariables.enabled && MinecraftClient.getInstance().player != null
                && getHudElement().isEnabled()
                && getHudElement().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}