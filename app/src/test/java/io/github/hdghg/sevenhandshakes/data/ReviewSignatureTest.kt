package io.github.hdghg.sevenhandshakes.data

import com.google.common.truth.Truth.assertThat
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.junit.Test

class ReviewSignatureTest {
    private val authorPrivateKey = Ed25519Keys.generatePrivateKey()
    private val author = Ed25519Keys.publicKeyOf(authorPrivateKey)!!
    private val receiver = Ed25519Keys.publicKeyOf(Ed25519Keys.generatePrivateKey())!!
    private val stranger = Ed25519Keys.publicKeyOf(Ed25519Keys.generatePrivateKey())!!
    private val uuid = "0190a6c8-7b1e-7c3a-9d2f-1a2b3c4d5e6f"
    private val rating: Byte = 2
    private val text = "Thanks\nfor everything"

    private val signature = ReviewSignature.sign(
        uuid,
        author,
        receiver,
        rating,
        text,
        Ed25519Keys.parsePrivateKey(authorPrivateKey)!!,
    )

    @Test
    fun acceptsTheReviewAsSigned() {
        assertThat(ReviewSignature.isValid(uuid, author, receiver, rating, text, signature)).isTrue()
    }

    @Test
    fun rejectsAnyChangedValue() {
        val otherUuid = "0190a6c8-7b1e-7c3a-9d2f-1a2b3c4d5e70"
        assertThat(ReviewSignature.isValid(otherUuid, author, receiver, rating, text, signature)).isFalse()
        assertThat(ReviewSignature.isValid(uuid, author, stranger, rating, text, signature)).isFalse()
        assertThat(ReviewSignature.isValid(uuid, author, receiver, 3, text, signature)).isFalse()
        assertThat(ReviewSignature.isValid(uuid, author, receiver, rating, "Thanks", signature)).isFalse()
    }

    @Test
    fun rejectsAnotherAuthor() {
        // The stranger claims the review as their own.
        assertThat(ReviewSignature.isValid(uuid, stranger, receiver, rating, text, signature)).isFalse()
        // Author and receiver swapped.
        assertThat(ReviewSignature.isValid(uuid, receiver, author, rating, text, signature)).isFalse()
        assertThat(ReviewSignature.isValid(uuid, "not a key", receiver, rating, text, signature)).isFalse()
    }

    @Test
    fun rejectsBrokenSignatures() {
        assertThat(ReviewSignature.isValid(uuid, author, receiver, rating, text, ByteArray(0))).isFalse()
        assertThat(ReviewSignature.isValid(uuid, author, receiver, rating, text, ByteArray(64))).isFalse()
        val flipped = signature.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertThat(ReviewSignature.isValid(uuid, author, receiver, rating, text, flipped)).isFalse()
    }

    @Test
    fun rejectsASignatureOfTheTextAlone() {
        val message = text.toByteArray(Charsets.UTF_8)
        val textOnly = Ed25519Signer().run {
            init(true, Ed25519Keys.parsePrivateKey(authorPrivateKey)!!)
            update(message, 0, message.size)
            generateSignature()
        }

        assertThat(ReviewSignature.isValid(uuid, author, receiver, rating, text, textOnly)).isFalse()
    }

    @Test
    fun textCannotPoseAsOtherValues() {
        // The same lines, split differently between the rating and the text.
        val one = ReviewSignature.sign(uuid, author, receiver, 1, "2\nx", Ed25519Keys.parsePrivateKey(authorPrivateKey)!!)

        assertThat(ReviewSignature.isValid(uuid, author, receiver, 1, "2\nx", one)).isTrue()
        assertThat(ReviewSignature.isValid(uuid, author, receiver, 12, "x", one)).isFalse()
    }
}
