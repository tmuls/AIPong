package org.rex.junietest.renderer

import org.rex.junietest.entity.Entity
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import javax.swing.JPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.time.TimeSource

class GamePanel : JPanel() {
        private val entities = mutableListOf<Entity>()

        private var rendering = false
        private val renderScope = CoroutineScope(Dispatchers.Default)
        private var renderJob: Job? = null
        private var currentFPS = 0
        var displayBoundingBoxes = false

        private val maxFPS = 500
        private val minFrameTime = 1000 / maxFPS // in milliseconds

        init {
            preferredSize = Dimension(PANEL_WIDTH, PANEL_HEIGHT)
            background = Color.BLACK
            isDoubleBuffered = true
        }

        companion object {
            const val PANEL_WIDTH = 800
            const val PANEL_HEIGHT = 600
        }

        fun registerEntity(entity: Entity) {
            synchronized(entities) {
                entities.add(entity)
            }
        }

        fun unregisterEntity(entity: Entity) {
            synchronized(entities) {
                entities.remove(entity)
            }
        }

        fun startRendering() {
            if (rendering) return

            rendering = true

            // Runs as a coroutine on a background dispatcher instead of
            // java.lang.Thread, so this loop stays portable to Kotlin/Native
            // (Dispatchers.Default runs on real OS threads there too). The
            // repaint()/paintComponent() calls below remain JVM-only Swing.
            renderJob = renderScope.launch {
                val clock = TimeSource.Monotonic
                var lastRenderTime = clock.markNow()
                var frameCount = 0
                var lastFpsTime = lastRenderTime

                while (rendering) {
                    val elapsedMs = lastRenderTime.elapsedNow().inWholeMilliseconds

                    if (elapsedMs >= minFrameTime) {
                        repaint()

                        frameCount++
                        if (lastFpsTime.elapsedNow().inWholeMilliseconds >= 1000) {
                            currentFPS = frameCount
                            frameCount = 0
                            lastFpsTime = clock.markNow()
                        }

                        val frameStart = clock.markNow()
                        lastRenderTime = frameStart

                        val sleepTime = minFrameTime - frameStart.elapsedNow().inWholeMilliseconds
                        if (sleepTime > 0) {
                            delay(sleepTime)
                        }
                    } else {
                        yield()
                    }
                }
            }
        }

        fun stopRendering() {
            rendering = false
            runBlocking { renderJob?.join() }
            renderJob = null
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D

            // Enable anti-aliasing for smoother graphics
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            synchronized(entities) {
                for (entity in entities) {
                    entity.render(g2d)

                    if (displayBoundingBoxes) {
                        g2d.color = Color(150, 150, 150, 128) // Medium gray with 50% opacity
                        entity.renderBoundingBox(g2d)
                    }
                }
            }

            g2d.font = Font("Arial", Font.PLAIN, 14)
            g2d.color = Color.WHITE
            val fpsText = "fps: $currentFPS"
            val metrics = g2d.fontMetrics
            val textWidth = metrics.stringWidth(fpsText)
            g2d.drawString(fpsText, width - textWidth - 10, 20)

            // Ensure rendering is completed
            Toolkit.getDefaultToolkit().sync()
        }
    }
