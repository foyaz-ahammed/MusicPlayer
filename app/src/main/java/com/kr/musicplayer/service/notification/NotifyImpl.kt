package com.kr.musicplayer.service.notification

import android.app.Notification
import android.app.Service
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.NotificationCompat
import android.view.LayoutInflater
import android.view.View
import android.widget.RemoteViews
import com.kr.musicplayer.R
import com.kr.musicplayer.request.RemoteUriRequest
import com.kr.musicplayer.request.RequestConfig
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.util.ColorUtil
import com.kr.musicplayer.util.DensityUtil
import com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
import com.kr.musicplayer.util.SPUtil
import timber.log.Timber


/**
 * 알림창
 */

class NotifyImpl(context: MusicService) : Notify(context) {

  val context: Service = context

  init {
    try {
      LayoutInflater.from(context).inflate(R.layout.notification_big, null)
    } catch (e: Exception) {
      Timber.w(e)
    }
  }


  /**
   * 알림창갱신
   */
  override fun updateForPlaying() {
    val remoteView = RemoteViews(service.packageName, R.layout.notification)
    val remoteBigView = RemoteViews(service.packageName, R.layout.notification_big)
    val isPlay = service.isPlaying
    val song = service.currentSong
    val isSystemColor = SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_SYSTEM_COLOR, true)

    buildAction(service, remoteView, remoteBigView)
    val notification = buildNotification(service, remoteView, remoteBigView)

    //예술가, 노래 이름
    remoteBigView.setTextViewText(R.id.notify_song, song.displayName)

    remoteView.setTextViewText(R.id.notify_song, song.displayName)

    //비 체계 배경식, 검은색 배경
    if (!isSystemColor) {
      //서체색
      remoteBigView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.dark_text_color_primary))
      remoteView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.dark_text_color_primary))
      //배경
      remoteBigView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black)
      remoteBigView.setViewVisibility(R.id.notify_bg, View.VISIBLE)
      remoteView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black)
      remoteView.setViewVisibility(R.id.notify_bg, View.VISIBLE)
    }

    //재생단추
    if (!isPlay) {
      remoteBigView.setImageViewResource(R.id.notify_play, R.drawable.bg_stroke_circle_green)
      remoteView.setImageViewResource(R.id.notify_play, R.drawable.icon_notify_play)
    } else {
      remoteBigView.setImageViewResource(R.id.notify_play, R.drawable.bg_stroke_circle_red)
      remoteView.setImageViewResource(R.id.notify_play, R.drawable.icon_notify_pause)
    }

    val size = DensityUtil.dip2px(service, 128f)

    disposable?.dispose()
    disposable = object : RemoteUriRequest(getSearchRequestWithAlbumType(song), RequestConfig.Builder(size, size).build()) {
      override fun onStart() {
        remoteBigView.setInt(R.id.notify_image, "setColorFilter", Color.BLACK)
        remoteView.setInt(R.id.notify_image, "setColorFilter", Color.BLACK)
        remoteBigView.setImageViewResource(R.id.notify_image, R.drawable.ic_disc)
        remoteView.setImageViewResource(R.id.notify_image, R.drawable.ic_disc)
      }

      override fun onError(throwable: Throwable) {
        remoteBigView.setInt(R.id.notify_image, "setColorFilter", Color.BLACK)
        remoteView.setInt(R.id.notify_image, "setColorFilter", Color.BLACK)
        remoteBigView.setImageViewResource(R.id.notify_image, R.drawable.ic_disc)
        remoteView.setImageViewResource(R.id.notify_image, R.drawable.ic_disc)
        pushNotify(notification)
      }

      override fun onSuccess(result: Bitmap?) {
        try {
          if (result != null && !result.isRecycled) {
            remoteBigView.setInt(R.id.notify_image, "setColorFilter", 0)
            remoteView.setInt(R.id.notify_image, "setColorFilter", 0)
            remoteBigView.setImageViewBitmap(R.id.notify_image, result)
            remoteView.setImageViewBitmap(R.id.notify_image, result)
          } else {
            remoteBigView.setInt(R.id.notify_image, "setColorFilter", Color.BLACK)
            remoteView.setInt(R.id.notify_image, "setColorFilter", Color.BLACK)
            remoteBigView.setImageViewResource(R.id.notify_image, R.drawable.ic_disc)
            remoteView.setImageViewResource(R.id.notify_image, R.drawable.ic_disc)
          }
        } catch (e: Exception) {
          Timber.v(e)
        } finally {
          pushNotify(notification)
        }
      }
    }.load()
  }

  /**
   * 알림생성
   * @param context The application context
   * @param remoteView LockScreen View
   * @param remoteBigView StatusBar View
   */
  private fun buildNotification(context: Context, remoteView: RemoteViews, remoteBigView: RemoteViews): Notification {
    val builder = NotificationCompat.Builder(context, PLAYING_NOTIFICATION_CHANNEL_ID)
    builder.setContent(remoteView)
        .setCustomBigContentView(remoteBigView)
        .setContentText("")
        .setContentTitle("")
        .setShowWhen(false)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setOngoing(service.isPlaying)
        .setContentIntent(contentIntent)
        .setSmallIcon(R.drawable.ic_music)
    builder.setCustomBigContentView(remoteBigView)
    builder.setCustomContentView(remoteView)
    return builder.build()
  }

  /**
   * Action 생성
   * @param context The application context
   * @param remoteView LockScreen View
   * @param remoteBigView StatusBar View
   */
  private fun buildAction(context: Context, remoteView: RemoteViews, remoteBigView: RemoteViews) {
    val playIntent = buildPendingIntent(context, Command.TOGGLE)
    remoteBigView.setOnClickPendingIntent(R.id.notify_play, playIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_play, playIntent)
    //next
    val nextIntent = buildPendingIntent(context, Command.NEXT)
    remoteBigView.setOnClickPendingIntent(R.id.notify_next, nextIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_next, nextIntent)
    //previous
    val prevIntent = buildPendingIntent(context, Command.PREV)
    remoteBigView.setOnClickPendingIntent(R.id.notify_prev, prevIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_prev, prevIntent)

    //Close notification bar
    val closeIntent = buildPendingIntent(context, Command.CLOSE_NOTIFY)
    remoteBigView.setOnClickPendingIntent(R.id.notify_close, closeIntent)
    remoteView.setOnClickPendingIntent(R.id.notify_close, closeIntent)
  }

}
