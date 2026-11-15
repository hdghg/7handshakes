package io.github.hdghg.sevenhandshakes.data.share

import com.google.common.truth.Truth.assertThat
import io.github.hdghg.sevenhandshakes.data.ReviewSignature
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.data.writeShareYaml
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import io.github.hdghg.sevenhandshakes.util.UuidV7
import kotlinx.coroutines.test.runTest
import org.bouncycastle.util.encoders.Base64
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

class ShareImporterTest {
    /** The database, in memory. */
    private class FakeStore : ShareImportStore {
        val contacts = ArrayList<ContactRecord>()
        val reviews = ArrayList<ReviewRecord>()

        override suspend fun <T> transaction(block: suspend () -> T): T = block()

        override suspend fun contactByPublicKey(publicKey: String) = contacts.firstOrNull { it.publicKey == publicKey }

        override suspend fun insertContact(publicKey: String, name: String?, email: String?): Int {
            check(contacts.none { it.publicKey == publicKey }) { "duplicate public key" }
            val id = contacts.size + 100
            contacts += ContactRecord(id, publicKey, name, isFavorite = false, isIgnored = false, email = email)
            return id
        }

        override suspend fun updateContactDetails(contact: ContactRecord, name: String?, email: String?) {
            contacts[contacts.indexOfFirst { it.id == contact.id }] =
                ContactRecord(contact.id, contact.publicKey, name, contact.isFavorite, contact.isIgnored, email)
        }

        override suspend fun insertReviewIfAbsent(review: ReviewRecord): Boolean {
            if (reviews.any { it.uuid == review.uuid && it.sourceContactId == review.sourceContactId }) return false
            reviews += review
            return true
        }

        fun contact(publicKey: String) = contacts.single { it.publicKey == publicKey }
    }

    private class Person(val fileId: Int, val name: String?) {
        val privateKey = Ed25519Keys.generatePrivateKey()
        val publicKey = Ed25519Keys.publicKeyOf(privateKey)!!

        /** This person's signature of a review they give to [to]. */
        fun sign(uuid: String, to: Person, rating: Byte, text: String): String = Base64.toBase64String(
            ReviewSignature.sign(uuid, publicKey, to.publicKey, rating, text, Ed25519Keys.parsePrivateKey(privateKey)!!),
        )
    }

    private val alice = Person(1, "Alice")
    private val bob = Person(2, "Bob")
    private val store = FakeStore()
    private val importer = ShareImporter(store) { it.contains('@') }

    private val all = setOf(ShareSection.CONTACTS, ShareSection.REVIEWS)

