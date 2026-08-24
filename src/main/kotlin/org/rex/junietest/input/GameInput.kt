package org.rex.junietest.input

import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles keyboard input for the game
 */
class GameInput(private val frame: JFrame) : KeyListener {
    // Map of key codes to lists of callbacks
    private val keyCallbacks = mutableMapOf<Int, MutableList<() -> Unit>>()

    // Set of currently pressed keys, written from the AWT event thread
    // (keyPressed/keyReleased) and read from the game loop thread
    // (isKeyPressed). Guarded by keyLock instead of a JVM-only concurrent
    // collection (java.util.concurrent has no Kotlin/Native equivalent);
    // kotlinx.coroutines.sync.Mutex works identically on every target.
    private val keyLock = Mutex()
    private val pressedKeys = mutableSetOf<Int>()

    init {
        frame.addKeyListener(this)
        frame.isFocusable = true
        frame.requestFocus()
    }

    /**
     * Register a callback for when a key is pressed
     * @param keyCode The key code to listen for (from KeyEvent)
     * @param callback The function to call when the key is pressed
     */
    fun registerKeyPress(keyCode: Int, callback: () -> Unit) {
        keyCallbacks.getOrPut(keyCode) { mutableListOf() }.add(callback)
    }

    /**
     * Unregister a callback for a key press
     * @param keyCode The key code to stop listening for
     * @param callback The callback to remove
     */
    fun unregisterKeyPress(keyCode: Int, callback: () -> Unit) {
        keyCallbacks[keyCode]?.remove(callback)
    }

    override fun keyPressed(e: KeyEvent) {
        // Check-and-add happens atomically inside the lock; callbacks run
        // outside it so arbitrary callback code never executes while held.
        val justPressed = runBlocking { keyLock.withLock { pressedKeys.add(e.keyCode) } }
        if (justPressed) {
            keyCallbacks[e.keyCode]?.forEach { it() }
        }
    }

    override fun keyReleased(e: KeyEvent) {
        runBlocking { keyLock.withLock { pressedKeys.remove(e.keyCode) } }
    }

    override fun keyTyped(e: KeyEvent) {
        // Not used
    }

    /**
     * Check if a key is currently pressed
     * @param keyCode The key code to check
     * @return true if the key is pressed, false otherwise
     */
    fun isKeyPressed(keyCode: Int): Boolean {
        return runBlocking { keyLock.withLock { pressedKeys.contains(keyCode) } }
    }
} 