package com.kr.musicplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.service.MusicService.Companion.ACTION_CMD
import com.kr.musicplayer.service.MusicService.Companion.EXTRA_CONTROL
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.Util.sendLocalBroadcast
import timber.log.Timber

/**
 * Media 단추조종관련 BroadcastReceiver
 */
class MediaButtonReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    if (!SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.HEADSET, false)) {
      return
    }
    if (handleMediaButtonIntent(context, intent)) {
      Timber.v("onReceive")
      abortBroadcast()
    }
  }

  companion object {
    const val TAG = "MediaButtonReceiver"
    //Pressed several times
    private var clickCount = 0

    @JvmStatic
    fun handleMediaButtonIntent(context: Context, intent: Intent?): Boolean {
      Timber.v("handleMediaButtonIntent")
      if (intent == null)
        return false
      val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return false
      //Filter press events
      val isActionUp = event.action == KeyEvent.ACTION_UP
      if (!isActionUp) {
        return true
      }

      val keyCode = event.keyCode
      if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
          keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
          keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS ||
              keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
              keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
        val ctrlIntent = Intent(ACTION_CMD)

        ctrlIntent.putExtra(EXTRA_CONTROL, when (keyCode) {
          KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> Command.TOGGLE
          KeyEvent.KEYCODE_MEDIA_PAUSE -> Command.TOGGLE
          KeyEvent.KEYCODE_MEDIA_PLAY -> Command.TOGGLE
          KeyEvent.KEYCODE_MEDIA_NEXT -> Command.NEXT
          KeyEvent.KEYCODE_MEDIA_PREVIOUS -> Command.PREV
          else -> -1
        })
        Timber.v("sendLocalBroadcast: $ctrlIntent")
        sendLocalBroadcast(ctrlIntent)
        return true
      }

      //처음으로 누르는 경우 Thread 를 시작하여 사용자 작업을 판단
      if (clickCount == 0) {
        object : Thread() {
          override fun run() {
            try {
              sleep(800)
              val action = Intent(ACTION_CMD)
              action.putExtra(EXTRA_CONTROL, when (clickCount) {
                1 -> Command.TOGGLE
                2 -> Command.NEXT
                3 -> Command.PREV
                else -> -1
              })
              sendLocalBroadcast(action)
              Timber.v("count=$clickCount")
              clickCount = 0
            } catch (e: InterruptedException) {
              e.printStackTrace()
            }
          }
        }.start()
      }
      clickCount++
      return true
    }
  }
}
