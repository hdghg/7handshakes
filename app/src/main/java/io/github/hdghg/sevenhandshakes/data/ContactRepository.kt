package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContactsStream(): Flow<List<ContactRecord>>
}
