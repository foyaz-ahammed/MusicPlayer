package com.kr.musicplayer.db.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 기록 model
 */
@Entity
data class History(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audio_id: Int,
    val play_count: Int,
    val last_play: Long
) {
}