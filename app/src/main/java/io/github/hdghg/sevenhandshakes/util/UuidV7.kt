package io.github.hdghg.sevenhandshakes.util

import java.security.SecureRandom
import java.util.UUID

/**
 * UUID version 7 (RFC 9562): 48-bit Unix time in milliseconds, then random bits.
 * Values made later sort after earlier ones (to the millisecond).
 */
object UuidV7 {
    private val random = SecureRandom()

    fun generate(timeMillis: Long = System.currentTimeMillis()): UUID {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        for (i in 0 until 6) {
            bytes[i] = (timeMillis ushr (40 - 8 * i)).toByte()
        }
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte() // version 7
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte() // RFC 9562 variant
        var msb = 0L
        var lsb = 0L
        for (i in 0 until 8) msb = (msb shl 8) or (bytes[i].toLong() and 0xFF)
        for (i in 8 until 16) lsb = (lsb shl 8) or (bytes[i].toLong() and 0xFF)
        return UUID(msb, lsb)
    }

    /** Creation time in Unix milliseconds, or null if [uuid] is not a version 7 UUID. */
    fun timestampOf(uuid: String): Long? {
        val parsed = try {
            UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (parsed.version() != 7) return null
        return parsed.mostSignificantBits ushr 16
    }
}
