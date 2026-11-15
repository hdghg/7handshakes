package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

/**
 * The author's Ed25519 signature of a review.
 *
 * It covers everything a review says: who gave it, to whom, when (the uuid), the rating and the text.
 * So a review can be checked on its own, with nothing but the review itself: the signature only
 * holds if the review was signed with the private key of the author it names, and nothing in it changed since.
 *
 * The signed bytes are these lines as UTF-8, joined by a line feed:
 *
 * ```
 * sevenhandshakes-review-v1
 * <uuid>
 * <author's public key>
 * <receiver's public key>
 * <rating, as a decimal number>
 * <text>
 * ```
 *
 * The text comes last, so even a text with line feeds, which the app does not accept
 * (see [isValidReviewText]), could not make two different reviews give the same bytes. The first line keeps the signature from being valid for anything else.
 *
 * Signing and verifying live together so they cannot drift apart.
 */
object ReviewSignature {
    private const val HEADER = "sevenhandshakes-review-v1"

    /**
     * @param uuid the review's uuid in its standard lower-case form.
     * @param authorPublicKey the public key of [authorPrivateKey], as [Ed25519Keys.canonicalPublicKey] gives it.
     * @param receiverPublicKey the receiver's public key, in the same form.
     */
    fun sign(
        uuid: String,
        authorPublicKey: String,
        receiverPublicKey: String,
        rating: Byte,
        text: String,
        authorPrivateKey: Ed25519PrivateKeyParameters,
    ): ByteArray {
        val message = signedBytes(uuid, authorPublicKey, receiverPublicKey, rating, text)
        val signer = Ed25519Signer()
        signer.init(true, authorPrivateKey)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    /**
     * True if [signature] was made over exactly these values with the private key of [authorPublicKey].
     * The values must be in the form described at [sign].
     */
    fun isValid(
        uuid: String,
        authorPublicKey: String,
        receiverPublicKey: String,
        rating: Byte,
        text: String,
        signature: ByteArray,
    ): Boolean {
        val authorKey = Ed25519Keys.parsePublicKey(authorPublicKey) ?: return false
        val message = signedBytes(uuid, authorPublicKey, receiverPublicKey, rating, text)
        val verifier = Ed25519Signer()
        verifier.init(false, authorKey)
        verifier.update(message, 0, message.size)
        return try {
            verifier.verifySignature(signature)
        } catch (e: RuntimeException) {
            // A signature of the wrong length, for one.
            false
        }
    }

    private fun signedBytes(
        uuid: String,
        authorPublicKey: String,
        receiverPublicKey: String,
        rating: Byte,
        text: String,
    ): ByteArray {
        // A line feed inside one of these would let two different reviews give the same bytes.
        for (line in listOf(uuid, authorPublicKey, receiverPublicKey)) {
            require('\n' !in line) { "A signed value holds a line feed" }
        }
        return listOf(HEADER, uuid, authorPublicKey, receiverPublicKey, rating.toString(), text)
            .joinToString("\n")
            .toByteArray(Charsets.UTF_8)
    }
}
