package com.kr.musicplayer.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.kr.musicplayer.db.room.model.History


/**
 * 기록관련 Dao
 */
@Dao
interface HistoryDao {
  @Insert(onConflict = REPLACE)
  fun insertHistory(histories: List<History>): LongArray

  @Insert(onConflict = REPLACE)
  fun insertHistory(history: History): Long

  @Query("""
    SELECT * FROM History
  """)
  fun selectAll(): List<History>

  @Query("""
    SELECT * FROM History
    WHERE audio_id = :audioId
  """)
  fun selectByAudioId(audioId: Int): History?

  @Update
  fun update(history: History): Int
}