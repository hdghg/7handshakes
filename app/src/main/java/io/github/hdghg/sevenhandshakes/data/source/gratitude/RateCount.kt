package io.github.hdghg.sevenhandshakes.data.source.gratitude

/** Number of reviews written by [sourceContactId] with one stored [gratitude] value. */
data class RateCount(
    val sourceContactId: Int,
    val gratitude: Byte,
    val count: Int,
)
