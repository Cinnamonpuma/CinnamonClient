package code.cinnamon.mixin.client;

// Updated import to use the new shader renderer
import code.cinnamon.client.gui.NewGlobalShaderBackgroundRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
// Removed specific screen imports

import net.minecraft.text.Text; // For constructor injection
import org.spongepowered.asm.mixin.Mixin;
// Removed java.util.List and java.util.Arrays as they are no longer needed
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

        System.out.println("[Cinnamon/ScreenMixin] Checking screen: " + screenClassName);

        if (screenClassName.startsWith("code.cinnamon.")) {
            System.out.println("[Cinnamon/ScreenMixin] Excluding screen (code.cinnamon): " + screenClassName);
            // Do nothing, let original background render for mod's own screens
        } else {
            System.out.println("[Cinnamon/ScreenMixin] Rendering particles for screen: " + screenClassName);
            ci.cancel();
            NewGlobalShaderBackgroundRenderer.INSTANCE.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/text/Text;)V", at = @At("RETURN"))
    private void cinnamon$onScreenConstructed(Text title, CallbackInfo ci) {
        System.out.println("[Cinnamon/ScreenMixin] Screen constructor called! Title: " + title.getString() + 
                          " | Class: " + ((Screen)(Object)this).getClass().getName());
    }
}