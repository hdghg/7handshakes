package io.github.hdghg.sevenhandshakes.data.share

import com.google.common.truth.Truth.assertThat
import io.github.hdghg.sevenhandshakes.ui.newcontact.NewContactViewModel
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import org.junit.Test

class ContactLinkTest {
    private val publicKey = Ed25519Keys.publicKeyOf(Ed25519Keys.generatePrivateKey())!!

    /** Stands in for the Android email pattern, which unit tests do not have. */
    private val isEmailAddress: (String) -> Boolean = { '@' in it }

    private fun decode(link: String) = ContactLink.decode(link, isEmailAddress)

    @Test
    fun aLinkGivesBackTheContact() {
        val decoded = decode(ContactLink.encode(SharedContact(publicKey, "Алиса 🙂", "alice@example.com"))!!)!!

        assertThat(decoded.publicKey).isEqualTo(publicKey)
        assertThat(decoded.name).isEqualTo("Алиса 🙂")
        assertThat(decoded.email).isEqualTo("alice@example.com")
    }

    @Test
    fun nameAndEmailAreOptional() {
        val unknown = decode(ContactLink.encode(SharedContact(publicKey, null, null))!!)!!
        assertThat(unknown.publicKey).isEqualTo(publicKey)
        assertThat(unknown.name).isNull()
        assertThat(unknown.email).isNull()

        val emailOnly = decode(ContactLink.encode(SharedContact(publicKey, " ", "bob@example.com"))!!)!!
        assertThat(emailOnly.name).isNull()
        assertThat(emailOnly.email).isEqualTo("bob@example.com")

        val nameOnly = decode(ContactLink.encode(SharedContact(publicKey, "Bob", ""))!!)!!
        assertThat(nameOnly.name).isEqualTo("Bob")
        assertThat(nameOnly.email).isNull()
    }

    @Test
    fun aLinkIsPlainUrlText() {
        val link = ContactLink.encode(SharedContact(publicKey, "Alice", "alice@example.com"))!!

        assertThat(link).matches("hs7://contact/[A-Za-z0-9_-]+")
        assertThat(ContactLink.isContactLink(" $link\n")).isTrue()
        assertThat(decode(" $link\n")).isNotNull()
    }

    @Test
    fun aContactWithoutAKeyHasNoLink() {
        assertThat(ContactLink.encode(SharedContact("typed by hand", "Alice", null))).isNull()
    }

    @Test
    fun aContactWithTooLongDetailsHasNoLink() {
        val longName = "n".repeat(NewContactViewModel.BYTES_NAME_MAX + 1)
        val longEmail = "e".repeat(NewContactViewModel.BYTES_EMAIL_MAX) + "@"

        assertThat(ContactLink.encode(SharedContact(publicKey, longName, null))).isNull()
        assertThat(ContactLink.encode(SharedContact(publicKey, null, longEmail))).isNull()
    }

    @Test
    fun anEmailThatIsNotAnAddressIsRejected() {
        assertThat(decode(ContactLink.encode(SharedContact(publicKey, "Alice", "not an address"))!!)).isNull()
    }

    @Test
    fun aReviewLinkIsNotAContactLink() {
        val payload = ContactLink.encode(SharedContact(publicKey, "Alice", null))!!.substringAfterLast('/')

        assertThat(ContactLink.isContactLink("hs7://review/$payload")).isFalse()
        assertThat(decode("hs7://review/$payload")).isNull()
    }

    @Test
    fun otherTextIsNotALink() {
        assertThat(decode("")).isNull()
        assertThat(decode(publicKey)).isNull()
        assertThat(decode("hs7://contact/")).isNull()
        assertThat(decode("hs7://contact/not base64 !")).isNull()
        assertThat(decode("hs7://contact/AAAA")).isNull()
    }
}
