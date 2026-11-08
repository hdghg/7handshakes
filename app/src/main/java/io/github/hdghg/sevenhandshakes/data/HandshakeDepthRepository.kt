package io.github.hdghg.sevenhandshakes.data

import kotlinx.coroutines.flow.Flow

/** Deepest handshake level the user can pick. */
const val MAX_HANDSHAKE_DEPTH = 7

/** Depth used until the user picks one. */
const val DEFAULT_HANDSHAKE_DEPTH = 5

/**
 * Remembers how many handshake levels the contact screen calculates: levels 1..depth.
 * The setting is shared by all contacts. 0 turns handshake ratings off.
 */
interface HandshakeDepthRepository {
    /** Emits the depth, 0..[MAX_HANDSHAKE_DEPTH]. */
    fun getHandshakeDepthStream(): Flow<Int>

    /** Values outside 0..[MAX_HANDSHAKE_DEPTH] are clamped. */
    suspend fun setHandshakeDepth(depth: Int)
}
