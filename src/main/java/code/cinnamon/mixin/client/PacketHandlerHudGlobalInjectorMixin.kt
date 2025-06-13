package code.cinnamon.mixin.client

import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.DrawContext
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import code.cinnamon.hud.elements.PacketHandlerHudElement

@Mixin(Screen::class)
abstract class PacketHandlerHudGlobalInjectorMixin {

    private companion object {
        // Initialize with default coordinates (e.g., 10.0f, 10.0f).
        // These can be made configurable later.
        val packetHandlerHudElement = PacketHandlerHudElement(10.0f, 10.0f)
    }

    @Inject(method = ["render(Lnet/minecraft/client/gui/DrawContext;IIF)V"], at = [At("RETURN")])
    private fun onRenderScreen(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float, ci: CallbackInfo) {
        // The PacketHandlerHudElement.render method (from HudElement contract) takes (DrawContext, Float).
        // It is assumed that PacketHandlerHudElement.mouseMoved updates internal lastMouseX/Y fields,
        // and its render method uses these to pass to CinnamonButton.render.
        // In the previous step, PacketHandlerHudElement's render was updated to use client.mouse.x/y
        // which should be suitable.
        PacketHandlerHudGlobalInjectorMixin.packetHandlerHudElement.render(context, delta)
    }

    @Inject(method = ["mouseClicked"], at = [At("HEAD")], cancellable = true)
    private fun onMouseClicked(mouseX: Double, mouseY: Double, button: Int, cir: CallbackInfoReturnable<Boolean>) {
        if (PacketHandlerHudGlobalInjectorMixin.packetHandlerHudElement.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true)
        }
    }

    @Inject(method = ["mouseMoved"], at = [At("HEAD")])
    private fun onMouseMoved(mouseX: Double, mouseY: Double, ci: CallbackInfo) {
        PacketHandlerHudGlobalInjectorMixin.packetHandlerHudElement.mouseMoved(mouseX, mouseY)
    }

    @Inject(method = ["mouseReleased"], at = [At("HEAD")], cancellable = true)
    private fun onMouseReleased(mouseX: Double, mouseY: Double, button: Int, cir: CallbackInfoReturnable<Boolean>) {
        if (PacketHandlerHudGlobalInjectorMixin.packetHandlerHudElement.mouseReleased(mouseX, mouseY, button)) {
            cir.setReturnValue(true)
        }
    }

    @Inject(method = ["mouseScrolled"], at = [At("HEAD")], cancellable = true)
    private fun onMouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double, cir: CallbackInfoReturnable<Boolean>) {
        // Assumes PacketHandlerHudElement.mouseScrolled was updated in Step 1 to have a compatible signature
        // (e.g., fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean)
        // and delegates to its CinnamonButtons. This was done in the previous step.
        if (PacketHandlerHudGlobalInjectorMixin.packetHandlerHudElement.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true)
        }
    }
}
