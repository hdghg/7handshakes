package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import org.bouncycastle.util.encoders.Base64
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.emitter.Emitter
import org.yaml.snakeyaml.events.DocumentEndEvent
import org.yaml.snakeyaml.events.DocumentStartEvent
import org.yaml.snakeyaml.events.ImplicitTuple
import org.yaml.snakeyaml.events.MappingEndEvent
import org.yaml.snakeyaml.events.MappingStartEvent
import org.yaml.snakeyaml.events.ScalarEvent
import org.yaml.snakeyaml.events.SequenceEndEvent
import org.yaml.snakeyaml.events.SequenceStartEvent
import org.yaml.snakeyaml.events.StreamEndEvent
import org.yaml.snakeyaml.events.StreamStartEvent
import java.io.Writer

/**
 * Writes all [contacts] and [reviews] to [writer] as one YAML document in the [YamlFormat.SHARE_V1] format:
 *
 * ```yaml
 * format: "share/v1"
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
 *
 * The text goes to [writer] piece by piece, so the whole document is never held in memory.
 * The caller closes [writer].
 */
fun writeShareYaml(contacts: List<ContactRecord>, reviews: List<ReviewRecord>, writer: Writer) {
    val yaml = YamlEventWriter(writer)
    yaml.document {
        string(YAML_FORMAT_KEY, YamlFormat.SHARE_V1.id)
        list("contacts", contacts.sortedBy { it.id }) { contact ->
            number("id", contact.id)
            string("name", contact.name)
            string("email", contact.email)
            string("publicKey", contact.publicKey)
        }
        list("reviews", reviews.sortedBy { it.id }) { review ->
            string("uuid", review.uuid)
            number("fromId", review.sourceContactId)
            number("toId", review.targetContactId)
            string("message", review.text)
            number("rating", review.gratitude.toInt())
            string("signature", Base64.toBase64String(review.signature))
        }
    }
}

/** Writes a YAML document of mappings, lists of mappings and scalars, one event at a time. */
private class YamlEventWriter(writer: Writer) {
    private val emitter = Emitter(
        writer,
        DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            // List items are indented under their key.
            indent = 2
            indicatorIndent = 2
            indentWithIndicator = true
            // Long text stays on one line.
            splitLines = false
        },
    )

    /** Writes a document whose root is a mapping filled by [content]. */
    fun document(content: YamlEventWriter.() -> Unit) {
        emitter.emit(StreamStartEvent(null, null))
        emitter.emit(DocumentStartEvent(null, null, false, null, null))
        mapping(content)
        emitter.emit(DocumentEndEvent(null, null, false))
        emitter.emit(StreamEndEvent(null, null))
    }

    /** Writes [key] with a list of mappings, each filled by [item]. */
    fun <T> list(key: String, items: List<T>, item: YamlEventWriter.(T) -> Unit) {
        plain(key)
        emitter.emit(SequenceStartEvent(null, null, true, null, null, DumperOptions.FlowStyle.BLOCK))
        for (element in items) mapping { item(element) }
        emitter.emit(SequenceEndEvent(null, null))
    }

    fun number(key: String, value: Int) {
        plain(key)
        plain(value.toString())
    }

    /** Text is always quoted, so it is never read back as a number, a boolean or null. */
    fun string(key: String, value: String?) {
        plain(key)
        if (value == null) {
            plain("null")
        } else {
            emitter.emit(
                ScalarEvent(null, null, QUOTED, value, null, null, DumperOptions.ScalarStyle.DOUBLE_QUOTED),
            )
        }
    }

    private fun mapping(content: YamlEventWriter.() -> Unit) {
        emitter.emit(MappingStartEvent(null, null, true, null, null, DumperOptions.FlowStyle.BLOCK))
        content()
        emitter.emit(MappingEndEvent(null, null))
    }

    private fun plain(value: String) {
        emitter.emit(ScalarEvent(null, null, PLAIN, value, null, null, DumperOptions.ScalarStyle.PLAIN))
    }

    private companion object {
        // Neither kind of scalar needs an explicit tag.
        val PLAIN = ImplicitTuple(true, false)
        val QUOTED = ImplicitTuple(false, true)
    }
}
