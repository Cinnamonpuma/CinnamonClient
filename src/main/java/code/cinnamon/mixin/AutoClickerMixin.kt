// AutoClickerMixin.kt
package code.cinnamon.mixin

import net.minecraft.client.Mouse
import net.minecraft.client.MinecraftClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(Mouse::class)
class AutoClickerMixin {
    
    @Shadow
    private var client: MinecraftClient? = null
    
    companion object {
        private var pendingLeftClick = false
        private var pendingRightClick = false
        
        @JvmStatic
        fun simulateLeftClick() {
            pendingLeftClick = true
        }
        
        @JvmStatic
        fun simulateRightClick() {
            pendingRightClick = true
        }
    }
    
    @Inject(method = ["onMouseButton"], at = [At("HEAD")])
    private fun onMouseButton(window: Long, button: Int, action: Int, mods: Int, info: CallbackInfo) {
        // This injection allows us to intercept mouse events
        // We can add custom logic here if needed
    }
    
    @Inject(method = ["tick"], at = [At("HEAD")])
    private fun onTick(info: CallbackInfo) {
        if (client == null) return
        
        // Process pending clicks during mouse tick
        if (pendingLeftClick) {
            pendingLeftClick = false
            performLeftClick()
        }
        
        if (pendingRightClick) {
            pendingRightClick = false
            performRightClick()
        }
    }
    
    private fun performLeftClick() {
        try {
            val client = this.client ?: return
            
            // Simulate the actual left click behavior
            if (client.currentScreen == null) {
                // Attack/break block
                if (client.crosshairTarget != null) {
                    when (client.crosshairTarget?.type) {
                        net.minecraft.util.hit.HitResult.Type.ENTITY -> {
                            // Attack entity
                            client.interactionManager?.attackEntity(client.player, (client.crosshairTarget as net.minecraft.util.hit.EntityHitResult).entity)
                            client.player?.swingHand(net.minecraft.util.Hand.MAIN_HAND)
                        }
                        net.minecraft.util.hit.HitResult.Type.BLOCK -> {
                            // Start breaking block
                            val blockHit = client.crosshairTarget as net.minecraft.util.hit.BlockHitResult
                            client.interactionManager?.attackBlock(blockHit.blockPos, blockHit.side)
                            client.player?.swingHand(net.minecraft.util.Hand.MAIN_HAND)
                        }
                        else -> {
                            // Swing in air
                            client.player?.swingHand(net.minecraft.util.Hand.MAIN_HAND)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("AutoClicker: Error performing left click: ${e.message}")
        }
    }
    
    private fun performRightClick() {
        try {
            val client = this.client ?: return
            
            // Simulate the actual right click behavior
            if (client.currentScreen == null) {
                // Use item/interact
                if (client.crosshairTarget != null) {
                    when (client.crosshairTarget?.type) {
                        net.minecraft.util.hit.HitResult.Type.BLOCK -> {
                            // Interact with block
                            val blockHit = client.crosshairTarget as net.minecraft.util.hit.BlockHitResult
                            val result = client.interactionManager?.interactBlock(
                                client.player, 
                                net.minecraft.util.Hand.MAIN_HAND, 
                                blockHit
                            )
                            
                            if (result?.isAccepted != true) {
                                // Use item if block interaction failed
                                client.interactionManager?.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND)
                            }
                        }
                        net.minecraft.util.hit.HitResult.Type.ENTITY -> {
                            // Interact with entity
                            val entityHit = client.crosshairTarget as net.minecraft.util.hit.EntityHitResult
                            client.interactionManager?.interactEntity(client.player, entityHit.entity, net.minecraft.util.Hand.MAIN_HAND)
                        }
                        else -> {
                            // Use item in air
                            client.interactionManager?.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("AutoClicker: Error performing right click: ${e.message}")
        }
    }
}