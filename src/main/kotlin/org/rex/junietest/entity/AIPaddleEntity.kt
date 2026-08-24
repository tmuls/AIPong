package org.rex.junietest.entity

import java.awt.Color
import java.awt.Graphics2D
import org.rex.junietest.core.Bounds

class AIPaddleEntity(
    x: Float,
    y: Float,
    color: Color,
    bounds: Bounds,
    paddleSpeed: Float = 600f,
    private val ball: BallEntity
) : PaddleEntity(x, y, color, bounds, paddleSpeed, 0, 0, null) {

    private val minReactionDelay = 0.15f // seconds
    private val maxReactionDelay = 0.3f // seconds
    private var reactionDelay = minReactionDelay
    private var reactionTimer = 0f
    private var targetY = y
    private var mistakeTimer = 0f
    private var isMakingMistake = false
    private var mistakeTargetY = y

    override fun update(deltaTime: Float) {
        reactionTimer += deltaTime
        mistakeTimer += deltaTime

        if (!isMakingMistake && mistakeTimer > 3f && kotlin.random.Random.nextFloat() < 0.1f) {
            isMakingMistake = true
            mistakeTimer = 0f
            mistakeTargetY = kotlin.random.Random.nextFloat() * (bounds.height - height)
            reactionDelay = minReactionDelay + kotlin.random.Random.nextFloat() * (maxReactionDelay - minReactionDelay)
        }

        if (reactionTimer >= reactionDelay) {
            reactionTimer = 0f

            if (isMakingMistake) {
                targetY = mistakeTargetY
                if (mistakeTimer > 0.5f) {
                    isMakingMistake = false
                    mistakeTimer = 0f
                }
            } else {
                // Predict where the ball will be when it reaches the paddle's x position
                if (ball.velocityX > 0) { // Only track ball when it's moving towards the AI
                    val timeToReach = (x - ball.x) / (ball.velocityX * ball.ballSpeed)
                    val predictedY = ball.y + (ball.velocityY * ball.ballSpeed * timeToReach)
                    val predictionError = (kotlin.random.Random.nextFloat() - 0.5f) * 20f
                    targetY = (predictedY - height / 2 + predictionError).coerceIn(0f, bounds.height - height)
                }
            }
        }

        val distanceToTarget = targetY - y
        val movement = when {
            distanceToTarget > 5f -> paddleSpeed * deltaTime
            distanceToTarget < -5f -> -paddleSpeed * deltaTime
            else -> 0f
        }

        y += movement
        y = y.coerceIn(0f, bounds.height - height)
    }
}
