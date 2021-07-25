package com.kr.musicplayer.ui.fragment.base

import android.content.Context
import android.os.Bundle
import android.view.View
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.helper.MusicEventCallback
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.ui.activity.base.BaseMusicActivity

/**
 * 기초 음악 Fragment
 */
open class BaseMusicFragment : BaseFragment(), MusicEventCallback {
  private var mMusicActivity: BaseMusicActivity? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    try {
      mMusicActivity = context as BaseMusicActivity?
    } catch (e: ClassCastException) {
      throw RuntimeException(context!!.javaClass.simpleName + " must be an instance of " + BaseMusicActivity::class.java.simpleName)
    }

  }

  override fun onDetach() {
    super.onDetach()
    mMusicActivity = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    mMusicActivity?.addMusicServiceEventListener(this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    mMusicActivity?.removeMusicServiceEventListener(this)
  }

  override fun onMediaStoreChanged() {

  }

  override fun onPermissionChanged(has: Boolean) {
    mHasPermission = has
  }

  override fun onPlayListChanged(name: String) {

  }

  override fun onMetaChanged() {
  }

  override fun onPlayStateChange() {
  }

  override fun onTagChanged(oldSong: Song, newSong: Song) {

  }

  override fun onServiceConnected(service: MusicService) {}

  override fun onServiceDisConnected() {}
}
