package com.example.simpleplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    // --- Playlist ---

    @Query("SELECT * FROM playlists ORDER BY id ASC")
    fun observePlaylists(): Flow<List<Playlist>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // --- Brani di una playlist ---

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observeSongs(playlistId: Long): Flow<List<PlaylistSong>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getSongs(playlistId: Long): List<PlaylistSong>

    @Insert
    suspend fun insertSongs(songs: List<PlaylistSong>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND mediaId = :mediaId")
    suspend fun removeSong(playlistId: Long, mediaId: Long)

    // --- Operazioni composte ---

    @Transaction
    suspend fun createPlaylistWithSongs(name: String, songs: List<PlaylistSong>) {
        val playlistId = insertPlaylist(Playlist(name = name))
        val fixed = songs.mapIndexed { index, song ->
            song.copy(playlistId = playlistId, position = index)
        }
        insertSongs(fixed)
    }
}
