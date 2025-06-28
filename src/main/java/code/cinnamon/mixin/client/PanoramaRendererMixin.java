package code.cinnamon.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingCubeMapRenderer.class)
public class PanoramaRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIFF)V", at = @At("HEAD"), cancellable = true)
    private void cinnamon$conditionallyCancelPanoramaRender(DrawContext context, int width, int height, float alpha, float tickDelta, CallbackInfo ci) {
        // Only cancel panorama if the current screen is TitleScreen
        if (MinecraftClient.getInstance().currentScreen instanceof TitleScreen) {
            ci.cancel();
        }
    }
}