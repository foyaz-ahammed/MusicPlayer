package com.kr.musicplayer.appshortcuts

import android.content.Context
import com.kr.musicplayer.service.MusicService

/**
 * Shortcut 관리를 위한 클라스
 */
class Controller {
    lateinit var mDynamicShortcutManager: DynamicShortcutManager
    companion object {
        //정적변수로 한개의 객체를 생성한다.
        private val controller = Controller()
        fun getController(): Controller {
            return controller
        }
    }

    fun setContext(context: Context) {
        mDynamicShortcutManager = DynamicShortcutManager(context)
    }

    /* Shortcut갱신을 위한 함수들 */
    fun setupShortcuts() {
        mDynamicShortcutManager.setUpShortcuts()
    }

    fun updateShortcuts() {
        mDynamicShortcutManager.updateShortcuts()
    }

    fun updateContinueShortcut(service: MusicService) {
        mDynamicShortcutManager.updateContinueShortcut(service)
    }
}