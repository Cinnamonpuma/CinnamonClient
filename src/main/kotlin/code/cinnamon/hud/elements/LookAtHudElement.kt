package code.cinnamon.hud.elements

import code.cinnamon.gui.theme.CinnamonTheme
import code.cinnamon.hud.HudElement
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.BlockPos
import net.minecraft.world.RaycastContext
import kotlin.math.sqrt

class LookAtHudElement(x: Float, y: Float) : HudElement(x, y) {
    private val mc = MinecraftClient.getInstance()

    // Cache for performance
    private var cachedText: Text? = null
    private var cachedWidth: Int = 0
    private var cachedHeight: Int = 0
    private var lastUpdateTime: Long = 0

    // Configuration
    private val updateInterval = 50L // Update every 50ms for smoother performance
    private val decimalPlaces = 2
    private val maxRaycastDistance = 1000.0 // Maximum distance to raycast

    override fun renderElement(context: DrawContext, tickDelta: Float) {
        if (!isEnabled) return

        val currentTime = System.currentTimeMillis()
        val shouldUpdate = currentTime - lastUpdateTime > updateInterval

        if (shouldUpdate) {
            updateCachedData()
            lastUpdateTime = currentTime
        }

        val textToRender = cachedText ?: return

        context.matrices.pushMatrix()
        context.matrices.translate(getX(), getY(), context.matrices)
        context.matrices.scale(this.scale, this.scale, context.matrices)

        // Draw background if enabled
        if (backgroundColor != 0) {
            drawRoundedBackground(
                context,
                -2,
                -2,
                cachedWidth + 4,
                cachedHeight + 4,
                this.backgroundColor
            )
        }

        // Draw text with shadow if enabled
        if (this.textShadowEnabled) {
            context.drawText(mc.textRenderer, textToRender, 1, 1, 0x40000000, false)
        }
        context.drawText(mc.textRenderer, textToRender, 0, 0, this.textColor, false)

        context.matrices.popMatrix()
    }

    private fun updateCachedData() {
        val player = mc.player
        val world = mc.world

        if (player == null || world == null) {
            cachedText = null
            cachedWidth = 0
            cachedHeight = 0
            return
        }

        // Perform custom raycast with extended range
        val eyePos = player.getCameraPosVec(1.0f)
        val lookVec = player.getRotationVec(1.0f)
        val endPos = eyePos.add(lookVec.multiply(maxRaycastDistance))

        // Raycast for blocks
        val blockHitResult = world.raycast(RaycastContext(
            eyePos,
            endPos,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            player
        ))

        // Raycast for entities
        val entityHitResult = raycastEntities(eyePos, endPos, player)

        val text = when {
            entityHitResult != null && (blockHitResult.type == HitResult.Type.MISS ||
                    eyePos.squaredDistanceTo(entityHitResult.entity.pos) < eyePos.squaredDistanceTo(blockHitResult.pos)) -> {
                val distance = calculateEntityDistance(eyePos, entityHitResult.entity.pos)
                val entityName = entityHitResult.entity.displayName?.string ?: "Entity"
                Text.literal("$entityName: ${formatDistance(distance)}")
            }
            blockHitResult.type != HitResult.Type.MISS -> {
                val distance = calculateBlockDistance(eyePos, blockHitResult.blockPos)
                val blockName = getBlockName(blockHitResult.blockPos)
                Text.literal("$blockName: ${formatDistance(distance)}")
            }
            else -> null
        }

        if (text != null) {
            cachedText = text.setStyle(Style.EMPTY.withFont(CinnamonTheme.getCurrentFont()))
            cachedWidth = mc.textRenderer.getWidth(cachedText)
            cachedHeight = mc.textRenderer.fontHeight
        } else {
            cachedText = null
            cachedWidth = 0
            cachedHeight = 0
        }
    }

