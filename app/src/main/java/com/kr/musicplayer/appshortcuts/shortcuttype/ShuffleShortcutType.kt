package com.kr.musicplayer.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.Build

import com.kr.musicplayer.R
import com.kr.musicplayer.appshortcuts.AppShortcutActivity

/**
 * 임의로 재생 shortcut
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class ShuffleShortcutType(context: Context) : BaseShortcutType(context) {

  override val shortcutInfo: ShortcutInfo
    get() = ShortcutInfo.Builder(context, ID_PREFIX + "shuffle")
        .setShortLabel(context.getString(R.string.model_random))
        .setLongLabel(context.getString(R.string.model_random))
        .setIcon(Icon.createWithResource(context, R.drawable.icon_appshortcut_shuffle))
        .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_SHUFFLE_ALL))
        .build()
}
