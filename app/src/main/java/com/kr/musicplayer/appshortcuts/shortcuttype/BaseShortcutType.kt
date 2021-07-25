package com.kr.musicplayer.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.os.Build

import com.kr.musicplayer.appshortcuts.AppShortcutActivity

/**
 * 기초 shortcut
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
abstract class BaseShortcutType constructor(val context: Context) {

  abstract val shortcutInfo: ShortcutInfo


  /**
   * Intent 얻기
   * @param type shortcut 형태
   */
  fun getIntent(type: Int): Intent {
    val intent = Intent(context, AppShortcutActivity::class.java)
    intent.putExtra(AppShortcutActivity.KEY_SHORTCUT_TYPE, type)
    intent.action = Intent.ACTION_VIEW
    return intent
  }

  companion object {
    val ID_PREFIX = "com.kr.myplayer.appshortcuts.id."

  }
}
