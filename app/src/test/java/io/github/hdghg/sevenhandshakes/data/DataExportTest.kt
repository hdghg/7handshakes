package io.github.hdghg.sevenhandshakes.data

import com.google.common.truth.Truth.assertThat
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import org.junit.Test
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.io.StringWriter

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

    private fun shareYaml(contacts: List<ContactRecord>, reviews: List<ReviewRecord>): String =
        StringWriter().also { writeShareYaml(contacts, reviews, it) }.toString()

    @Test
    fun writesContactsAndReviews() {
        val yaml = shareYaml(
            contacts = listOf(
                contact(2, name = null, email = null, publicKey = "keyB"),
                contact(1, name = "Alice", email = "alice@example.com", publicKey = "keyA"),
            ),
            reviews = listOf(review(1, from = 1, to = 2, text = "Thanks", rate = 3, signature = byteArrayOf(1, 2, 3))),
        )

        assertThat(yaml).isEqualTo(
            """
            format: "share/v1"
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
        assertThat(shareYaml(emptyList(), emptyList())).isEqualTo("format: \"share/v1\"\ncontacts: []\nreviews: []\n")
    }

    @Test
    fun textIsReadBackUnchanged() {
        val texts = listOf(
            "a \"b\" \\ c\nd: #e\u0001",
            "- not a list",
            "null",
            "123",
            "  spaces around  ",
            "Tack så mycket 谢谢 🙏",
            "long ".repeat(100),
            "",
        )
        val yaml = shareYaml(
            contacts = listOf(contact(1, name = "true", email = "~", publicKey = "key")),
            reviews = texts.mapIndexed { index, text ->
                review(index + 1, from = 1, to = 1, text = text, rate = 1, signature = byteArrayOf())
            },
        )

        val document = Yaml().load<Map<String, Any?>>(yaml)
        val contacts = document["contacts"] as List<*>
        assertThat(contacts.single()).isEqualTo(mapOf("id" to 1, "name" to "true", "email" to "~", "publicKey" to "key"))
        val reviews = document["reviews"] as List<*>
        assertThat(reviews.map { (it as Map<*, *>)["message"] }).isEqualTo(texts)
    }

    @Test
    fun exportIsRecognisedAsShareFormat() {
        val yaml = shareYaml(emptyList(), emptyList())

        assertThat(readYamlFormat(StringReader(yaml))).isEqualTo(YamlFormat.SHARE_V1)
    }
}
