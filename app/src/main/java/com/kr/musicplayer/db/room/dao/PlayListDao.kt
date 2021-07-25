package com.kr.musicplayer.db.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kr.musicplayer.db.room.model.PlayList
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.room.RawQuery

/**
 * 재생목록관련 Dao
 */
@Dao
interface PlayListDao {
  @Insert(onConflict = OnConflictStrategy.FAIL)
  fun insertPlayList(playlist: PlayList): Long

  @Query("""
    SELECT * FROM PlayList
  """)
  fun selectAll(): List<PlayList>

  @Query("""
    SELECT * FROM PlayList
  """)
  fun selectAllPlayList(): LiveData<List<PlayList>>

  @RawQuery
  fun runtimeQuery(sortQuery: SupportSQLiteQuery): List<PlayList>

  @Query("""
    SELECT * FROM PlayList
    WHERE id = :id
  """)
  fun selectSongIdsById(id: Int): LiveData<PlayList>

  @Query("""
    SELECT * FROM PlayList
    WHERE name = :name
  """)
  fun selectByName(name: String): PlayList?

  @Query("""
    SELECT * FROM PlayList
    WHERE id = :id
  """)
  fun selectById(id: Int): PlayList?

  @Query("""
    UPDATE PlayList
    SET name = :name
    WHERE id = :playlistId
  """)
  fun updateName(playlistId: Int, name: String): Int

  @Query("""
    UPDATE PlayList
    SET audioIds = :audioIds
    WHERE id = :playlistId
  """)
  fun updateAudioIDs(playlistId: Int, audioIds: String): Int

  @Query("""
    UPDATE PlayList
    SET date = :date
    WHERE id = :playlistId
  """)
  fun updateDate(playlistId: Int, date: Long): Int

  @Query("""
    UPDATE PlayList
    SET audioIds = :audioIds
    WHERE id = :name
  """)
  fun updateAudioIDs(name: String, audioIds: String): Int


  @Update
  fun update(playlist: PlayList): Int

  @Query("""
    DELETE FROM PlayList
    WHERE id = :id
  """)
  fun deletePlayList(id: Int): Int

  @Query("""
    DELETE FROM PlayList
  """)
  fun clear(): Int

}