package com.kr.musicplayer.bean.misc

import com.kr.musicplayer.App
import com.kr.musicplayer.R

enum class LyricPriority(val priority: Int, val desc: String) {
  DEF(0, App.getContext().getString(R.string.default_lyric_priority)),
  IGNORE(4, App.getContext().getString(R.string.ignore_lrc)),
  LOCAL(1, App.getContext().getString(R.string.local)),
  EMBEDED(2, App.getContext().getString(R.string.embedded_lyric)),
  MANUAL(3, App.getContext().getString(R.string.select_lrc));

  override fun toString(): String {
    return desc
  }
}
