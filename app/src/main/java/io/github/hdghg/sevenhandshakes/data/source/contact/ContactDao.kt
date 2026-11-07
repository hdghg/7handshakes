package io.github.hdghg.sevenhandshakes.data.source.contact

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact ORDER BY isFavorite DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactRecord>>

    @Query("SELECT * FROM contact WHERE id = :id")
    fun observeById(id: Int): Flow<ContactRecord?>

    @Query("SELECT * FROM contact WHERE publicKey = :publicKey")
    suspend fun getByPublicKey(publicKey: String): ContactRecord?

    @Upsert
    suspend fun upsert(record: ContactRecord)

    @Query("UPDATE contact SET name = :name WHERE id = :id")
    suspend fun updateName(id: Int, name: String?)

    /**
     * Returns the new row id.
     * Fails with SQLiteConstraintException if a contact with the same publicKey already exists.
     */
    @Insert
    suspend fun insert(record: ContactRecord): Long
}
