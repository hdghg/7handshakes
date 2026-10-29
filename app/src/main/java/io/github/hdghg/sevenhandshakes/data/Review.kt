package io.github.hdghg.sevenhandshakes.data

class Review(
    val id: Int,
    val uuid: String, // UUIDv7
    val text: String,
    val rate: Byte,
)
