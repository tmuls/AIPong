package org.rex.junietest.input

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyEdgeTest {
    @Test
    fun `reports PRESSED only on the tick a key transitions from up to down`() {
        var pressed = false
        val edge = KeyEdge { pressed }

        assertEquals(KeyEdge.Transition.NONE, edge.poll()) // starts up, stays up

        pressed = true
        assertEquals(KeyEdge.Transition.PRESSED, edge.poll()) // up -> down
        assertEquals(KeyEdge.Transition.NONE, edge.poll())    // held down
        assertEquals(KeyEdge.Transition.NONE, edge.poll())    // still held
    }

    @Test
    fun `reports RELEASED only on the tick a key transitions from down to up`() {
        var pressed = true
        val edge = KeyEdge { pressed }

        assertEquals(KeyEdge.Transition.PRESSED, edge.poll()) // starts down
        assertEquals(KeyEdge.Transition.NONE, edge.poll())    // still down

        pressed = false
        assertEquals(KeyEdge.Transition.RELEASED, edge.poll()) // down -> up
        assertEquals(KeyEdge.Transition.NONE, edge.poll())     // still up
    }

    @Test
    fun `handles repeated press-release cycles`() {
        var pressed = false
        val edge = KeyEdge { pressed }

        pressed = true
        assertEquals(KeyEdge.Transition.PRESSED, edge.poll())
        pressed = false
        assertEquals(KeyEdge.Transition.RELEASED, edge.poll())
        pressed = true
        assertEquals(KeyEdge.Transition.PRESSED, edge.poll())
        pressed = false
        assertEquals(KeyEdge.Transition.RELEASED, edge.poll())
    }
}
