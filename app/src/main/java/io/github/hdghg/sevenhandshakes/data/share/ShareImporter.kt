package io.github.hdghg.sevenhandshakes.data.share

import android.util.Patterns
import io.github.hdghg.sevenhandshakes.data.RATE_LEVELS
import io.github.hdghg.sevenhandshakes.data.ReviewSignature
import io.github.hdghg.sevenhandshakes.data.isValidReviewText
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.ui.newcontact.NewContactViewModel
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import io.github.hdghg.sevenhandshakes.util.UuidV7
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.bouncycastle.util.encoders.Base64
import timber.log.Timber
import java.io.Reader
import java.util.UUID

/** What happened to the contacts of an imported file. */
data class ContactsImportResult(
    /** New contacts. */
    val added: Int = 0,
    /** Contacts the app already had, which got a name or an email they lacked. */
    val updated: Int = 0,
    /** Contacts the app already had, left as they were. */
    val unchanged: Int = 0,
    /** Contacts that break a rule of the app; see [ShareImporter]. */
    val rejected: Int = 0,
)

/** What happened to the reviews of an imported file. */
data class ReviewsImportResult(
    val added: Int = 0,
    /** Reviews the app already had. */
    val alreadyPresent: Int = 0,
    /** Reviews with a wrong signature, or that break another rule of the app; see [ShareImporter]. */
    val rejected: Int = 0,
)

/** A part is null when it was not imported. */
data class ShareImportResult(
    val contacts: ContactsImportResult? = null,
    val reviews: ReviewsImportResult? = null,
)

/**
 * Imports a [io.github.hdghg.sevenhandshakes.data.YamlFormat.SHARE_V1] file into the database.
 *
 * The file is read entry by entry and saved in small batches, so its size does not matter.
 * If the import stops half way, the batches saved so far stay; importing the file again is safe.
 *
 * Contacts are matched by public key, since the ids in the file mean nothing outside it.
 * A contact is rejected unless:
 * - its public key is a valid Ed25519 key,
 * - its name and email are no longer than the app lets the user type, and the email is an address.
 *
 * A contact the app already has keeps its own name, email, favorite and ignored marks;
 * the file only fills in a name or email that is missing.
 *
 * A review is rejected unless:
 * - its uuid is a UUIDv7,
 * - its author and receiver are contacts of the file with valid public keys,
 * - its text is one the app lets the user type; see [isValidReviewText],
 * - its rating is one of [RATE_LEVELS],
 * - its signature is the author's and covers the uuid, both public keys, the rating and the text
 *   exactly as the file gives them; see [ReviewSignature].
 *
 * An author or receiver the app does not know yet is added as a contact without a name.
 * A review the app already has (same uuid and author) is left alone.
 */
