package code.cinnamon.hud.elements

import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.entity.EquipmentSlot

class ArmorHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()
    private val cornerRadius = 2
    private val padding = 1

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled || mc.player == null) return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        val armorSlotsInDisplayOrder = listOf(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        )

        var currentRelativeY = 0

        for (slot in armorSlotsInDisplayOrder) {
            val itemStack = mc.player!!.getEquippedStack(slot)
            if (itemStack.isEmpty) continue

            val itemRenderHeight = 16
            val textFontHeight = mc.textRenderer.fontHeight

            val singleItemRowHeight = itemRenderHeight

            drawRoundedBackground(
                context,
                0,
                currentRelativeY,
                getWidth(),
                singleItemRowHeight,
                this.backgroundColor
            )

            context.drawItem(itemStack, padding, currentRelativeY + (singleItemRowHeight - 16) / 2)

            val durability = itemStack.maxDamage - itemStack.damage
            val maxDurability = itemStack.maxDamage
            val durabilityText = if (maxDurability > 0) {
                Text.literal("$durability/$maxDurability").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            } else {
                Text.literal("").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            }

            val textX = padding + 16 + padding

            val textDrawY = currentRelativeY + (singleItemRowHeight - textFontHeight) / 2

            if (this.textShadowEnabled) {
                context.drawText(mc.textRenderer, durabilityText, textX + 1, textDrawY + 1, 0x40000000, false)
            }
            context.drawText(mc.textRenderer, durabilityText, textX, textDrawY, this.textColor, false)

            currentRelativeY += singleItemRowHeight + padding
        }
        context.matrices.popMatrix()
    }

    private fun drawRoundedBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, backgroundColor: Int) {
        drawRoundedRect(context, x, y, width, height, cornerRadius, backgroundColor)
    }

    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return

        val r = java.lang.Math.min(radius, java.lang.Math.min(width / 2, height / 2))

        if (r <= 0) {
            context.fill(x, y, x + width, y + height, color)
            return
        }

        context.fill(x + r, y, x + width - r, y + height, color)
        context.fill(x, y + r, x + r, y + height - r, color)
        context.fill(x + width - r, y + r, x + width, y + height - r, color)

        drawRoundedCorner(context, x, y, r, color, 0)
        drawRoundedCorner(context, x + width - r, y, r, color, 1)
        drawRoundedCorner(context, x, y + height - r, r, color, 2)
        drawRoundedCorner(context, x + width - r, y + height - r, r, color, 3)
    }

    private fun drawRoundedCorner(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, corner: Int) {
        for (dy in 0 until radius) {
            for (dx in 0 until radius) {
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared <= radius * radius) {
                    val pixelX: Int
                    val pixelY: Int

                    when (corner) {
                        0 -> {
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + (radius - 1 - dy)
                        }
                        1 -> {
                            pixelX = x + dx
                            pixelY = y + (radius - 1 - dy)
                        }
                        2 -> {
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + dy
                        }
                        3 -> {
                            pixelX = x + dx
                            pixelY = y + dy
                        }
                        else -> continue
                    }
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }

    override fun getWidth(): Int {
        val iconWidth = 16
        val exampleDurabilityText = Text.literal("000/000").setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
        val textWidth = mc.textRenderer.getWidth(exampleDurabilityText)
        val contentWidth = iconWidth + padding + textWidth
        return contentWidth + padding * 2
    }

    override fun getHeight(): Int {
        if (mc.player == null) return 0

        val armorSlots = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
        val equippedArmorItems = armorSlots.mapNotNull { slot ->
            val stack = mc.player!!.getEquippedStack(slot)
            if (stack.isEmpty) null else stack
        }

        if (equippedArmorItems.isEmpty()) return 0

        val itemHeight = 16
        val textHeight = mc.textRenderer.fontHeight

        val singleElementHeight = itemHeight + textHeight + padding / 2

        val contentStackHeight = (itemHeight * equippedArmorItems.size) + (padding * (equippedArmorItems.size - 1).coerceAtLeast(0))
        return contentStackHeight + padding * 2
    }

    override fun getName(): String = "Armor"
}
