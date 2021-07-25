package com.kr.musicplayer.helper

import android.app.Activity
import android.content.*
import android.media.MediaPlayer
import android.os.IBinder
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.service.PlayQueue
import com.kr.musicplayer.util.Constants
import timber.log.Timber
import java.util.*

/**
 * Service 관련 object
 */
object MusicServiceRemote {
  val TAG = MusicServiceRemote::class.java.simpleName

  @JvmStatic
  var service: MusicService? = null

  private val connectionMap = WeakHashMap<Context, ServiceBinder>()

  /**
   * MusicService 를 실행 및 bind 를 진행하는 함수
   * @param context The application context
   * @param callback ServiceConnection callback
   */
  @JvmStatic
  fun bindToService(context: Context, callback: ServiceConnection): ServiceToken? {
    Timber.v("MusicServiceRemote bindToService is called")
    var realActivity: Activity? = (context as Activity).parent
    if (realActivity == null)
      realActivity = context
    val contextWrapper = ContextWrapper(realActivity)
    contextWrapper.startService(Intent(contextWrapper, MusicService::class.java))

    val binder = ServiceBinder(callback)

    if (contextWrapper.bindService(Intent().setClass(contextWrapper, MusicService::class.java), binder, Context.BIND_AUTO_CREATE)) {
      connectionMap[contextWrapper] = binder
      return ServiceToken(contextWrapper)
    }

    return null
  }


  /**
   * MusicService 에 대한 련결을 끊는 함수
   */
  @JvmStatic
  fun unbindFromService(token: ServiceToken?) {
    if (token == null) {
      return
    }
    val contextWrapper = token.wrapperContext
    val binder = connectionMap.remove(contextWrapper) ?: return
    contextWrapper.unbindService(binder)
    if (connectionMap.isEmpty()) {
      service = null
    }
  }

  /**
   * Service 련결관련 class
   */
  class ServiceBinder(private val mCallback: ServiceConnection?) : ServiceConnection {

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      Timber.v("MusicServiceRemote ServiceBinder class onServiceConnected is called")
      val binder = service as MusicService.MusicBinder
      MusicServiceRemote.service = binder.service
      mCallback?.onServiceConnected(className, service)
    }

    override fun onServiceDisconnected(className: ComponentName) {
      mCallback?.onServiceDisconnected(className)
      MusicServiceRemote.service = null
    }
  }

  class ServiceToken(var wrapperContext: ContextWrapper)

  /**
   * 재생대기렬 새로 설정함수
   * @param newQueue 새로 재생대기렬로 설정될 노래목록
   */
  @JvmStatic
  fun setPlayQueue(newQueue: List<Song>) {
    service?.setPlayQueue(newQueue)
  }

  /**
   * 재생대기렬 새로 설정함수
   * @param newQueueIdList 새로 재생대기렬로 설정될 노래목록
   * @param intent
   */
  @JvmStatic
  fun setPlayQueue(newQueueIdList: List<Song>?, intent: Intent) {
    service?.setPlayQueue(newQueueIdList, intent)
  }

  /**
   * 재생대기렬 얻기
   * @return 얻어진 재생대기렬
   */
  @JvmStatic
  fun getPlayQueues(): PlayQueue? {
    return service?.playQueues
  }

  /**
   * 재생방식설정함수
   * @param model 새로 설정될 재생방식
   */
  @JvmStatic
  fun setPlayModel(model: Int) {
    service?.playModel = model
  }

  /**
   * 현재 재생방식 얻는 함수
   * @return 현재 재생방식
   */
  @JvmStatic
  fun getPlayModel(): Int {
    return service?.playModel ?: Constants.MODE_LOOP
  }

  /**
   * MediaPlayer 얻는 함수
   * @return MediaPlayer
   */
  @JvmStatic
  fun getMediaPlayer(): MediaPlayer? {
    return service?.mediaPlayer
  }

  /**
   * 다음 노래 얻는 함수
   * @return 다음 노래
   */
  @JvmStatic
  fun getNextSong(): Song {
    return service?.nextSong ?: Song.EMPTY_SONG
  }

  /**
   * 현재 재생중인 노래 얻는 함수
   * @return 현재 재생중인 노래
   */
  @JvmStatic
  fun getCurrentSong(): Song {
    return service?.currentSong ?: Song.EMPTY_SONG
  }

  /**
   * 지속시간 얻는 함수
   * @return 지속시간
   */
  @JvmStatic
  fun getDuration(): Int {
    return service?.duration ?: 0
  }

  /**
   * 현재 진행률 얻는 함수
   * @return 현재 진행률
   */
  @JvmStatic
  fun getProgress(): Int {
    return service?.progress ?: 0
  }

  /**
   * 진행률 설정 함수
   * @param progress 설정할 진행률
   */
  @JvmStatic
  fun setProgress(progress: Int) {
    service?.setProgress(progress)
  }

  /**
   * 반복조종
   */
  @JvmStatic
  fun handleLoop() {
    service?.handleLoop()
  }

  /**
   * 현재 재생상태 얻는 함수
   * @return true 이면 재생상태이고 false 이면 재생상태가 아니다
   */
  @JvmStatic
  fun isPlaying(): Boolean {
    return service?.isPlaying ?: false
  }

  /**
   * 재생대기렬에서 삭제
   * @param songs 삭제할려는 노래목록
   */
  @JvmStatic
  fun deleteFromService(songs: List<Song>) {
    service?.deleteSongFromService(songs)
  }

  /**
   * 현재 조작상태 얻는 함수
   * @return 현재 조작상태
   */
  @JvmStatic
  fun getOperation(): Int {
    return service?.operation ?: Command.NEXT
  }

  /**
   * 가사 offset 설정함수
   * @param offset 설정하려는 가사 offset
   */
  @JvmStatic
  fun setLyricOffset(offset: Int) {
    service?.setLyricOffset(offset)
  }
}
