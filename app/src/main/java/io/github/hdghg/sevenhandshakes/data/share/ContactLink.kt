package io.github.hdghg.sevenhandshakes.data.share

import android.util.Patterns
import io.github.hdghg.sevenhandshakes.ui.newcontact.NewContactViewModel
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import java.nio.ByteBuffer

/** A contact as it travels between devices: who it is, without the marks its owner gave it. */
class SharedContact(
    /** As [Ed25519Keys.canonicalPublicKey] gives it. */
    val publicKey: String,
    val name: String?,
    val email: String?,
)

/**
 * A link that holds one contact, so it can be sent as text or shown as a QR code:
 *
 * ```
 * hs7://contact/<payload>
 * ```
 *
 * The payload (see [Hs7Link]) is these bytes:
 *
 * ```
 * 1 byte    format version, 1
 * 32 bytes  public key
 * 1 byte    length of the name in bytes, 0 for no name
 *           name, UTF-8
 * the rest  email, UTF-8; nothing for no email
 * ```
 *
 * Unlike a review, a contact is not signed: the name and email are what the sender calls the key.
 */
object ContactLink {
    const val HOST = "contact"

    private const val VERSION: Byte = 1
    private const val KEY_SIZE = 32
    private const val HEADER_SIZE = 1 + KEY_SIZE + 1

    /** True if [text] claims to be a contact link. It may still hold no valid contact; see [decode]. */
    fun isContactLink(text: String): Boolean = Hs7Link.hasHost(text, HOST)

    /**
     * Returns the link for [contact], or null if it cannot be put in one: its public key is not
     * an Ed25519 key (a contact typed in by hand may hold any text), or its name or email is
     * longer than the app lets the user type.
     */
    fun encode(contact: SharedContact): String? {
        val publicKey = Ed25519Keys.rawPublicKey(contact.publicKey) ?: return null
        val name = contact.name?.trim().orEmpty().toByteArray(Charsets.UTF_8)
        val email = contact.email?.trim().orEmpty().toByteArray(Charsets.UTF_8)
        if (name.size > NewContactViewModel.BYTES_NAME_MAX || email.size > NewContactViewModel.BYTES_EMAIL_MAX) return null
        val bytes = ByteBuffer.allocate(HEADER_SIZE + name.size + email.size)
            .put(VERSION)
            .put(publicKey)
            .put(name.size.toByte())
            .put(name)
            .put(email)
            .array()
        return Hs7Link.of(HOST, bytes)
    }

    /**
     * Returns the contact in [link], or null unless [link] is a contact link whose contact passes
     * the checks of an imported one (see [ShareImporter]): a valid public key, a name and an email
     * no longer than the app lets the user type, and an email that is an address.
     */
    fun decode(link: String, isEmailAddress: (String) -> Boolean = ::isEmailAddress): SharedContact? {
        val bytes = Hs7Link.payloadOf(link, HOST) ?: return null
        if (bytes.size < HEADER_SIZE) return null
        val buffer = ByteBuffer.wrap(bytes)
        if (buffer.get() != VERSION) return null
        val publicKey = Ed25519Keys.publicKeyOfRaw(ByteArray(KEY_SIZE).also(buffer::get)) ?: return null
        val nameSize = buffer.get().toInt() and 0xFF
        if (nameSize > NewContactViewModel.BYTES_NAME_MAX || nameSize > buffer.remaining()) return null
        val name = Hs7Link.utf8(ByteBuffer.wrap(bytes, HEADER_SIZE, nameSize)) ?: return null
        buffer.position(HEADER_SIZE + nameSize)
        if (buffer.remaining() > NewContactViewModel.BYTES_EMAIL_MAX) return null
        val email = (Hs7Link.utf8(buffer) ?: return null).trim().ifEmpty { null }
        if (email != null && !isEmailAddress(email)) return null
        return SharedContact(publicKey, name.trim().ifEmpty { null }, email)
    }
}

private fun isEmailAddress(text: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(text).matches()
