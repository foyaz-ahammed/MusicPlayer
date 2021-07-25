package com.kr.musicplayer.ui.activity.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.TextUtils
import android.util.Log
import com.facebook.drawee.backends.pipeline.Fresco
import com.soundcloud.android.crop.Crop
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import com.kr.musicplayer.R
import com.kr.musicplayer.bean.misc.CustomCover
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.helper.MusicEventCallback
import com.kr.musicplayer.helper.MusicServiceRemote
import com.kr.musicplayer.misc.cache.DiskCache
import com.kr.musicplayer.request.ImageUriRequest
import com.kr.musicplayer.request.network.RxUtil
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.util.Constants
import com.kr.musicplayer.util.MediaStoreUtil
import com.kr.musicplayer.util.ToastUtil
import com.kr.musicplayer.util.Util
import com.kr.musicplayer.util.Util.registerLocalReceiver
import com.kr.musicplayer.util.Util.unregisterLocalReceiver
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 * 기초음악 Activity
 */
@SuppressLint("Registered")
open class BaseMusicActivity : BaseActivity(), MusicEventCallback, CoroutineScope by MainScope() {
  private var serviceToken: MusicServiceRemote.ServiceToken? = null
  private val serviceEventListeners = ArrayList<MusicEventCallback>() // MusicService 의 사건을 받을 Listener 목록
  private var musicStateReceiver: MusicStateReceiver? = null // 상태관련 Receiver
  private var receiverRegistered: Boolean = false // Service 에 접속되였는지 판별
  private var pendingBindService = false

