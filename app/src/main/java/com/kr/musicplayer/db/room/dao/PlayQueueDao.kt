package com.kr.musicplayer.db.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.kr.musicplayer.db.room.model.PlayQueue

/**
 * 재생대기렬관련 Dao
 */
@Dao
interface PlayQueueDao {
  @Insert(onConflict = OnConflictStrategy.FAIL)
  fun insertPlayQueue(playQueue: List<PlayQueue>): LongArray

  @Insert(onConflict = OnConflictStrategy.FAIL)
  fun insertPlayQueue(playListSongs: PlayQueue): Long

  @Query("""
    SELECT * FROM PlayQueue
  """)
  fun getPlayQueue(): LiveData<List<PlayQueue>>

  @Query("""
    SELECT * FROM PlayQueue
  """)
  fun selectAll(): List<PlayQueue>

  @Query("""
    DELETE FROM PlayQueue
    WHERE audio_id IN (:audioIds)
  """)
  fun deleteSongs(audioIds: List<Int>): Int


  @Query("""
    DELETE FROM PlayQueue
  """)
  fun clear(): Int

  @Transaction
  fun reArrangeQueueList(queueList : List<PlayQueue>) {
    clear()
    insertPlayQueue(queueList)
  }
}