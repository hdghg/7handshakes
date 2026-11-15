package io.github.hdghg.sevenhandshakes.data.share

import io.github.hdghg.sevenhandshakes.data.RATE_LEVELS
import io.github.hdghg.sevenhandshakes.data.ReviewSignature
import io.github.hdghg.sevenhandshakes.data.isValidReviewText
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import io.github.hdghg.sevenhandshakes.util.UuidV7
import org.bouncycastle.util.encoders.Base64
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.UUID

/** A review as it travels between devices: contacts are named by public key, not by database id. */
class SharedReview(
    /** In the standard lower-case form. */
    val uuid: String,
    /** As [Ed25519Keys.canonicalPublicKey] gives it. */
    val authorPublicKey: String,
    /** In the same form. */
    val receiverPublicKey: String,
    val rating: Byte,
    val text: String,
    val signature: ByteArray,
)

/**
 * A link that holds one whole review, so it can be sent as text and opened on another device:
 *
 * ```
 * hs7://review/<payload>
 * ```
 *
 * The payload is URL-safe Base64 without padding of these bytes:
 *
 * ```
 * 1 byte    format version, 1
 * 16 bytes  uuid
 * 32 bytes  author's public key
 * 32 bytes  receiver's public key
 * 1 byte    rating
 * 64 bytes  signature
 * the rest  text, UTF-8
 * ```
 *
 * The longest link is under 500 characters. The review carries its author's signature
 * (see [ReviewSignature]), so a link that was made up or changed on the way is not accepted.
 */
object ReviewLink {
    const val SCHEME = "hs7"
    const val HOST = "review"

    private const val PREFIX = "$SCHEME://$HOST/"
    private const val VERSION: Byte = 1
    private const val UUID_SIZE = 16
    private const val KEY_SIZE = 32
    private const val SIGNATURE_SIZE = 64
    private const val HEADER_SIZE = 1 + UUID_SIZE + 2 * KEY_SIZE + 1 + SIGNATURE_SIZE

    /**
     * Returns the link for [review], or null if it cannot be put in one: its uuid is not a uuid,
     * a public key is not an Ed25519 key (a receiver typed in by hand may be any text),
     * or the signature has the wrong length.
     */
    fun encode(review: SharedReview): String? {
        val uuid = try {
            UUID.fromString(review.uuid)
        } catch (e: IllegalArgumentException) {
            return null
        }
        val author = Ed25519Keys.rawPublicKey(review.authorPublicKey) ?: return null
        val receiver = Ed25519Keys.rawPublicKey(review.receiverPublicKey) ?: return null
        if (review.signature.size != SIGNATURE_SIZE) return null
        val text = review.text.toByteArray(Charsets.UTF_8)
        val bytes = ByteBuffer.allocate(HEADER_SIZE + text.size)
            .put(VERSION)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .put(author)
            .put(receiver)
            .put(review.rating)
            .put(review.signature)
            .put(text)
            .array()
        return PREFIX + Base64.toBase64String(bytes).trimEnd('=').replace('+', '-').replace('/', '_')
    }

    /**
     * Returns the review in [link], or null unless [link] is a review link whose review passes
     * the checks of an imported one (see [ShareImporter]): a UUIDv7, a text and a rating the app
     * allows, and the author's signature over all of it.
     */
    fun decode(link: String): SharedReview? {
        val trimmed = link.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        val payload = trimmed.substring(PREFIX.length).replace('-', '+').replace('_', '/')
        val bytes = try {
            Base64.decode(payload + "=".repeat((4 - payload.length % 4) % 4))
        } catch (e: RuntimeException) {
            return null
        }
        if (bytes.size < HEADER_SIZE) return null
        val buffer = ByteBuffer.wrap(bytes)
        if (buffer.get() != VERSION) return null
        val uuid = UUID(buffer.getLong(), buffer.getLong()).toString()
        if (UuidV7.timestampOf(uuid) == null) return null
        val author = Ed25519Keys.publicKeyOfRaw(ByteArray(KEY_SIZE).also(buffer::get)) ?: return null
        val receiver = Ed25519Keys.publicKeyOfRaw(ByteArray(KEY_SIZE).also(buffer::get)) ?: return null
        val rating = buffer.get()
        if (rating !in RATE_LEVELS) return null
        val signature = ByteArray(SIGNATURE_SIZE).also(buffer::get)
        val text = try {
            // Bytes that are not UTF-8 are refused rather than replaced.
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(buffer)
                .toString()
        } catch (e: CharacterCodingException) {
            return null
        }
        if (!isValidReviewText(text)) return null
        if (!ReviewSignature.isValid(uuid, author, receiver, rating, text, signature)) return null
        return SharedReview(uuid, author, receiver, rating, text, signature)
    }
}
