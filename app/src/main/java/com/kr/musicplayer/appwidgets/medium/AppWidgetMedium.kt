package com.kr.musicplayer.appwidgets.medium

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.kr.musicplayer.R
import com.kr.musicplayer.appwidgets.AppWidgetSkin
import com.kr.musicplayer.appwidgets.BaseAppwidget
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.util.Util

/**
 * 중간크기의 Widget
 */

class AppWidgetMedium : BaseAppwidget() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context,appWidgetManager,appWidgetIds)
    defaultAppWidget(context, appWidgetIds)
    val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
    intent.putExtra(EXTRA_WIDGET_NAME, this.javaClass.simpleName)
    intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
    Util.sendLocalBroadcast(intent)
  }

  private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_medium)
    buildAction(context, remoteViews)
    pushUpdate(context, appWidgetIds, remoteViews)
  }

  /**
   * Widget 갱신
   */
  override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(service, remoteViews, song)
    //시간설정
    val currentTime = service.progress.toLong()
    val remainTime = song.getDuration()
    if (currentTime > 0 && remainTime > 0) {
      remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime))
    }
    //Cover 갱신
    updateCover(service, remoteViews, appWidgetIds, reloadCover)
  }

  /**
   * 부분적으로 Widget 갱신
   */
  override fun partiallyUpdateWidget(service: MusicService){
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(service, remoteViews, song)
    //시간설정
    val currentTime = service.progress.toLong()
    val remainTime = song.getDuration()
    if (currentTime > 0 && remainTime > 0) {
      remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime))
    }
    val appIds = AppWidgetManager.getInstance(service).getAppWidgetIds(ComponentName(service, javaClass))
    pushPartiallyUpdate(service,appIds,remoteViews)
  }

  companion object {
    @Volatile
    private var INSTANCE: AppWidgetMedium? = null

    @JvmStatic
    fun getInstance(): AppWidgetMedium =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: AppWidgetMedium()
        }
  }
}
