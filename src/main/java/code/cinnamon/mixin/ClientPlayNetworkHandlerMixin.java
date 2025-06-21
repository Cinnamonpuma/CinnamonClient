package code.cinnamon.mixin;

import code.cinnamon.modules.ModuleManager;
import code.cinnamon.modules.Module;
import code.cinnamon.modules.all.ChatPrefixModule;
import net.minecraft.client.network.ClientPlayNetworkHandler; 


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.ModifyVariable; // Keep for ChatPrefix or change its logic

// TODO: Consider moving ChatPrefix logic to an @Inject if it also needs to conditionally cancel or if complex logic is added.
// For now, we will assume ChatPrefix's @ModifyVariable runs *before* our new @Inject for command handling.
// This order isn't strictly guaranteed without explicit priority, but typically @ModifyVariable runs early.
// A more robust solution might involve a single @Inject that handles both commands and prefixes.

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    // This @ModifyVariable is for the ChatPrefixModule.
    // It will run and potentially modify the message before our @Inject for commands.
    @ModifyVariable(
        method = "sendChatMessage(Ljava/lang/String;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private String onSendChatMessagePrefix(String message) {
        try {
            Module module = ModuleManager.INSTANCE.getModule("ChatPrefix");
            if (module instanceof ChatPrefixModule) {
                ChatPrefixModule chatPrefixModule = (ChatPrefixModule) module;
                if (chatPrefixModule.isEnabled() && chatPrefixModule.getSelectedColorCode() != null) {
                    // Apply prefix if the message is not a command handled by FakeItemsModule
                    // This check is imperfect because the command parsing happens in the @Inject below.
                    // A more integrated solution would be better.
                    if (!message.startsWith("/give ")) { // Updated check to match new /give structure
                        return chatPrefixModule.getSelectedColorCode() + message;
                    }
                }
            }
        } catch (Exception e) {
            // Log error or handle
        }
        return message;
    }

    @Inject(
        method = "sendChatMessage(Ljava/lang/String;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSendChatMessageCommand(String message, CallbackInfo ci) {
        // Check for "/give " (with a space) to distinguish from other commands like "/giverole"
        if (message.startsWith("/give ")) {
            String[] args = message.substring("/give ".length()).split(" ");
            // Expected: /give <itemNameOrId> [count]

            if (args.length >= 1 && !args[0].isEmpty()) { // Ensure itemNameOrId is not empty
                String itemNameOrId = args[0];
                int count = 1; // Default count is 1

                if (args.length >= 2) {
                    try {
                        count = Integer.parseInt(args[1]);
                        if (count < 1) { // Ensure count is positive
                            count = 1;
                        }
                    } catch (NumberFormatException e) {
                        // Second argument is not a valid number, count remains 1.
                        // Or, you could choose to show an error to the user.
                        // For now, we'll just use the default count.
                        System.err.println("[FakeItemsModule] Invalid count for /give command: " + args[1] + ". Defaulting to 1.");
                    }
                }

                // Call the handler in FakeItemsModule for all /give commands
                code.cinnamon.modules.all.FakeItemsModule.INSTANCE.handleFakeGiveCommand(itemNameOrId, count);
                
                // Prevent the command from being sent to the server
                ci.cancel(); 
            } else {
                // Message was just "/give " or "/give  " (empty item name)
                // Let it pass through to the server to handle the error, or handle client-side.
                // For now, letting it pass. If you want to show a client-side error:
                // System.out.println("[FakeItemsModule] Usage: /give <itemNameOrId> [count]");
                // ci.cancel(); // if you want to prevent server seeing malformed command
            }
        }
        // If not "/give ", the message proceeds to be sent (potentially prefixed by ChatPrefixModule)
    }
}
