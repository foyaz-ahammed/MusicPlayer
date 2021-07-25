package com.kr.musicplayer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kr.musicplayer.appshortcuts.Controller

/**
 * 체계에서 보내오는 broadcast들을 받고 Shortcut들을 갱신해주기 위한 [BroadcastReceiver]이다.
 */
class ShortcutUpdateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if(intent == null || intent.action == null)
            return

        //다음의 경우들에 shortcut들을 갱신한다.
        val action = intent.action
        if(action == Intent.ACTION_BOOT_COMPLETED               //전화기가 기동할때
                || action == Intent.ACTION_LOCALE_CHANGED       //언어가 바뀔때
                || action == Intent.ACTION_MY_PACKAGE_REPLACED  //새로운 version의 음악프로그람이 설치되였을때
        )
            Controller.getController().updateShortcuts()
    }
}