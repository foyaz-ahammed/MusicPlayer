package com.kr.musicplayer.ui.misc

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import androidx.core.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kr.musicplayer.App
import com.kr.musicplayer.R
import com.kr.musicplayer.bean.mp3.Album
import com.kr.musicplayer.bean.mp3.Artist
import com.kr.musicplayer.bean.mp3.Folder
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.db.room.DatabaseRepository.Companion.getInstance
import com.kr.musicplayer.db.room.model.PlayList
import com.kr.musicplayer.helper.CloseEvent
import com.kr.musicplayer.misc.getSongIds
import com.kr.musicplayer.request.network.RxUtil.applySingleScheduler
import com.kr.musicplayer.theme.Theme
import com.kr.musicplayer.theme.Theme.getBaseDialog
import com.kr.musicplayer.ui.activity.AllSongsActivity
import com.kr.musicplayer.ui.adapter.*
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder
import com.kr.musicplayer.ui.dialog.ShareDialog
import com.kr.musicplayer.ui.widget.MultiPopupTopWindow
import com.kr.musicplayer.ui.widget.MultiPopupWindow
import com.kr.musicplayer.util.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference

/**
 * 여러가지 유용한 함수들을 담고 있는 class
 */
class MultipleChoice<T>(activity: Activity, val type: Int) : View.OnClickListener, View.OnTouchListener {
  private val activityRef = WeakReference(activity)
  private val disposableContainer = CompositeDisposable()

  private val databaseRepository = getInstance()
  //선택된 모든 position
  private val checkPos = ArrayList<Int>()
  //선택된 모든 파라메터 (Song, Album, Artist, Playlist, Folder)
  private val checkParam = ArrayList<T>()
  //popup window 표시결정
  var isActive: Boolean = false
  var adapter: BaseAdapter<T, BaseViewHolder> ? = null
  var popup: MultiPopupWindow? = null
  var popupTop: MultiPopupTopWindow? = null
  get() {
    return field
  }
  var extra: Int = 0
  var event: CloseEvent? = null
    set(value) {
      field = value
    }

  /**
   * 주어진 id 목록에 관한 노래목록 얻는 함수
   * @param ids id 목록
   */
  private fun getSongsSingle(ids: List<Int>): Single<List<Song>> {
    return Single.fromCallable {
      val songs = ArrayList<Song>()
      ids.forEach {
        songs.add(MediaStoreUtil.getSongById(it))
      }
      songs
    }
  }

  /**
   * 현재 재생목록에 관한 노래 id 목록 얻는 함수
   * @return 노래 Id 목록
   */
  private fun getSongIdSingle(): Single<List<Int>> {
    return Single.fromCallable {
      val ids = ArrayList<Int>()
      if (checkParam.isEmpty()) {
        return@fromCallable ids
      }

      when (type) {
        Constants.SONG, Constants.PLAYLISTSONG -> {
          checkParam.forEach {
            ids.add((it as Song).id)
          }
        }
        Constants.ALBUM -> {
          checkParam.forEach {
            ids.addAll((it as Album).getSongIds())
          }
        }
        Constants.ARTIST -> {
          checkParam.forEach {
            ids.addAll((it as Artist).getSongIds())
          }
        }
        Constants.PLAYLIST -> {
          checkParam.forEach {
            ids.addAll((it as PlayList).audioIds)
          }
        }
        Constants.FOLDER -> {
          checkParam.forEach {
            ids.addAll((it as Folder).getSongIds())
          }
        }

      }
      ids
    }
  }

