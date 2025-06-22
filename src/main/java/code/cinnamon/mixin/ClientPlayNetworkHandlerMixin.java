package code.cinnamon.mixin;

import code.cinnamon.modules.ModuleManager;
import code.cinnamon.modules.Module;
import code.cinnamon.modules.all.ChatPrefixModule;
import net.minecraft.client.network.ClientPlayNetworkHandler; 


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class) 
public abstract class ClientPlayNetworkHandlerMixin { 

    @ModifyVariable(
        method = "sendChatMessage(Ljava/lang/String;)V", 
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0 
    )
    private String onSendChatMessage(String message) { 
        try {
            Module module = ModuleManager.INSTANCE.getModule("ChatPrefix");

            if (module instanceof ChatPrefixModule) {
                ChatPrefixModule chatPrefixModule = (ChatPrefixModule) module;
                
                if (chatPrefixModule.isEnabled() && chatPrefixModule.getSelectedColorCode() != null) { 
                    return chatPrefixModule.getSelectedColorCode() + message;
                }
            }
        } catch (Exception e) {
        }
        return message;
    }
}