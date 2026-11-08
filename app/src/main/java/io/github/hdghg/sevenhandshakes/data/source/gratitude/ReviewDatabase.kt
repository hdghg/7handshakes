package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityDao
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityRecord

@Database(
    entities = [ReviewRecord::class, ContactRecord::class, IdentityRecord::class],
    version = 6,
    exportSchema = false,
)
abstract class ReviewDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao

    abstract fun contactDao(): ContactDao

    abstract fun identityDao(): IdentityDao

    companion object {
        /** Adds the unique (uuid, sourceContactId) index on review. Keeps existing data. */
        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_review_uuid_sourceContactId` " +
                            "ON `review` (`uuid`, `sourceContactId`)",
                    )
                }
            }

        /** Adds the optional email column to contact. Keeps existing data. */
        val MIGRATION_5_6 =
            object : Migration(5, 6) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `contact` ADD COLUMN `email` TEXT")
                }
            }
    }
}
