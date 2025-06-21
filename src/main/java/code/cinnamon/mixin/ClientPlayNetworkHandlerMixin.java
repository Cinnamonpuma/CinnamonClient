package code.cinnamon.mixin;

import code.cinnamon.modules.ModuleManager;
import code.cinnamon.modules.Module;
import code.cinnamon.modules.all.ChatPrefixModule;
import code.cinnamon.modules.all.FakeItemsModule;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.MinecraftClient;

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
        // Debug: Print all chat messages to see if mixin is working
        System.out.println("[FakeItemsModule] Chat message intercepted: " + message);
        System.out.println("[FakeItemsModule] Module enabled: " + FakeItemsModule.INSTANCE.isEnabled());
        
        // Check for any /give command (with or without space after)
        if ((message.startsWith("/give ") || message.equals("/give")) && FakeItemsModule.INSTANCE.isEnabled()) {
            System.out.println("[FakeItemsModule] Intercepting /give command: " + message);
            
            // Handle empty /give command
            if (message.equals("/give")) {
                System.out.println("[FakeItemsModule] Empty /give command, sending usage help");
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.literal("§c[FakeItems] Usage: /give <item> [count]"), false);
                }
                ci.cancel();
                return;
            }
            
            // Parse the command properly
            String[] parts = message.substring(6).trim().split("\\s+");
            
            if (parts.length >= 1 && !parts[0].isEmpty()) {
                String itemNameOrId = parts[0];
                int count = 1;

                // Parse count if provided
                if (parts.length >= 2) {
                    try {
                        count = Integer.parseInt(parts[1]);
                        if (count < 1) count = 1;
                        if (count > 64) count = 64; // Reasonable limit
                    } catch (NumberFormatException e) {
                        System.err.println("[FakeItemsModule] Invalid count: " + parts[1] + ". Using 1.");
                        // Send error message to player
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player != null) {
                            client.player.sendMessage(net.minecraft.text.Text.literal("§c[FakeItems] Invalid count: " + parts[1]), false);
                        }
                    }
                }

                // Call the handler
                System.out.println("[FakeItemsModule] Processing fake give: " + itemNameOrId + " x" + count);
                FakeItemsModule.INSTANCE.handleFakeGiveCommand(itemNameOrId, count);
                ci.cancel(); // Prevent sending to server
                return;
            } else {
                System.out.println("[FakeItemsModule] Invalid /give syntax, sending usage help");
                // Send usage help to player
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.sendMessage(net.minecraft.text.Text.literal("§c[FakeItems] Usage: /give <item> [count]"), false);
                }
                ci.cancel(); // Still cancel to prevent server error
                return;
            }
        }
        
        // Also intercept our debug command
        if (message.startsWith("/fakeitems ")) {
            System.out.println("[FakeItemsModule] Debug command intercepted: " + message);
            String[] args = message.substring(11).split(" ");
            
            if (args.length > 0) {
                switch (args[0]) {
                    case "enable":
                        FakeItemsModule.INSTANCE.setEnabled(true);
                        MinecraftClient.getInstance().player.sendMessage(
                            net.minecraft.text.Text.literal("§a[FakeItems] Module enabled"), false);
                        break;
                        
                    case "disable":
                        FakeItemsModule.INSTANCE.setEnabled(false);
                        MinecraftClient.getInstance().player.sendMessage(
                            net.minecraft.text.Text.literal("§c[FakeItems] Module disabled"), false);
                        break;
                        
                    case "clear":
                        FakeItemsModule.INSTANCE.clearAllFakeData();
                        break;
                        
                    case "test":
                        if (FakeItemsModule.INSTANCE.isEnabled()) {
                            FakeItemsModule.INSTANCE.debugAddItem("stone", 10);
                            FakeItemsModule.INSTANCE.debugAddItem("dirt", 5);
                            MinecraftClient.getInstance().player.sendMessage(
                                net.minecraft.text.Text.literal("§a[FakeItems] Added test items"), false);
                        } else {
                            MinecraftClient.getInstance().player.sendMessage(
                                net.minecraft.text.Text.literal("§c[FakeItems] Module is disabled"), false);
                        }
                        break;
                        
                    default:
                        MinecraftClient.getInstance().player.sendMessage(
                            net.minecraft.text.Text.literal("§e[FakeItems] Usage: /fakeitems <enable|disable|clear|test>"), false);
                }
            }
            ci.cancel();
            return;
        }
        
        // Let other messages through normally (including /give when module is disabled)
    }
}