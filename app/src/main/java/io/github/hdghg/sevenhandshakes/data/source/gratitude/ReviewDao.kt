package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review")
    fun observeAll(): Flow<List<ReviewRecord>>

    /** Per-author, per-rate counts of reviews exchanged between contacts [a] and [b], in both directions. */
    @Query(
        "SELECT sourceContactId, gratitude, COUNT(*) AS count FROM review " +
            "WHERE (sourceContactId = :a AND targetContactId = :b) " +
            "OR (sourceContactId = :b AND targetContactId = :a) " +
            "GROUP BY sourceContactId, gratitude",
    )
    fun observeExchangeRateCounts(a: Int, b: Int): Flow<List<RateCount>>

    @Upsert
    suspend fun upsert(record: ReviewRecord)

    @Insert
    suspend fun insert(record: ReviewRecord)
}
