package io.github.hdghg.sevenhandshakes.data.source.gratitude

/** Number of reviews with one stored [gratitude] value. */
data class RateCount(
    val gratitude: Byte,
    val count: Int,
)
