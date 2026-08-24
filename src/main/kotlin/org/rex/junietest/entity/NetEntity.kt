package org.rex.junietest.entity

import java.awt.Color
import java.awt.Graphics2D
import org.rex.junietest.core.Bounds

class NetEntity(private val bounds: Bounds) : Entity(
    x = bounds.width / 2 - 2.5f, // Center the net with 5px width
    y = 10f,
    width = 5,
    height = (bounds.height - 20).toInt() // Subtract 20 to account for 10px gap at top and bottom
) {
    override fun render(g: Graphics2D) {
        g.color = Color.WHITE
        g.fillRect(x.toInt(), y.toInt(), width, height)
    }

    override fun update(deltaTime: Float) {
        // Re-derive position/size from bounds every frame so the net
        // follows if bounds are resized after construction.
        x = bounds.width / 2 - 2.5f
        height = (bounds.height - 20).toInt()
    }
}