  /**
   * 선택한 노래들을 삭제하는 함수
   */
  @SuppressLint("CheckResult")
  private fun delete() {
    val context = activityRef.get() ?: return

    val dialog = Theme.getLoadingDialog(context, context.getString(R.string.deleting)).build()

    val view = context.layoutInflater.inflate(R.layout.dialog_delete, null)
    val p = view.findViewById<TextView>(R.id.confirm)
    val n = view.findViewById<TextView>(R.id.cancel)
    val mdialog = AlertDialog.Builder(context, R.style.CustomDialog)
            .setView(view)
            .create()

    p.setOnClickListener {
      val disposable = getSongIdSingle()
              .flatMap { ids ->
                getSongsSingle(ids)
              }
              .flatMap { songs ->
                deleteSingle(type != Constants.PLAYLISTSONG, songs)
              }
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .doOnSubscribe {
                dialog.show()
              }
              .doFinally {
                if (dialog.isShowing) {
                  dialog.dismiss()
                }
                mdialog.dismiss()
                close()
                event?.closeListener()
              }
              .subscribe { count ->
                ToastUtil.show(context, context.getString(R.string.delete_multi_song, count))
              }
      disposableContainer.add(disposable)
    }
    n.setOnClickListener {
      mdialog.dismiss()
    }

    mdialog.show()

    mdialog.window?.setGravity(Gravity.BOTTOM)
    mdialog.window?.setBackgroundDrawable(context.getDrawable(R.drawable.round_gray_top))
    val layoutParams = WindowManager.LayoutParams()
    layoutParams.copyFrom(mdialog.window?.attributes)
    layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
    layoutParams.height = -2
    mdialog.window?.attributes = layoutParams
  }

  /**
   * 노래목록 삭제 함수
   */
  private fun deleteSingle(deleteSource: Boolean, songs: List<Song>): Single<Int> {
    return Single
        .fromCallable {
          when (type) {
            Constants.PLAYLIST -> { //재생목록삭제
              checkParam.forEach {
                val playlist = it as PlayList
                if (playlist.name != activityRef.get()?.getString(R.string.db_my_favorite)) {
                  databaseRepository.deletePlayList((it as PlayList).id).subscribe()
                }
              }

              songs.size
            }
            Constants.PLAYLISTSONG -> { //재생목록에서 노래 삭제
              if (deleteSource) {
                MediaStoreUtil.delete(songs, true)
              } else {
                databaseRepository.deleteFromPlayList(songs.map { it.id }, extra).blockingGet()
              }
            }
            else -> {
              MediaStoreUtil.delete(songs, deleteSource)
            }
          }
        }
  }

