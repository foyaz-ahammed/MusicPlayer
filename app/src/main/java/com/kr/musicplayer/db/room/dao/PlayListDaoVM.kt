package com.kr.musicplayer.db.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.kr.musicplayer.db.room.model.PlayList

@Dao
interface PlayListDaoVM {
    @Query("SELECT * FROM PlayList WHERE id = :id")
    suspend fun selectPlayListById(id: Int): PlayList?

    @Query("SELECT * FROM PlayList WHERE id = :id")
    fun selectPlayListWithId(id: Int): PlayList?

    @Query("""
    SELECT * FROM PlayList
  """)
    suspend fun selectAll(): List<PlayList>

    @Query("""
    SELECT * FROM PlayList
    WHERE id = :id
  """)
    suspend fun selectById(id: Int): PlayList?

    @Update
    suspend fun update(playlist: PlayList): Int
}