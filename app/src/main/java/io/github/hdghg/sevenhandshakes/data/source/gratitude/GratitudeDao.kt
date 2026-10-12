package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GratitudeDao {
    @Query("SELECT * FROM gratitude")
    fun observeAll(): Flow<List<GratitudeRecord>>

    @Upsert
    suspend fun upsert(gratitude: GratitudeRecord)
}
