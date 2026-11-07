package io.github.hdghg.sevenhandshakes.data

/** Rate levels shown in the UI: 1 silver, 2 gold, 3 diamond. */
val RATE_LEVELS: List<Byte> = listOf(1, 2, 3)

/** The level a stored rate is shown as. Values outside 1..3 are clamped to the nearest level. */
fun rateLevelOf(rate: Byte): Byte = rate.coerceIn(1, 3)
