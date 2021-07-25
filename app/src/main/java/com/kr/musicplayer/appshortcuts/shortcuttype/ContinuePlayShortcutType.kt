package com.kr.musicplayer.appshortcuts.shortcuttype

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Icon
import android.os.Build

import com.kr.musicplayer.R
import com.kr.musicplayer.appshortcuts.AppShortcutActivity
import com.kr.musicplayer.helper.MusicServiceRemote
import com.kr.musicplayer.service.MusicService

/**
 * 재생/중지 shortcut
 */
@TargetApi(Build.VERSION_CODES.N_MR1)
class ContinuePlayShortcutType(context: Context) : BaseShortcutType(context) {

  override val shortcutInfo: ShortcutInfo
    get() = ShortcutInfo.Builder(context, ID_PREFIX + "continue_play")
        .setShortLabel(context.getString(if (isPlaying) R.string.pause_play else R.string.continue_play))
        .setLongLabel(context.getString(if (isPlaying) R.string.pause_play else R.string.continue_play))
        .setIcon(Icon.createWithResource(context, if (isPlaying) R.drawable.icon_appshortcut_pause else R.drawable.icon_appshortcut_play))
        .setIntent(getIntent(AppShortcutActivity.SHORTCUT_TYPE_CONTINUE_PLAY))
        .build()

  private val isPlaying: Boolean
    get() = (context as? MusicService)?.isPlaying ?: MusicServiceRemote.isPlaying()
}
