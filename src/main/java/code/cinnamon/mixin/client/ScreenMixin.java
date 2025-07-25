package code.cinnamon.mixin.client;



import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenMixin {


    @Inject(method = "<init>(Lnet/minecraft/text/Text;)V", at = @At("RETURN"))
    private void cinnamon$onScreenConstructed(Text title, CallbackInfo ci) {
    }
}