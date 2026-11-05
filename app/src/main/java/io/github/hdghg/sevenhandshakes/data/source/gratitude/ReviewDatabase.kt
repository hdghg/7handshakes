package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityDao
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityRecord

@Database(
    entities = [ReviewRecord::class, ContactRecord::class, IdentityRecord::class],
    version = 4,
    exportSchema = false,
)
abstract class ReviewDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao

    abstract fun contactDao(): ContactDao

    abstract fun identityDao(): IdentityDao
}
