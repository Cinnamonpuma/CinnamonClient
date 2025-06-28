package code.cinnamon.mixin.client;

// import code.cinnamon.client.gui.NewGlobalShaderBackgroundRenderer; // No longer needed here

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {

    /*
     * Removed custom background rendering from all screens.
     * The custom background is now only applied via TitleScreenMixin.
    @Inject(
        method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;IIF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cinnamon$renderCustomBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen screen = (Screen)(Object)this;
        String screenClassName = screen.getClass().getName();

        if (screenClassName.startsWith("code.cinnamon.")) {
            // For Cinnamon screens, do nothing, let them handle their own background or use vanilla.
        } else {
            // For non-Cinnamon screens, we no longer render a global background.
            // Vanilla screens will render their default background.
            // Other mod screens will also render their default background.
            // ci.cancel(); // We don't cancel, to allow default behavior.
            // NewGlobalShaderBackgroundRenderer.INSTANCE.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight()); // Removed
        }
    }
    */

    @Inject(method = "<init>(Lnet/minecraft/text/Text;)V", at = @At("RETURN"))
    private void cinnamon$onScreenConstructed(Text title, CallbackInfo ci) {
        // This injection seems unrelated to background, leaving it as is.
    }
}