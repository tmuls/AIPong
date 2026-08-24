package org.rex.junietest.entity

import java.awt.Graphics2D
import java.awt.Rectangle

/**
 * Base class for all game entities.
 * Provides position, rendering, and collision detection capabilities.
 */
abstract class Entity(
    var x: Float,
    var y: Float,
    var width: Int,
    var height: Int,
    var boundingBox: Rectangle = Rectangle(0, 0, width, height)
) {
    var onCollision: ((Entity) -> Unit)? = null

    abstract fun render(g: Graphics2D)

    abstract fun update(deltaTime: Float)

    fun renderBoundingBox(g: Graphics2D) {
        g.draw(Rectangle(
            x.toInt() + boundingBox.x,
            y.toInt() + boundingBox.y,
            boundingBox.width,
            boundingBox.height
        ))
    }

    fun collidesWith(other: Entity): Boolean {
        val thisBox = Rectangle(
            x.toInt() + boundingBox.x,
            y.toInt() + boundingBox.y,
            boundingBox.width,
            boundingBox.height
        )
        val otherBox = Rectangle(
            other.x.toInt() + other.boundingBox.x,
            other.y.toInt() + other.boundingBox.y,
            other.boundingBox.width,
            other.boundingBox.height
        )
        return thisBox.intersects(otherBox)
    }

    fun handleCollision(other: Entity) {
        onCollision?.invoke(other)
    }
}
