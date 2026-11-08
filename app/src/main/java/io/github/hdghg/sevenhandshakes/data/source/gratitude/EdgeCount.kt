package io.github.hdghg.sevenhandshakes.data.source.gratitude

/** Number of reviews [sourceContactId] wrote to [targetContactId] with one stored [gratitude] value. */
data class EdgeCount(
    val sourceContactId: Int,
    val targetContactId: Int,
    val gratitude: Byte,
    val count: Int,
)
