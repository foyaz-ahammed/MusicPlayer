package com.kr.musicplayer.ui.widget

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import com.kr.musicplayer.R
import com.kr.musicplayer.util.StatusBarUtil

/**
 * 다중 Popup Top Window
 */
class MultiPopupTopWindow(activity: Activity) : PopupWindow(activity) {

    init {
        contentView = LayoutInflater.from(activity).inflate(R.layout.toolbar_multi_top, activity.window.decorView as ViewGroup, false)
        width = ViewGroup.LayoutParams.MATCH_PARENT
        val ta = activity.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarSize = ta.getDimensionPixelSize(0, 0)
        ta.recycle()
        height = StatusBarUtil.getStatusBarHeight(activity) + actionBarSize
        isFocusable = false
        isOutsideTouchable = false
        this.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun show(parent: View) {
        showAsDropDown(parent)
    }
}