    private fun contactYaml(person: Person, name: String? = person.name, email: String? = null, key: String = person.publicKey) =
        "  - id: ${person.fileId}\n" +
            "    name: ${name?.let { "\"$it\"" }}\n" +
            "    email: ${email?.let { "\"$it\"" }}\n" +
            "    publicKey: \"$key\"\n"

    private fun reviewYaml(
        from: Person = alice,
        to: Person = bob,
        text: String = "Thanks",
        uuid: String = UuidV7.generate().toString(),
        rating: String = "2",
        fromId: Int = from.fileId,
        // By default the review is signed as written. A test changes one of these to break the signature.
        signature: String = from.sign(uuid, to, rating.toByteOrNull() ?: 0, text),
    ) = "  - uuid: \"$uuid\"\n" +
        "    fromId: $fromId\n" +
        "    toId: ${to.fileId}\n" +
        "    message: \"$text\"\n" +
        "    rating: $rating\n" +
        "    signature: \"$signature\"\n"

    private fun file(contacts: String, reviews: String = "") =
        "format: \"share/v1\"\ncontacts:\n$contacts" + if (reviews.isEmpty()) "reviews: []\n" else "reviews:\n$reviews"

    private suspend fun import(yaml: String, sections: Set<ShareSection> = all) =
        importer.import(sections) { StringReader(yaml) }

    @Test
    fun importsContacts() = runTest {
        val yaml = file(contactYaml(alice, email = "alice@example.com") + contactYaml(bob, name = null))

        val result = import(yaml, setOf(ShareSection.CONTACTS))

        assertThat(result).isEqualTo(ShareImportResult(contacts = ContactsImportResult(added = 2)))
        assertThat(store.contact(alice.publicKey).name).isEqualTo("Alice")
        assertThat(store.contact(alice.publicKey).email).isEqualTo("alice@example.com")
        assertThat(store.contact(bob.publicKey).name).isNull()
        assertThat(store.reviews).isEmpty()
    }

    @Test
    fun knownContactKeepsItsDetailsAndGetsMissingOnes() = runTest {
        store.contacts += ContactRecord(1, alice.publicKey, "My Alice", isFavorite = true, isIgnored = true)
        store.contacts += ContactRecord(2, bob.publicKey, "Bob", isFavorite = false, isIgnored = false)
        val yaml = file(contactYaml(alice, email = "alice@example.com") + contactYaml(bob))

        val result = import(yaml, setOf(ShareSection.CONTACTS))

        assertThat(result.contacts).isEqualTo(ContactsImportResult(updated = 1, unchanged = 1))
        val saved = store.contact(alice.publicKey)
        assertThat(saved.name).isEqualTo("My Alice")
        assertThat(saved.email).isEqualTo("alice@example.com")
        assertThat(saved.isFavorite).isTrue()
        assertThat(saved.isIgnored).isTrue()
    }

    @Test
    fun rejectsContactsThatBreakTheRules() = runTest {
        val carol = Person(3, "Carol")
        val dave = Person(4, "Dave")
        val yaml = file(
            contactYaml(alice, key = "not a key") +
                contactYaml(bob, name = "x".repeat(65)) +
                contactYaml(carol, email = "not an address") +
                contactYaml(dave, key = "  ${dave.publicKey}\n"),
        )

        val result = import(yaml, setOf(ShareSection.CONTACTS))

        assertThat(result.contacts).isEqualTo(ContactsImportResult(added = 1, rejected = 3))
        assertThat(store.contacts.map { it.publicKey }).containsExactly(dave.publicKey)
    }

    @Test
    fun importsReviewsAndAddsTheirContactsWithoutNames() = runTest {
        val uuid = UuidV7.generate().toString()
        val yaml = file(contactYaml(alice) + contactYaml(bob), reviewYaml(text = "Tack så mycket", uuid = uuid, rating = "3"))

        val result = import(yaml, setOf(ShareSection.REVIEWS))

        assertThat(result).isEqualTo(ShareImportResult(reviews = ReviewsImportResult(added = 1)))
        val review = store.reviews.single()
        assertThat(review.uuid).isEqualTo(uuid)
        assertThat(review.text).isEqualTo("Tack så mycket")
        assertThat(review.gratitude).isEqualTo(3)
        assertThat(review.sourceContactId).isEqualTo(store.contact(alice.publicKey).id)
        assertThat(review.targetContactId).isEqualTo(store.contact(bob.publicKey).id)
        assertThat(store.contacts.map { it.name }).containsExactly(null, null)
    }

    @Test
    fun importsEverything() = runTest {
        val yaml = file(contactYaml(alice) + contactYaml(bob), reviewYaml() + reviewYaml(from = bob, to = alice))

        val result = import(yaml)

        assertThat(result).isEqualTo(
            ShareImportResult(contacts = ContactsImportResult(added = 2), reviews = ReviewsImportResult(added = 2)),
        )
        assertThat(store.contacts.map { it.name }).containsExactly("Alice", "Bob")
    }

    @Test
    fun importingTwiceAddsNothing() = runTest {
        val yaml = file(contactYaml(alice) + contactYaml(bob), reviewYaml() + reviewYaml())
        import(yaml)

        val result = import(yaml)

        assertThat(result).isEqualTo(
            ShareImportResult(
                contacts = ContactsImportResult(unchanged = 2),
                reviews = ReviewsImportResult(alreadyPresent = 2),
            ),
        )
        assertThat(store.contacts).hasSize(2)
        assertThat(store.reviews).hasSize(2)
    }

    @Test
    fun rejectsReviewsWithAWrongSignature() = runTest {
        val carol = Person(3, "Carol")
        val uuid = UuidV7.generate().toString()
        val otherUuid = UuidV7.generate().toString()
        val signed = alice.sign(uuid, bob, 2, "Thanks")
        val yaml = file(
            contactYaml(alice) + contactYaml(bob) + contactYaml(carol),
            // Signed by someone other than the author the review names.
            reviewYaml(uuid = uuid, signature = bob.sign(uuid, bob, 2, "Thanks")) +
                // Each value changed after signing, one at a time.
                reviewYaml(uuid = uuid, text = "Thanks a lot", signature = signed) +
                reviewYaml(uuid = uuid, rating = "3", signature = signed) +
                reviewYaml(uuid = uuid, to = carol, signature = signed) +
                reviewYaml(uuid = otherUuid, signature = signed) +
                // Another contact passed off as the author.
                reviewYaml(uuid = uuid, from = carol, signature = signed) +
                reviewYaml(signature = "") +
                reviewYaml(signature = "!!! not base64 !!!") +
                reviewYaml(signature = "AQID"),
        )

        val result = import(yaml)

        assertThat(result.reviews).isEqualTo(ReviewsImportResult(rejected = 9))
        assertThat(store.reviews).isEmpty()
    }

    @Test
    fun rejectsReviewsThatBreakTheRules() = runTest {
        val longText = "x".repeat(201)
        val lineFeedUuid = UuidV7.generate().toString()
        val yaml = file(
            contactYaml(alice) + contactYaml(bob, key = "not a key"),
            reviewYaml(uuid = "not-a-uuid") +
                // A valid UUID, but version 4.
                reviewYaml(uuid = "2f1e4c5a-9a3b-4c6d-8e7f-0123456789ab") +
                reviewYaml(fromId = 99) +
                // The receiver's public key is not valid.
                reviewYaml(to = bob) +
                reviewYaml(to = alice, text = longText) +
                // Correctly signed, but the text holds a line feed (written \n in YAML) or a direction override.
                reviewYaml(to = alice, text = "two\\nlines", signature = alice.sign(lineFeedUuid, alice, 2, "two\nlines"), uuid = lineFeedUuid) +
                reviewYaml(to = alice, text = "abc‮def") +
                reviewYaml(to = alice, rating = "0") +
                reviewYaml(to = alice, rating = "4") +
                reviewYaml(to = alice, rating = "many") +
                reviewYaml(to = alice),
        )

        val result = import(yaml)

        assertThat(result.reviews).isEqualTo(ReviewsImportResult(added = 1, rejected = 10))
        assertThat(result.contacts).isEqualTo(ContactsImportResult(added = 1, rejected = 1))
    }

    @Test
    fun readsWhatTheExportWrites() = runTest {
        val text = "One: \"two\" \\ #three 🙏"
        val uuid = UuidV7.generate().toString()
        val yaml = StringWriter().also {
            writeShareYaml(
                contacts = listOf(
                    ContactRecord(7, alice.publicKey, "Alice", isFavorite = false, isIgnored = false),
                    ContactRecord(9, bob.publicKey, null, isFavorite = false, isIgnored = false),
                ),
                reviews = listOf(
                    ReviewRecord(
                        id = 1,
                        uuid = uuid,
                        sourceContactId = 7,
                        targetContactId = 9,
                        text = text,
                        gratitude = 1,
                        signature = Base64.decode(alice.sign(uuid, bob, 1, text)),
                    ),
                ),
                writer = it,
            )
        }.toString()

        val result = import(yaml)

        assertThat(result).isEqualTo(
            ShareImportResult(contacts = ContactsImportResult(added = 2), reviews = ReviewsImportResult(added = 1)),
        )
        assertThat(store.reviews.single().text).isEqualTo(text)
    }

    @Test
    fun acceptsAnUppercaseUuidAndAKeyWithSpaces() = runTest {
        // Signed over the standard forms; the file writes the same values differently.
        val uuid = UuidV7.generate().toString()
        val yaml = file(
            contactYaml(alice, key = " ${alice.publicKey} ") + contactYaml(bob),
            reviewYaml(uuid = uuid.uppercase(), signature = alice.sign(uuid, bob, 2, "Thanks")),
        )

        val result = import(yaml)

        assertThat(result.reviews).isEqualTo(ReviewsImportResult(added = 1))
        assertThat(store.reviews.single().uuid).isEqualTo(uuid)
    }

    @Test
    fun importsMoreEntriesThanOneBatch() = runTest {
        val reviews = buildString { repeat(450) { append(reviewYaml()) } }

        val result = import(file(contactYaml(alice) + contactYaml(bob), reviews))

        assertThat(result.reviews).isEqualTo(ReviewsImportResult(added = 450))
        assertThat(store.reviews).hasSize(450)
        assertThat(store.contacts).hasSize(2)
    }

    @Test
    fun refusesAFileOfAnotherFormat() {
        assertThrows(ShareFileException::class.java) {
            kotlinx.coroutines.runBlocking { import("format: \"request-review/v1\"\n") }
        }
        assertThat(store.contacts).isEmpty()
    }
}
