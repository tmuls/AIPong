package org.rex.junietest.entity

import java.awt.Color
import java.awt.Graphics2D
import org.rex.junietest.core.Bounds

/**
 * AI-controlled paddle entity that extends the base PaddleEntity
 */
class AIPaddleEntity(
    x: Float,
    y: Float,
    color: Color,
    bounds: Bounds,
    paddleSpeed: Float = 600f,
    private val ball: BallEntity
) : PaddleEntity(x, y, color, bounds, paddleSpeed, 0, 0, null) {

    // AI behavior constants
    private val minReactionDelay = 0.15f // seconds
    private val maxReactionDelay = 0.3f // seconds
    private var reactionDelay = minReactionDelay
    private var reactionTimer = 0f
    private var targetY = y
    private var mistakeTimer = 0f
    private var isMakingMistake = false
    private var mistakeTargetY = y

    override fun update(deltaTime: Float) {
        // Update reaction timer
        reactionTimer += deltaTime

        // Update mistake timer
        mistakeTimer += deltaTime

        // Randomly decide to make a mistake
        if (!isMakingMistake && mistakeTimer > 3f && kotlin.random.Random.nextFloat() < 0.1f) {
            isMakingMistake = true
            mistakeTimer = 0f
            // Set a random mistake target position
            mistakeTargetY = kotlin.random.Random.nextFloat() * (bounds.height - height)
            // Set a random reaction delay for the mistake
            reactionDelay = minReactionDelay + kotlin.random.Random.nextFloat() * (maxReactionDelay - minReactionDelay)
        }

        // Only update target position after reaction delay
        if (reactionTimer >= reactionDelay) {
            reactionTimer = 0f

            if (isMakingMistake) {
                // Move towards mistake target
                targetY = mistakeTargetY
                // End mistake after a short time
                if (mistakeTimer > 0.5f) {
                    isMakingMistake = false
                    mistakeTimer = 0f
                }
            } else {
                // Predict where the ball will be when it reaches the paddle's x position
                if (ball.velocityX > 0) { // Only track ball when it's moving towards the AI
                    // Calculate time until ball reaches paddle's x position
                    val timeToReach = (x - ball.x) / (ball.velocityX * ball.ballSpeed)
                    
                    // Predict ball's y position at that time
                    val predictedY = ball.y + (ball.velocityY * ball.ballSpeed * timeToReach)
                    
                    // Add some prediction error
                    val predictionError = (kotlin.random.Random.nextFloat() - 0.5f) * 20f
                    
                    // Set target to predicted position with error, adjusted for paddle height
                    targetY = (predictedY - height / 2 + predictionError).coerceIn(0f, bounds.height - height)
                }
            }
        }

        // Move towards target position
        val distanceToTarget = targetY - y
        val movement = when {
            distanceToTarget > 5f -> paddleSpeed * deltaTime // Move down if more than 5 pixels away
            distanceToTarget < -5f -> -paddleSpeed * deltaTime // Move up if more than 5 pixels away
            else -> 0f // Stay still if close enough to target
        }

        // Update position
        y += movement

        // Keep paddle within screen bounds
        y = y.coerceIn(0f, bounds.height - height)
    }
} 