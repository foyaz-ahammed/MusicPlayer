package com.kr.musicplayer.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.Build

import com.kr.musicplayer.R
import com.kr.musicplayer.appshortcuts.AppShortcutActivity

/**
 * 즐겨찾기 shortcut
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class MyLoveShortcutType(context: Context) : BaseShortcutType(context) {

  override val shortcutInfo: ShortcutInfo
    get() = ShortcutInfo.Builder(context, ID_PREFIX + "my_love")
        .setShortLabel(context.getString(R.string.my_favorite))
        .setLongLabel(context.getString(R.string.my_favorite))
        .setIcon(Icon.createWithResource(context, R.drawable.icon_appshortcut_my_love))
        .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_MY_LOVE))
        .build()
}
