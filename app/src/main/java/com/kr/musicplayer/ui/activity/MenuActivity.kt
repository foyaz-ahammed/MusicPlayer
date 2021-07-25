package com.kr.musicplayer.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import com.kr.musicplayer.R
import com.kr.musicplayer.helper.SortOrder
import com.kr.musicplayer.misc.MediaScanner
import com.kr.musicplayer.ui.dialog.FolderChooserDialog
import com.kr.musicplayer.util.SPUtil
import java.io.File

/**
 * 기초 Activity
 */
@SuppressLint("Registered")
abstract class MenuActivity : ToolbarActivity(), FolderChooserDialog.FolderCallback {
  open fun getMenuLayoutId(): Int {
    return R.menu.menu_main_simple
  }

  /**
   * 노래검색대화창에서 folder를 선택하였을때 호출되는 callback
   */
  override fun onFolderSelection(dialog: FolderChooserDialog, folder: File) {
    var tag = dialog.tag ?: return
    var playListName = ""
    try {
      if (tag.contains("ExportPlayList")) {
        val tagAndName = tag.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        tag = tagAndName[0]
        playListName = tagAndName[1]
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    when (tag) {
      "Scan" -> {
        if (folder.exists() && folder.isDirectory && folder.list() != null) {
          SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MANUAL_SCAN_FOLDER, folder.absolutePath)
        }

        MediaScanner(mContext).scanFiles(folder)
      }
    }
  }

  /**
   * Menu 항목을 선택하였을때 호출되는 callback
   */
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.action_manual_search) {
      val initialFile = File(
              SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MANUAL_SCAN_FOLDER, ""))
      val builder = FolderChooserDialog.Builder(this)
              .chooseButton(R.string.choose_folder)
              .tag("Scan")
              .allowNewFolder(false, R.string.new_folder)
      if (initialFile.exists() && initialFile.isDirectory && initialFile.list() != null) {
        builder.initialPath(initialFile.absolutePath)
      }
      builder.show()
    } else if (item.itemId == R.id.action_search) {
      startActivity(Intent(mContext, SearchActivity::class.java))
      return true
    } else if (item.itemId == R.id.action_timer) {
//      TimerDialog.newInstance().show(supportFragmentManager, TimerDialog::class.java.simpleName)
      return true
    } else {
      var sortOrder: String? = null
      when (item.itemId) {
        R.id.action_sort_order_title -> {
          sortOrder = SortOrder.SongSortOrder.SONG_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_title_desc -> {
          sortOrder = SortOrder.SongSortOrder.SONG_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_display_title -> {
          sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_display_title_desc -> {
          sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_album -> {
          sortOrder = SortOrder.SongSortOrder.SONG_ALBUM_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_album_desc -> {
          sortOrder = SortOrder.SongSortOrder.SONG_ALBUM_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_artist -> {
          sortOrder = SortOrder.SongSortOrder.SONG_ARTIST_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_artist_desc -> {
          sortOrder = SortOrder.SongSortOrder.SONG_ARTIST_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_date -> {
          sortOrder = SortOrder.SongSortOrder.SONG_DATE
          item.isChecked = true
        }
        R.id.action_sort_order_date_desc -> {
          sortOrder = SortOrder.SongSortOrder.SONG_DATE_DESC
          item.isChecked = true
        }
        R.id.action_sort_order_playlist_name -> {
          sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_playlist_name_desc -> {
          sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_playlist_date -> {
          sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_DATE
          item.isChecked = true
        }
        R.id.action_sort_order_custom -> {
          sortOrder = SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM
          item.isChecked = true
        }
        R.id.action_sort_order_track_number -> {
          sortOrder = SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER
          item.isChecked = true
        }
      }
      if (!TextUtils.isEmpty(sortOrder))
        saveSortOrder(sortOrder)
    }
    return true
  }

  /**
   * Menu 를 생성하는 함수
   */
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    super.onCreateOptionsMenu(menu)
    menuInflater.inflate(getMenuLayoutId(), menu)
    return true
  }

  /**
   * Menu 항목들을 생성하는 함수
   */
  protected fun setUpMenuItem(menu: Menu, sortOrder: String) {
    val subMenu = menu.findItem(R.id.action_sort_order).subMenu
    when (sortOrder) {
      SortOrder.SongSortOrder.SONG_A_Z -> subMenu.findItem(R.id.action_sort_order_title).isChecked = true
      SortOrder.SongSortOrder.SONG_Z_A -> subMenu.findItem(R.id.action_sort_order_title_desc).isChecked = true
      SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z -> subMenu.findItem(R.id.action_sort_order_display_title).isChecked = true
      SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A -> subMenu.findItem(R.id.action_sort_order_display_title_desc).isChecked = true
      SortOrder.SongSortOrder.SONG_ALBUM_A_Z -> subMenu.findItem(R.id.action_sort_order_album).isChecked = true
      SortOrder.SongSortOrder.SONG_ALBUM_Z_A -> subMenu.findItem(R.id.action_sort_order_album_desc).isChecked = true
      SortOrder.SongSortOrder.SONG_ARTIST_A_Z -> subMenu.findItem(R.id.action_sort_order_artist).isChecked = true
      SortOrder.SongSortOrder.SONG_ARTIST_Z_A -> subMenu.findItem(R.id.action_sort_order_artist_desc).isChecked = true
      SortOrder.SongSortOrder.SONG_DATE -> subMenu.findItem(R.id.action_sort_order_date).isChecked = true
      SortOrder.SongSortOrder.SONG_DATE_DESC -> subMenu.findItem(R.id.action_sort_order_date_desc).isChecked = true
      SortOrder.PlayListSortOrder.PLAYLIST_A_Z -> subMenu.findItem(R.id.action_sort_order_playlist_name).isChecked = true
      SortOrder.PlayListSortOrder.PLAYLIST_Z_A -> subMenu.findItem(R.id.action_sort_order_playlist_name_desc).isChecked = true
      SortOrder.PlayListSortOrder.PLAYLIST_DATE -> subMenu.findItem(R.id.action_sort_order_playlist_date).isChecked = true
      SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER -> subMenu.findItem(R.id.action_sort_order_track_number).isChecked = true
      SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM -> subMenu.findItem(R.id.action_sort_order_custom).isChecked = true
    }
  }

  protected open fun saveSortOrder(sortOrder: String?) {

  }

}
