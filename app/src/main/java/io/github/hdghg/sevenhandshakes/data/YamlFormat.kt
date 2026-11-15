package io.github.hdghg.sevenhandshakes.data

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.events.DocumentStartEvent
import org.yaml.snakeyaml.events.MappingStartEvent
import org.yaml.snakeyaml.events.ScalarEvent
import org.yaml.snakeyaml.events.StreamStartEvent
import org.yaml.snakeyaml.parser.ParserImpl
import org.yaml.snakeyaml.reader.StreamReader
import java.io.Reader

/** Name of the top-level key that holds a file's [YamlFormat]. It is always the file's first key. */
const val YAML_FORMAT_KEY = "format"

/**
 * The kinds of YAML file the app works with. Every file names its kind in its first key:
 *
 * ```yaml
 * format: "share/v1"
 * ```
 *
 * A change that old readers cannot handle gets a new version, hence a new entry.
 */
enum class YamlFormat(val id: String) {
    /** All contacts and reviews; see [writeShareYaml]. */
    SHARE_V1("share/v1"),

    /** Reserved; the schema is not defined yet. */
    REQUEST_REVIEW_V1("request-review/v1"),

    /** Reserved; the schema is not defined yet. */
    RESPONSE_REVIEW_V1("response-review/v1"),
    ;

    companion object {
        fun ofId(id: String): YamlFormat? = entries.firstOrNull { it.id == id }
    }
}

/**
 * Tells which [YamlFormat] the YAML text in [reader] is, or null if it is not one of the app's files.
 *
 * Reads only the start of the text, so it is quick and needs little memory whatever the file's size.
 * The caller closes [reader].
 */
fun readYamlFormat(reader: Reader): YamlFormat? {
    val parser = ParserImpl(StreamReader(reader), LoaderOptions())
    return try {
        if (parser.event !is StreamStartEvent) return null
        if (parser.event !is DocumentStartEvent) return null
        if (parser.event !is MappingStartEvent) return null
        val key = parser.event as? ScalarEvent ?: return null
        if (key.value != YAML_FORMAT_KEY) return null
        val value = parser.event as? ScalarEvent ?: return null
        YamlFormat.ofId(value.value)
    } catch (e: YAMLException) {
        null
    }
}
