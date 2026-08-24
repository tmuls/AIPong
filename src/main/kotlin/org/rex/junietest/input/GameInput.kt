package org.rex.junietest.input

import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import kotlin.concurrent.Volatile

/**
 * Tracks keyboard state, frozen once per game tick via sync().
 * No callback registration on purpose: consumers poll isKeyPressed() /
 * wasKeyJustPressed() / wasKeyJustReleased(), which is one clear read path
 * to reason about instead of callbacks firing out of a different (AWT)
 * thread mid-frame.
 */
class GameInput(private val frame: JFrame) : KeyListener {
    // Build buffer: mutated only from the AWT event thread. Swing dispatches
    // keyPressed/keyReleased serially, never concurrently, so this needs no
    // locking - there's exactly one writer.
    private val keyBuffer = mutableSetOf<Int>()

    // Published snapshot of currently-held keys, republished by the AWT
    // thread on every event. @Volatile is enough (single writer, no
    // compare-and-swap involved) and is stable/multiplatform
    // (kotlin.concurrent.Volatile), so this stays portable to Kotlin/Native.
    @Volatile
    private var liveKeys: Set<Int> = emptySet()

    // Frame-scoped state: read and written only by the game loop thread,
    // inside sync(). The AWT thread never touches these, so they need no
    // synchronization at all.
    private var heldKeys: Set<Int> = emptySet()
    private var justPressedKeys: Set<Int> = emptySet()
    private var justReleasedKeys: Set<Int> = emptySet()

    init {
        frame.addKeyListener(this)
        frame.isFocusable = true
        frame.requestFocus()
    }

    override fun keyPressed(e: KeyEvent) {
        keyBuffer.add(e.keyCode)
        liveKeys = keyBuffer.toSet()
    }

    override fun keyReleased(e: KeyEvent) {
        keyBuffer.remove(e.keyCode)
        liveKeys = keyBuffer.toSet()
    }

    override fun keyTyped(e: KeyEvent) {
        // Not used
    }

    /**
     * Freezes this tick's input state from whatever the AWT thread has
     * published so far, and computes which keys transitioned since the
     * previous call. Call once per game loop tick, before entities/scenes
     * update - otherwise a key event landing mid-tick could make two reads
     * of the same key within the same frame disagree with each other.
     */
    fun sync() {
        val current = liveKeys // single volatile read; stable for the rest of this frame
        justPressedKeys = current - heldKeys
        justReleasedKeys = heldKeys - current
        heldKeys = current
    }

    fun isKeyPressed(keyCode: Int): Boolean = keyCode in heldKeys

    fun wasKeyJustPressed(keyCode: Int): Boolean = keyCode in justPressedKeys

    fun wasKeyJustReleased(keyCode: Int): Boolean = keyCode in justReleasedKeys
}
