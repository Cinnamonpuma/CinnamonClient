package code.cinnamon.mixin.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingCubeMapRenderer.class)
public class PanoramaRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIFF)V", at = @At("HEAD"), cancellable = true)
    private void cinnamon$cancelPanoramaRender(DrawContext context, int width, int height, float alpha, float tickDelta, CallbackInfo ci) {
        ci.cancel(); // Prevent the panorama from drawing
    }
}