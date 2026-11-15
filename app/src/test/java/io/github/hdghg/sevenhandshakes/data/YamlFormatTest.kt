package io.github.hdghg.sevenhandshakes.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.Reader
import java.io.StringReader

class YamlFormatTest {
    private fun formatOf(yaml: String) = readYamlFormat(StringReader(yaml))

    @Test
    fun recognisesEveryFormat() {
        for (format in YamlFormat.entries) {
            assertThat(formatOf("format: \"${format.id}\"\nother: 1\n")).isEqualTo(format)
            assertThat(formatOf("format: ${format.id}\n")).isEqualTo(format)
        }
    }

    @Test
    fun formatIdsAreStable() {
        assertThat(YamlFormat.entries.map { it.id })
            .containsExactly("share/v1", "request-review/v1", "response-review/v1")
    }

    @Test
    fun unknownFormat() {
        assertThat(formatOf("format: \"share/v2\"\n")).isNull()
        assertThat(formatOf("format: [share/v1]\n")).isNull()
    }

    @Test
    fun formatMustBeTheFirstKey() {
        assertThat(formatOf("contacts: []\nformat: \"share/v1\"\n")).isNull()
    }

    @Test
    fun notOneOfOurFiles() {
        assertThat(formatOf("")).isNull()
        assertThat(formatOf("just text")).isNull()
        assertThat(formatOf("- format: share/v1\n")).isNull()
        assertThat(formatOf("format: \"share/v1\n")).isNull()
        assertThat(formatOf("{{{")).isNull()
    }

    @Test
    fun readsOnlyTheStartOfALargeFile() {
        val head = "format: \"share/v1\"\ncontacts:\n"
        val item = "  - id: 1\n"
        var served = 0L

        // An endless file: reading all of it would never finish.
        val endless = object : Reader() {
            override fun read(buffer: CharArray, offset: Int, length: Int): Int {
                var written = 0
                while (written < length) {
                    val position = served + written
                    buffer[offset + written] = if (position < head.length) {
                        head[position.toInt()]
                    } else {
                        item[((position - head.length) % item.length).toInt()]
                    }
                    written++
                }
                served += written
                return written
            }

            override fun close() = Unit
        }

        assertThat(readYamlFormat(endless)).isEqualTo(YamlFormat.SHARE_V1)
        assertThat(served).isLessThan(100_000L)
    }
}