  /**
   * 재생목록에 노래 추가하는 함수
   */
  @SuppressLint("CheckResult")
  private fun addToPlayList() {
    val activity = activityRef.get() ?: return
    //모든 재생 목록에 대한 정보 얻기
    val disposable = databaseRepository.getAllPlaylist()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ playLists ->
          val list = playLists.map {
            it.name
          }.toMutableList()
          list.removeAt(0)
          list.add(0, activity.getString(R.string.my_favorite))
          list.add(activity.getString(R.string.create_playlist))
          val d = getBaseDialog(activity)
                  .title(R.string.add_to_playlist)
                  .items(list.toList())
                  .itemsCallback { dialog, view, which, text ->
                    //기존재생목록에 직접 추가
                    if (which != list.indexOf(list.last())) {
                      getSongIdSingle()
                              .flatMap {
                                databaseRepository.insertToPlayList(it, playLists[which].name)
                              }
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribeOn(Schedulers.io())
                              .doFinally {
                                close()
                              }
                              .subscribe(Consumer {
                                if (playLists[which].name == activity.getString(R.string.db_my_favorite)) {
                                  if (it == 0) {
                                    ToastUtil.show(activity, activity.getString(R.string.already_exists_songs_in_favorite))
                                  } else {
                                    ToastUtil.show(activity, activity.getString(R.string.add_song_my_favorite_success, it))
                                  }
                                } else {
                                  if (it == 0) {
                                    ToastUtil.show(activity, activity.getString(R.string.already_exists_songs_in_playlist, playLists[which].name))
                                  } else {
                                    ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, it, playLists[which].name))
                                  }
                                }
                              })
                    } else {
                      //새로 추가한 후 새 목록에 추가

                      val v: View = activity.layoutInflater.inflate(R.layout.dialog_new_playlist, null)
                      val p = v.findViewById<TextView>(R.id.confirm)
                      val n = v.findViewById<TextView>(R.id.cancel)
                      val et = v.findViewById<EditText>(R.id.title)
                      et.setText(activity.getString(R.string.local_list) + playLists.size)
                      et.isFocusable = true
                      et.requestFocus()

                      val mdialog = AlertDialog.Builder(activity, R.style.CustomDialog)
                              .setView(v)
                              .create()
                      n.setOnClickListener { v1: View? ->
                        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(et.windowToken, 0)
                        mdialog.dismiss()
                      }
                      p.setOnClickListener { v1: View? ->
                        if (!TextUtils.isEmpty(et.text)) {
                          databaseRepository
                                  .insertPlayList(et.text.toString())
                                  .flatMap {
                                    getSongIdSingle()
                                  }
                                  .flatMap {
                                    databaseRepository.insertToPlayList(it, et.text.toString())
                                  }
                                  .compose(applySingleScheduler())
                                  .subscribe({
                                    ToastUtil.show(activity, R.string.add_playlist_success)
                                    ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, it, et.text.toString()))
                                  }, {
                                    ToastUtil.show(activity, activity.getString(R.string.add_error))
                                  })
                        }
                        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(et.windowToken, 0)
                        mdialog.dismiss()
                        close()
                      }

                      mdialog.window.setSoftInputMode(
                              WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                      mdialog.show()

                      mdialog.window.setGravity(Gravity.BOTTOM)
                      mdialog.window.setBackgroundDrawable(activity.getDrawable(R.drawable.round_gray_top))
                      val layoutParams = WindowManager.LayoutParams()
                      layoutParams.copyFrom(mdialog.window.attributes)
                      layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                      layoutParams.height = -2
                      mdialog.window.attributes = layoutParams
                    }
                  }
                  .build()
          MaterialDialogHelper.adjustAlertDialog(d, activity.getDrawable(R.drawable.round_gray_top))
          d.show()
        }, {
          ToastUtil.show(activity, activity.getString(R.string.add_error))
        })
    disposableContainer.add(disposable)
  }

  /**
   * Click Event 처리함수
   * @param pos Click 한 위치
   * @param data Click 한 자료
   * @return popup window 가 현시되였으면 true 아니면 false
   */
  fun click(pos: Int, data: T): Boolean {
    if (!isActive)
      return false
    changeData(pos, data)
    closeIfNeed()
    adapter?.notifyItemChanged(pos)
    return true
  }

  /**
   * LongClick Event 처리함수
   * @param pos Click 한 위치
   * @param data Click 한 자료
   * @return popup window 가 현시되였으면 true 아니면 false
   */
  fun longClick(pos: Int, data: T): Boolean {
    //목록이 이미 선택되여 있는지 확인
    if (!isActive && isActiveSomeWhere) {
      return false
    }
    if (!isActive) {
      open()
      isActive = true
      isActiveSomeWhere = true
      Util.vibrate(App.getContext(), 100)
    }

    changeData(pos, data)
    closeIfNeed()
    adapter?.notifyItemChanged(pos)
    return true
  }

  /**
   * 자료를 받아 모두 선택
   * @param data 선택하려는 자료목록
   * @return popup window 가 현시되였으면 true 아니면 false
   */
  fun selectAll(data: List<T>): Boolean {
    if (!isActive && isActiveSomeWhere) {
      return false
    }
    if (!isActive) {
      open()
      isActive = true
      isActiveSomeWhere = true
      Util.vibrate(App.getContext(), 100)
    }

    for (x in data.indices) {
      if (!checkPos.contains(x)) {
        changeData(x, data[x])
      }
    }
    closeIfNeed()
    adapter?.notifyDataSetChanged()
    return true
  }

  /**
   * 선택한 항목이 하나도 없으면 popup window 닫기
   */
  private fun closeIfNeed() {
    if (checkPos.isEmpty()) {
      close()
    }
  }

  /**
   * 선택한 항목들을 모두 취소하는 함수
   */
  private fun clearCheck() {
    checkPos.clear()
    checkParam.clear()
    adapter?.notifyDataSetChanged()
  }

  /**
   * 선택상태변경함수
   * @param pos 변경하려는 위치
   * @param data 변경하려는 자료
   */
  private fun changeData(pos: Int, data: T) {
    if (checkPos.contains(pos)) {
      checkPos.remove(pos)
      checkParam.remove(data)
    } else {
      checkPos.add(pos)
      checkParam.add(data)
    }
    val text: TextView = popupTop?.contentView?.findViewById<View>(R.id.multi_text) as TextView
    text.text = activityRef.get()?.getString(R.string.multi_top_text, checkPos.size)
    if (adapter?.currentList?.size == checkPos.size) {
      changeSelectStatusTitleToNone()
    } else {
      changeSelectStatusTitleToAll()
    }

    val flag = type != Constants.FOLDER && type != Constants.PLAYLIST && type != Constants.ARTIST && checkPos.size == 1
    val flagList = type != Constants.PLAYLIST

    popup?.contentView?.findViewById<View>(R.id.set_ringtone)?.isEnabled = flag
    popup?.contentView?.findViewById<View>(R.id.multi_playlist)?.isEnabled = flagList

    val ringtoneText = popup?.contentView?.findViewById<TextView>(R.id.set_ringtone_title)
    val addList = popup?.contentView?.findViewById<TextView>(R.id.multi_playlist_text)
    if (!flag) {
      ringtoneText?.setTextColor(App.getContext().getColor(R.color.drawer_effect_dark))
    } else {
      ringtoneText?.setTextColor(App.getContext().getColor(R.color.light_text_color_primary))
    }

    if (!flagList) {
      addList?.setTextColor(App.getContext().getColor(R.color.drawer_effect_dark))
    } else {
      addList?.setTextColor(App.getContext().getColor(R.color.light_text_color_primary))
    }
  }

  /**
   * Popup Window 현시
   */
  @SuppressLint("RestrictedApi")
  fun open() {
    val activity = activityRef.get() ?: return
    activity.findViewById<FloatingActionButton>(R.id.playbar_play)?.visibility = View.GONE
    activity.findViewById<BottomAppBar>(R.id.bottom_action_bar)?.visibility = View.GONE
    activity.findViewById<LinearLayout>(R.id.container_play)?.visibility = View.GONE
    activity.findViewById<View>(R.id.container_playbar_play)?.visibility = View.GONE
    activity.findViewById<TabLayout>(R.id.tabs)?.visibility = View.GONE
    activity.findViewById<TextView>(R.id.title)?.visibility = View.GONE
    popupTop = MultiPopupTopWindow(activity)
    popupTop?.show(View(activity))
    popupTop?.contentView?.findViewById<View>(R.id.select_all)?.setOnClickListener(this)
    popup = MultiPopupWindow(activity)
    popup?.show(View(activity))

    popup?.contentView?.findViewById<View>(R.id.multi_divider)?.visibility = if (activity is AllSongsActivity) View.VISIBLE else View.GONE
    popup?.contentView?.findViewById<View>(R.id.multi_playlist)?.setOnClickListener(this)
    popup?.contentView?.findViewById<View>(R.id.check_all)?.setOnClickListener(this)
    popup?.contentView?.findViewById<View>(R.id.multi_popup)?.setOnClickListener(this)
    popup?.contentView?.findViewById<View>(R.id.set_ringtone)?.setOnClickListener(this)
    popup?.contentView?.findViewById<View>(R.id.remove)?.setOnClickListener(this)
    popup?.contentView?.findViewById<View>(R.id.transfer)?.setOnClickListener(this)

    popup?.contentView?.findViewById<View>(R.id.multi_playlist)?.setOnTouchListener(this)
    popup?.contentView?.findViewById<View>(R.id.set_ringtone)?.setOnTouchListener(this)
    popup?.contentView?.findViewById<View>(R.id.remove)?.setOnTouchListener(this)
    popup?.contentView?.findViewById<View>(R.id.transfer)?.setOnTouchListener(this)
  }

  /**
   * Popup Window 닫기
   */
  @SuppressLint("RestrictedApi")
  fun close() {
    // Popup Window 를 닫을때 tabLayout 현시
    val activity = activityRef.get() ?: return
    activity.findViewById<TabLayout>(R.id.tabs)?.visibility = View.VISIBLE
    activity.findViewById<TextView>(R.id.title)?.visibility = View.VISIBLE
    disposableContainer.clear()
    isActive = false
    isActiveSomeWhere = false
    popup?.dismiss()
    activity.findViewById<FloatingActionButton>(R.id.playbar_play)?.visibility = View.VISIBLE
    activity.findViewById<BottomAppBar>(R.id.bottom_action_bar)?.visibility = View.VISIBLE
    activity.findViewById<LinearLayout>(R.id.container_play)?.visibility = View.VISIBLE
    activity.findViewById<View>(R.id.container_playbar_play)?.visibility = View.VISIBLE

    popup = null
    popupTop?.dismiss()
    popupTop = null
    clearCheck()
  }

  /**
   * 주어진 위치의 항목의 선택여부를 얻는 함수
   * @param pos 선택여부를 얻을려는 위치
   * @return 선택상태이면 true 아니면 false
   */
  fun isPositionCheck(pos: Int): Boolean {
    return checkPos.contains(pos)
  }

  /**
   * 선택상태를 보여주는 제목을 "모두 선택 취소" 로 설정하는 함수
   */
  fun changeSelectStatusTitleToNone() {
    popupTop?.contentView?.findViewById<TextView>(R.id.select_all)?.setText(R.string.select_none)
  }

  /**
   * 선택상태를 보여주는 제목을 "모두선택"으로 설정하는 함수
   */
  fun changeSelectStatusTitleToAll() {
    popupTop?.contentView?.findViewById<TextView>(R.id.select_all)?.setText(R.string.select_all)
  }

  @RequiresApi(Build.VERSION_CODES.KITKAT)
  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.set_ringtone -> {
        MediaStoreUtil.setRing(activityRef.get(), (checkParam[0] as Song).id)
      }
      R.id.transfer -> {
        val songs = ArrayList<Song>()

        when (type) {
          Constants.SONG, Constants.PLAYLISTSONG -> {
            songs.addAll(checkParam.map {
              it as Song
            }.toTypedArray())
          }
          Constants.ALBUM -> {
            checkParam.map {
              songs.addAll(MediaStoreUtil.getSongsByIds((it as Album).getSongIds()).toTypedArray())
            }
          }
          Constants.ARTIST -> {
            checkParam.map {
              songs.addAll(MediaStoreUtil.getSongsByIds((it as Artist).getSongIds()).toTypedArray())
            }
          }
          Constants.PLAYLIST -> {
            checkParam.map {
              songs.addAll(MediaStoreUtil.getSongsByIds((it as PlayList).audioIds.toList()).toTypedArray())
            }
          }
          Constants.FOLDER -> {
            checkParam.map {
              songs.addAll(MediaStoreUtil.getSongsByIds((it as Folder).getSongIds()).toTypedArray())
            }
          }
        }
        ShareDialog(activityRef.get(), songs.toTypedArray()).show()
      }
      R.id.remove -> {
        delete()
      }
      R.id.multi_playlist -> {
        addToPlayList()
      }
      R.id.check_all -> {
        adapter?.currentList?.let { selectAll(it) }
      }
      R.id.select_all -> {
        if (adapter?.currentList?.size == checkPos.size) {
          close()
        } else {
          changeSelectStatusTitleToNone()
          adapter?.currentList?.let { selectAll(it) }
        }
        event?.closeListener()
      }
    }
  }

  override fun toString(): String {
    return "MultipleChoice(activity=${activityRef.get()}, type=$type, checkPos=$checkPos, checkParam=$checkParam, isActive=$isActive, adapter=$adapter, popup=$popup, extra=$extra)"
  }

  override fun onTouch(v: View?, event: MotionEvent?): Boolean {
    val c: ColorStateList? = if (event!!.action == MotionEvent.ACTION_UP) {
      activityRef.get()?.applicationContext?.let {
        ContextCompat.getColorStateList(
                it,
                R.color.light_text_color_secondary
        )
      }
    } else {
      activityRef.get()?.applicationContext?.let {
        ContextCompat.getColorStateList(
                it,
                R.color.md_blue_primary
        )
      }
    }
    ((v as ViewGroup).getChildAt(0) as ImageView).imageTintList = c
    return false
  }

  companion object {
    @JvmStatic
    var isActiveSomeWhere = false
  }
}