class ShareImporter internal constructor(
    private val store: ShareImportStore,
    private val isEmailAddress: (String) -> Boolean,
) {
    constructor(store: RoomShareImportStore) : this(store, { Patterns.EMAIL_ADDRESS.matcher(it).matches() })

    /**
     * Imports the lists named in [sections]. [open] opens the file from its start; it is called
     * once or twice, and each reader is closed here.
     *
     * @throws ShareFileException if the file is not a share file. Batches saved before the bad spot stay.
     */
    suspend fun import(sections: Set<ShareSection>, open: () -> Reader): ShareImportResult {
        val withContacts = ShareSection.CONTACTS in sections
        val withReviews = ShareSection.REVIEWS in sections

        // First pass: the contacts. Reviews name contacts by file id, so their keys are needed either way.
        val publicKeyByFileId = HashMap<Int, String>()
        var contacts = ContactsImportResult()
        val contactBatch = ArrayList<ContactToImport>(BATCH_SIZE)
        forEachEntry(open, ShareSection.CONTACTS) { fields ->
            val contact = checkContact(fields)
            val fileId = fields["id"]?.trim()?.toIntOrNull()
            val publicKey = contact?.publicKey ?: fields["publicKey"]?.let(Ed25519Keys::canonicalPublicKey)
            if (fileId != null && publicKey != null && fileId !in publicKeyByFileId) {
                publicKeyByFileId[fileId] = publicKey
            }
            if (!withContacts) return@forEachEntry
            if (contact == null) {
                contacts = contacts.copy(rejected = contacts.rejected + 1)
            } else {
                contactBatch += contact
                if (contactBatch.size == BATCH_SIZE) contacts = saveContacts(contactBatch, contacts)
            }
        }
        contacts = saveContacts(contactBatch, contacts)
        if (!withReviews) return ShareImportResult(contacts = contacts)

        // Second pass: the reviews.
        var reviews = ReviewsImportResult()
        val reviewBatch = ArrayList<ReviewToImport>(BATCH_SIZE)
        val contactIds = HashMap<String, Int>()
        forEachEntry(open, ShareSection.REVIEWS) { fields ->
            val review = checkReview(fields, publicKeyByFileId)
            if (review == null) {
                reviews = reviews.copy(rejected = reviews.rejected + 1)
            } else {
                reviewBatch += review
                if (reviewBatch.size == BATCH_SIZE) reviews = saveReviews(reviewBatch, contactIds, reviews)
            }
        }
        reviews = saveReviews(reviewBatch, contactIds, reviews)
        return ShareImportResult(contacts = contacts.takeIf { withContacts }, reviews = reviews)
    }

    private suspend fun forEachEntry(
        open: () -> Reader,
        section: ShareSection,
        action: suspend (Map<String, String?>) -> Unit,
    ) {
        open().use { reader ->
            val yaml = ShareYamlReader(reader)
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = yaml.next() ?: break
                if (entry.section == section) action(entry.fields)
            }
        }
    }

    private class ContactToImport(val publicKey: String, val name: String?, val email: String?)

    private fun checkContact(fields: Map<String, String?>): ContactToImport? {
        val publicKey = fields["publicKey"]?.let(Ed25519Keys::canonicalPublicKey) ?: return null
        val name = fields["name"]?.trim()?.ifEmpty { null }
        if (name != null && name.toByteArray().size > NewContactViewModel.BYTES_NAME_MAX) return null
        val email = fields["email"]?.trim()?.ifEmpty { null }
        if (email != null && (email.toByteArray().size > NewContactViewModel.BYTES_EMAIL_MAX || !isEmailAddress(email))) {
            return null
        }
        return ContactToImport(publicKey, name, email)
    }

    /** Saves and empties [batch]. Returns [result] with the batch counted in. */
    private suspend fun saveContacts(
        batch: MutableList<ContactToImport>,
        result: ContactsImportResult,
    ): ContactsImportResult {
        if (batch.isEmpty()) return result
        var added = 0
        var updated = 0
        var unchanged = 0
        store.transaction {
            for (contact in batch) {
                val existing = store.contactByPublicKey(contact.publicKey)
                if (existing == null) {
                    store.insertContact(contact.publicKey, contact.name, contact.email)
                    added++
                    continue
                }
                val name = existing.name?.takeIf { it.isNotBlank() } ?: contact.name
                val email = existing.email?.takeIf { it.isNotBlank() } ?: contact.email
                if (name != existing.name || email != existing.email) {
                    store.updateContactDetails(existing, name, email)
                    updated++
                } else {
                    unchanged++
                }
            }
        }
        batch.clear()
        return result.copy(
            added = result.added + added,
            updated = result.updated + updated,
            unchanged = result.unchanged + unchanged,
        )
    }

    private class ReviewToImport(
        val uuid: String,
        val authorPublicKey: String,
        val receiverPublicKey: String,
        val text: String,
        val rating: Byte,
        val signature: ByteArray,
    )

    private fun checkReview(
        fields: Map<String, String?>,
        publicKeyByFileId: Map<Int, String>,
    ): ReviewToImport? {
        fun reject(reason: String): ReviewToImport? {
            Timber.w("Review %s rejected: %s", fields["uuid"], reason)
            return null
        }

        val writtenUuid = fields["uuid"]?.trim().orEmpty()
        if (UuidV7.timestampOf(writtenUuid) == null) return reject("uuid is not a UUIDv7")
        // One text value per uuid, so a review the app already has is recognised whatever the letter case.
        val uuid = UUID.fromString(writtenUuid).toString()
        val authorPublicKey = fields["fromId"]?.trim()?.toIntOrNull()?.let(publicKeyByFileId::get)
            ?: return reject("unknown author")
        val receiverPublicKey = fields["toId"]?.trim()?.toIntOrNull()?.let(publicKeyByFileId::get)
            ?: return reject("unknown receiver")
        val text = fields["message"] ?: return reject("no message")
        if (!isValidReviewText(text)) return reject("message too long or holds a forbidden character")
        val rating = fields["rating"]?.trim()?.toIntOrNull()?.takeIf { it in Byte.MIN_VALUE..Byte.MAX_VALUE }?.toByte()
        if (rating == null || rating !in RATE_LEVELS) return reject("rating out of range")
        val signature = try {
            Base64.decode(fields["signature"].orEmpty())
        } catch (e: RuntimeException) {
            return reject("signature is not Base64")
        }
        if (!ReviewSignature.isValid(uuid, authorPublicKey, receiverPublicKey, rating, text, signature)) {
            return reject("wrong signature")
        }
        return ReviewToImport(uuid, authorPublicKey, receiverPublicKey, text, rating, signature)
    }

    /**
     * Saves and empties [batch]. Returns [result] with the batch counted in.
     * [contactIds] remembers the database id of each public key seen so far.
     */
    private suspend fun saveReviews(
        batch: MutableList<ReviewToImport>,
        contactIds: MutableMap<String, Int>,
        result: ReviewsImportResult,
    ): ReviewsImportResult {
        if (batch.isEmpty()) return result
        var added = 0
        var alreadyPresent = 0
        // Ids of contacts made in this batch are only kept once the batch is saved.
        val batchContactIds = HashMap<String, Int>()
        suspend fun contactIdOf(publicKey: String): Int = contactIds[publicKey] ?: batchContactIds.getOrPut(publicKey) {
            store.contactByPublicKey(publicKey)?.id ?: store.insertContact(publicKey, name = null, email = null)
        }
        store.transaction {
            for (review in batch) {
                val record = ReviewRecord(
                    uuid = review.uuid,
                    sourceContactId = contactIdOf(review.authorPublicKey),
                    targetContactId = contactIdOf(review.receiverPublicKey),
                    text = review.text,
                    gratitude = review.rating,
                    signature = review.signature,
                )
                if (store.insertReviewIfAbsent(record)) added++ else alreadyPresent++
            }
        }
        contactIds += batchContactIds
        batch.clear()
        return result.copy(added = result.added + added, alreadyPresent = result.alreadyPresent + alreadyPresent)
    }

    private companion object {
        /** Entries saved per database transaction. */
        const val BATCH_SIZE = 200
    }
}
