package org.rex.junietest.input

import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tracks keyboard state, frozen once per game tick via update().
 * No callback registration on purpose: consumers poll isKeyPressed() /
 * wasKeyJustPressed() / wasKeyJustReleased(), which is one clear read path
 * to reason about instead of callbacks firing out of a different (AWT)
 * thread mid-frame.
 */
class GameInput(private val frame: JFrame) : KeyListener {
    // Buffers events since the last update() call. Both the AWT thread
    // (adding) and the game loop thread (draining + replacing in update())
    // mutate these, so unlike a simple publish this needs real mutual
    // exclusion. kotlinx.coroutines.sync.Mutex is stable and multiplatform,
    // so this stays portable to Kotlin/Native.
    private val lock = Mutex()
    private var pressedBuffer = mutableSetOf<Int>()
    private var releasedBuffer = mutableSetOf<Int>()

    // This frame's state, read and written only by the game loop thread
    // inside update() - the AWT thread never touches these.
    private var heldKeys: Set<Int> = emptySet()
    private var pressedKeys: Set<Int> = emptySet()
    private var releasedKeys: Set<Int> = emptySet()

    init {
        frame.addKeyListener(this)
        frame.isFocusable = true
        frame.requestFocus()
    }

    override fun keyPressed(e: KeyEvent) {
        runBlocking { lock.withLock { pressedBuffer.add(e.keyCode) } }
    }

    override fun keyReleased(e: KeyEvent) {
        runBlocking { lock.withLock { releasedBuffer.add(e.keyCode) } }
    }

    override fun keyTyped(e: KeyEvent) {
        // Not used
    }

    /**
     * Moves this tick's buffered press/release events to live and starts
     * fresh buffers for the next window. Call once per game loop tick,
     * before entities/scenes update, so everything within that tick sees
     * the same frozen input state.
     */
    fun update() {
        val (pressedThisTick, releasedThisTick) = runBlocking {
            lock.withLock {
                val pressed = pressedBuffer
                val released = releasedBuffer
                pressedBuffer = mutableSetOf()
                releasedBuffer = mutableSetOf()
                pressed to released
            }
        }

        // Validate against last frame's held state: a key can only become
        // pressed if it wasn't already held, and can only become released
        // if it was already held. Filters out things like OS auto-repeat
        // presses on an already-held key.
        pressedKeys = pressedThisTick.filterTo(mutableSetOf()) { it !in heldKeys }
        releasedKeys = releasedThisTick.filterTo(mutableSetOf()) { it in heldKeys }

        heldKeys = (heldKeys + pressedKeys) - releasedKeys
    }

    fun isKeyPressed(keyCode: Int): Boolean = keyCode in heldKeys

    fun wasKeyJustPressed(keyCode: Int): Boolean = keyCode in pressedKeys

    fun wasKeyJustReleased(keyCode: Int): Boolean = keyCode in releasedKeys
}
