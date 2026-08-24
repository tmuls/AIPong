package org.rex.junietest.input

import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import kotlin.concurrent.Volatile

/**
 * Tracks which keys are currently held down, polled via isKeyPressed().
 * No callback registration on purpose: PaddleEntity/AIPaddleEntity just
 * poll every frame, which is one clear read path to reason about instead
 * of callbacks firing out of a different (AWT) thread mid-frame.
 */
class GameInput(private val frame: JFrame) : KeyListener {
    // Build buffer: mutated only from the AWT event thread. Swing dispatches
    // keyPressed/keyReleased serially, never concurrently, so this needs no
    // locking - there's exactly one writer.
    private val keyBuffer = mutableSetOf<Int>()

    // Published snapshot the game loop thread reads via isKeyPressed(). Every
    // change to keyBuffer is republished here as a fresh immutable copy.
    // @Volatile is enough (not a full atomic type) because there's exactly
    // one writer and no compare-and-swap involved - just a safe publish -
    // and it's stable/multiplatform (kotlin.concurrent.Volatile, not the
    // deprecated JVM-only kotlin.jvm.Volatile), so this stays portable to
    // Kotlin/Native with no experimental API opt-in.
    @Volatile
    private var pressedKeys: Set<Int> = emptySet()

    init {
        frame.addKeyListener(this)
        frame.isFocusable = true
        frame.requestFocus()
    }

    override fun keyPressed(e: KeyEvent) {
        keyBuffer.add(e.keyCode)
        pressedKeys = keyBuffer.toSet()
    }

    override fun keyReleased(e: KeyEvent) {
        keyBuffer.remove(e.keyCode)
        pressedKeys = keyBuffer.toSet()
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
        return keyCode in pressedKeys
    }
}
