package com.kr.musicplayer.service

/**
 * 재생조종 interface
 */

interface Playback {
  fun playSelectSong(position: Int)

  fun toggle()

  fun playNext()

  fun playPrevious()

  fun play(fadeIn: Boolean)

  fun pause(updateMediasessionOnly: Boolean)
}