    private fun raycastEntities(start: Vec3d, end: Vec3d, player: net.minecraft.entity.player.PlayerEntity): EntityHitResult? {
        val world = mc.world ?: return null
        var closestEntity: net.minecraft.entity.Entity? = null
        var closestDistance = Double.MAX_VALUE

        // Get entities in the path
        val box = net.minecraft.util.math.Box(start, end)
        val entities = world.getOtherEntities(player, box)

        for (entity in entities) {
            val entityBox = entity.boundingBox.expand(0.3) // Small expansion for easier targeting
            val hitResult = entityBox.raycast(start, end)

            if (hitResult.isPresent) {
                val distance = start.squaredDistanceTo(hitResult.get())
                if (distance < closestDistance) {
                    closestDistance = distance
                    closestEntity = entity
                }
            }
        }

        return if (closestEntity != null) {
            EntityHitResult(closestEntity)
        } else {
            null
        }
    }

    private fun calculateBlockDistance(playerPos: Vec3d, blockPos: BlockPos): Double {
        return sqrt(playerPos.squaredDistanceTo(Vec3d.ofCenter(blockPos)))
    }

    private fun calculateEntityDistance(playerPos: Vec3d, entityPos: Vec3d): Double {
        return sqrt(playerPos.squaredDistanceTo(entityPos))
    }

    private fun getBlockName(blockPos: BlockPos): String {
        val world = mc.world ?: return "Unknown"
        val blockState = world.getBlockState(blockPos)
        return blockState.block.translationKey.let { key ->
            // Get the display name or fall back to a simplified version
            val displayName = Text.translatable(key).string
            if (displayName.contains("block.minecraft.")) {
                displayName.substringAfter("block.minecraft.")
                    .replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { it.capitalize() }
            } else {
                displayName
            }
        }
    }

    private fun formatDistance(distance: Double): String {
        return when {
            distance < 1000.0 -> String.format("%.${decimalPlaces}f blocks", distance)
            else -> String.format("%.${decimalPlaces}fk blocks", distance / 1000.0)
        }
    }

    private fun drawRoundedBackground(context: DrawContext, x: Int, y: Int, width: Int, height: Int, backgroundColor: Int) {
        drawRoundedRect(context, x, y, width, height, 6, backgroundColor)
    }

    private fun drawRoundedRect(context: DrawContext, x: Int, y: Int, width: Int, height: Int, radius: Int, color: Int) {
        if (color == 0) return

        val r = minOf(radius, minOf(width / 2, height / 2))
        if (r <= 0) {
            context.fill(x, y, x + width, y + height, color)
            return
        }

        // Draw main rectangles
        context.fill(x + r, y, x + width - r, y + height, color) // Center
        context.fill(x, y + r, x + r, y + height - r, color) // Left
        context.fill(x + width - r, y + r, x + width, y + height - r, color) // Right

        // Draw rounded corners
        drawRoundedCorner(context, x, y, r, color, Corner.TOP_LEFT)
        drawRoundedCorner(context, x + width - r, y, r, color, Corner.TOP_RIGHT)
        drawRoundedCorner(context, x, y + height - r, r, color, Corner.BOTTOM_LEFT)
        drawRoundedCorner(context, x + width - r, y + height - r, r, color, Corner.BOTTOM_RIGHT)
    }

    private enum class Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private fun drawRoundedCorner(context: DrawContext, x: Int, y: Int, radius: Int, color: Int, corner: Corner) {
        val radiusSquared = radius * radius

        for (dy in 0 until radius) {
            for (dx in 0 until radius) {
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared <= radiusSquared) {
                    val pixelX: Int
                    val pixelY: Int
                    when (corner) {
                        Corner.TOP_LEFT -> {
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + (radius - 1 - dy)
                        }
                        Corner.TOP_RIGHT -> {
                            pixelX = x + dx
                            pixelY = y + (radius - 1 - dy)
                        }
                        Corner.BOTTOM_LEFT -> {
                            pixelX = x + (radius - 1 - dx)
                            pixelY = y + dy
                        }
                        Corner.BOTTOM_RIGHT -> {
                            pixelX = x + dx
                            pixelY = y + dy
                        }
                    }
                    context.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color)
                }
            }
        }
    }

    override fun getWidth(): Int = cachedWidth

    override fun getHeight(): Int = cachedHeight

    override fun getName(): String = "Look At"
}