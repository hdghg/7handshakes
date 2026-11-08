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

    /** Per-author, per-target, per-rate counts of all reviews: the edges of the handshake graph. */
    @Query(
        "SELECT sourceContactId, targetContactId, gratitude, COUNT(*) AS count FROM review " +
            "GROUP BY sourceContactId, targetContactId, gratitude",
    )
    fun observeEdgeCounts(): Flow<List<EdgeCount>>

    @Upsert
    suspend fun upsert(record: ReviewRecord)

    @Insert
    suspend fun insert(record: ReviewRecord)
}
