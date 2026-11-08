package io.github.hdghg.sevenhandshakes.data.source.contact

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contact ORDER BY isFavorite DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ContactRecord>>

    @Query("SELECT * FROM contact WHERE id = :id")
    fun observeById(id: Int): Flow<ContactRecord?>

    @Query("SELECT * FROM contact WHERE id = :id")
    suspend fun getById(id: Int): ContactRecord?

    @Query("SELECT id FROM contact WHERE isIgnored")
    fun observeIgnoredIds(): Flow<List<Int>>

    @Query("SELECT * FROM contact WHERE publicKey = :publicKey")
    suspend fun getByPublicKey(publicKey: String): ContactRecord?

    @Upsert
    suspend fun upsert(record: ContactRecord)

    @Query("UPDATE contact SET name = :name WHERE id = :id")
    suspend fun updateName(id: Int, name: String?)

    /** Changes everything but the public key, which identifies the contact. */
    @Query(
        "UPDATE contact SET name = :name, email = :email, isFavorite = :isFavorite, isIgnored = :isIgnored " +
            "WHERE id = :id",
    )
    suspend fun updateDetails(id: Int, name: String?, email: String?, isFavorite: Boolean, isIgnored: Boolean)

    @Query("DELETE FROM review WHERE sourceContactId = :id OR targetContactId = :id")
    suspend fun deleteReviewsOf(id: Int)

    @Query("DELETE FROM contact WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * Deletes the contact and every review written by or to it, all or nothing.
     * Fails with SQLiteConstraintException if an identity uses the contact.
     */
    @Transaction
    suspend fun deleteWithReviews(id: Int) {
        deleteReviewsOf(id)
        deleteById(id)
    }

    /**
     * Returns the new row id.
     * Fails with SQLiteConstraintException if a contact with the same publicKey already exists.
     */
    @Insert
    suspend fun insert(record: ContactRecord): Long
}
