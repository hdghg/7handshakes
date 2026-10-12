package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GratitudeRecord::class], version = 1, exportSchema = false)
abstract class GratitudeDatabase : RoomDatabase() {
    abstract fun gratitudeDao(): GratitudeDao
}
