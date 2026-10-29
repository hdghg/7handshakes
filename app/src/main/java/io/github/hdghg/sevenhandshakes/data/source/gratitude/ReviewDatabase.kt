package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReviewRecord::class], version = 1, exportSchema = false)
abstract class ReviewDatabase : RoomDatabase() {
    abstract fun reviewDao(): ReviewDao
}
