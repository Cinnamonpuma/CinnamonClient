package code.cinnamon.mixin.client;

// Updated import to use the new shader renderer
import code.cinnamon.client.gui.NewGlobalShaderBackgroundRenderer; 

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
        
        // Render our NEW custom shader background
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        // Updated call to the new renderer
        NewGlobalShaderBackgroundRenderer.INSTANCE.render(context, width, height);
    }
}