package com.kr.musicplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.kr.musicplayer.helper.MusicServiceRemote
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.Util.sendCMDLocalBroadcast
import timber.log.Timber

/**
 * earphone 의 련결 및 해제를 감지하는 BroadcastReceiver
 */
class HeadsetPlugReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent == null) {
      return
    }

    if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
      Timber.v("becoming noise")
      sendCMDLocalBroadcast(Command.PAUSE)
      return
    }

    val name = intent.getStringExtra("name")
    val microphone = intent.getIntExtra("microphone", -1)
    val state = intent.getIntExtra("state", -1)
    Timber.v("state: $state name: $name mic: $microphone")

    if (state == PLUGGED) {
      if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.AUTO_PLAY, NEVER) == HEADSET_PLUG) {
        sendCMDLocalBroadcast(Command.START)
      }
    } else if (state == UNPLUGGED && MusicServiceRemote.isPlaying()) {
      sendCMDLocalBroadcast(Command.PAUSE)

    }
  }

  companion object {
    const val UNPLUGGED = 0
    const val PLUGGED = 1

    const val HEADSET_PLUG = 0
    const val OPEN_SOFTWARE = 1
    const val NEVER = 2
  }
}
