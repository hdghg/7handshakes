package io.github.hdghg.sevenhandshakes.data

/** Numbers of golden stars a review can give: 1, 2 or 3. */
val RATE_LEVELS: List<Byte> = listOf(1, 2, 3)

/** Stars a review counts for. Stored values outside 1..3 are clamped to the nearest valid number. */
fun rateLevelOf(rate: Byte): Byte = rate.coerceIn(1, 3)
