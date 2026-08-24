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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.time.TimeSource

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

    private var running = false
    private val gameScope = CoroutineScope(Dispatchers.Default)
    private var gameJob: Job? = null
    private var leftScore = 0
    private var rightScore = 0

    private val targetFPS = 60
    private val targetFrameTime = 1000 / targetFPS // in milliseconds

    init {
        title = "Ball Game"
        defaultCloseOperation = EXIT_ON_CLOSE
        isResizable = false

        gamePanel = GamePanel()
        add(gamePanel)

        gameInput = GameInput(this)

        ball = BallEntity(0f, 0f, 12, Color.ORANGE, bounds, BallEntity.DEFAULT_VELOCITY, this::pointScored)

        net = NetEntity(bounds)

        leftScoreText = TextEntity(
            x = bounds.width / 4,
            y = 30f,
            fontSize = 32
        )
        rightScoreText = TextEntity(
            x = bounds.width * 3 / 4,
            y = 30f,
            fontSize = 32
        )

        leftPaddle = PaddleEntity(
            x = 40f,
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

        gamePanel.registerEntity(ball)
        gamePanel.registerEntity(net)
        gamePanel.registerEntity(leftScoreText)
        gamePanel.registerEntity(rightScoreText)
        gamePanel.registerEntity(leftPaddle)
        gamePanel.registerEntity(rightPaddle)

        pack()
        setLocationRelativeTo(null) // Center on screen

        ball.centerInBounds()

        updateScoreDisplay()

        isVisible = true

        startGame()
    }

    private fun update(deltaTime: Float) {
        gameInput.sync()

        ball.update(deltaTime)

        leftPaddle.update(deltaTime)
        rightPaddle.update(deltaTime)

        if (ball.collidesWith(leftPaddle)) {
            ball.handlePaddleCollision(leftPaddle)
        }
        if (ball.collidesWith(rightPaddle)) {
            ball.handlePaddleCollision(rightPaddle)
        }
    }

    private fun updateScoreDisplay() {
        leftScoreText.updateText(leftScore.toString())
        rightScoreText.updateText(rightScore.toString())
    }

    private fun startGame() {
        if (running) return

        running = true

        gamePanel.startRendering()

        // Runs as a coroutine on a background dispatcher instead of
        // java.lang.Thread, so this loop stays portable to Kotlin/Native
        // (Dispatchers.Default runs on real OS threads there too).
        gameJob = gameScope.launch {
            val clock = TimeSource.Monotonic
            var lastUpdateTime = clock.markNow()

            while (running) {
                val elapsedMs = lastUpdateTime.elapsedNow().inWholeMilliseconds

                if (elapsedMs >= targetFrameTime) {
                    update(elapsedMs.toFloat() / 1000f)
                    val frameStart = clock.markNow()
                    lastUpdateTime = frameStart

                    val sleepTime = targetFrameTime - frameStart.elapsedNow().inWholeMilliseconds
                    if (sleepTime > 0) {
                        delay(sleepTime)
                    }
                } else {
                    yield()
                }
            }
        }
    }

    fun stopGame() {
        running = false
        gamePanel.stopRendering()
        runBlocking { gameJob?.join() }
        gameJob = null
    }

    private fun pointScored(side: Side) {
        when (side) {
            Side.LEFT -> leftScore++
            Side.RIGHT -> rightScore++
        }
        updateScoreDisplay()
        println("Point scored by ${side.name.lowercase()} side! Score: Left $leftScore - Right $rightScore")
    }

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
