package com.kr.musicplayer.bean.mp3

/**
 * 재생목록정보를 담고 있는  object
 */
data class PlayList(val id: Int = 0,
                    val name: String,
                    val count: Int,
                    val date: Int) {
  companion object {
    @JvmStatic val EMPTY_PLAYLIST = PlayList(-1, "", -1, -1)
  }

  override fun toString(): String {
    return "PlayList{" +
        "_Id=" + id +
        ", Name='" + name + '\''.toString() +
        ", Count=" + count +
        ", Date=" + date +
        '}'.toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PlayList) return false

    if (id != other.id) return false
    if (name != other.name) return false
    if (count != other.count) return false
    if (date != other.date) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + name.hashCode()
    result = 31 * result + count
    result = 31 * result + date
    return result
  }
}
