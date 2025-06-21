package code.cinnamon.mixin.client;

import code.cinnamon.modules.all.FakeItemsModule;
import kotlin.Pair; // Assuming FakeItemsModule.INSTANCE.getFakeItems() returns List<kotlin.Pair<String, Integer>>
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.DrawContext; // Added import for DrawContext
import net.minecraft.client.render.item.ItemRenderer; // Added import
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemDisplayContext; // Added for new rendering method
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items; // Required for the AIR check
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor; // Added for @Accessor
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List; // For List<Pair<...>>

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    // Accessors for screen x and y
    @Accessor("x")
    protected abstract int getScreenX();

    @Accessor("y")
    protected abstract int getScreenY();

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    // Using the more refined version of createItemStack from the second duplicated block
    private ItemStack createItemStack(String itemNameOrId, int count) {
        try {
            Identifier id;
            if (itemNameOrId.contains(":")) {
                id = Identifier.tryParse(itemNameOrId);
            } else {
                id = Identifier.of("minecraft", itemNameOrId); // Explicitly use 'minecraft' namespace
            }

            if (id == null) { // Identifier.tryParse can return null
                System.err.println("[FakeItemsModule] Failed to parse identifier: " + itemNameOrId);
                return ItemStack.EMPTY;
            }

            Item item = Registries.ITEM.get(id); // Assumed path
            // More robust AIR check from the second version
            if (item == Items.AIR && !itemNameOrId.equalsIgnoreCase("air") && !itemNameOrId.equalsIgnoreCase("minecraft:air")) {
                System.err.println("[FakeItemsModule] Unknown item ID: " + itemNameOrId + " resolved to AIR. Original was: " + itemNameOrId);
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, count);
        } catch (Exception e) {
            System.err.println("[FakeItemsModule] Error creating ItemStack for " + itemNameOrId + ": " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderFakeItemsInSlots(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Ensure client and player are not null
        MinecraftClient currentClient = MinecraftClient.getInstance(); // Use getter for safety
        if (currentClient == null || currentClient.player == null) {
            return;
        }

        List<Pair<String, Integer>> fakeItemsToRender = FakeItemsModule.INSTANCE.getFakeItems();
        if (fakeItemsToRender.isEmpty()) {
            return;
        }

        HandledScreen<?> thisScreen = (HandledScreen<?>) (Object) this;
        ScreenHandler screenHandler = thisScreen.getScreenHandler();
        PlayerInventory playerInventory = currentClient.player.getInventory();

        int fakeItemIndex = 0;

        for (Slot slot : screenHandler.slots) {
            if (fakeItemIndex >= fakeItemsToRender.size()) {
                break; 
            }

            boolean isMainPlayerInventorySlot = slot.inventory == playerInventory &&
                                                slot.getIndex() >= 9 && slot.getIndex() <= 35;
            
            if (isMainPlayerInventorySlot && slot.getStack().isEmpty()) {
                Pair<String, Integer> fakeItemData = fakeItemsToRender.get(fakeItemIndex);
                // Use this.createItemStack to call the method within this class instance
                ItemStack itemStackToRender = this.createItemStack(fakeItemData.getFirst(), fakeItemData.getSecond());

                if (itemStackToRender != null && !itemStackToRender.isEmpty()) {
                    int renderX = getScreenX() + slot.x;
                    int renderY = getScreenY() + slot.y;

                    // Use DrawContext to render the item - much simpler!
                    context.drawItem(itemStackToRender, renderX, renderY);
                    
                    // Use DrawContext to render the item count
                    String countText = "";
                    if (itemStackToRender.getCount() > 1) {
                        countText = String.valueOf(itemStackToRender.getCount());
                    }
                                        
                    // Manual Item Count Rendering using DrawContext
                    if (countText != null && !countText.isEmpty()) {
                        context.drawText(currentClient.textRenderer, countText, renderX + 19 - 2 - currentClient.textRenderer.getWidth(countText), renderY + 6 + 3, 0xFFFFFF, true);
                    }
                    
                    fakeItemIndex++;
                }
            }
        }
    }
}