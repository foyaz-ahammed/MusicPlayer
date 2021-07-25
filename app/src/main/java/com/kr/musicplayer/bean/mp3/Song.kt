package com.kr.musicplayer.bean.mp3

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import com.kr.musicplayer.App
import com.kr.musicplayer.util.SPUtil

/**
 * 노래관련정보를 담고 있는 object
 */
@Parcelize
data class Song(
    val id: Int,
    val displayName: String,
    val title: String,
    val album: String,
    val albumId: Int,
    val artist: String,
    val artistId: Int,
    private var duration: Long,
    val realTime: String,
    val url: String,
    val size: Long,
    val year: String?,
    val titleKey: String?,
    val addTime: Long,
    val isFavorite: Boolean) : Parcelable {


  val showName: String?
    get() = if (!SHOW_DISPLAYNAME) title else displayName

  override fun toString(): String {
    return "Song{" +
        "id=" + id +
        ", title='" + title + '\''.toString() +
        ", displayName='" + displayName + '\''.toString() +
        ", addTime='" + addTime + '\''.toString() +
        ", album='" + album + '\''.toString() +
        ", albumId=" + albumId +
        ", artist='" + artist + '\''.toString() +
        ", duration=" + duration +
        ", realTime='" + realTime + '\''.toString() +
        ", url='" + url + '\''.toString() +
        ", size=" + size +
        ", year=" + year +
        '}'.toString()
  }


  fun getDuration(): Long {
    return duration
  }

  fun setDuration(duration: Long) {
    this.duration = duration
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + displayName.hashCode()
    result = 31 * result + title.hashCode()
    result = 31 * result + album.hashCode()
    result = 31 * result + albumId
    result = 31 * result + artist.hashCode()
    result = 31 * result + artistId
    result = 31 * result + duration.hashCode()
    result = 31 * result + realTime.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + size.hashCode()
    result = 31 * result + year.hashCode()
    result = 31 * result + titleKey.hashCode()
    result = 31 * result + addTime.hashCode()
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Song) return false

    if (id != other.id) return false
    if (displayName != other.displayName) return false
    if (title != other.title) return false
    if (album != other.album) return false
    if (albumId != other.albumId) return false
    if (artist != other.artist) return false
    if (artistId != other.artistId) return false
    if (duration != other.duration) return false
    if (realTime != other.realTime) return false
    if (url != other.url) return false
    if (size != other.size) return false
    if (year != other.year) return false
    if (titleKey != other.titleKey) return false
    if (addTime != other.addTime) return false

    return true
  }

  companion object {
    @JvmStatic
    val EMPTY_SONG = Song(-1, "", "", "", -1, "", -1, -1, "", "", -1, "", "", -1, false)

    //모든 목록에 파일이름을 표시할지 여부
    @JvmStatic
    var SHOW_DISPLAYNAME = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SHOW_DISPLAYNAME, false)
  }
}
