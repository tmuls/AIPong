package org.rex.junietest.entity

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import org.rex.junietest.core.Bounds
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.math.sqrt

/**
 * Ball entity that extends the base Entity class.
 */
class BallEntity(
    x: Float,
    y: Float,
    val radius: Int,
    val color: Color,
    private val bounds: Bounds,
    var ballSpeed: Float = DEFAULT_VELOCITY,
    val scorePoint: ((Side) -> Unit)? = null
) : Entity(x, y, radius * 2, radius * 2) {
    // Normalized velocity vector (values between 0 and 1)
    var velocityX: Float = 0f
    var velocityY: Float = 0f

    // Custom bounding box that is smaller on X axis and larger on Y axis
    init {
        boundingBox = Rectangle(
            (width * 0.2).toInt(), // x is relative to ball position
            -(height * 0.2).toInt(), // y is relative to ball position
            width - ((width * 0.4).toInt()), // 20% smaller width
            height + ((height * 0.4).toInt()) // 20% larger height
        )
    }

    init {
        // Set initial velocity
        setRandomVelocity()
    }

    companion object {
        const val MAX_VELOCITY = 1000f
        const val DEFAULT_VELOCITY = 400f
        const val VELOCITY_INCREASE = 15f
    }

    override fun render(g: Graphics2D) {
        // Draw ball
        g.color = color
        g.fillOval(x.toInt(), y.toInt(), width, height)
    }

    /**
     * Update the ball's position based on its velocity
     */
    override fun update(deltaTime: Float) {
        // Update position based on normalized velocity and ball speed
        x += velocityX * ballSpeed * deltaTime
        y += velocityY * ballSpeed * deltaTime

        var bounced = false

        // Check if ball hits left or right sides
        if (x < 0f) {
            // Call scorePoint lambda for right side scoring
            scorePoint?.invoke(Side.RIGHT)
            // Reset ball position to center
            centerInBounds()
            // Set a new random velocity
            setRandomVelocity()
        } else if (x > bounds.width - width) {
            // Call scorePoint lambda for left side scoring
            scorePoint?.invoke(Side.LEFT)
            // Reset ball position to center
            centerInBounds()
            // Set a new random velocity
            setRandomVelocity()
        }

        if (y < 0f) {
            y = 0f
            velocityY = -velocityY // Bounce off top wall
            bounced = true
        } else if (y > bounds.height - height) {
            y = bounds.height - height
            velocityY = -velocityY // Bounce off bottom wall
            bounced = true
        }

        // Increase ball speed if the ball bounced
        if (bounced) {
            increaseVelocity()
        }
    }

    /**
     * Handle collision with a paddle
     * @param paddle The paddle that the ball collided with
     */
    fun handlePaddleCollision(paddle: PaddleEntity) {
        // Calculate the distance from the center of the paddle (-1 to 1)
        val distanceFromCenter = ((y + height / 2) - (paddle.y + paddle.height / 2)) / (paddle.height / 2)
        
        // Calculate bounce angle based on distance from center
        // -1 = top of paddle, 0 = center, 1 = bottom
        val bounceAngle = distanceFromCenter * 0.8f // Scale down to 80% to prevent too steep angles
        
        // Reverse X direction and set Y velocity based on bounce angle
        velocityX = -velocityX
        velocityY = bounceAngle
        
        // Ensure minimum Y velocity of 0.1
        if (velocityY.absoluteValue < 0.1f) {
            velocityY = if (velocityY >= 0) 0.1f else -0.1f
        }
        
        // Normalize the velocity vector
        val magnitude = kotlin.math.sqrt(velocityX * velocityX + velocityY * velocityY)
        velocityX /= magnitude
        velocityY /= magnitude
        
        // Increase ball speed
        ballSpeed = (ballSpeed + VELOCITY_INCREASE).coerceAtMost(MAX_VELOCITY)

        // Push the ball fully outside the paddle's bounding box so it can't
        // still be overlapping (and re-trigger this collision) next frame.
        val paddleLeft = paddle.x + paddle.boundingBox.x
        val paddleRight = paddleLeft + paddle.boundingBox.width
        x = if (velocityX > 0) {
            paddleRight - boundingBox.x
        } else {
            paddleLeft - boundingBox.x - boundingBox.width
        }
    }

    /**
     * Increases the ball's speed by VELOCITY_INCREASE amount
     * and ensures it doesn't exceed MAX_VELOCITY
     */
    private fun increaseVelocity() {
        // Calculate new ball speed
        val newBallSpeed = (ballSpeed + VELOCITY_INCREASE).coerceAtMost(MAX_VELOCITY)

        println("ballSpeed: $ballSpeed newBallSpeed: $newBallSpeed")

        if (newBallSpeed != ballSpeed) {
            // Calculate current direction
            val currentDirection = Math.atan2(velocityY.toDouble(), velocityX.toDouble())

            // Update ball speed
            ballSpeed = newBallSpeed
        }
    }

    /**
     * Centers the ball within its bounds and resets its speed
     */
    fun centerInBounds() {
        x = bounds.width / 2 - radius
        y = bounds.height / 2 - radius
        ballSpeed = DEFAULT_VELOCITY
    }

    /**
     * Sets a random velocity direction and ball speed
     * Ensures that:
     * 1. The sum of absolute values of velocityX and velocityY is exactly 1
     * 2. The absolute value of velocityX is at least 0.25
     */
    fun setRandomVelocity() {
        // Generate a random X velocity with absolute value at least 0.25
        val minXVelocity = 0.25f
        val xSign = if (Random.nextBoolean()) 1 else -1
        val absVelocityX = minXVelocity + Random.nextFloat() * (1f - minXVelocity)
        velocityX = xSign * absVelocityX

        // Calculate Y velocity so that |velocityX| + |velocityY| = 1 exactly
        val absVelocityY = 1f - absVelocityX
        val ySign = if (Random.nextBoolean()) 1 else -1
        velocityY = ySign * absVelocityY

        println("velocityX: $velocityX, velocityY: $velocityY")
    }
}
