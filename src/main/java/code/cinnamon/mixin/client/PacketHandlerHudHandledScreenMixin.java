package code.cinnamon.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import code.cinnamon.hud.HudManager;
import code.cinnamon.SharedVariables;

@Mixin(HandledScreen.class)
public abstract class PacketHandlerHudHandledScreenMixin {

    private static code.cinnamon.hud.elements.PacketHandlerHudElement getHudElement() {
        return HudManager.INSTANCE.getPacketHandlerHudElement();
    }

    private boolean isHudScreen() {
        Screen current = MinecraftClient.getInstance().currentScreen;
        return current != null && current.getClass().getName().equals("code.cinnamon.hud.HudScreen");
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldRender = SharedVariables.enabled && client.player != null;

        if (isHudScreen()) {
            shouldRender = true;
        }

        if (shouldRender && getHudElement().isEnabled()) {
            getHudElement().renderElement(context, delta);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldHandle = SharedVariables.enabled && client.player != null;
        if (isHudScreen()) {
            shouldHandle = true;
        }
        if (!HudManager.INSTANCE.isEditMode() && shouldHandle && getHudElement().isEnabled() && getHudElement().mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldHandle = SharedVariables.enabled && client.player != null;
        if (isHudScreen()) {
            shouldHandle = true;
        }
        if (!HudManager.INSTANCE.isEditMode() && shouldHandle && getHudElement().isEnabled() && getHudElement().mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldHandle = SharedVariables.enabled && client.player != null;
        if (isHudScreen()) {
            shouldHandle = true;
        }
        if (!HudManager.INSTANCE.isEditMode() && shouldHandle && getHudElement().isEnabled() && getHudElement().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldHandle = SharedVariables.enabled && client.player != null;
        if (isHudScreen()) {
            shouldHandle = true;
        }
        if (!HudManager.INSTANCE.isEditMode() && shouldHandle && getHudElement().isEnabled() && getHudElement().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }
}