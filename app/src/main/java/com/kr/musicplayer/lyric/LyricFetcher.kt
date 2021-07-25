package com.kr.musicplayer.lyric

import io.reactivex.disposables.Disposable
import com.kr.musicplayer.App
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.lyric.bean.LrcRow
import com.kr.musicplayer.lyric.bean.LrcRow.LYRIC_EMPTY_ROW
import com.kr.musicplayer.lyric.bean.LyricRowWrapper
import com.kr.musicplayer.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_NO
import com.kr.musicplayer.lyric.bean.LyricRowWrapper.LYRIC_WRAPPER_SEARCHING
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.util.SPUtil
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

class LyricFetcher(service: MusicService) {

  private val lrcRows = CopyOnWriteArrayList<LrcRow>()
  private val reference: WeakReference<MusicService> = WeakReference(service)
  private var disposable: Disposable? = null
  private var song: Song = Song.EMPTY_SONG
  private var status = Status.SEARCHING
  var offset = 0
  private val lyricSearcher = LyricSearcher()

  /**
   * 현재 가사 읽기
   */
  fun findCurrentLyric(): LyricRowWrapper {
    val wrapper = LyricRowWrapper()
    wrapper.status = status
    val service = reference.get()

    when {
      service == null || status == Status.NO -> {
        return LYRIC_WRAPPER_NO
      }
      status == Status.SEARCHING -> {
        return LYRIC_WRAPPER_SEARCHING
      }
      status == Status.NORMAL -> {
        val song = service.currentSong
        if (song == Song.EMPTY_SONG) {
          return wrapper
        }
        val progress = service.progress + offset

        for (i in lrcRows.indices.reversed()) {
          val lrcRow = lrcRows[i]
          val interval = progress - lrcRow.time
          if (i == 0 && interval < 0) {
            //노래정보표시
            wrapper.lineOne = LrcRow("", 0, song.title)
            wrapper.lineTwo = LrcRow("", 0, song.artist + " - " + song.album)
            return wrapper
          } else if (progress >= lrcRow.time) {
            if (lrcRow.hasTranslate()) {
              wrapper.lineOne = LrcRow(lrcRow)
              wrapper.lineOne.content = lrcRow.content
              wrapper.lineTwo = LrcRow(lrcRow)
              wrapper.lineTwo.content = lrcRow.translate
            } else {
              wrapper.lineOne = lrcRow
              wrapper.lineTwo = LrcRow(if (i + 1 < lrcRows.size) lrcRows[i + 1] else LYRIC_EMPTY_ROW)
            }
            return wrapper
          }
        }
        return wrapper
      }
      else -> {
        return LYRIC_WRAPPER_NO
      }
    }

  }

  /**
   * 가사줄갱신
   */
  fun updateLyricRows(song: Song) {
    this.song = song

    if (song == Song.EMPTY_SONG) {
      status = Status.NO
      lrcRows.clear()
      return
    }

    val id = song.id

    disposable?.dispose()
    disposable = lyricSearcher.setSong(song)
        .getLyricObservable()
        .doOnSubscribe {
          status = Status.SEARCHING
        }
        .subscribe({
          Timber.v("updateLyricRows, lrc: $it")
          if (id == song.id) {
            status = Status.NORMAL
            offset = SPUtil.getValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, id.toString(), 0)
            lrcRows.clear()
            lrcRows.addAll(it)
          } else {
            lrcRows.clear()
            status = Status.NO
          }
        }, { throwable ->
          Timber.v(throwable)
          if (id == song.id) {
            status = Status.NO
            lrcRows.clear()
          }
        })
  }

  fun dispose() {
    disposable?.dispose()
  }

  enum class Status {
    NO, SEARCHING, NORMAL
  }

  companion object {
    const val LYRIC_FIND_INTERVAL = 400L
  }

}