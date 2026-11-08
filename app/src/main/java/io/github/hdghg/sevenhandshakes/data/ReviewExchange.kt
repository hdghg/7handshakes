package io.github.hdghg.sevenhandshakes.data

/** How many handshake ratings the contact screen shows: levels 1..MAX_HANDSHAKES. */
const val MAX_HANDSHAKES = 5

/**
 * A chain of contacts found by the handshake search, in review direction: each contact reviewed the next.
 * [weights] has one entry per edge: how many reviews of the rate level the edge stands for.
 */
data class HandshakePath(
    val contactIds: List<Int>,
    val weights: List<Int>,
) {
    init {
        require(weights.isNotEmpty() && contactIds.size == weights.size + 1)
    }

    /** A path is worth its weakest edge. */
    val weight: Int get() = weights.min()
}

/**
 * Ratings between a contact and the selected identity, keyed by rate level (see [rateLevelOf]).
 * Each level holds the heaviest path found; levels without a path are absent.
 */
data class ReviewExchange(
    /** Paths from the contact to the selected identity. */
    val receivedPaths: Map<Byte, HandshakePath> = emptyMap(),
    /** Paths from the selected identity to the contact. */
    val givenPaths: Map<Byte, HandshakePath> = emptyMap(),
) {
    fun receivedOf(level: Byte): Int = receivedPaths[level]?.weight ?: 0

    fun givenOf(level: Byte): Int = givenPaths[level]?.weight ?: 0

    /** Received minus given: positive means the contact thanked you more often than you thanked them. */
    fun differenceOf(level: Byte): Int = receivedOf(level) - givenOf(level)
}
