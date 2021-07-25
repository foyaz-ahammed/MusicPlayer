package com.kr.musicplayer.helper

import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.service.MusicService

interface MusicEventCallback {
  fun onMediaStoreChanged()

  fun onPermissionChanged(has: Boolean)

  fun onPlayListChanged(name: String)

  fun onServiceConnected(service: MusicService)

  fun onMetaChanged()

  fun onPlayStateChange()

  fun onServiceDisConnected()

  fun onTagChanged(oldSong: Song, newSong: Song)
}
