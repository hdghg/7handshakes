package io.github.hdghg.sevenhandshakes.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReviewTextTest {
    @Test
    fun ordinaryTextIsValid() {
        for (text in listOf(
            "",
            "Thanks a lot!",
            "Tack så mycket",
            "谢谢你的帮助",
            "شكرًا جزيلًا",
            // Right-to-left mark, as used in mixed-direction writing.
            "شكرًا‏!",
            // Persian with a zero width non-joiner.
            "می‌خواهم",
            // An emoji made of several joined with zero width joiners.
            "👨‍👩‍👧 🙏",
            // A flag written with tag characters.
            "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
            "non breaking space",
        )) {
            assertThat(isValidReviewText(text)).isTrue()
            assertThat(cleanReviewText(text)).isEqualTo(text)
        }
    }

    @Test
    fun forbiddenCharacters() {
        for (forbidden in listOf(
            "\n", "\r", "\t", "\u000B", "\u000C", "\u0000", "\u001B", "\u007F", "\u0085",
            " ", " ",
            "‪", "‫", "‬", "‭", "‮",
            "⁦", "⁧", "⁨", "⁩",
            "﻿", "￹", "￺", "￻", "￼",
            // Half of a surrogate pair.
            "\uD83D", "\uDE4F",
        )) {
            assertThat(isValidReviewText("a${forbidden}b")).isFalse()
            assertThat(isValidReviewText(forbidden)).isFalse()
            assertThat(isValidReviewText(cleanReviewText("a${forbidden}b"))).isTrue()
        }
    }

    @Test
    fun lengthIsCountedInBytes() {
        assertThat(isValidReviewText("x".repeat(200))).isTrue()
        assertThat(isValidReviewText("x".repeat(201))).isFalse()
        // Two bytes each.
        assertThat(isValidReviewText("å".repeat(100))).isTrue()
        assertThat(isValidReviewText("å".repeat(101))).isFalse()
    }

    @Test
    fun cleaningTurnsLineBreaksAndTabsIntoOneSpace() {
        assertThat(cleanReviewText("one\ntwo")).isEqualTo("one two")
        assertThat(cleanReviewText("one\r\ntwo\n\n\nthree")).isEqualTo("one two three")
        assertThat(cleanReviewText("one \n two")).isEqualTo("one  two")
        assertThat(cleanReviewText("a\tb c d")).isEqualTo("a b c d")
        assertThat(cleanReviewText("\nstart")).isEqualTo(" start")
    }

    @Test
    fun cleaningDropsTheOtherForbiddenCharacters() {
        assertThat(cleanReviewText("abc‮def‬")).isEqualTo("abcdef")
        assertThat(cleanReviewText("﻿a\u0000b\uD83Dc")).isEqualTo("abc")
        // A whole pair stays.
        assertThat(cleanReviewText("\n🙏")).isEqualTo(" 🙏")
    }
}
