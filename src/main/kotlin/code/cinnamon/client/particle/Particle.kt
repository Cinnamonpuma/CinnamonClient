package code.cinnamon.client.particle

import kotlin.random.Random

class Particle(initialWidth: Int, initialHeight: Int, private val random: Random) {
    var x: Int = 0
    var y: Int = 0
    var speedX: Int = 0
    var speedY: Int = 0

    init {
        reset(initialWidth, initialHeight)
    }

    fun update(width: Int, height: Int) {
        x += speedX
        y += speedY

        if (x < 0 || x > width || y < 0 || y > height) {
            reset(width, height)
        }
    }

    fun reset(width: Int, height: Int) {
        // Ensure width and height are positive for Random.nextInt
        val safeWidth = if (width > 0) width else 1
        val safeHeight = if (height > 0) height else 1

        x = random.nextInt(safeWidth)
        y = random.nextInt(safeHeight)

        do {
            speedX = random.nextInt(-1, 2) // Velocity between -1 and 1 inclusive
            speedY = random.nextInt(-1, 2)
        } while (speedX == 0 && speedY == 0) // Ensure particles are not static
    }
}
