package io.github.hdghg.sevenhandshakes.data.share

import com.google.common.truth.Truth.assertThat
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.data.writeShareYaml
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

class ShareYamlReaderTest {
    private fun entriesOf(yaml: String): List<ShareEntry> {
        val reader = ShareYamlReader(StringReader(yaml))
        return generateSequence { reader.next() }.toList()
    }

    @Test
    fun readsWhatTheExportWrites() {
        val yaml = StringWriter().also {
            writeShareYaml(
                contacts = listOf(
                    ContactRecord(id = 1, publicKey = "keyA", name = "Alice", isFavorite = true, isIgnored = false),
                    ContactRecord(id = 2, publicKey = "keyB", name = null, isFavorite = false, isIgnored = false),
                ),
                reviews = listOf(
                    ReviewRecord(
                        id = 7,
                        uuid = "uuid-7",
                        sourceContactId = 1,
                        targetContactId = 2,
                        text = "null",
                        gratitude = 3,
                        signature = byteArrayOf(1, 2, 3),
                    ),
                ),
                writer = it,
            )
        }.toString()

        val entries = entriesOf(yaml)

        assertThat(entries.map { it.section })
            .containsExactly(ShareSection.CONTACTS, ShareSection.CONTACTS, ShareSection.REVIEWS).inOrder()
        assertThat(entries[0].fields)
            .containsExactly("id", "1", "name", "Alice", "email", null, "publicKey", "keyA")
        assertThat(entries[1].fields).containsExactly("id", "2", "name", null, "email", null, "publicKey", "keyB")
        // A quoted "null" is text, not a missing value.
        assertThat(entries[2].fields).containsExactly(
            "uuid", "uuid-7", "fromId", "1", "toId", "2", "message", "null", "rating", "3", "signature", "AQID",
        )
        assertThat(readShareSummary(StringReader(yaml))).isEqualTo(ShareSummary(contacts = 2, reviews = 1))
    }

    @Test
    fun listsInAnyOrderAndUnknownPartsSkipped() {
        val yaml = """
            format: share/v1
            comment: {nested: [1, 2, {deep: true}]}
            reviews:
              - uuid: a
                tags: [x, y]
              - just text
              - [a, list]
            later: 1
            contacts:
              - id: 5
                publicKey: key
        """.trimIndent()

        val entries = entriesOf(yaml)

        assertThat(entries.map { it.section }).containsExactly(ShareSection.REVIEWS, ShareSection.CONTACTS).inOrder()
        assertThat(entries[0].fields).containsExactly("uuid", "a")
        assertThat(entries[1].fields).containsExactly("id", "5", "publicKey", "key")
    }

    @Test
    fun emptyLists() {
        assertThat(entriesOf("format: \"share/v1\"\ncontacts: []\nreviews: []\n")).isEmpty()
        assertThat(entriesOf("format: \"share/v1\"\n")).isEmpty()
    }

    @Test
    fun otherFormatsAreRefused() {
        for (yaml in listOf(
            "format: \"request-review/v1\"\ncontacts: []\n",
            "contacts: []\nformat: \"share/v1\"\n",
            "- format: share/v1\n",
            "plain text",
            "",
        )) {
            assertThrows(ShareFileException::class.java) { entriesOf(yaml) }
        }
    }

    @Test
    fun brokenYamlIsRefused() {
        assertThrows(ShareFileException::class.java) {
            entriesOf("format: \"share/v1\"\ncontacts:\n  - id: 1\n    name: \"unclosed\n")
        }
    }

    @Test
    fun readsFilesLargerThanTheLibraryDefaultLimit() {
        // Just over the library's 3 MB default.
        val contacts = 80_000
        val yaml = buildString {
            append("format: \"share/v1\"\ncontacts:\n")
            repeat(contacts) { append("  - id: ").append(it).append("\n    publicKey: \"0123456789012345678901\"\n") }
        }
        assertThat(yaml.length).isGreaterThan(3 * 1024 * 1024)

        assertThat(readShareSummary(StringReader(yaml))).isEqualTo(ShareSummary(contacts = contacts, reviews = 0))
    }
}
