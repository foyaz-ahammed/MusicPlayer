package com.kr.musicplayer.db.room.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 재생대기렬 model
 */
@Entity(indices = [Index(value = ["audio_id"], unique = true)])
data class PlayQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audio_id: Int
) {

  companion object {
    const val TABLE_NAME = "PlayQueue"
  }
}