package code.cinnamon.mixin.client;

import code.cinnamon.client.gui.CustomTitleScreenRenderer; // Our Kotlin renderer
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    // Required constructor for Screen subclass
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    private static final CustomTitleScreenRenderer customRenderer = new CustomTitleScreenRenderer();

    @Inject(method = "render", at = @At("HEAD"))
    private void cinnamon$renderCustomBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Use context's methods for screen dimensions
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        customRenderer.render(context, width, height);
        // Do not cancel, let the rest of the title screen render on top (logo, buttons, etc.)
    }
}
