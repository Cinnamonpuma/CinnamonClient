package code.cinnamon.mixin.client;

import code.cinnamon.modules.all.FakeItemsModule;
import kotlin.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

    @Accessor("x")
    protected abstract int getScreenX();

    @Accessor("y")
    protected abstract int getScreenY();

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    private ItemStack createItemStack(String itemNameOrId, int count) {
        try {
            Identifier id;
            if (itemNameOrId.contains(":")) {
                id = Identifier.tryParse(itemNameOrId);
            } else {
                id = Identifier.of("minecraft", itemNameOrId);
            }

            if (id == null) {
                return ItemStack.EMPTY;
            }

            Item item = Registries.ITEM.get(id);
            
            // Validate item exists
            if (item == Items.AIR && !itemNameOrId.equalsIgnoreCase("air") && 
                !itemNameOrId.equalsIgnoreCase("minecraft:air")) {
                return ItemStack.EMPTY;
            }
            
            return new ItemStack(item, Math.max(1, Math.min(count, 64)));
            
        } catch (Exception e) {
            System.err.println("[FakeItemsModule] Error creating ItemStack: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderFakeItemsInSlots(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Only render if FakeItemsModule is enabled
        if (!FakeItemsModule.INSTANCE.isEnabled()) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        List<Pair<String, Integer>> fakeItems = FakeItemsModule.INSTANCE.getFakeItems();
        if (fakeItems.isEmpty()) {
            return;
        }

        HandledScreen<?> thisScreen = (HandledScreen<?>) (Object) this;
        ScreenHandler screenHandler = thisScreen.getScreenHandler();
        PlayerInventory playerInventory = client.player.getInventory();

        int fakeItemIndex = 0;

        // Debug output
        System.out.println("[FakeItemsModule] Rendering " + fakeItems.size() + " fake items");

        // Only render in main inventory slots (9-35 are the main inventory, 0-8 are hotbar)
        for (Slot slot : screenHandler.slots) {
            if (fakeItemIndex >= fakeItems.size()) {
                break;
            }

            // Check if this is a main inventory slot that's empty
            boolean isMainInventorySlot = slot.inventory == playerInventory && 
                                        slot.getIndex() >= 9 && slot.getIndex() <= 35;
            
            if (isMainInventorySlot && slot.getStack().isEmpty()) {
                Pair<String, Integer> fakeItemData = fakeItems.get(fakeItemIndex);
                ItemStack itemToRender = this.createItemStack(fakeItemData.getFirst(), fakeItemData.getSecond());

                if (!itemToRender.isEmpty()) {
                    int renderX = getScreenX() + slot.x;
                    int renderY = getScreenY() + slot.y;

                    System.out.println("[FakeItemsModule] Rendering fake item " + fakeItemData.getFirst() + 
                                     " at slot " + slot.getIndex() + " (screen pos: " + renderX + ", " + renderY + ")");

                    // Render the item with a slightly transparent overlay to indicate it's fake
                    context.drawItem(itemToRender, renderX, renderY);
                    
                    // Add a subtle overlay to indicate it's fake (optional)
                    context.fill(renderX, renderY, renderX + 16, renderY + 16, 0x22FF0000); // Semi-transparent red tint
                    
                    // Render count if > 1
                    if (itemToRender.getCount() > 1) {
                        String countText = String.valueOf(itemToRender.getCount());
                        context.drawText(client.textRenderer, countText, 
                                       renderX + 19 - 2 - client.textRenderer.getWidth(countText), 
                                       renderY + 6 + 3, 0xFFFFFF, true);
                    }
                    
                    fakeItemIndex++;
                }
            }
        }
        
        System.out.println("[FakeItemsModule] Rendered " + fakeItemIndex + " fake items in inventory");
    }
}