package io.github.hdghg.sevenhandshakes.data

/** Longest review text, in UTF-8 bytes. */
const val REVIEW_TEXT_BYTES_MAX = 200

/**
 * True if [text] may be the text of a review: it is no longer than [REVIEW_TEXT_BYTES_MAX]
 * and holds none of the characters that would break how a review is shown.
 *
 * A review is one run of text. These characters are forbidden:
 * - line feeds, carriage returns, tabs and every other control character,
 * - the Unicode line and paragraph separators,
 * - the characters that change text direction for what follows them (embeddings, overrides, isolates),
 *   which can make a text read differently from what was typed, or spill into the text next to it,
 * - the byte order mark, the object replacement character and the interlinear annotation marks, which show as nothing or as a box,
 * - half of a surrogate pair, which is not a character at all.
 *
 * Marks that belong to ordinary writing stay allowed: the left-to-right and right-to-left marks,
 * and the zero width joiners used by emoji and by scripts such as Persian.
 *
 * The rule only uses character classes that never change between Unicode versions,
 * so every device agrees on which texts are valid.
 */
fun isValidReviewText(text: String): Boolean =
    text.toByteArray(Charsets.UTF_8).size <= REVIEW_TEXT_BYTES_MAX && codePointsOf(text).none(::isForbiddenInReviewText)

/**
 * Returns [text] with the forbidden characters taken out, for text the user types or pastes:
 * a line break or tab becomes one space, so the words around it stay apart; the others are dropped.
 * The length is not changed otherwise; check it with [isValidReviewText].
 */
fun cleanReviewText(text: String): String {
    if (codePointsOf(text).none(::isForbiddenInReviewText)) return text
    val cleaned = StringBuilder(text.length)
    for (codePoint in codePointsOf(text)) {
        when {
            !isForbiddenInReviewText(codePoint) -> cleaned.appendCodePoint(codePoint)
            isLineBreakOrTab(codePoint) -> if (!cleaned.endsWith(' ')) cleaned.append(' ')
        }
    }
    return cleaned.toString()
}

private fun isForbiddenInReviewText(codePoint: Int): Boolean = when (Character.getType(codePoint).toByte()) {
    Character.CONTROL, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR, Character.SURROGATE -> true
    else ->
        codePoint in 0x202A..0x202E || // direction embeddings and overrides
            codePoint in 0x2066..0x2069 || // direction isolates
            codePoint == 0xFEFF || // byte order mark
            codePoint in 0xFFF9..0xFFFC // interlinear annotation marks, object replacement character
}

private fun isLineBreakOrTab(codePoint: Int): Boolean =
    codePoint in 0x09..0x0D || codePoint == 0x85 || codePoint == 0x2028 || codePoint == 0x2029

/** The characters of [text]; half of a surrogate pair comes out as itself. */
private fun codePointsOf(text: String): Sequence<Int> = sequence {
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        yield(codePoint)
        index += Character.charCount(codePoint)
    }
}
