package org.rex.junietest

import javax.swing.JFrame
import org.rex.junietest.core.Bounds
import org.rex.junietest.renderer.GamePanel
import org.rex.junietest.entity.BallEntity
import org.rex.junietest.entity.Entity
import org.rex.junietest.entity.Side
import org.rex.junietest.entity.NetEntity
import org.rex.junietest.entity.TextEntity
import org.rex.junietest.entity.PaddleEntity
import org.rex.junietest.entity.AIPaddleEntity
import org.rex.junietest.input.GameInput
import java.awt.Color
import java.awt.event.KeyEvent
import java.lang.Thread.sleep

class Game : JFrame() {
    // Shared, mutable playfield size that entities read from instead of
    // hardcoding GamePanel.PANEL_WIDTH/HEIGHT.
    private val bounds = Bounds(GamePanel.PANEL_WIDTH.toFloat(), GamePanel.PANEL_HEIGHT.toFloat())

    private val gamePanel: GamePanel
    private val ball: BallEntity
    private val net: NetEntity
    private val leftScoreText: TextEntity
    private val rightScoreText: TextEntity
    private val leftPaddle: PaddleEntity
    private val rightPaddle: AIPaddleEntity
    private val gameInput: GameInput

    // Game state
    private var running = false
    private var gameThread: Thread? = null
    private var leftScore = 0
    private var rightScore = 0

    // Game loop constants
    private val targetFPS = 60
    private val targetFrameTime = 1000 / targetFPS // in milliseconds

    init {
        title = "Ball Game"
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false // Prevent window resizing

        // Create the game panel
        gamePanel = GamePanel()
        add(gamePanel)

        // Create the game input handler
        gameInput = GameInput(this)

        // Create the ball entity (50% smaller than before) with default velocity and scorePoint lambda
        ball = BallEntity(0f, 0f, 12, Color.ORANGE, bounds, BallEntity.DEFAULT_VELOCITY, this::pointScored)

        // Create the net entity
        net = NetEntity(bounds)

        // Create score text entities
        leftScoreText = TextEntity(
            x = bounds.width / 4, // Position at 1/4 of screen width
            y = 30f, // Position near top
            fontSize = 32
        )
        rightScoreText = TextEntity(
            x = bounds.width * 3 / 4, // Position at 3/4 of screen width
            y = 30f, // Position near top
            fontSize = 32
        )

        // Create paddles
        leftPaddle = PaddleEntity(
            x = 40f, // 40 pixels from left edge
            y = bounds.height / 2 - 40, // Center vertically (half of 80px height)
            color = Color(255, 182, 193), // Pastel red
            bounds = bounds,
            upKey = KeyEvent.VK_W,
            downKey = KeyEvent.VK_S,
            gameInput = gameInput
        )
        rightPaddle = AIPaddleEntity(
            x = bounds.width - 50, // 40 pixels from right edge (50 = 40 + 10 width)
            y = bounds.height / 2 - 40, // Center vertically (half of 80px height)
            color = Color(173, 216, 230), // Pastel blue
            bounds = bounds,
            ball = ball
        )

        // Register the entities with the game panel
        gamePanel.registerEntity(ball)
        gamePanel.registerEntity(net)
        gamePanel.registerEntity(leftScoreText)
        gamePanel.registerEntity(rightScoreText)
        gamePanel.registerEntity(leftPaddle)
        gamePanel.registerEntity(rightPaddle)

        pack()
        setLocationRelativeTo(null) // Center on screen

        // Position the ball in the center of the panel
        ball.centerInBounds()

        // Initialize score display
        updateScoreDisplay()

        isVisible = true

        // Start the game and rendering loops
        startGame()
    }

    /**
     * Updates the game state
     * @param deltaTime Time elapsed since the last update in seconds
     */
    private fun update(deltaTime: Float) {
        // Update the ball entity
        ball.update(deltaTime)
        
        // Update the paddles
        leftPaddle.update(deltaTime)
        rightPaddle.update(deltaTime)

        // Check for collisions between ball and paddles
        if (ball.collidesWith(leftPaddle)) {
            ball.handlePaddleCollision(leftPaddle)
        }
        if (ball.collidesWith(rightPaddle)) {
            ball.handlePaddleCollision(rightPaddle)
        }
    }

    /**
     * Updates the score display text entities
     */
    private fun updateScoreDisplay() {
        leftScoreText.updateText(leftScore.toString())
        rightScoreText.updateText(rightScore.toString())
    }

    /**
     * Starts the game loop
     */
    private fun startGame() {
        if (running) return

        running = true

        // Start the rendering loop in GamePanel
        gamePanel.startRendering()

        // Start the game loop in a separate thread
        gameThread = Thread {
            var lastUpdateTime = System.currentTimeMillis()

            while (running) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - lastUpdateTime

                if (elapsedTime >= targetFrameTime) {
                    update(elapsedTime.toFloat() / 1000f) // Convert to seconds
                    lastUpdateTime = currentTime

                    // Sleep to maintain the target frame rate
                    val sleepTime = targetFrameTime - (System.currentTimeMillis() - currentTime)
                    if (sleepTime > 0) {
                        try {
                            sleep(sleepTime)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    // Yield to other threads if we're ahead of schedule
                    Thread.yield()
                }
            }
        }.apply {
            name = "GameLoop"
            isDaemon = true
            start()
        }
    }

    /**
     * Stops the game loop
     */
    fun stopGame() {
        running = false
        gamePanel.stopRendering()
        gameThread?.join(1000) // Wait for the game thread to finish
        gameThread = null
    }

    /**
     * Called when a point is scored
     * @param side The side that scored (LEFT or RIGHT)
     */
    private fun pointScored(side: Side) {
        when (side) {
            Side.LEFT -> leftScore++
            Side.RIGHT -> rightScore++
        }
        updateScoreDisplay()
        println("Point scored by ${side.name.lowercase()} side! Score: Left $leftScore - Right $rightScore")
    }

    /**
     * Get the game input handler
     */
    fun getGameInput(): GameInput {
        return gameInput
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Game()
        }
    }
}
