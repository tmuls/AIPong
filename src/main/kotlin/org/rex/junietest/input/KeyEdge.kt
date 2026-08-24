package org.rex.junietest.input

/**
 * Detects press/release transitions by comparing this poll's state
 * against the previous one.
 *
 * Takes a plain isPressed lambda rather than depending on GameInput
 * directly, so it can be tested without any AWT/JFrame machinery, and so
 * the same tracker works for any boolean input source.
 *
 * Lives separately from GameInput on purpose: "just pressed" only means
 * something relative to "since I last checked," which is a property of
 * whoever's consuming it, not of the shared input state. GameInput stays
 * the single source of truth for "is this key currently held" - the one
 * fact every consumer can agree on regardless of when they poll. Each
 * KeyEdge instance tracks its own key for its own caller, so two
 * different consumers watching the same key (e.g. gameplay and a menu)
 * never step on each other.
 */
class KeyEdge(private val isPressed: () -> Boolean) {
    private var wasPressed = false

    /** Call once per tick. Returns which transition (if any) happened since the last poll(). */
    fun poll(): Transition {
        val pressed = isPressed()
        val transition = when {
            pressed && !wasPressed -> Transition.PRESSED
            !pressed && wasPressed -> Transition.RELEASED
            else -> Transition.NONE
        }
        wasPressed = pressed
        return transition
    }

    enum class Transition { NONE, PRESSED, RELEASED }
}
