package com.kr.musicplayer.bean.mp3

/**
 * 등록부정보를 담고 있는 object
 */

data class Folder(val name: String?, val count: Int, val path: String?, val parentId: Int) {


  override fun equals(other: Any?): Boolean {
    return when (other) {
      !is Folder -> false
      else -> {
        this.parentId == other.parentId
      }
    }
  }

  override fun hashCode(): Int {
    var result = name?.hashCode() ?: 0
    result = 31 * result + count
    result = 31 * result + (path?.hashCode() ?: 0)
    result = 31 * result + parentId
    return result
  }
}
