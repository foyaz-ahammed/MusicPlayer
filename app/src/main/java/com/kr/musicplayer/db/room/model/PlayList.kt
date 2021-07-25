package com.kr.musicplayer.db.room.model

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 재생목록 model
 */
@Entity(indices = [Index(value = ["name"], unique = true)])
@TypeConverters(PlayList.Converter::class)
data class PlayList(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val audioIds: LinkedHashSet<Int>,
    val date: Long
) {

  class Converter {
    @TypeConverter
    fun toStrList(listStr: String?): LinkedHashSet<Int>? {
      val gson = Gson()
      return gson.fromJson(listStr, object : TypeToken<LinkedHashSet<Int>>() {}.type)
    }

    @TypeConverter
    fun toListStr(list: LinkedHashSet<Int>?): String? {
      return Gson().toJson(list)
    }
  }

  companion object {
    const val TABLE_NAME = "PlayList"
  }
}