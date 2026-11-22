package io.github.hdghg.sevenhandshakes.data

/**
 * A chain of contacts found by the handshake search, in review direction: each contact reviewed the next.
 * [weights] has one entry per edge: how many stars the edge stands for.
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

/** Ratings between a contact and the selected identity: the heaviest path each way, null when there is none. */
data class ReviewExchange(
    /** Path from the contact to the selected identity. */
    val receivedPath: HandshakePath? = null,
    /** Path from the selected identity to the contact. */
    val givenPath: HandshakePath? = null,
) {
    val received: Int get() = receivedPath?.weight ?: 0

    val given: Int get() = givenPath?.weight ?: 0

    /** Received minus given: positive means the contact thanked you more than you thanked them. */
    val difference: Int get() = received - given
}
