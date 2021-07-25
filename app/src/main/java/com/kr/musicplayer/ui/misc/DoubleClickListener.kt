package com.kr.musicplayer.ui.misc

import android.view.View

/**
 * 두번 누름상태를 위한 Listener
 */

abstract class DoubleClickListener : View.OnClickListener {

  override fun onClick(v: View) {
    val currentTimeMillis = System.currentTimeMillis()
    if (currentTimeMillis - lastClickTime < DOUBLE_TIME) {
      onDoubleClick(v)
    }
    lastClickTime = currentTimeMillis
  }

  abstract fun onDoubleClick(v: View)

  companion object {

    private val DOUBLE_TIME: Long = 600
    private var lastClickTime: Long = 0
  }
}
