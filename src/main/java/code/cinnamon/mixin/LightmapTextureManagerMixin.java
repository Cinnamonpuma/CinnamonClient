package code.cinnamon.mixin;

import code.cinnamon.modules.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {

    @Shadow @Final private GpuTexture glTexture;

    @Inject(method = "update(F)V",
            at = @At("HEAD"),
            cancellable = true)
    private void cinnamon$onUpdateHEAD(float tickProgress, CallbackInfo ci) {
        if (ModuleManager.INSTANCE.isModuleEnabled("Fullbright")) {
            if (!RenderSystem.isOnRenderThread()) {
                MinecraftClient.getInstance().execute(() -> {
                    if (this.glTexture != null) {
                        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.glTexture, 0xFFFFFFFF);
                    }
                });
            } else {
                if (this.glTexture != null) {
                    RenderSystem.getDevice().createCommandEncoder().clearColorTexture(this.glTexture, 0xFFFFFFFF);
                }
            }
            ci.cancel();
        }
    }
}
