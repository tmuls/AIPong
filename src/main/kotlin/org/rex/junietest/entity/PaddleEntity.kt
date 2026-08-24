package org.rex.junietest.entity

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import org.rex.junietest.core.Bounds
import org.rex.junietest.input.GameInput
import java.awt.event.KeyEvent

open class PaddleEntity(
    x: Float,
    y: Float,
    private val color: Color,
    protected val bounds: Bounds,
    var paddleSpeed: Float = 600f,
    private val upKey: Int,
    private val downKey: Int,
    private val gameInput: GameInput?
) : Entity(x, y, 10, 80) {

    init {
        // 10% taller than the paddle (5% above and 5% below)
        boundingBox = Rectangle(
            0, // x is relative to paddle position
            -(height * 0.05).toInt(), // y is relative to paddle position, extend upward by 5%
            width,
            (height * 1.1).toInt()
        )
    }

    override fun render(g: Graphics2D) {
        g.color = color
        g.fillRoundRect(x.toInt(), y.toInt(), width, height, 10, 10)
    }

    override fun update(deltaTime: Float) {
        val movement = when {
            gameInput?.isKeyPressed(upKey) == true -> -paddleSpeed * deltaTime
            gameInput?.isKeyPressed(downKey) == true -> paddleSpeed * deltaTime
            else -> 0f
        }

        y += movement
        y = y.coerceIn(0f, bounds.height - height)
    }
}
