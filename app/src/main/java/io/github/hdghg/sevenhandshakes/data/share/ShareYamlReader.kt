package io.github.hdghg.sevenhandshakes.data.share

import io.github.hdghg.sevenhandshakes.data.YAML_FORMAT_KEY
import io.github.hdghg.sevenhandshakes.data.YamlFormat
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.events.CollectionEndEvent
import org.yaml.snakeyaml.events.CollectionStartEvent
import org.yaml.snakeyaml.events.DocumentStartEvent
import org.yaml.snakeyaml.events.Event
import org.yaml.snakeyaml.events.MappingEndEvent
import org.yaml.snakeyaml.events.MappingStartEvent
import org.yaml.snakeyaml.events.ScalarEvent
import org.yaml.snakeyaml.events.SequenceEndEvent
import org.yaml.snakeyaml.events.SequenceStartEvent
import org.yaml.snakeyaml.events.StreamStartEvent
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import java.io.Reader

/** The file is not a readable [YamlFormat.SHARE_V1] file. */
class ShareFileException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The lists of a [YamlFormat.SHARE_V1] file. */
enum class ShareSection(val key: String) {
    CONTACTS("contacts"),
    REVIEWS("reviews"),
}

/**
 * One item of a list in a share file, as written: nothing is checked yet.
 * [fields] maps a field name to its text, or to null for a YAML null.
 */
class ShareEntry(val section: ShareSection, val fields: Map<String, String?>)

/** How many entries a share file has. */
data class ShareSummary(val contacts: Int, val reviews: Int)

/**
 * Reads a [YamlFormat.SHARE_V1] file one entry at a time, so a file of any size needs little memory.
 *
 * The lists may come in any order. Keys and list items the format does not know are skipped,
 * so a later version may add to the format.
 */
class ShareYamlReader(reader: Reader) {
    private val parser = ParserImpl(
        StreamReader(reader),
        // The library refuses input over 3 MB unless told otherwise.
        LoaderOptions().apply { codePointLimit = Int.MAX_VALUE },
    )
    private var headerRead = false
    private var finished = false

    /** The list being read, or null between lists. */
    private var section: ShareSection? = null

    /**
     * Returns the next entry, or null at the end of the file.
     * @throws ShareFileException if the text is not YAML or not a share file.
     */
    fun next(): ShareEntry? = try {
        nextEntry()
    } catch (e: YAMLException) {
        throw ShareFileException("The file is not valid YAML", e)
    } catch (e: NoSuchElementException) {
        throw ShareFileException("The file ends too early", e)
    }

    private fun nextEntry(): ShareEntry? {
        if (finished) return null
        if (!headerRead) {
            readHeader()
            headerRead = true
        }
        while (true) {
            val event = parser.event
            val current = section
            if (current != null) {
                when (event) {
                    is SequenceEndEvent -> section = null
                    is MappingStartEvent -> return ShareEntry(current, readFields())
                    else -> skipNode(event)
                }
            } else if (event is MappingEndEvent) {
                finished = true
                return null
            } else {
                // A key of the root mapping.
                val list = (event as? ScalarEvent)?.let { key -> ShareSection.entries.firstOrNull { it.key == key.value } }
                if (list != null && parser.peekEvent() is SequenceStartEvent) {
                    parser.event
                    section = list
                } else {
                    skipNode(event)
                    skipNode(parser.event)
                }
            }
        }
    }

    /** The file must start with `format: "share/v1"`. */
    private fun readHeader() {
        val isRootMapping = parser.event is StreamStartEvent &&
            parser.event is DocumentStartEvent &&
            parser.event is MappingStartEvent
        if (!isRootMapping) throw ShareFileException("The file is not a YAML mapping")
        val key = parser.event as? ScalarEvent
        val value = parser.event as? ScalarEvent
        if (key?.value != YAML_FORMAT_KEY || value?.value != YamlFormat.SHARE_V1.id) {
            throw ShareFileException("The file is not in the ${YamlFormat.SHARE_V1.id} format")
        }
    }

    /** Reads the rest of an entry, whose start has just been consumed. */
    private fun readFields(): Map<String, String?> {
        val fields = HashMap<String, String?>()
        while (true) {
            val key = parser.event
            if (key is MappingEndEvent) return fields
            val value = parser.event
            if (key is ScalarEvent && value is ScalarEvent) {
                fields[key.value] = if (value.isPlain && value.value in NULLS) null else value.value
            } else {
                skipNode(key)
                skipNode(value)
            }
        }
    }

    /** Consumes the rest of the value that starts with [first], which has just been consumed. */
    private fun skipNode(first: Event) {
        if (first !is CollectionStartEvent) return
        var depth = 1
        while (depth > 0) {
            when (parser.event) {
                is CollectionStartEvent -> depth++
                is CollectionEndEvent -> depth--
            }
        }
    }

    private companion object {
        /** The ways YAML writes null. */
        val NULLS = setOf("", "~", "null", "Null", "NULL")
    }
}

/**
 * Counts the entries of the share file in [reader]. The caller closes [reader].
 * @throws ShareFileException if the text is not a share file.
 */
fun readShareSummary(reader: Reader): ShareSummary {
    val yaml = ShareYamlReader(reader)
    var contacts = 0
    var reviews = 0
    while (true) {
        when ((yaml.next() ?: break).section) {
            ShareSection.CONTACTS -> contacts++
            ShareSection.REVIEWS -> reviews++
        }
    }
    return ShareSummary(contacts, reviews)
}
