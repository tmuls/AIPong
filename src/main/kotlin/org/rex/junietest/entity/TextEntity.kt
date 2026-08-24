package org.rex.junietest.entity

import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D

class TextEntity(
    x: Float,
    y: Float,
    private val fontSize: Int = 24,
    private val color: Color = Color.WHITE
) : Entity(x, y, 0, 0) {
    private var text: String = ""

    override fun render(g: Graphics2D) {
        if (text.isEmpty()) return

        g.color = color
        g.font = Font("Arial", Font.BOLD, fontSize)

        val metrics = g.fontMetrics
        val textWidth = metrics.stringWidth(text)
        val textHeight = metrics.height

        width = textWidth
        height = textHeight

        g.drawString(text, x.toInt(), y.toInt() + metrics.ascent)
    }

    override fun update(deltaTime: Float) {
        // Text update logic will be handled by the game
    }

    fun updateText(newText: String) {
        text = newText
    }
}
