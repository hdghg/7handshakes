package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import org.bouncycastle.util.encoders.Base64

/**
 * Writes all [contacts] and [reviews] as one YAML document:
 *
 * ```yaml
 * contacts:
 *   - id: 1
 *     name: "Alice"
 *     email: null
 *     publicKey: "MCowBQYDK2VwAyEA..."
 * reviews:
 *   - uuid: "0190..."
 *     fromId: 1
 *     toId: 2
 *     message: "Thanks"
 *     rating: 3
 *     signature: "<Base64>"
 * ```
 *
 * `fromId` and `toId` are contact ids. Both lists are ordered by id.
 */
fun exportToYaml(contacts: List<ContactRecord>, reviews: List<ReviewRecord>): String = buildString {
    appendList("contacts", contacts.sortedBy { it.id }) { contact ->
        listOf(
            "id" to contact.id.toString(),
            "name" to yamlString(contact.name),
            "email" to yamlString(contact.email),
            "publicKey" to yamlString(contact.publicKey),
        )
    }
    appendList("reviews", reviews.sortedBy { it.id }) { review ->
        listOf(
            "uuid" to yamlString(review.uuid),
            "fromId" to review.sourceContactId.toString(),
            "toId" to review.targetContactId.toString(),
            "message" to yamlString(review.text),
            "rating" to review.gratitude.toString(),
            "signature" to yamlString(Base64.toBase64String(review.signature)),
        )
    }
}

private fun <T> StringBuilder.appendList(key: String, items: List<T>, fields: (T) -> List<Pair<String, String>>) {
    if (items.isEmpty()) {
        append(key).append(": []\n")
        return
    }
    append(key).append(":\n")
    for (item in items) {
        fields(item).forEachIndexed { index, (name, value) ->
            append(if (index == 0) "  - " else "    ")
            append(name).append(": ").append(value).append('\n')
        }
    }
}

/** A YAML double-quoted scalar, so any text stays on one line and is read back unchanged. */
private fun yamlString(value: String?): String {
    if (value == null) return "null"
    return buildString(value.length + 2) {
        append('"')
        for (char in value) {
            when {
                char == '"' -> append("\\\"")
                char == '\\' -> append("\\\\")
                char == '\n' -> append("\\n")
                char == '\r' -> append("\\r")
                char == '\t' -> append("\\t")
                // Control characters, and the characters YAML treats as line breaks or a byte order mark.
                char < ' ' || char in '\u007F'..'\u009F' || char == ' ' || char == ' ' || char == '﻿' ->
                    append("\\u").append(char.code.toString(16).uppercase().padStart(4, '0'))
                else -> append(char)
            }
        }
        append('"')
    }
}
