package com.kr.musicplayer.appshortcuts

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.ui.activity.MusicPlayer
import com.kr.musicplayer.ui.activity.PlayerActivity
import com.kr.musicplayer.ui.activity.base.BaseMusicActivity

/**
 * Shortcut 관련 activity
 */

class AppShortcutActivity : BaseMusicActivity() {
  var shortcutType = -1
  private val loadFinishedReceiver = object : BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
      if (p1?.action == MusicService.LOAD_FINISHED) {
        startService(shortcutType)
        val intent = Intent(baseContext, MusicPlayer::class.java)
        startActivity(intent)
        val playerIntent = Intent(baseContext, PlayerActivity::class.java)
        startActivity(playerIntent)
        finish()
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    shortcutType = intent.getIntExtra(KEY_SHORTCUT_TYPE, -1)
    if (isServiceRunning()) {
      startService(shortcutType)
      val intent = Intent(baseContext, MusicPlayer::class.java)
      startActivity(intent)
      val playerIntent = Intent(baseContext, PlayerActivity::class.java)
      startActivity(playerIntent)
      finish()
    }

    super.onCreate(savedInstanceState)

    LocalBroadcastManager.getInstance(this).registerReceiver(loadFinishedReceiver, IntentFilter(MusicService.LOAD_FINISHED))
  }

  override fun onDestroy() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(loadFinishedReceiver)
    super.onDestroy()
  }

  /**
   * 현재 MusicService 가 실행중인지 확인
   * @return 실행중이면 true 아니면 false
   */
  private fun isServiceRunning(): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
      if (MusicService::class.java.name == service.service.className) {
        return true
      }
    }
    return false
  }

  /**
   * Shortcut 형태에 따라 MusicService 실행
   */
  private fun startService(type: Int) {
    val intent = Intent(this, MusicService::class.java)
    when (type) {
      SHORTCUT_TYPE_LAST_ADDED -> intent.action = MusicService.ACTION_SHORTCUT_LASTADDED
      SHORTCUT_TYPE_SHUFFLE_ALL -> intent.action = MusicService.ACTION_SHORTCUT_SHUFFLE
      SHORTCUT_TYPE_MY_LOVE -> intent.action = MusicService.ACTION_SHORTCUT_MYLOVE
      SHORTCUT_TYPE_CONTINUE_PLAY -> intent.action = MusicService.ACTION_SHORTCUT_CONTINUE_PLAY
    }
    startService(intent)
  }

  companion object {
    const val SHORTCUT_TYPE_SHUFFLE_ALL = 0
    const val SHORTCUT_TYPE_MY_LOVE = 1
    const val SHORTCUT_TYPE_LAST_ADDED = 2
    const val SHORTCUT_TYPE_CONTINUE_PLAY = 3

    const val KEY_SHORTCUT_TYPE = "com.kr.myplayer.appshortcuts.ShortcutType"
  }
}
