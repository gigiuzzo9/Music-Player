package com.example.simpleplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedFileDao {

    @Query("SELECT * FROM cached_files ORDER BY title ASC")
    fun observeAll(): Flow<List<CachedFile>>

    @Query("SELECT * FROM cached_files ORDER BY title ASC")
    suspend fun getAll(): List<CachedFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<CachedFile>)

    @Query("DELETE FROM cached_files")
    suspend fun clearAll()

    /**
     * Sostituisce l'intera cache con la lista aggiornata.
     * Cancella tutto e reinserisce: semplice e affidabile.
     */
    @Transaction
    suspend fun replaceAll(files: List<CachedFile>) {
        clearAll()
        insertAll(files)
    }
}
