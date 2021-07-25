package com.kr.musicplayer.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.TaskStackBuilder
import io.reactivex.disposables.Disposable
import com.kr.musicplayer.R
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.service.MusicService.Companion.EXTRA_CONTROL
import com.kr.musicplayer.ui.activity.MusicPlayer
import com.kr.musicplayer.ui.activity.PlayerActivity

/**
 * 알림창을 위한 기초클라스
 */

abstract class Notify internal constructor(internal var service: MusicService) {
  protected var disposable: Disposable? = null

  private val notificationManager: NotificationManager by lazy {
    service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private var notifyMode = NOTIFY_MODE_BACKGROUND

  internal val contentIntent: PendingIntent
    get() {
      val result = Intent(service, PlayerActivity::class.java)
      val result0 = Intent(service, MusicPlayer::class.java)

      val stackBuilder = TaskStackBuilder.create(service)
      stackBuilder.addParentStack(MusicPlayer::class.java)
      stackBuilder.addNextIntent(result0)
      stackBuilder.addNextIntent(result)

      return stackBuilder.getPendingIntent(
          0,
          PendingIntent.FLAG_UPDATE_CURRENT
      )!!
    }

  init {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      createNotificationChannel()
    }
  }

  /**
   * Notification Channel 생성
   */
  @RequiresApi(api = Build.VERSION_CODES.O)
  private fun createNotificationChannel() {
    val playingNotificationChannel = NotificationChannel(PLAYING_NOTIFICATION_CHANNEL_ID, service.getString(R.string.playing_notification), NotificationManager.IMPORTANCE_LOW)
    playingNotificationChannel.setShowBadge(false)
    playingNotificationChannel.enableLights(false)
    playingNotificationChannel.enableVibration(false)
    playingNotificationChannel.description = service.getString(R.string.playing_notification_description)
    notificationManager.createNotificationChannel(playingNotificationChannel)
  }

  abstract fun updateForPlaying()

  /**
   * 알림창 갱신
   */
  internal fun pushNotify(notification: Notification) {
    if (service.stop)
      return
    val newNotifyMode: Int = if (service.isPlaying) {
      NOTIFY_MODE_FOREGROUND
    } else {
      NOTIFY_MODE_BACKGROUND
    }

    if (notifyMode != newNotifyMode && newNotifyMode == NOTIFY_MODE_BACKGROUND) {
      service.stopForeground(false)
    }
    if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
      service.startForeground(PLAYING_NOTIFICATION_ID, notification)
    } else {
      notificationManager.notify(PLAYING_NOTIFICATION_ID, notification)
    }

    notifyMode = newNotifyMode
    isNotifyShowing = true
  }

  /**
   * 알림창 취소
   */
  fun cancelPlayingNotify() {
    service.stopForeground(true)
    notificationManager.cancel(PLAYING_NOTIFICATION_ID)
    isNotifyShowing = false
    //        notifyMode = NOTIFY_MODE_NONE;
  }

  /**
   * PendingIntent 생성
   * @return 생성된 PendingIntent
   */
  internal fun buildPendingIntent(context: Context, operation: Int): PendingIntent {
    val intent = Intent(MusicService.ACTION_CMD)
    intent.putExtra(EXTRA_CONTROL, operation)
    intent.component = ComponentName(context, MusicService::class.java)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    } else {
      if (operation != Command.TOGGLE_DESKTOP_LYRIC &&
          operation != Command.CLOSE_NOTIFY &&
          operation != Command.UNLOCK_DESKTOP_LYRIC) {
        return PendingIntent.getForegroundService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT)
      } else {
        PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT)
      }
    }

    return PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT)
  }

  companion object {
    /**
     * 알림창 표시여부
     */
    @JvmStatic
    var isNotifyShowing = false

    private const val NOTIFY_MODE_FOREGROUND = 1
    private const val NOTIFY_MODE_BACKGROUND = 2

    internal const val PLAYING_NOTIFICATION_CHANNEL_ID = "playing_notification"
    private const val PLAYING_NOTIFICATION_ID = 1
  }
}
