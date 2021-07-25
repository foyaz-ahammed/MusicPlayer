package com.kr.musicplayer.appshortcuts

import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.Build
import com.kr.musicplayer.appshortcuts.shortcuttype.ContinuePlayShortcutType
import com.kr.musicplayer.appshortcuts.shortcuttype.LastAddedShortcutType
import com.kr.musicplayer.appshortcuts.shortcuttype.MyLoveShortcutType
import com.kr.musicplayer.appshortcuts.shortcuttype.ShuffleShortcutType
import com.kr.musicplayer.service.MusicService

/**
 * Shortcut 관리
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class DynamicShortcutManager(private val context: Context) : ContextWrapper(context.applicationContext) {
  private var shortcutManger: ShortcutManager = getSystemService(ShortcutManager::class.java)

  private val defaultShortcut: List<ShortcutInfo>
    get() = listOf(ContinuePlayShortcutType(context).shortcutInfo, LastAddedShortcutType(context).shortcutInfo, MyLoveShortcutType(context).shortcutInfo, ShuffleShortcutType(context).shortcutInfo)

  /**
   * Shortcut 갱신
   */
  fun setUpShortcuts() {
    if(shortcutManger.dynamicShortcuts.isEmpty())
      updateShortcuts()
  }

  /**
   * 재생/중지 shortcut 갱신
   */
  fun updateContinueShortcut(service: MusicService) {
    shortcutManger.updateShortcuts(listOf(ContinuePlayShortcutType(service).shortcutInfo))
  }

  fun updateShortcuts() {
    shortcutManger.dynamicShortcuts = defaultShortcut
  }
}
