package io.github.hdghg.sevenhandshakes.data

/** How many handshake ratings the contact screen shows: levels 1..MAX_HANDSHAKES. */
const val MAX_HANDSHAKES = 5

/**
 * Review counts between a contact and the selected identity, keyed by rate level (see [rateLevelOf]).
 * Levels without reviews are absent.
 */
data class ReviewExchange(
    /** Reviews the contact wrote to the selected identity. */
    val received: Map<Byte, Int> = emptyMap(),
    /** Reviews the selected identity wrote to the contact. */
    val given: Map<Byte, Int> = emptyMap(),
) {
    fun receivedOf(level: Byte): Int = received[level] ?: 0

    fun givenOf(level: Byte): Int = given[level] ?: 0

    /** Received minus given: positive means the contact thanked you more often than you thanked them. */
    fun differenceOf(level: Byte): Int = receivedOf(level) - givenOf(level)
}
