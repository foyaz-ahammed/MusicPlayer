package com.kr.musicplayer.db.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kr.musicplayer.db.room.model.PlayQueue

@Dao
interface PlayQueueVM {
    @Insert(onConflict = OnConflictStrategy.FAIL)
    suspend fun insertPlayQueue(playQueue: List<PlayQueue>): LongArray

    @Query("""
        SELECT * FROM PlayQueue
    """)
    suspend fun selectAllPlayQueueSongs(): List<PlayQueue>
}