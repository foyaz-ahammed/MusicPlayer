package com.kr.musicplayer.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import com.kr.musicplayer.App
import com.kr.musicplayer.R
import com.kr.musicplayer.appwidgets.big.AppWidgetBig
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.request.RemoteUriRequest
import com.kr.musicplayer.request.RequestConfig
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.service.MusicService.Companion.EXTRA_CONTROL
import com.kr.musicplayer.ui.activity.MusicPlayer
import com.kr.musicplayer.util.DensityUtil
import com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
import timber.log.Timber

/**
 * App Widget 기초클라스
 */
abstract class BaseAppwidget
    : AppWidgetProvider() {

    protected lateinit var skin: AppWidgetSkin

    private val defaultDrawableRes: Int
        @DrawableRes
        get() = R.drawable.ic_disc// if (skin == WHITE_1F) R.drawable.album_empty_bg_night else R.drawable.album_empty_bg_day

    /**
     * PendingIntent 를 얻는 함수
     */
    private fun buildServicePendingIntent(context: Context, componentName: ComponentName, cmd: Int): PendingIntent {
        val intent = Intent(MusicService.ACTION_APPWIDGET_OPERATE)
        intent.putExtra(EXTRA_CONTROL, cmd)
        intent.component = componentName
        return if (isAllowForForegroundService(cmd)) {
            PendingIntent.getForegroundService(context, cmd, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getService(context, cmd, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    /**
     * ForegroundService 로 실행시킬수 있는지 확인여부
     */
    private fun isAllowForForegroundService(cmd: Int): Boolean {
        return cmd != Command.CHANGE_MODEL && cmd != Command.LOVE && cmd != Command.TOGGLE_TIMER
    }

    protected fun hasInstances(context: Context): Boolean {
        try {
            val appIds = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, javaClass))
            return appIds != null && appIds.isNotEmpty()
        } catch (e: Exception) {
            Timber.v(e)
        }
        return false
    }

    /**
     * Cover 갱신
     */
    protected fun updateCover(service: MusicService, remoteViews: RemoteViews, appWidgetIds: IntArray?, reloadCover: Boolean) {
        val song = service.currentSong
        val size = if (this.javaClass.simpleName == AppWidgetBig::class.java.simpleName) IMAGE_SIZE_BIG else IMAGE_SIZE_MEDIUM
        object : RemoteUriRequest(getSearchRequestWithAlbumType(song), RequestConfig.Builder(size, size).build()) {
            override fun onError(throwable: Throwable) {
                Timber.v("onError: $throwable")
                remoteViews.setImageViewResource(R.id.appwidget_image, defaultDrawableRes)
                pushUpdate(service, appWidgetIds, remoteViews)
            }

            override fun onSuccess(result: Bitmap?) {
                try {
                    val bitmap = MusicService.copy(result)
                    if (bitmap != null) {
                        remoteViews.setImageViewBitmap(R.id.appwidget_image, bitmap)
                    } else {
                        remoteViews.setImageViewResource(R.id.appwidget_image, defaultDrawableRes)
                    }
                    pushUpdate(service, appWidgetIds, remoteViews)
                } catch (e: Exception) {
                    Timber.v("onSuccess: $e")
                } finally {

                }
            }
        }.load()
    }

    /**
     * Action 갱신
     */
    protected fun buildAction(context: Context, views: RemoteViews) {
        val componentNameForService = ComponentName(context, MusicService::class.java)
        views.setOnClickPendingIntent(R.id.appwidget_toggle, buildServicePendingIntent(context, componentNameForService, Command.TOGGLE))
        views.setOnClickPendingIntent(R.id.appwidget_prev, buildServicePendingIntent(context, componentNameForService, Command.PREV))
        views.setOnClickPendingIntent(R.id.appwidget_next, buildServicePendingIntent(context, componentNameForService, Command.NEXT))
        views.setOnClickPendingIntent(R.id.appwidget_model, buildServicePendingIntent(context, componentNameForService, Command.CHANGE_MODEL))
        views.setOnClickPendingIntent(R.id.appwidget_timer, buildServicePendingIntent(context, componentNameForService, Command.TOGGLE_TIMER))

        val action = Intent(context, MusicPlayer::class.java)
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        views.setOnClickPendingIntent(R.id.appwidget_clickable, PendingIntent.getActivity(context, 0, action, 0))
    }

    /**
     * Widget 갱신
     */
    protected fun pushUpdate(context: Context, appWidgetId: IntArray?, remoteViews: RemoteViews) {
        if (!hasInstances(context)) {
            return
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetId != null) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        } else {
            appWidgetManager.updateAppWidget(ComponentName(context, javaClass), remoteViews)
        }
    }

    /**
     * 부분적으로 Widget 갱신
     */
    protected fun pushPartiallyUpdate(context: Context, appWidgetId: IntArray?, remoteViews: RemoteViews) {
        if (!hasInstances(context)) {
            return
        }
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetId != null) {
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
        }
    }

    /**
     * Widget view 들 갱신
     */
    protected fun updateRemoteViews(service: MusicService, remoteViews: RemoteViews, song: Song) {
        updateTitle(remoteViews, song)
        updatePlayPause(service, remoteViews)
        updateLove(service, remoteViews, song)
        updateModel(service, remoteViews)
        updateNextAndPrev(remoteViews)
        updateProgress(service, remoteViews, song)
        updateTimer(remoteViews)
    }

    /**
     * 시간갱신
     */
    private fun updateTimer(remoteViews: RemoteViews) {
        remoteViews.setImageViewResource(R.id.appwidget_timer, skin.timerRes)
    }

    /**
     * 진행률 갱신
     */
    private fun updateProgress(service: MusicService, remoteViews: RemoteViews, song: Song) {
        remoteViews.setProgressBar(R.id.appwidget_seekbar, song.getDuration().toInt(), service.progress, false)
    }

    /**
     * 즐겨찾기 view 갱신
     */
    private fun updateLove(service: MusicService, remoteViews: RemoteViews, song: Song) {
        remoteViews.setImageViewResource(R.id.appwidget_love, if (service.isLove) skin.lovedRes else skin.loveRes)
    }

    /**
     * 이전 및 다음 view 갱신
     */
    private fun updateNextAndPrev(remoteViews: RemoteViews) {
        remoteViews.setImageViewResource(R.id.appwidget_next, skin.nextRes)
        remoteViews.setImageViewResource(R.id.appwidget_prev, skin.prevRes)
    }

    /**
     * 재생방식 view 갱신
     */
    private fun updateModel(service: MusicService, remoteViews: RemoteViews) {
        //재생방식
        remoteViews.setImageViewResource(R.id.appwidget_model, skin.getModeRes(service))
    }

    /**
     * 재생 및 중지 단추 갱신
     */
    private fun updatePlayPause(service: MusicService, remoteViews: RemoteViews) {
        //재생 및 일시중지 단추
        remoteViews.setImageViewResource(R.id.appwidget_toggle, if (service.isPlaying) skin.pauseRes else skin.playRes)
    }

    /**
     * 제목 갱신
     */
    private fun updateTitle(remoteViews: RemoteViews, song: Song) {
        remoteViews.setTextViewText(R.id.appwidget_title, song.displayName)
    }

    abstract fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean)

    abstract fun partiallyUpdateWidget(service: MusicService)

    companion object {
        const val EXTRA_WIDGET_NAME = "WidgetName"
        const val EXTRA_WIDGET_IDS = "WidgetIds"


        private val TAG = "桌面部件"
        private val IMAGE_SIZE_BIG = DensityUtil.dip2px(App.getContext(), 270f)
        private val IMAGE_SIZE_MEDIUM = DensityUtil.dip2px(App.getContext(), 72f)
    }
}
