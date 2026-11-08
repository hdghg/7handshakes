package io.github.hdghg.sevenhandshakes.data.source.contact

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContactRecordTest {
    private fun contact(name: String?, email: String?) =
        ContactRecord(publicKey = "key", name = name, isFavorite = false, isIgnored = false, email = email)

    @Test
    fun nameComesFirst() {
        assertThat(contact(name = "Alice", email = "alice@example.com").displayName).isEqualTo("Alice")
    }

    @Test
    fun emailWhenNoName() {
        assertThat(contact(name = null, email = "alice@example.com").displayName).isEqualTo("alice@example.com")
        assertThat(contact(name = " ", email = "alice@example.com").displayName).isEqualTo("alice@example.com")
    }

    @Test
    fun unknownWhenNeither() {
        assertThat(contact(name = null, email = null).displayName).isNull()
        assertThat(contact(name = "", email = "  ").displayName).isNull()
    }
}
