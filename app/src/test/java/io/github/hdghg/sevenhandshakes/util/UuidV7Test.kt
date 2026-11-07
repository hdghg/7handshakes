package io.github.hdghg.sevenhandshakes.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UuidV7Test {
    @Test
    fun hasVersion7AndRfcVariant() {
        val uuid = UuidV7.generate()
        assertThat(uuid.version()).isEqualTo(7)
        assertThat(uuid.variant()).isEqualTo(2)
    }

    @Test
    fun startsWithTimestamp() {
        val time = 0x0189_ABCD_EF01L
        val uuid = UuidV7.generate(time)
        assertThat(uuid.mostSignificantBits ushr 16).isEqualTo(time)
        assertThat(uuid.toString()).startsWith("0189abcd-ef01-7")
    }

    @Test
    fun timestampOf_readsBackTime() {
        val time = 1_762_500_000_000L
        assertThat(UuidV7.timestampOf(UuidV7.generate(time).toString())).isEqualTo(time)
    }

    @Test
    fun timestampOf_nonV7_isNull() {
        assertThat(UuidV7.timestampOf("f47ac10b-58cc-4372-a567-0e02b2c3d479")).isNull() // v4
        assertThat(UuidV7.timestampOf("not a uuid")).isNull()
        assertThat(UuidV7.timestampOf("")).isNull()
    }

    @Test
    fun laterTimeSortsAfter() {
        val earlier = UuidV7.generate(1_000L).toString()
        val later = UuidV7.generate(1_001L).toString()
        assertThat(later).isGreaterThan(earlier)
    }

    @Test
    fun sameTimeGivesDifferentValues() {
        assertThat(UuidV7.generate(1_000L)).isNotEqualTo(UuidV7.generate(1_000L))
    }
}
