package code.cinnamon.mixin;

import code.cinnamon.modules.ModuleManager;
import code.cinnamon.modules.Module;
import code.cinnamon.modules.all.ChatPrefixModule;
import code.cinnamon.modules.all.FakeItemsModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @ModifyVariable(
        method = "sendChatMessage(Ljava/lang/String;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private String onSendChatMessagePrefix(String message) {
        try {
            // Skip prefix for fake commands when module is enabled
            if (message.startsWith("/give ") && FakeItemsModule.INSTANCE.isEnabled()) {
                return message;
            }
            
            Module module = ModuleManager.INSTANCE.getModule("ChatPrefix");
            if (module instanceof ChatPrefixModule) {
                ChatPrefixModule chatPrefixModule = (ChatPrefixModule) module;
                if (chatPrefixModule.isEnabled() && chatPrefixModule.getSelectedColorCode() != null) {
                    return chatPrefixModule.getSelectedColorCode() + message;
                }
            }
        } catch (Exception e) {
            System.err.println("Error in ChatPrefix: " + e.getMessage());
        }
        return message;
    }

    @Inject(
        method = "sendChatMessage(Ljava/lang/String;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendChatMessageCommand(String message, CallbackInfo ci) {
        // Only intercept /give if FakeItemsModule is enabled
        if (message.startsWith("/give ") && FakeItemsModule.INSTANCE.isEnabled()) {
            System.out.println("[FakeItemsModule] Intercepting /give command: " + message);
            
            String[] args = message.substring(6).split(" ", 2); // Split into max 2 parts
            
            if (args.length >= 1 && !args[0].trim().isEmpty()) {
                String itemNameOrId = args[0].trim();
                int count = 1;

                if (args.length >= 2) {
                    try {
                        count = Integer.parseInt(args[1].trim());
                        if (count < 1) count = 1;
                        if (count > 64) count = 64; // Reasonable limit
                    } catch (NumberFormatException e) {
                        System.err.println("[FakeItemsModule] Invalid count: " + args[1] + ". Using 1.");
                    }
                }

                // Call the handler
                System.out.println("[FakeItemsModule] Processing fake give: " + itemNameOrId + " x" + count);
                FakeItemsModule.INSTANCE.handleFakeGiveCommand(itemNameOrId, count);
                ci.cancel(); // Prevent sending to server
                return;
            } else {
                System.out.println("[FakeItemsModule] Invalid /give syntax, letting server handle it");
            }
        }
        // Let other messages through normally (including /give when module is disabled)
    }
}