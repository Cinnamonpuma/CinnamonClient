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

    @Inject(
        method = "render(Lnet/minecraft/client/gui/DrawContext;IIZ)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cinnamon$cancelPanoramaRender(
        DrawContext context,
        int width,
        int height,
        boolean rotate,
        CallbackInfo ci
    ) {
        if (MinecraftClient.getInstance().currentScreen instanceof TitleScreen) {
            ci.cancel();
        }
    }
}