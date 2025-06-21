package code.cinnamon.modules.all

import code.cinnamon.modules.Module
import code.cinnamon.modules.ModuleManager

import net.minecraft.util.math.BlockPos
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.client.MinecraftClient

object FakeItemsModule : Module("FakeItems", "Fake items system for client-side items.") {

    private val fakeItems = mutableListOf<Pair<String, Int>>()
    private val placedFakeBlocks = mutableListOf<Triple<BlockPos, Block, ItemStack>>()

    fun init() {
        // Register this module with the ModuleManager
        ModuleManager.registerModule(this)
        println("[$name] Module initialized and registered")
    }

    override fun onEnable() {
        println("[$name] Module enabled")
    }

    override fun onDisable() {
        println("[$name] Module disabled")
        // Optionally clear items when disabled
        // clearAllFakeData()
    }

    fun handleFakeGiveCommand(itemName: String, count: Int) {
        println("[$name] Processing fake give command: $itemName x$count")
        
        // Validate the item exists before adding
        val itemStack = createItemStack(itemName, count)
        if (itemStack.isEmpty) {
            println("[$name] Invalid item: $itemName")
            // Send feedback to player
            val client = MinecraftClient.getInstance()
            client.player?.sendMessage(net.minecraft.text.Text.literal("§c[FakeItems] Invalid item: $itemName"), false)
            return
        }
        
        // Use the registry ID for consistency
        val itemId = Registries.ITEM.getId(itemStack.item).toString()
        
        // Check if we already have this item, if so, increase count
        val existingIndex = fakeItems.indexOfFirst { it.first == itemId }
        if (existingIndex != -1) {
            val existing = fakeItems[existingIndex]
            val newCount = (existing.second + count).coerceAtMost(64 * 64) // Reasonable limit
            fakeItems[existingIndex] = Pair(itemId, newCount)
            println("[$name] Updated existing item $itemId: ${existing.second} -> $newCount")
        } else {
            fakeItems.add(Pair(itemId, count))
            println("[$name] Added new fake item: $itemId x$count")
        }
        
        // Send success feedback to player
        val client = MinecraftClient.getInstance()
        val displayName = itemStack.item.name.string
        client.player?.sendMessage(net.minecraft.text.Text.literal("§a[FakeItems] Added $displayName x$count to fake inventory"), false)
        
        println("[$name] Total fake items: ${fakeItems.size}")
        printFakeInventory()
    }

    private fun createItemStack(itemNameOrId: String, count: Int): ItemStack {
        try {
            val id = if (itemNameOrId.contains(":")) {
                Identifier.tryParse(itemNameOrId)
            } else {
                Identifier.of("minecraft", itemNameOrId)
            } ?: return ItemStack.EMPTY

            val item = Registries.ITEM.get(id)
            
            // Check if item exists (not AIR unless explicitly requested)
            if (item == Items.AIR && !itemNameOrId.equals("air", ignoreCase = true) && 
                !itemNameOrId.equals("minecraft:air", ignoreCase = true)) {
                println("[$name] Unknown item: $itemNameOrId")
                return ItemStack.EMPTY
            }
            
            return ItemStack(item, count.coerceIn(1, 64))
        } catch (e: Exception) {
            println("[$name] Error creating ItemStack for $itemNameOrId: ${e.message}")
            return ItemStack.EMPTY
        }
    }

    fun getFakeItems(): List<Pair<String, Int>> {
        return fakeItems.toList()
    }

    fun consumeFakeItem(itemStackToConsume: ItemStack): Boolean {
        if (itemStackToConsume.isEmpty) return false

        val itemId = Registries.ITEM.getId(itemStackToConsume.item).toString()
        
        println("[$name] Attempting to consume: $itemId")
        println("[$name] Available fake items:")
        fakeItems.forEachIndexed { index, item ->
            println("  [$index] ${item.first} x${item.second}")
        }
        
        for (i in fakeItems.indices) {
            val fakeItem = fakeItems[i]
            if (fakeItem.first == itemId) {
                if (fakeItem.second > 1) {
                    // Decrease count
                    fakeItems[i] = Pair(fakeItem.first, fakeItem.second - 1)
                    println("[$name] Consumed 1x $itemId, remaining: ${fakeItem.second - 1}")
                } else {
                    // Remove item completely
                    fakeItems.removeAt(i)
                    println("[$name] Consumed last $itemId, removed from inventory")
                }
                return true
            }
        }
        
        println("[$name] Item $itemId not found in fake inventory")
        return false // Item not found in fake inventory
    }
    
    fun addPlacedFakeBlock(pos: BlockPos, block: Block, itemUsed: ItemStack) {
        placedFakeBlocks.add(Triple(pos, block, itemUsed.copy()))
        println("[$name] Placed fake block: $block at $pos")
        
        // Send feedback to player
        val client = MinecraftClient.getInstance()
        client.player?.sendMessage(net.minecraft.text.Text.literal("§e[FakeItems] Placed fake ${block.name.string} at ${pos.x}, ${pos.y}, ${pos.z}"), false)
    }

    fun getPlacedFakeBlocks(): List<Triple<BlockPos, Block, ItemStack>> {
        return placedFakeBlocks.toList()
    }

    fun removePlacedFakeBlock(pos: BlockPos): Boolean {
        val removed = placedFakeBlocks.removeIf { it.first == pos }
        if (removed) {
            println("[$name] Removed fake block at $pos")
        }
        return removed
    }

    fun clearAllFakeData() {
        val itemCount = fakeItems.size
        val blockCount = placedFakeBlocks.size
        
        fakeItems.clear()
        placedFakeBlocks.clear()
        
        println("[$name] Cleared $itemCount fake items and $blockCount fake blocks")
        
        // Send feedback to player if available
        try {
            val client = MinecraftClient.getInstance()
            if (client.player != null) {
                client.player?.sendMessage(net.minecraft.text.Text.literal("§6[FakeItems] Cleared all fake data"), false)
            }
        } catch (e: Exception) {
            // Player might not be available during disconnect
        }
    }
    
    private fun printFakeInventory() {
        println("[$name] Current fake inventory:")
        if (fakeItems.isEmpty()) {
            println("  (empty)")
        } else {
            fakeItems.forEachIndexed { index, item ->
                println("  [$index] ${item.first} x${item.second}")
            }
        }
    }
    
    // Debug function to manually add items for testing
    fun debugAddItem(itemName: String, count: Int = 1) {
        handleFakeGiveCommand(itemName, count)
    }
}