  private val TAG = this.javaClass.simpleName

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.tag(TAG).v("onCreate is called")
    bindToService()
  }

  override fun onStart() {
    super.onStart()
    Timber.tag(TAG).v("onStart(), $pendingBindService")
  }

  override fun onResume() {
    super.onResume()
    Timber.tag(TAG).v("onResume")
    if (pendingBindService) {
      bindToService()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    cancel()
    MusicServiceRemote.unbindFromService(serviceToken)
    musicStateHandler?.removeCallbacksAndMessages(null)
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = true
    }
  }

  /**
   * MusicService 를 binding 하는 함수
   */
  private fun bindToService() {
    if (!Util.isAppOnForeground()) {
      pendingBindService = true
      return
    }
    serviceToken = MusicServiceRemote.bindToService(this, object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val musicService = (service as MusicService.MusicBinder).service
        this@BaseMusicActivity.onServiceConnected(musicService)
      }

      override fun onServiceDisconnected(name: ComponentName) {
        this@BaseMusicActivity.onServiceDisConnected()
      }
    })
    pendingBindService = false
  }

  /**
   * MusicService Event 를 받을 listener 를 추가하는 함수
   */
  fun addMusicServiceEventListener(listener: MusicEventCallback?) {
    if (listener != null) {
      serviceEventListeners.add(listener)
    }
  }

  /**
   * MusicService Event 를 받을 listener 를 제거하는 함수
   */
  fun removeMusicServiceEventListener(listener: MusicEventCallback?) {
    if (listener != null) {
      serviceEventListeners.remove(listener)
    }
  }

  /**
   * MediaStore 가 변경되였을 때 호출되는 callback
   */
  override fun onMediaStoreChanged() {
    ImageUriRequest.clearUriCache()
    for (listener in serviceEventListeners) {
      listener.onMediaStoreChanged()
    }
  }

  /**
   * 저장공간 권한여부가 변경되였을때 호출되는 callback
   * @param has true 이면 권한설정됨, false 이면 권한설정안됨
   */
  override fun onPermissionChanged(has: Boolean) {
    mHasPermission = has
    for (listener in serviceEventListeners) {
      listener.onPermissionChanged(has)
    }
  }

  /**
   * 자료기지의 Playlist table 이 변경되였을때 호출되는 callback
   * @param name 변경된 playList table 이름
   */
  override fun onPlayListChanged(name: String) {
    for (listener in serviceEventListeners) {
      listener.onPlayListChanged(name)
    }
  }

  /**
   * 현재 재생중인 노래가 갱신되였을때 호출되는 callback
   */
  override fun onMetaChanged() {
    Log.d("--------------BaseMusicActivity", serviceEventListeners.toString())
    for (listener in serviceEventListeners) {
      listener.onMetaChanged()
    }
  }

  /**
   * 재생상태가 변경되였을때 호출되는 callback
   */
  override fun onPlayStateChange() {
    for (listener in serviceEventListeners) {
      listener.onPlayStateChange()
    }
  }

  override fun onTagChanged(oldSong: Song, newSong: Song) {
    for (listener in serviceEventListeners) {
      listener.onTagChanged(oldSong, newSong)
    }
  }

  /**
   * MusicService 에 접속하였을때 호출되는 callback
   */
  override fun onServiceConnected(service: MusicService) {
    Log.d("--------------BaseMusicActivity", "onServiceConnected")
    if (!receiverRegistered) {
      musicStateReceiver = MusicStateReceiver(this)
      val filter = IntentFilter()
      filter.addAction(MusicService.PLAYLIST_CHANGE)
      filter.addAction(MusicService.PERMISSION_CHANGE)
      filter.addAction(MusicService.MEDIA_STORE_CHANGE)
      filter.addAction(MusicService.META_CHANGE)
      filter.addAction(MusicService.PLAY_STATE_CHANGE)
      filter.addAction(MusicService.TAG_CHANGE)
      registerLocalReceiver(musicStateReceiver, filter)
      receiverRegistered = true
    }
    for (listener in serviceEventListeners) {
      listener.onServiceConnected(service)
    }
    musicStateHandler = MusicStateHandler(this)
  }

  /**
   * MusicService에서 접속해제되였을때 호출되는 callback
   */
  override fun onServiceDisConnected() {
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = false
    }
    for (listener in serviceEventListeners) {
      listener.onServiceDisConnected()
    }
    musicStateHandler?.removeCallbacksAndMessages(null)
  }

  private var musicStateHandler: MusicStateHandler? = null

  /**
   * Music 상태관련 Handler
   */
  private class MusicStateHandler(activity: BaseMusicActivity) : Handler() {
    private val ref: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun handleMessage(msg: Message?) {
      val action = msg?.obj?.toString()
      Timber.v("BaseMusicActivity MusicStateHandler handleMessage is called action is %s", action)
      val activity = ref.get()
      if (action != null && activity != null) {
        when (action) {
          MusicService.PERMISSION_CHANGE -> {
            activity.onPermissionChanged(msg.data.getBoolean(EXTRA_PERMISSION))
          }
          MusicService.META_CHANGE -> {
            activity.onMetaChanged()
          }
          MusicService.PLAY_STATE_CHANGE -> {
            activity.onPlayStateChange()
          }
          MusicService.TAG_CHANGE -> {
            val newSong = msg.data.getParcelable<Song?>(EXTRA_NEW_SONG)
            val oldSong = msg.data.getParcelable<Song?>(EXTRA_OLD_SONG)

            if (newSong != null && oldSong != null) {
              activity.onTagChanged(oldSong, newSong)
            }
          }
        }
      }

    }
  }

  private class MusicStateReceiver(activity: BaseMusicActivity) : BroadcastReceiver() {
    private val ref: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun onReceive(context: Context, intent: Intent) {
      ref.get()?.musicStateHandler?.let {
        val action = intent.action
        val msg = it.obtainMessage(action.hashCode())
        msg.obj = action
        msg.data = intent.extras
        it.removeMessages(msg.what)
        it.sendMessageDelayed(msg, 50)
      }
    }
  }


  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      Crop.REQUEST_CROP, Crop.REQUEST_PICK -> {
        val intent = intent
        val customCover = intent.getParcelableExtra<CustomCover>("thumb") ?: return
        val errorTxt = getString(
            when {
              customCover.type == Constants.ALBUM -> R.string.set_album_cover_error
              customCover.type == Constants.ARTIST -> R.string.set_artist_cover_error
              else -> R.string.set_playlist_cover_error
            })
        val id = customCover.id //Album, 예술가, 재생목록 표지

        if (resultCode != Activity.RESULT_OK) {
          ToastUtil.show(this, errorTxt)
          return
        }
        if (requestCode == Crop.REQUEST_PICK) {
          //화상선택
          val cacheDir = DiskCache.getDiskCacheDir(this,
              "thumbnail/" + when {
                customCover.type == Constants.ALBUM -> "album"
                customCover.type == Constants.ARTIST -> "artist"
                else -> "playlist"
              })
          if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            ToastUtil.show(this, errorTxt)
            return
          }
          val destination = Uri.fromFile(File(cacheDir, Util.hashKeyForDisk(id.toString() + "")))
          Crop.of(data?.data, destination).asSquare().start(this)
        } else {
          if (data == null) {
            return
          }
          if (Crop.getOutput(data) == null) {
            return
          }

          val path = Crop.getOutput(data).encodedPath
          if (TextUtils.isEmpty(path) || id == -1) {
            ToastUtil.show(mContext, errorTxt)
            return
          }
          Observable
              .create(ObservableOnSubscribe<Uri> { emitter ->
                emitter.onNext(Uri.EMPTY)
                emitter.onComplete()
              })
              .doOnSubscribe {
                //Album Cover 를 설정한 경우 삽입된 표지수정
                if (customCover.type == Constants.ALBUM) {
                  MediaStoreUtil.saveArtwork(mContext, customCover.id, File(path))
                }
              }
              .compose(RxUtil.applyScheduler())
              .doFinally {
                onMediaStoreChanged()
              }
              .subscribe({ uri ->
                val imagePipeline = Fresco.getImagePipeline()
                imagePipeline.clearCaches()
              }, { throwable ->
                ToastUtil.show(mContext, R.string.save_error_arg, throwable.toString())
              })
        }
      }
    }
  }

  companion object {
    const val EXTRA_PLAYLIST = "extra_playlist"
    const val EXTRA_PERMISSION = "extra_permission"
    const val EXTRA_NEW_SONG = "extra_new_song"
    const val EXTRA_OLD_SONG = "extra_old_song"

    //更新适配器
    const val MSG_UPDATE_ADAPTER = 100
    //多选更新
    const val MSG_RESET_MULTI = 101
    //重建activity
    const val MSG_RECREATE_ACTIVITY = 102
  }
}
