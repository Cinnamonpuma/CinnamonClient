package code.cinnamon.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import code.cinnamon.hud.HudManager;
import code.cinnamon.SharedVariables;

@Mixin(InventoryScreen.class)
public abstract class PacketHandlerInventoryScreenMixin {

    private static final float TARGET_SCALE_FACTOR = 2.0f;

    private static code.cinnamon.hud.elements.PacketHandlerHudElement getHudElement() {
        return HudManager.INSTANCE.getPacketHandlerHudElement();
    }

    private boolean isHudScreen() {
        Screen current = MinecraftClient.getInstance().currentScreen;
        return current != null && current.getClass().getName().equals("code.cinnamon.hud.HudScreen");
    }

    private float getScaleRatio() {
        MinecraftClient client = MinecraftClient.getInstance();
        float currentScale = (float) client.getWindow().getScaleFactor();
        return TARGET_SCALE_FACTOR / currentScale;
    }

    private double scaleMouseX(double mouseX) {
        return mouseX / getScaleRatio();
    }

    private double scaleMouseY(double mouseY) {
        return mouseY / getScaleRatio();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderScreen(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldRender = SharedVariables.enabled && client.player != null;

        if (isHudScreen()) {
            shouldRender = true;
        }

        if (shouldRender && getHudElement().isEnabled()) {
            double scaledMouseX = scaleMouseX(mouseX);
            double scaledMouseY = scaleMouseY(mouseY);

            getHudElement().setCurrentMousePosition(scaledMouseX, scaledMouseY);

            context.getMatrices().pushMatrix();

            float scaleRatio = getScaleRatio();
            context.getMatrices().scale(scaleRatio, scaleRatio, context.getMatrices());

            getHudElement().renderElement(context, delta);

            context.getMatrices().popMatrix();
        }
    }
}