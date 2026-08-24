package org.rex.junietest.core

/**
 * The width/height of the playfield an entity operates within.
 *
 * Entities read this instead of hardcoding GamePanel.PANEL_WIDTH/HEIGHT so
 * the same entity classes can run inside differently-sized playfields (e.g.
 * a scaled-down viewport). Fields are mutable and shared by reference:
 * entities re-read width/height on every update(), so mutating a Bounds
 * instance that's already wired into a game (e.g. on a window resize) takes
 * effect on the next frame with no further plumbing.
 */
class Bounds(var width: Float, var height: Float)
