package io.github.hdghg.sevenhandshakes.data.source.identity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Transaction
    @Query("SELECT * FROM identity ORDER BY id ASC")
    fun observeAllWithContact(): Flow<List<IdentityWithContact>>

    @Query("SELECT EXISTS(SELECT 1 FROM identity WHERE privateKey = :privateKey)")
    suspend fun existsByPrivateKey(privateKey: String): Boolean

    /**
     * Returns the new row id.
     * Fails with SQLiteConstraintException if the private key already exists or the contact does not.
     */
    @Insert
    suspend fun insert(record: IdentityRecord): Long
}
