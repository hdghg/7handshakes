package io.github.hdghg.sevenhandshakes.data

class Review(
    val id: Int,
    val uuid: String, // UUIDv7
    val text: String,
    val rate: Byte,
    /** Name of the author contact, or null if it has none. */
    val sourceName: String? = null,
    /** Name of the receiver contact, or null if it has none. */
    val targetName: String? = null,
    /** Creation time in Unix milliseconds, read from [uuid]; null if [uuid] is not a UUIDv7. */
    val createdAt: Long? = null,
)
