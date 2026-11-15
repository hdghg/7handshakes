package io.github.hdghg.sevenhandshakes.data.share

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.hdghg.sevenhandshakes.data.ReviewSignature
import io.github.hdghg.sevenhandshakes.data.YamlFormat
import io.github.hdghg.sevenhandshakes.data.cleanReviewText
import io.github.hdghg.sevenhandshakes.data.isValidReviewText
import io.github.hdghg.sevenhandshakes.data.readYamlFormat
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDatabase
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.data.writeShareYaml
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import io.github.hdghg.sevenhandshakes.util.UuidV7
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.yaml.snakeyaml.reader.UnicodeReader
import java.io.File

/**
 * Exports one database to a share file and imports the file into another, on a device.
 * This is the part the unit tests cannot cover: the real SQLite database and the device's own
 * Java runtime, which on old Android versions lacks classes a desktop JVM has.
 */
@RunWith(AndroidJUnit4::class)
class ShareRoundTripDeviceTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val source = Room.inMemoryDatabaseBuilder(context, ReviewDatabase::class.java).build()
    private val target = Room.inMemoryDatabaseBuilder(context, ReviewDatabase::class.java).build()
    private val file = File(context.cacheDir, "share-round-trip-test.yaml")

    private val alicePrivateKey = Ed25519Keys.generatePrivateKey()
    private val alice = Ed25519Keys.publicKeyOf(alicePrivateKey)!!
    private val bob = Ed25519Keys.publicKeyOf(Ed25519Keys.generatePrivateKey())!!

    @After
    fun tearDown() {
        source.close()
        target.close()
        file.delete()
    }

    private suspend fun addReview(aliceId: Int, bobId: Int, text: String, rating: Byte, tamper: Boolean = false) {
        val uuid = UuidV7.generate().toString()
        val signature = ReviewSignature.sign(uuid, alice, bob, rating, text, Ed25519Keys.parsePrivateKey(alicePrivateKey)!!)
        source.reviewDao().insert(
            ReviewRecord(
                uuid = uuid,
                sourceContactId = aliceId,
                targetContactId = bobId,
                text = text,
                // A rating changed after signing.
                gratitude = if (tamper) (rating % 3 + 1).toByte() else rating,
                signature = signature,
            ),
        )
    }

    @Test
    fun exportThenImport() = runBlocking {
        val aliceId = source.contactDao().insert(
            ContactRecord(publicKey = alice, name = "Alice", isFavorite = true, isIgnored = false, email = "alice@example.com"),
        ).toInt()
        val bobId = source.contactDao().insert(
            ContactRecord(publicKey = bob, name = null, isFavorite = false, isIgnored = false),
        ).toInt()
        addReview(aliceId, bobId, "Thanks a lot!", 3)
        addReview(aliceId, bobId, "Tack så mycket: \"谢谢\" 🙏 شكرًا", 1)
        addReview(aliceId, bobId, "Changed after signing", 2, tamper = true)

        file.bufferedWriter().use {
            writeShareYaml(source.contactDao().observeAll().first(), source.reviewDao().observeAll().first(), it)
        }

        assertEquals(YamlFormat.SHARE_V1, file.inputStream().use { readYamlFormat(UnicodeReader(it)) })
        assertEquals(
            ShareSummary(contacts = 2, reviews = 3),
            file.inputStream().use { readShareSummary(UnicodeReader(it)) },
        )

        val importer = ShareImporter(RoomShareImportStore(target, target.contactDao(), target.reviewDao()))
        val everything = setOf(ShareSection.CONTACTS, ShareSection.REVIEWS)
        val result = importer.import(everything) { UnicodeReader(file.inputStream()) }

        assertEquals(
            ShareImportResult(
                contacts = ContactsImportResult(added = 2),
                reviews = ReviewsImportResult(added = 2, rejected = 1),
            ),
            result,
        )
        val contacts = target.contactDao().observeAll().first()
        assertEquals(setOf(alice, bob), contacts.map { it.publicKey }.toSet())
        val savedAlice = contacts.single { it.publicKey == alice }
        assertEquals("Alice", savedAlice.name)
        assertEquals("alice@example.com", savedAlice.email)
        // Marks are the user's own and do not travel.
        assertFalse(savedAlice.isFavorite)
        assertNull(contacts.single { it.publicKey == bob }.name)
        val reviews = target.reviewDao().observeAll().first()
        assertEquals(setOf("Thanks a lot!", "Tack så mycket: \"谢谢\" 🙏 شكرًا"), reviews.map { it.text }.toSet())
        assertTrue(reviews.all { it.sourceContactId == savedAlice.id })

        // A second import finds everything already there.
        val again = importer.import(everything) { UnicodeReader(file.inputStream()) }
        assertEquals(
            ShareImportResult(
                contacts = ContactsImportResult(unchanged = 2),
                reviews = ReviewsImportResult(alreadyPresent = 2, rejected = 1),
            ),
            again,
        )
        assertEquals(2, target.reviewDao().observeAll().first().size)
    }

    @Test
    fun reviewTextRuleOnThisDevice() {
        assertTrue(isValidReviewText("Tack så mycket 谢谢 🙏 می‌خواهم"))
        assertFalse(isValidReviewText("two\nlines"))
        assertFalse(isValidReviewText("abc‮def"))
        assertFalse(isValidReviewText("half \uD83D pair"))
        assertEquals("two lines 🙏", cleanReviewText("two\r\nlines‮ 🙏"))
    }
}
