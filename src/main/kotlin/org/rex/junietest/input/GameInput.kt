package org.rex.junietest.input

import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Tracks which keys are currently held down, polled via isKeyPressed().
 * No callback registration on purpose: PaddleEntity/AIPaddleEntity just
 * poll every frame, which is one clear read path to reason about instead
 * of callbacks firing out of a different (AWT) thread mid-frame.
 */
@OptIn(ExperimentalAtomicApi::class)
class GameInput(private val frame: JFrame) : KeyListener {
    // Written from the AWT event thread (keyPressed/keyReleased), read from
    // the game loop thread (isKeyPressed). Published as an immutable Set
    // through an atomic reference (copy-on-write) instead of a JVM-only
    // concurrent collection, so isKeyPressed() is a lock-free read and this
    // stays portable to Kotlin/Native.
    private val pressedKeys = AtomicReference<Set<Int>>(emptySet())

    init {
        frame.addKeyListener(this)
        frame.isFocusable = true
        frame.requestFocus()
    }

    override fun keyPressed(e: KeyEvent) {
        while (true) {
            val current = pressedKeys.load()
            if (e.keyCode in current) return
            if (pressedKeys.compareAndSet(current, current + e.keyCode)) return
        }
    }

    override fun keyReleased(e: KeyEvent) {
        while (true) {
            val current = pressedKeys.load()
            if (e.keyCode !in current) return
            if (pressedKeys.compareAndSet(current, current - e.keyCode)) return
        }
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
        return keyCode in pressedKeys.load()
    }
}
