package io.github.hdghg.sevenhandshakes.data.share

import com.google.common.truth.Truth.assertThat
import io.github.hdghg.sevenhandshakes.data.REVIEW_TEXT_BYTES_MAX
import io.github.hdghg.sevenhandshakes.data.ReviewSignature
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import io.github.hdghg.sevenhandshakes.util.UuidV7
import org.junit.Test

class ReviewLinkTest {
    private val authorPrivateKey = Ed25519Keys.generatePrivateKey()
    private val author = Ed25519Keys.publicKeyOf(authorPrivateKey)!!
    private val receiver = Ed25519Keys.publicKeyOf(Ed25519Keys.generatePrivateKey())!!
    private val uuid = UuidV7.generate().toString()

    private fun review(
        text: String = "Tack för hjälpen 🙏",
        rating: Byte = 2,
        signedText: String = text,
        receiverPublicKey: String = receiver,
    ) = SharedReview(
        uuid = uuid,
        authorPublicKey = author,
        receiverPublicKey = receiverPublicKey,
        rating = rating,
        text = text,
        signature = ReviewSignature.sign(
            uuid,
            author,
            receiver,
            rating,
            signedText,
            Ed25519Keys.parsePrivateKey(authorPrivateKey)!!,
        ),
    )

    @Test
    fun aLinkGivesBackTheReview() {
        val original = review()

        val decoded = ReviewLink.decode(ReviewLink.encode(original)!!)!!

        assertThat(decoded.uuid).isEqualTo(original.uuid)
        assertThat(decoded.authorPublicKey).isEqualTo(original.authorPublicKey)
        assertThat(decoded.receiverPublicKey).isEqualTo(original.receiverPublicKey)
        assertThat(decoded.rating).isEqualTo(original.rating)
        assertThat(decoded.text).isEqualTo(original.text)
        assertThat(decoded.signature).isEqualTo(original.signature)
    }

    @Test
    fun aLinkIsPlainUrlText() {
        val link = ReviewLink.encode(review(text = "я".repeat(REVIEW_TEXT_BYTES_MAX / 2)))!!

        assertThat(link).matches("hs7://review/[A-Za-z0-9_-]+")
        assertThat(link.length).isLessThan(500)
    }

    @Test
    fun whitespaceAroundALinkIsIgnored() {
        assertThat(ReviewLink.decode("  " + ReviewLink.encode(review()) + "\n")).isNotNull()
    }

    @Test
    fun aReviewWithAReceiverThatIsNotAKeyHasNoLink() {
        assertThat(ReviewLink.encode(review(receiverPublicKey = "typed by hand"))).isNull()
    }

    @Test
    fun aChangedLinkIsRejected() {
        val link = ReviewLink.encode(review())!!
        // A character in the middle of the payload, away from the unused bits at its end.
        val index = link.length / 2
        val changed = link.replaceRange(index, index + 1, if (link[index] == 'A') "B" else "A")

        assertThat(ReviewLink.decode(changed)).isNull()
    }

    @Test
    fun aReviewSignedOverAnotherTextIsRejected() {
        val link = ReviewLink.encode(review(text = "Thanks a lot", signedText = "Thanks"))!!

        assertThat(ReviewLink.decode(link)).isNull()
    }

    @Test
    fun aTextTheAppDoesNotAllowIsRejected() {
        assertThat(ReviewLink.decode(ReviewLink.encode(review(text = "two\nlines"))!!)).isNull()
    }

    @Test
    fun aRatingOutOfRangeIsRejected() {
        assertThat(ReviewLink.decode(ReviewLink.encode(review(rating = 7))!!)).isNull()
    }

    @Test
    fun otherTextIsNotALink() {
        assertThat(ReviewLink.decode("")).isNull()
        assertThat(ReviewLink.decode("https://example.com/review/AAAA")).isNull()
        assertThat(ReviewLink.decode("hs7://review/")).isNull()
        assertThat(ReviewLink.decode("hs7://review/not base64 !")).isNull()
        assertThat(ReviewLink.decode("hs7://review/AAAA")).isNull()
    }
}
