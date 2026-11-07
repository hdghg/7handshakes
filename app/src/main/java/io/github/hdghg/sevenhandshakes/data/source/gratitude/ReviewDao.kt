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

    /** Per-rate counts of reviews written by [sourceContactId] to [targetContactId]. */
    @Query(
        "SELECT gratitude, COUNT(*) AS count FROM review " +
            "WHERE sourceContactId = :sourceContactId AND targetContactId = :targetContactId " +
            "GROUP BY gratitude",
    )
    fun observeRateCounts(sourceContactId: Int, targetContactId: Int): Flow<List<RateCount>>

    @Upsert
    suspend fun upsert(record: ReviewRecord)

    @Insert
    suspend fun insert(record: ReviewRecord)
}
