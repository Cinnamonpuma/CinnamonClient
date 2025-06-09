package code.cinnamon.mixin.client;

import code.cinnamon.client.gui.GlobalBackgroundRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(
        method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cinnamon$renderCustomBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Cancel the original background rendering
        ci.cancel();
        
        // Render our custom wave background
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        GlobalBackgroundRenderer.INSTANCE.render(context, width, height);
    }
}