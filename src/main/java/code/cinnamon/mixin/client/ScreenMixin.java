package code.cinnamon.mixin.client;

import code.cinnamon.client.gui.NewGlobalShaderBackgroundRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.text.Text;
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
        Screen screen = (Screen)(Object)this;
        String screenClassName = screen.getClass().getName();

        if (screenClassName.startsWith("code.cinnamon.")) {
        } else {
            ci.cancel();
            NewGlobalShaderBackgroundRenderer.INSTANCE.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/text/Text;)V", at = @At("RETURN"))
    private void cinnamon$onScreenConstructed(Text title, CallbackInfo ci) {
    }
}