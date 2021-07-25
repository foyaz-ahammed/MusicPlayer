package com.kr.musicplayer.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.Build

import com.kr.musicplayer.R
import com.kr.musicplayer.appshortcuts.AppShortcutActivity

/**
 * 최근에 추가된 노래재생 shortcut
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class LastAddedShortcutType(context: Context) : BaseShortcutType(context) {

  override val shortcutInfo: ShortcutInfo
    get() = ShortcutInfo.Builder(context, ID_PREFIX + "last_added")
        .setShortLabel(context.getString(R.string.recently))
        .setLongLabel(context.getString(R.string.recently))
        .setIcon(Icon.createWithResource(context, R.drawable.icon_appshortcut_last_add))
        .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_LAST_ADDED))
        .build()
}
