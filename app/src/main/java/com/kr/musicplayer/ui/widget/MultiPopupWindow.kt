package com.kr.musicplayer.ui.widget

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.kr.musicplayer.R
import com.kr.musicplayer.theme.ThemeStore

/**
 * 다중 Popup Window
 */
class MultiPopupWindow(activity: Activity) : PopupWindow(activity) {

  init {
    contentView = LayoutInflater.from(activity).inflate(R.layout.toolbar_multi, activity.window.decorView as ViewGroup, false)
    width = ViewGroup.LayoutParams.MATCH_PARENT
    val ta = activity.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    val actionBarSize = ta.getDimensionPixelSize(0, 0)
    ta.recycle()

    height = actionBarSize + 10
    setBackgroundDrawable(ColorDrawable(activity.resources.getColor(R.color.transparent)))
    isFocusable = false
    isOutsideTouchable = false
    showAtLocation(contentView, Gravity.BOTTOM, 0, 0);
  }

  fun show(parent: View) {
    showAsDropDown(parent, 0, 0)
  }
}