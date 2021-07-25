package com.kr.musicplayer.ui.misc

import android.content.Context
import android.content.ContextWrapper
import com.facebook.common.util.ByteConstants
import com.kr.musicplayer.R
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.helper.MusicServiceRemote.getCurrentSong
import com.kr.musicplayer.misc.tageditor.TagEditor
import com.kr.musicplayer.theme.Theme
import com.kr.musicplayer.util.MaterialDialogHelper
import com.kr.musicplayer.util.Util
import kotlinx.android.synthetic.main.dialog_song_detail.view.*

/**
 * 노래정보
 */
class Tag(context: Context, song: Song?) : ContextWrapper(context) {
  private val song: Song = song ?: getCurrentSong() // 현재 노래
  private val tagEditor: TagEditor

  init {
    tagEditor = TagEditor(this.song.url)
  }

  fun detail() {
    val detailDialog = Theme.getBaseDialog(this)
        .customView(R.layout.dialog_song_detail, true)
        .build()
    MaterialDialogHelper.adjustAlertDialog(detailDialog, resources.getDrawable(R.drawable.round_gray_top))
    detailDialog.show()
    detailDialog.customView?.let { root ->
      //노래경로
      root.song_detail_path.text = " " + song.url
      //노래이름
      root.song_detail_name.text = " " + song.displayName
      //노래크기
      root.song_detail_size.text = " " + getString(R.string.cache_size, 1.0f * song.size / ByteConstants.MB)
      //노래길이
      root.song_detail_duration.text = " " + Util.getTime(song.getDuration())
      root.song_detail_mime.text = " " + song.artist
    }

  }
}

