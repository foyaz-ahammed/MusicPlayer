package com.kr.musicplayer.appwidgets

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.kr.musicplayer.R
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.util.ColorUtil
import com.kr.musicplayer.util.Constants

/**
 * Widget 관련 icon
 */
enum class AppWidgetSkin(@param:ColorInt var titleColor: Int, @param:ColorInt var artistColor: Int,
                         @param:ColorInt var progressColor: Int, @param:ColorInt var btnColor: Int,
                         @param:DrawableRes var background: Int, @param:DrawableRes val timerRes: Int,
                         @param:DrawableRes val nextRes: Int, @param:DrawableRes val prevRes: Int,
                         @param:DrawableRes val loveRes: Int, @param:DrawableRes val modeRepeatRes: Int,
                         @param:DrawableRes val modeNormalRes: Int, @param:DrawableRes val modeShuffleRes: Int,
                         @param:DrawableRes val playRes: Int, @param:DrawableRes val pauseRes: Int) {
  WHITE_1F(ColorUtil.getColor(R.color.appwidget_title_color_white_1f),
      ColorUtil.getColor(R.color.appwidget_artist_color_white_1f),
      ColorUtil.getColor(R.color.appwidget_progress_color_white_1f),
      ColorUtil.getColor(R.color.appwidget_btn_color_white_1f),
      R.drawable.bg_corner_app_widget_white_1f,
      R.drawable.ic_timer_white_24dp, R.drawable.widget_btn_next_normal, R.drawable.widget_btn_previous_normal,
      R.drawable.ic_favorites, R.drawable.ic_btn_loop_one, R.drawable.ic_btn_loop,
      R.drawable.ic_btn_shuffle, R.drawable.widget_btn_play_normal, R.drawable.widget_btn_stop_normal),
  TRANSPARENT(ColorUtil.getColor(R.color.appwidget_title_color_transparent),
      ColorUtil.getColor(R.color.appwidget_artist_color_transparent),
      ColorUtil.getColor(R.color.appwidget_progress_color_transparent),
      ColorUtil.getColor(R.color.appwidget_btn_color_transparent),
      R.drawable.bg_corner_app_widget_transparent,
      R.drawable.ic_timer_white_24dp, R.drawable.widget_btn_next_normal_transparent,
      R.drawable.widget_btn_previous_normal_transparent, R.drawable.ic_favorites,
      R.drawable.ic_btn_loop_one, R.drawable.ic_btn_loop,
      R.drawable.ic_btn_shuffle, R.drawable.widget_btn_play_normal_transparent,
      R.drawable.widget_btn_stop_normal_transparent);

  val lovedRes: Int
    get() = R.drawable.ic_favorite_prs

  fun getModeRes(service: MusicService): Int {
    val playModel = service.playModel
    return when (playModel) {
      Constants.MODE_SHUFFLE -> modeShuffleRes
      Constants.MODE_REPEAT -> modeRepeatRes
      else -> modeNormalRes
    }
  }
}
