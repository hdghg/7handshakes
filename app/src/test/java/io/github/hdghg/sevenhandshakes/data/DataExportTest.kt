package io.github.hdghg.sevenhandshakes.data

import com.google.common.truth.Truth.assertThat
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import org.junit.Test

class DataExportTest {
    private fun contact(id: Int, name: String?, email: String?, publicKey: String) =
        ContactRecord(id = id, publicKey = publicKey, name = name, isFavorite = false, isIgnored = false, email = email)

    private fun review(id: Int, from: Int, to: Int, text: String, rate: Byte, signature: ByteArray) = ReviewRecord(
        id = id,
        uuid = "uuid-$id",
        sourceContactId = from,
        targetContactId = to,
        text = text,
        gratitude = rate,
        signature = signature,
    )

    @Test
    fun writesContactsAndReviews() {
        val yaml = exportToYaml(
            contacts = listOf(
                contact(2, name = null, email = null, publicKey = "keyB"),
                contact(1, name = "Alice", email = "alice@example.com", publicKey = "keyA"),
            ),
            reviews = listOf(review(1, from = 1, to = 2, text = "Thanks", rate = 3, signature = byteArrayOf(1, 2, 3))),
        )

        assertThat(yaml).isEqualTo(
            """
            contacts:
              - id: 1
                name: "Alice"
                email: "alice@example.com"
                publicKey: "keyA"
              - id: 2
                name: null
                email: null
                publicKey: "keyB"
            reviews:
              - uuid: "uuid-1"
                fromId: 1
                toId: 2
                message: "Thanks"
                rating: 3
                signature: "AQID"

            """.trimIndent(),
        )
    }

    @Test
    fun emptyTables() {
        assertThat(exportToYaml(emptyList(), emptyList())).isEqualTo("contacts: []\nreviews: []\n")
    }

    @Test
    fun escapesText() {
        val yaml = exportToYaml(
            contacts = emptyList(),
            reviews = listOf(
                review(1, from = 1, to = 2, text = "a \"b\" \\ c\nd: #e\u0001", rate = 1, signature = byteArrayOf()),
            ),
        )

        assertThat(yaml).contains("    message: \"a \\\"b\\\" \\\\ c\\nd: #e\\u0001\"\n")
        assertThat(yaml).contains("    signature: \"\"\n")
    }
}
