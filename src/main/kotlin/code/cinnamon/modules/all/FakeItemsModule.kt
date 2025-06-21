package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.modules.ModuleManager

import net.minecraft.util.math.BlockPos
import net.minecraft.block.Block
import net.minecraft.item.ItemStack // Assuming this is the correct ItemStack

object FakeItemsModule : Module("FakeItems", "A fake module for testing purposes.") {

    // Stores item name/ID string and count
    private val fakeItems = mutableListOf<Pair<String, Int>>() 
    
    // Stores position, block type, and the item stack that placed it
    private val placedFakeBlocks = mutableListOf<Triple<BlockPos, Block, ItemStack>>()

    fun init() {
        // TODO: Add any specific initialization logic for this module here
        ModuleManager.registerModule(this)
    }

    override fun onEnable() {
        // TODO: Implement logic to be executed when the module is enabled
        println("$name enabled")
        // Optionally, clear fake items when module is re-enabled, or persist them
        // fakeItems.clear() 
    }

    override fun onDisable() {
        // TODO: Implement logic to be executed when the module is disabled
        println("$name disabled")
        // Optionally, clear fake items when module is disabled
        // fakeItems.clear()
    }

    fun handleFakeGiveCommand(itemName: String, count: Int) {
        // For simplicity, we're still storing item name strings.
        // In a more advanced system, we might convert to ItemStacks here or store Item types.
        fakeItems.add(Pair(itemName, count))
        
        System.out.println("[FakeItemsModule] Added to fake inventory via command:");
        System.out.println("  Item: " + itemName);
        System.out.println("  Count: " + count);
        System.out.println("  Current fake inventory size: " + fakeItems.size);
    }

    fun getFakeItems(): List<Pair<String, Int>> {
        return fakeItems.toList() 
    }

    /**
     * Consumes a fake item from the internal inventory.
     * @param itemToConsume The ItemStack the player is trying to use.
     * @return True if the item was found and consumed, false otherwise.
     */
    fun consumeFakeItem(itemStackToConsume: ItemStack): Boolean {
        if (itemStackToConsume.isEmpty) return false

        // This is a simplified check. We'd need to resolve itemStackToConsume.item to its ID string
        // to compare with what's stored in fakeItems.
        // For now, let's assume fakeItems stores ItemStacks or we have a way to compare.
        // This part needs to be robust. How do we link an ItemStack to the String IDs in fakeItems?
        // Option 1: Iterate fakeItems, convert string to Item, compare with itemStackToConsume.item
        // Option 2: Change fakeItems to store ItemStacks directly (major refactor, for later)

        // For now, this is a placeholder for the logic to find and decrement.
        // Let's find by item name string (ID) first.
        val itemIdToConsume = net.minecraft.registry.Registries.ITEM.getId(itemStackToConsume.item).toString()

        val iterator = fakeItems.iterator()
        while (iterator.hasNext()) {
            val fakeItemEntry = iterator.next()
            if (fakeItemEntry.first == itemIdToConsume) {
                if (fakeItemEntry.second > 1) {
                    // Decrement count
                    val newEntry = Pair(fakeItemEntry.first, fakeItemEntry.second - 1)
                    fakeItems.remove(fakeItemEntry) // Remove old
                    fakeItems.add(newEntry)       // Add updated
                    System.out.println("[FakeItemsModule] Decremented fake item: $itemIdToConsume. New count: ${newEntry.second}")
                } else {
                    // Remove item
                    iterator.remove()
                    System.out.println("[FakeItemsModule] Consumed and removed fake item: $itemIdToConsume")
                }
                return true // Item found and consumed
            }
        }
        System.out.println("[FakeItemsModule] Attempted to consume $itemIdToConsume, but not found in fake inventory.")
        return false // Item not found
    }
    
    fun addPlacedFakeBlock(pos: BlockPos, block: Block, itemUsed: ItemStack) {
        placedFakeBlocks.add(Triple(pos, block, itemUsed.copy())) // Store a copy of the item stack
        System.out.println("[FakeItemsModule] Added fake block: ${block} at $pos. Total: ${placedFakeBlocks.size}")
    }

    fun getPlacedFakeBlocks(): List<Triple<BlockPos, Block, ItemStack>> {
        return placedFakeBlocks.toList()
    }

    // TODO: Add method to remove placed fake block (e.g., when "broken")

    fun clearAllFakeData() {
        fakeItems.clear()
        placedFakeBlocks.clear()
        System.out.println("[FakeItemsModule] All fake items and placed fake blocks have been cleared.")
    }
}
