package com.kr.musicplayer.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.WorkerThread
import com.kr.musicplayer.App
import com.kr.musicplayer.R
import com.kr.musicplayer.appshortcuts.Controller
import com.kr.musicplayer.appwidgets.BaseAppwidget
import com.kr.musicplayer.appwidgets.big.AppWidgetBig
import com.kr.musicplayer.appwidgets.extra.AppWidgetExtra
import com.kr.musicplayer.appwidgets.medium.AppWidgetMedium
import com.kr.musicplayer.appwidgets.medium.AppWidgetMediumTransparent
import com.kr.musicplayer.appwidgets.small.AppWidgetSmall
import com.kr.musicplayer.appwidgets.small.AppWidgetSmallTransparent
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.bean.mp3.Song.Companion.EMPTY_SONG
import com.kr.musicplayer.db.room.DatabaseRepository
import com.kr.musicplayer.db.room.model.PlayQueue
import com.kr.musicplayer.helper.*
import com.kr.musicplayer.lyric.LyricFetcher
import com.kr.musicplayer.lyric.LyricFetcher.Companion.LYRIC_FIND_INTERVAL
import com.kr.musicplayer.lyric.bean.LyricRowWrapper
import com.kr.musicplayer.misc.floatpermission.FloatWindowManager
import com.kr.musicplayer.misc.log.LogObserver
import com.kr.musicplayer.misc.receiver.ExitReceiver
import com.kr.musicplayer.misc.receiver.HeadsetPlugReceiver
import com.kr.musicplayer.misc.receiver.HeadsetPlugReceiver.Companion.NEVER
import com.kr.musicplayer.misc.receiver.HeadsetPlugReceiver.Companion.OPEN_SOFTWARE
import com.kr.musicplayer.misc.receiver.MediaButtonReceiver
import com.kr.musicplayer.misc.tryLaunch
import com.kr.musicplayer.request.RemoteUriRequest
import com.kr.musicplayer.request.RequestConfig
import com.kr.musicplayer.request.network.RxUtil.applySingleScheduler
import com.kr.musicplayer.service.notification.Notify
import com.kr.musicplayer.service.notification.NotifyImpl
import com.kr.musicplayer.ui.activity.base.BaseActivity.EXTERNAL_STORAGE_PERMISSIONS
import com.kr.musicplayer.ui.activity.base.BaseMusicActivity
import com.kr.musicplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PERMISSION
import com.kr.musicplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import com.kr.musicplayer.ui.viewmodel.registerObserver
import com.kr.musicplayer.ui.widget.desktop.DesktopLyricView
import com.kr.musicplayer.util.*
import com.kr.musicplayer.util.Constants.*
import com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
import com.kr.musicplayer.util.SPUtil.SETTING_KEY
import com.kr.musicplayer.util.Util.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

/**
 * 노래 재생 조종 및 Activity 로부터의 Callback 처리, 알림창 처리를 위한 Service
 */
@SuppressLint("CheckResult")
class MusicService : BaseService(), Playback, MusicEventCallback,
    SharedPreferences.OnSharedPreferenceChangeListener, CoroutineScope by MainScope() {
  private var contentObserver: ContentObserver? = null // MediaStore 변화감지를 위한 contentObserver
  // 재생대기렬
  private val playQueue = PlayQueue(this)
  val playQueues: com.kr.musicplayer.service.PlayQueue get() = playQueue

  // 현재 재생중인 노래
  var currentSong: Song
    get() = playQueue.song
    set(s){
      playQueue.song = s
    }

  // 다음 노래
  val nextSong: Song
    get() = playQueue.nextSong

  /**
   * 처음으로 Service 가 준비되였는지 확인여부
   */
  private var firstPrepared = true

  private var firstLoaded = true

  /**
   * MediaPlayer 의 자료 source 설정여부
   */
  private var prepared = false

  /**
   * 자료읽기가 끝났는지 확인여부
   */
  private var loadFinished = false

  private var playListJob : Job? = null

  /**
   * 재생방식을 설정하고 다음 노래를 갱신한다
   */
  var playModel: Int = MODE_LOOP
    set(newPlayModel) {
      val fromShuffleToNone = field == MODE_SHUFFLE
      field = newPlayModel
      desktopWidgetTask?.run()
      SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_MODEL, newPlayModel)

      // 임의로 재생하는 방식으로 전환하려면 현재 재생중인 노래에 따라 위치를 다시 결정
      if (fromShuffleToNone) {
        playQueue.rePosition()
      }
      playQueue.makeList()
      playQueue.updateNextSong()
      updateQueueItem()
    }

  /**
   * 현재 재생여부
   */
  private var isPlay: Boolean = false

  /**
   * 현재 재생중인 노래가 favourite 목록에 있는 노래인지 확인
   */
  var isLove: Boolean = false

  /**
   * 현재 노래 재생후 앱 중지여부
   */
  private var exitAfterCompletion: Boolean = false

  /**
   * 노래 재생조종을 관리한다
   */
  var mediaPlayer: MediaPlayer = MediaPlayer()

  /**
   * Desktop Widget
   */
  private val appWidgets: HashMap<String, BaseAppwidget> = HashMap()

  /**
   * AudioManager
   */
  private val audioManager: AudioManager by lazy {
    getSystemService(Context.AUDIO_SERVICE) as AudioManager
  }

  /**
   * 재생 조종을 위한 Receiver
   */
  private val controlReceiver: ControlReceiver by lazy {
    ControlReceiver()
  }

  /**
   * Music Event 관련 Receiver
   */
  private val musicEventReceiver: MusicEventReceiver by lazy {
    MusicEventReceiver()
  }

  /**
   * 련결되지 않은 headset을 monitoring 하는 Receiver
   */
  private val headSetReceiver: HeadsetPlugReceiver by lazy {
    HeadsetPlugReceiver()
  }

  /**
   * Widget 관련 Receiver
   */
  private val widgetReceiver: WidgetReceiver by lazy {
    WidgetReceiver()
  }

  /**
   * AudioFocus Listener
   */
  private val audioFocusListener by lazy {
    AudioFocusChangeListener()
  }

  /**
   * MediaSession
   */
  lateinit var mediaSession: MediaSessionCompat
    private set

  /**
   * 현재 AudioFocus를 가져올지 확인여부
   */
  private var audioFocus = false

  /**
   * Activity 와의 련동을 위한 Handler
   */
  private val uiHandler = PlaybackHandler(this)

  /**
   * 화면잠금
   */
  private val wakeLock: PowerManager.WakeLock by lazy {
    (getSystemService(Context.POWER_SERVICE) as PowerManager)
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.simpleName)
  }

  /**
   * 알림창
   */
  private lateinit var notify: Notify

  /**
   * 현재 조종된느 명령
   */
  private var control: Int = 0

  /**
   * WindowManager
   */
  private val windowManager: WindowManager by lazy {
    getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }

  /**
   * Desktop 가사 표시 여부
   */
  private var showDesktopLyric = false
    set(value) {
      field = value
    }

  /**
   * Desktop 가사표시 조종
   */
  private var desktopLyricView: DesktopLyricView? = null

  /**
   * Service 중지여부
   */
  var stop = true

  /**
   * 잠금화면 관련 Receiver
   */
  private val screenReceiver: ScreenReceiver by lazy {
    ScreenReceiver()
  }
  private var screenOn = true

  /**
   * 음량조절
   */
  private val volumeController: VolumeController by lazy {
    VolumeController(this)
  }

  /**
   * 프로그람을 끝낼때 재생진행률상태 반영
   */
  private var lastProgress: Int = 0

  /**
   * 중단점 재생 여부
   */
  private var playAtBreakPoint: Boolean = false
  private var progressTask: ProgressTask? = null

  /**
   * 작업류형
   */
  var operation = -1

  /**
   * Binder
   */
  private val musicBinder = MusicBinder()

  /**
   * db
   */
  val repository = DatabaseRepository.getInstance()

  private lateinit var service: MusicService

  private var hasPermission = false // 권한허용 검사

  private var alreadyUnInit: Boolean = false
  private var speed = 1.0f // 재생속도

  fun setExitAfterCompletion(value: Boolean) {
    exitAfterCompletion = value
  }

  /**
   * 재생중인지 확인
   */
  val isPlaying: Boolean
    get() = isPlay

  /**
   * 현재 재생진행률 가져오기
   */
  val progress: Int
    get() {
      try {
        if (prepared) {
          return mediaPlayer.currentPosition
        }
      } catch (e: IllegalStateException) {
        Timber.v("getProgress() %s", e.toString())
      }

      return 0
    }

  val duration: Int
    get() = if (prepared) {
      mediaPlayer.duration
    } else 0

  /**
   * Destkop 가사 및 widget 갱신
   */
  private var timer: Timer = Timer()
  private var desktopLyricTask: LyricTask? = null
  private var desktopWidgetTask: WidgetTask? = null


  private var needShowDesktopLyric: Boolean = false

  /**
   * Desktop 가사 상태여부
   */
  private var isDesktopLyricInitializing = false

  /**
   * Desktop 가사 표시여부
   */
  val isDesktopLyricShowing: Boolean
    get() = desktopLyricView != null

  /**
   * Desktop 가사가 잠겨있는지 확인여부
   */
  val isDesktopLyricLocked: Boolean
    get() = if (desktopLyricView == null)
      SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, false)
    else
      desktopLyricView?.isLocked == true

  var abLoopHandler = Handler()

  private val abLoop: Runnable = object : Runnable {
    override fun run() {
      if (MaterialDialogHelper.timeA > 0 && MaterialDialogHelper.timeB > 0) {
        val currPos = mediaPlayer.currentPosition
        if (currPos + 250 >= MaterialDialogHelper.timeB || currPos >= currentSong.getDuration() || currPos < MaterialDialogHelper.timeA) {
          mediaPlayer.seekTo(MaterialDialogHelper.timeA)
        }
        abLoopHandler.postDelayed(this, 100)
      }
    }
  }
  /**
   * 잠금화면
   */
  private var lockScreen: Int = CLOSE_LOCKSCREEN

  override fun onDestroy() {
    Timber.tag(TAG_LIFECYCLE).v("onDestroy")
    super.onDestroy()
    stop = true
    unInit()
  }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(LanguageHelper.setLocal(base))
  }

  override fun onCreate() {
    super.onCreate()
    Timber.tag(TAG_LIFECYCLE).v("onCreate")
    service = this
    setUp()
  }

  override fun onBind(intent: Intent): IBinder? {
    return musicBinder
  }

  inner class MusicBinder : Binder() {
    val service: MusicService
      get() = this@MusicService
  }

  @SuppressLint("CheckResult")
  override fun onStartCommand(commandIntent: Intent?, flags: Int, startId: Int): Int {
    val control = commandIntent?.getIntExtra(EXTRA_CONTROL, -1)
    val action = commandIntent?.action

    Timber.tag(TAG_LIFECYCLE).v("onStartCommand, control: $control action: $action flags: $flags startId: $startId")
    stop = false

    tryLaunch(block = {
      hasPermission = hasPermissions(EXTERNAL_STORAGE_PERMISSIONS)
      if (!loadFinished && hasPermission) {
        withContext(Dispatchers.IO) {
          load()
        }
      }
      handleStartCommandIntent(commandIntent, action)
      if (firstLoaded) {
        uiHandler.postDelayed({ sendLocalBroadcast(Intent(LOAD_FINISHED)) }, 400)
      }
      firstLoaded = false;
    })
    return START_NOT_STICKY
  }

  override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String) {
    Timber.v("onSharedPreferenceChanged, key: $key")
    when (key) {
      //알림창 배경색
      SETTING_KEY.NOTIFY_SYSTEM_COLOR,
        //알림창 style
      SETTING_KEY.NOTIFY_STYLE_CLASSIC -> {
        notify = NotifyImpl(this@MusicService)
        if (Notify.isNotifyShowing) {
          // 취소 및 다시표시하여 알림창을 완전히 새로 고침
          notify.cancelPlayingNotify()
          updateNotification()
        }
      }
      //잠금화면
      SETTING_KEY.LOCKSCREEN -> {
        lockScreen = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN, APLAYER_LOCKSCREEN)
        when (lockScreen) {
          CLOSE_LOCKSCREEN -> clearMediaSession()
          SYSTEM_LOCKSCREEN, APLAYER_LOCKSCREEN -> updateMediaSession(Command.NEXT)
        }
      }
      //중단점 재생
      SETTING_KEY.PLAY_AT_BREAKPOINT -> {
        playAtBreakPoint = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_AT_BREAKPOINT, false)
        if (!playAtBreakPoint) {
          stopSaveProgress()
        } else {
          startSaveProgress()
        }
      }
      //속도조절
      SETTING_KEY.SPEED -> {
        speed = java.lang.Float.parseFloat(SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.SPEED, "1.0"))
        setSpeed(speed)
      }
    }
  }

  private fun setUp() {
    //구성 변경
    getSharedPreferences(SETTING_KEY.NAME, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this)

    //화면잠금
    wakeLock.setReferenceCounted(false)
    //알림창
    notify = NotifyImpl(this)


    //Desktop Widget
    appWidgets[APPWIDGET_BIG] = AppWidgetBig.getInstance()
    appWidgets[APPWIDGET_MEDIUM] = AppWidgetMedium.getInstance()
    appWidgets[APPWIDGET_MEDIUM_TRANSPARENT] = AppWidgetMediumTransparent.getInstance()
    appWidgets[APPWIDGET_SMALL] = AppWidgetSmall.getInstance()
    appWidgets[APPWIDGET_SMALL_TRANSPARENT] = AppWidgetSmallTransparent.getInstance()
    appWidgets[APPWIDGET_EXTRA_TRANSPARENT] = AppWidgetExtra.getInstance()

    //Receiver 초기화
    val eventFilter = IntentFilter()
    eventFilter.addAction(MEDIA_STORE_CHANGE)
    eventFilter.addAction(PERMISSION_CHANGE)
    eventFilter.addAction(PLAYLIST_CHANGE)
    eventFilter.addAction(TAG_CHANGE)
    registerLocalReceiver(musicEventReceiver, eventFilter)

    registerLocalReceiver(controlReceiver, IntentFilter(ACTION_CMD))

    val noisyFilter = IntentFilter()
    noisyFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    noisyFilter.addAction(Intent.ACTION_HEADSET_PLUG)
    registerReceiver(headSetReceiver, noisyFilter)

    registerLocalReceiver(widgetReceiver, IntentFilter(ACTION_WIDGET_UPDATE))

    val screenFilter = IntentFilter()
    screenFilter.addAction(Intent.ACTION_SCREEN_ON)
    screenFilter.addAction(Intent.ACTION_SCREEN_OFF)
    registerReceiver(screenReceiver, screenFilter)

    //MediaStore 변경감지
    if (contentObserver == null) {
      contentObserver = contentResolver.registerObserver(
              MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
      ) {
        updatePlayList()
      }
    } else {
      updatePlayList()
    }

    updatePlayList()

    //SleepTimer
    SleepTimer.addCallback(object : SleepTimer.Callback {
      override fun onFinish() {
        exitAfterCompletion = true
      }
    })

    setUpPlayer()
    setUpSession()
  }

  /**
   * 재생목록 및 재생대기렬 갱신
   */
  private fun updatePlayList() {
    playListJob?.cancel()
    playListJob = CoroutineScope(Dispatchers.IO).launch {
      yield()
      //mediastore에 존재하지 않는 음악들이 playlist에 존재하면 제거
      val deletePlayListIds = arrayListOf<Int>()
      val playlists = DatabaseRepository.getInstance().getAllPlaylists()
      for (playlist in playlists) {
        val ids = playlist.audioIds
        for (id in ids) {
          yield()
          val song = MediaStoreUtil.getSongById(id)
          if (song.id < 0) {
            deletePlayListIds.add(id)
          }
        }
        if (deletePlayListIds.size > 0) {
          DatabaseRepository.getInstance().deleteFromPlayLists(deletePlayListIds, playlist.id)
        }
      }

      //현재 노래대기렬에 존재하지 않는 음악들이 있으면 대기렬에서 제거
      var queueList = ArrayList<Int>()
      queueList = DatabaseRepository.getInstance().getPlayQueue().blockingGet() as ArrayList<Int>

      val id = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE, -1)
      val type = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE_TYPE, -1)

      val deleteQueueIds = arrayListOf<Int>()
      for (queue in queueList) {
        yield()
        val song = MediaStoreUtil.getSongById(queue)
        if (song.id < 0) {
          deleteQueueIds.add(queue)
        }
      }

      if (deleteQueueIds.size > 0) {
        DatabaseRepository.getInstance().deleteIdsFromQueue(deleteQueueIds)
      }

      //현재 노래대기렬의 음악들이 playlist의 음악들이 아닌 경우 새로 추가된 노래들에 해당하여 대기렬에 추가
      if (type != PLAYLIST) {
        yield()
        var typeSongs = arrayListOf<Song>()
        when (type) {
          Constants.SONG -> typeSongs = MediaStoreUtil.getAllSong() as ArrayList<Song>
          Constants.ALBUM, Constants.ARTIST -> if (id > 0) typeSongs = MediaStoreUtil.getSongsByArtistIdOrAlbumId(id, type) as ArrayList<Song>
          Constants.FOLDER -> if (id > 0) typeSongs = MediaStoreUtil.getSongsByParentId(id) as ArrayList<Song>
        }

        val addedIds = arrayListOf<Int>()
        for (song in typeSongs) {
          if (!queueList.contains(song.id))
            addedIds.add(song.id)
        }

        yield()
        if (addedIds.size > 0) {
          DatabaseRepository.getInstance().insertIdsToQueue(addedIds)
        }
      }
    }

    playListJob?.start()
  }

  /**
   * MediaSession 초기화
   */
  private fun setUpSession() {
    val mediaButtonReceiverComponentName = ComponentName(applicationContext,
            MediaButtonReceiver::class.java)

    val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
    mediaButtonIntent.component = mediaButtonReceiverComponentName

    val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, mediaButtonIntent, 0)

    mediaSession = MediaSessionCompat(applicationContext, "APlayer", mediaButtonReceiverComponentName, pendingIntent)
    mediaSession.setCallback(object : MediaSessionCompat.Callback() {
      override fun onMediaButtonEvent(event: Intent): Boolean {
        return MediaButtonReceiver.handleMediaButtonIntent(this@MusicService, event)
      }

      override fun onSkipToNext() {
        Timber.v("onSkipToNext")
        playNext()
      }

      override fun onSkipToPrevious() {
        Timber.v("onSkipToPrevious")
        playPrevious()
      }

      override fun onPlay() {
        Timber.v("onPlay")
        play(true)
      }

      override fun onPause() {
        Timber.v("onPause")
        pause(false)
      }

      override fun onStop() {
        stopSelf()
      }

      override fun onSeekTo(pos: Long) {
        setProgress(pos.toInt())
      }
    })

    mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
    mediaSession.setMediaButtonReceiver(pendingIntent)
    mediaSession.isActive = true
  }

  /**
   * MediaPlayer 초기화
   */
  private fun setUpPlayer() {
    mediaPlayer = MediaPlayer()

    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
    mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)

    mediaPlayer.setOnCompletionListener { mp ->
      if(exitAfterCompletion){
        sendBroadcast(Intent(ACTION_EXIT)
                .setComponent(ComponentName(this@MusicService, ExitReceiver::class.java)))
        return@setOnCompletionListener
      }
      if (playModel == MODE_REPEAT) {
        if (isPlaying)
          prepare(playQueue.song.url)
      } else {
        if (isPlaying)
          playNextOrPrev(true)
      }
      operation = Command.NEXT
      acquireWakeLock()
    }
    mediaPlayer.setOnPreparedListener { mp ->
      Timber.v("MusicService setUpPlayer mediaplayer setOnPreparedListener is called and firstPrepared is %s and AUTO_PLAY is %s",
              firstPrepared, SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.AUTO_PLAY, NEVER))

      if (firstPrepared) {
        firstPrepared = false
        if (lastProgress > 0) {
          mediaPlayer.seekTo(lastProgress)
        }
        //자동재생
        if (SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.AUTO_PLAY, NEVER) != OPEN_SOFTWARE) {
          return@setOnPreparedListener
        }
      }

      //재생기록
      updatePlayHistory()
      //재생시작
      play(false)
    }

    mediaPlayer.setOnErrorListener { mp, what, extra ->
      try {
        prepared = false
        mediaPlayer.release()
        setUpPlayer()
        ToastUtil.show(service, R.string.mediaplayer_error, what, extra)
        if (isPlaying && playQueue.song.url.isNotEmpty()) {
          prepare(playQueue.song.url)
        }
        return@setOnErrorListener true
      } catch (ignored: Exception) {

      }
      false
    }

    EQHelper.init(this, mediaPlayer.audioSessionId)
    EQHelper.open(this, mediaPlayer.audioSessionId)
  }


  /**
   * 재생기록 갱신
   */
  private fun updatePlayHistory() {
    repository.updateHistory(playQueue.song)
        .compose(applySingleScheduler())
        .subscribe(LogObserver())
  }

  private fun unInit() {
    if (alreadyUnInit) {
      return
    }

    cancel()

    EQHelper.close(this, mediaPlayer.audioSessionId)
    if (isPlaying) {
      pause(false)
    }
    mediaPlayer.release()
    loadFinished = false
    prepared = false
    Controller.getController().updateContinueShortcut(this)

    timer.cancel()
    notify.cancelPlayingNotify()

    removeDesktopLyric()

    uiHandler.removeCallbacksAndMessages(null)
    showDesktopLyric = false

    audioManager.abandonAudioFocus(audioFocusListener)
    mediaSession.isActive = false
    mediaSession.release()

    unregisterLocalReceiver(controlReceiver)
    unregisterLocalReceiver(musicEventReceiver)
    unregisterLocalReceiver(widgetReceiver)
    unregisterReceiver(this, headSetReceiver)
    unregisterReceiver(this, screenReceiver)

    getSharedPreferences(SETTING_KEY.NAME, Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this)

    releaseWakeLock()

    ShakeDetector.getInstance().stopListen()

    alreadyUnInit = true
  }

  fun updateQueueItem() {
    Timber.v("updateQueueItem")
    tryLaunch(block = {
      withContext(Dispatchers.IO) {
        val queue = ArrayList(playQueue.playingQueue)
                .map { song ->
                  return@map MediaSessionCompat.QueueItem(MediaMetadataCompat.Builder()
                          .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id.toString())
                          .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                          .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                          .build().description, song.id.toLong())
                }
        Timber.v("updateQueueItem, queue: ${queue.size}")
        mediaSession.setQueueTitle(playQueue.song.title)
        mediaSession.setQueue(queue)
      }
    }, catch = {
      ToastUtil.show(this, it.toString())
      Timber.w(it)
    })
  }

  /**
   * 재생대기렬 설정
   */
  fun setPlayQueue(newQueue: List<Song>?) {
    Timber.v("setPlayQueue")
    if (newQueue == null || newQueue.isEmpty()) {
      return
    }
    if (newQueue == playQueue.originalQueue) {
      return
    }

    playQueue.setPlayQueue(newQueue)
    updateQueueItem()
  }

  /**
   * 재생대기렬설정
   */
  fun setPlayQueue(newQueue: List<Song>?, intent: Intent) {
    //임의로 재생인 경우 randomList 를 갱신
    val shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE, false)
    if (newQueue == null || newQueue.isEmpty()) {
      return
    }

    //설정된 재생대기렬이 이전 대기렬과 동일한지 확인
    val equals = newQueue == playQueue.originalQueue
    if (!equals) {
      playQueue.setPlayQueue(newQueue)
    }
    if (shuffle) {
      playModel = MODE_SHUFFLE
      playQueue.updateNextSong()
    }
    handleCommand(intent)

    if (equals) {
      return
    }
    updateQueueItem()
  }

  private fun setPlay(isPlay: Boolean) {
    this.isPlay = isPlay
    uiHandler.sendEmptyMessage(UPDATE_PLAY_STATE)
  }

  /**
   * 다음 노래 재생
   */
  override fun playNext() {
    playNextOrPrev(true)
  }

  /**
   * 이전 노래 재생
   */
  override fun playPrevious() {
    playNextOrPrev(false)
  }

  /**
   * 재생시작
   */
  override fun play(fadeIn: Boolean) {
    Timber.v("play: $fadeIn")
    audioFocus = audioManager.requestAudioFocus(
            audioFocusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    if (!audioFocus) {
      return
    }

    setPlay(true)

    //모든 interface 갱신
    uiHandler.sendEmptyMessage(UPDATE_META_DATA)

    //재생
    mediaPlayer.start()

    //속도설정
    setSpeed(speed)

    if (fadeIn) {
      volumeController.fadeIn()
    } else {
      volumeController.directTo(1f)
    }

    //현재 재생중인 노래의 id 보관
    launch {
      withContext(Dispatchers.IO) {
        SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.LAST_SONG_ID, playQueue.song.id)
      }
    }
  }


  /**
   * 현재 재생상태에 따라 재생 일시 중지 또는 재생
   */
  override fun toggle() {
    Timber.v("toggle")
    if (mediaPlayer.isPlaying) {
      pause(false)
    } else {
      if (MaterialDialogHelper.timeA > -1) {
        setProgress(MaterialDialogHelper.timeA)
      }
      play(true)
    }
  }

  /**
   * 일시중지
   */
  override fun pause(updateMediasessionOnly: Boolean) {
    Timber.v("pause: $updateMediasessionOnly")
    if (updateMediasessionOnly) {
      updateMediaSession(operation)
    } else {
      if (!isPlaying) { //이미 중지된 경우 알림창을 닫은후 다시 표시되지 않도록 작업을 반복하지 않는다
        return
      }
      setPlay(false)
      uiHandler.sendEmptyMessage(UPDATE_META_DATA)
      volumeController.fadeOut()
    }
  }

  /**
   * 선택한 노래를 재생한다
   *
   * @param position 재생하려는 노래의 위치
   */
  override fun playSelectSong(position: Int) {
    Timber.v("playSelectSong, $position")

    if (position == -1 || position >= playQueue.playingQueue.size) {
      ToastUtil.show(service, R.string.illegal_arg)
      return
    }

    playQueue.setPosition(position)

    if (playQueue.song.url.isEmpty()) {
      ToastUtil.show(service, R.string.song_lose_effect)
      return
    }
    prepare(playQueue.song.url)
    playQueue.updateNextSong()
  }

  override fun onMediaStoreChanged() {
    launch {
      val song = withContext(Dispatchers.IO) {
        MediaStoreUtil.getSongById(playQueue.song.id)
      }
      playQueue.song = song
    }
  }

  /**
   * App에 필요한 권한사항이 변경되는 경우 Service Logic 다시 실행
   */
  override fun onPermissionChanged(has: Boolean) {
    if (has != hasPermission && has) {
      hasPermission = true
      loadSync()
    }
  }

  override fun onTagChanged(oldSong: Song, newSong: Song) {
//    // 改变的歌曲是当前播放的
//    if (oldSong.id == currentSong.id) {
//      currentSong = newSong
//      currentId = newSong.id
//    }
  }

  /**
   * 자료기지의 PlayList 자료나 PlayQueue 자료가 갱신되면 호출
   */
  override fun onPlayListChanged(name: String) {
    if (name == PlayQueue.TABLE_NAME) {
      repository
          .getPlayQueueSongs()
          .compose(applySingleScheduler())
          .subscribe { songs ->
            if (songs.isEmpty() || songs == playQueue.originalQueue) {
              Timber.v("Ignore onPlayListChanged")
              return@subscribe
            }
            Timber.v("New PlayQueue: ${songs.size}")

            playQueue.setPlayQueue(songs)

            // 임의로 재생방식이면 RandomQueue 재생성
            if (playModel == MODE_SHUFFLE) {
              Timber.v("Reset the random queue after the play queue is changed")
              playQueue.makeList()
            }

            // 다음 노래가 대기렬에 없는 경우 다음 노래 재생성
            if (!playQueue.playingQueue.contains(playQueue.nextSong)) {
              Timber.v("Reset the next song after the play queue is changed")
              playQueue.updateNextSong()
            }
          }
    }
  }

  override fun onMetaChanged() {

  }

  override fun onPlayStateChange() {

  }

  override fun onServiceConnected(service: MusicService) {

  }

  override fun onServiceDisConnected() {

  }

  inner class WidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      val name = intent.getStringExtra(BaseAppwidget.EXTRA_WIDGET_NAME)
      val appIds = intent.getIntArrayExtra(BaseAppwidget.EXTRA_WIDGET_IDS)
      Timber.v("name: $name appIds: $appIds")
      when (name) {
        APPWIDGET_BIG -> if (appWidgets[APPWIDGET_BIG] != null) {
          appWidgets[APPWIDGET_BIG]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_MEDIUM -> if (appWidgets[APPWIDGET_MEDIUM] != null) {
          appWidgets[APPWIDGET_MEDIUM]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_SMALL -> if (appWidgets[APPWIDGET_SMALL] != null) {
          appWidgets[APPWIDGET_SMALL]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_MEDIUM_TRANSPARENT -> if (appWidgets[APPWIDGET_MEDIUM_TRANSPARENT] != null) {
          appWidgets[APPWIDGET_MEDIUM_TRANSPARENT]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_SMALL_TRANSPARENT -> if (appWidgets[APPWIDGET_SMALL_TRANSPARENT] != null) {
          appWidgets[APPWIDGET_SMALL_TRANSPARENT]?.updateWidget(service, appIds, true)
        }
        APPWIDGET_EXTRA_TRANSPARENT -> if (appWidgets[APPWIDGET_EXTRA_TRANSPARENT] != null) {
          appWidgets[APPWIDGET_EXTRA_TRANSPARENT]?.updateWidget(service, appIds, true)
        }
      }
    }
  }

  inner class MusicEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      handleMusicEvent(intent)
    }
  }

  private fun handleStartCommandIntent(commandIntent: Intent?, action: String?) {
    Timber.v("handleStartCommandIntent")
    if (action == null) {
      return
    }
    firstPrepared = false
    when (action) {
      ACTION_APPWIDGET_OPERATE -> {
        val appwidgetIntent = Intent(ACTION_CMD)
        val control = commandIntent?.getIntExtra(EXTRA_CONTROL, -1)
        if (control == UPDATE_APPWIDGET) {
          updateAppwidget()
        } else {
          appwidgetIntent.putExtra(EXTRA_CONTROL, control)
          handleCommand(appwidgetIntent)
        }
      }
      ACTION_SHORTCUT_CONTINUE_PLAY -> {
        val continueIntent = Intent(ACTION_CMD)
        continueIntent.putExtra(EXTRA_CONTROL, Command.TOGGLE)
        handleCommand(continueIntent)
      }
      ACTION_SHORTCUT_SHUFFLE -> {
        if (playModel != MODE_SHUFFLE) {
          playModel = MODE_SHUFFLE
        }
        val shuffleIntent = Intent(ACTION_CMD)
        shuffleIntent.putExtra(EXTRA_CONTROL, Command.NEXT)
        handleCommand(shuffleIntent)
      }
      ACTION_SHORTCUT_MYLOVE -> {
        tryLaunch {
          val songs = withContext(Dispatchers.IO) {
            val myLoveIds = repository.getMyLoveList().blockingGet()
            MediaStoreUtil.getSongsByIds(myLoveIds)
          }

          if (songs == null || songs.isEmpty()) {
            ToastUtil.show(service, R.string.list_is_empty)
            return@tryLaunch
          }

          val myloveIntent = Intent(ACTION_CMD)
          myloveIntent.putExtra(EXTRA_CONTROL, Command.PLAYSELECTEDSONG)
          myloveIntent.putExtra(EXTRA_POSITION, 0)
          setPlayQueue(songs, myloveIntent)
        }

      }
      ACTION_SHORTCUT_LASTADDED -> {
        tryLaunch {
          val songs = withContext(Dispatchers.IO) {
            MediaStoreUtil.getLastAddedSong()
          }
          if (songs == null || songs.size == 0) {
            ToastUtil.show(service, R.string.list_is_empty)
            return@tryLaunch
          }
          val lastedIntent = Intent(ACTION_CMD)
          lastedIntent.putExtra(EXTRA_CONTROL, Command.PLAYSELECTEDSONG)
          lastedIntent.putExtra(EXTRA_POSITION, 0)
          setPlayQueue(songs, lastedIntent)
        }

      }
      else -> if (action.equals(ACTION_CMD, ignoreCase = true)) {
        handleCommand(commandIntent)
      }
    }
  }

  private fun handleMusicEvent(intent: Intent?) {
    if (intent == null) {
      return
    }
    val action = intent.action
    when {
      MEDIA_STORE_CHANGE == action -> onMediaStoreChanged()
      PERMISSION_CHANGE == action -> onPermissionChanged(intent.getBooleanExtra(EXTRA_PERMISSION, false))
      PLAYLIST_CHANGE == action -> onPlayListChanged(intent.getStringExtra(EXTRA_PLAYLIST))
      TAG_CHANGE == action -> {
        val newSong = intent.getParcelableExtra<Song?>(BaseMusicActivity.EXTRA_NEW_SONG)
        val oldSong = intent.getParcelableExtra<Song?>(BaseMusicActivity.EXTRA_OLD_SONG)
        if (newSong != null && oldSong != null) {
          onTagChanged(oldSong, newSong)
        }
      }
    }
  }

  private fun handleMetaChange() {
    if (playQueue.song == EMPTY_SONG) {
      return
    }
    updateAppwidget()
    if (needShowDesktopLyric) {
      showDesktopLyric = true
      needShowDesktopLyric = false
    }
    updateDesktopLyric(false)
    updateNotification()
    updateMediaSession(operation)
    // 현재 재생된 정형을 보관
    if (playAtBreakPoint) {
      startSaveProgress()
    }

    sendLocalBroadcast(Intent(META_CHANGE))
  }

  /**
   * 알림창 갱신
   */
  private fun updateNotification() {
    notify.updateForPlaying()
  }

  /**
   * 재생상태가 변경처리를 위한 함수
   */
  private fun handlePlayStateChange() {
    if (playQueue.song == EMPTY_SONG) {
      return
    }
    //Desktop 재생단추 갱신
    desktopLyricView?.setPlayIcon(isPlaying)
    Controller.getController().updateContinueShortcut(this)
    sendLocalBroadcast(Intent(PLAY_STATE_CHANGE))
  }

  private var last = System.currentTimeMillis()

  /**
   * 재생조종관련 Receiver
   */
  inner class ControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
      handleCommand(intent)
    }
  }

  /**
   * 재생조종관련 Receiver 를 받은후 처리
   */
  private fun handleCommand(intent: Intent?) {
    Timber.v("handleCommand is called: %s", intent)
    if (intent == null || intent.extras == null) {
      return
    }
    val control = intent.getIntExtra(EXTRA_CONTROL, -1)
    this@MusicService.control = control
    Timber.v("control: $control")

    if (control == Command.PLAYSELECTEDSONG || control == Command.PREV || control == Command.NEXT
        || control == Command.TOGGLE || control == Command.PAUSE || control == Command.START) {
      //간격시간 판단
      if ((control == Command.PREV || control == Command.NEXT) && System.currentTimeMillis() - last < 500) {
        return
      }
      //재생조종명령 저장
      operation = control
      if (playQueue.originalQueue.isEmpty()) {
        //재생대기렬이 비여있는 경우 다시 읽기
        launch(context = Dispatchers.IO) {
          playQueue.restoreIfNecessary()
        }
        return
      }
    }


    when (control) {
      //알림창 닫기
      Command.CLOSE_NOTIFY -> {
        Notify.isNotifyShowing = false
        pause(false)
        needShowDesktopLyric = true
        showDesktopLyric = false
        uiHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
        stopUpdateLyric()
        uiHandler.postDelayed({ notify.cancelPlayingNotify() }, 300)
      }
      //선택한 노래 재생
      Command.PLAYSELECTEDSONG -> {
        playSelectSong(intent.getIntExtra(EXTRA_POSITION, -1))
      }
      //이전 노래 재생
      Command.PREV -> {
        playPrevious()
      }
      //다음 노래 재생
      Command.NEXT -> {
        playNext()
      }
      Command.FAST_FORWARD -> {
        if (mediaPlayer.duration > 3000) {
          mediaPlayer.seekTo(mediaPlayer.currentPosition + 3000)
        } else {
          mediaPlayer.seekTo(mediaPlayer.duration)
        }
        play(false)
      }
      Command.FAST_BACK -> {
        if (mediaPlayer.currentPosition > 3000) {
          mediaPlayer.seekTo(mediaPlayer.currentPosition - 3000)
        } else {
          mediaPlayer.seekTo(0)
        }
        play(false)
      }
      //재생 일시 중지 또는 재생
      Command.TOGGLE -> {
        toggle()
      }
      //재생 중지
      Command.PAUSE -> {
        pause(false)
      }
      //재생
      Command.START -> {
        play(false)
      }
      //재생방식 변경
      Command.CHANGE_MODEL -> {
        playModel = if (playModel == MODE_REPEAT) MODE_LOOP else playModel + 1
      }
      //Desktop 가사
      Command.TOGGLE_DESKTOP_LYRIC -> {
        val open: Boolean = if (intent.hasExtra(EXTRA_DESKTOP_LYRIC)) {
          intent.getBooleanExtra(EXTRA_DESKTOP_LYRIC, false)
        } else {
          !SPUtil.getValue(service,
                  SETTING_KEY.NAME,
                  SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
        }
        if (open && !FloatWindowManager.getInstance().checkPermission(service)) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissionIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            permissionIntent.data = Uri.parse("package:$packageName")
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isIntentAvailable(service, permissionIntent)) {
              startActivity(permissionIntent)
            }
          }
          ToastUtil.show(service, R.string.plz_give_float_permission)
          return
        }
        SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_SHOW, open)
        if (showDesktopLyric != open) {
          showDesktopLyric = open
          ToastUtil.show(service, if (showDesktopLyric) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc)
          if (showDesktopLyric) {
            updateDesktopLyric(false)
          } else {
            closeDesktopLyric()
          }
        }
      }
      //일시적으로 노래 재생
      Command.PLAY_TEMP -> {
        intent.getParcelableExtra<Song>(EXTRA_SONG)?.let {
          operation = Command.PLAY_TEMP
          playQueue.song = it
          prepare(playQueue.song.url)
        }
      }
      //Desktop 가사 잠금해제
      Command.UNLOCK_DESKTOP_LYRIC -> {
        if (desktopLyricView != null) {
          desktopLyricView?.saveLock(false, true)
        } else {
          SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, false)
        }
        //알림창 갱신
        updateNotification()
      }
      //Desktop 가사 잠금, 알림창 갱신
      Command.LOCK_DESKTOP_LYRIC -> {
        //알림창 갱신
        updateNotification()
      }
      //다음 노래 추가
      Command.ADD_TO_NEXT_SONG -> {
        val nextSong = intent.getParcelableExtra<Song>(EXTRA_SONG) ?: return
        //재생 대기렬에 추가
        playQueue.addNextSong(nextSong)
        ToastUtil.show(service, R.string.already_add_to_next_song)
      }
      //Desktop 가사 변경
      Command.CHANGE_LYRIC -> {
        if (showDesktopLyric) {
          updateDesktopLyric(true)
        }
      }
      //Timer 전환
      Command.TOGGLE_TIMER -> {
        val hasDefault = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.TIMER_DEFAULT, false)
        if (!hasDefault) {
          ToastUtil.show(service, getString(R.string.plz_set_default_time))
        }
        val time = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.TIMER_DURATION, -1)
        SleepTimer.toggleTimer((time * 1000).toLong())
      }
      else -> {
        Timber.v("unknown command")
      }
    }
  }

  /**
   * 재생상태만 갱신해야 하는지 확인여부
   */
  private fun updatePlayStateOnly(cmd: Int): Boolean {
    return cmd == Command.PAUSE || cmd == Command.START || cmd == Command.TOGGLE
  }

  /**
   * 잠금 화면에 표시된 내용 지우기
   */
  private fun clearMediaSession() {
    mediaSession.setMetadata(MediaMetadataCompat.Builder().build())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mediaSession.setPlaybackState(
              PlaybackStateCompat.Builder().setState(PlaybackState.STATE_NONE, 0, 1f).build())
    } else {
      mediaSession.setPlaybackState(PlaybackStateCompat.Builder().build())
    }
  }


  /**
   * 잠금화면 갱신
   */
  private fun updateMediaSession(control: Int) {
    val currentSong = playQueue.song
    if (currentSong == EMPTY_SONG || lockScreen == CLOSE_LOCKSCREEN) {
      return
    }

    val builder = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, currentSong.id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentSong.album)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentSong.artist)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, currentSong.artist)
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentSong.displayName)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentSong.getDuration())
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, (playQueue.position + 1).toLong())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentSong.title)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, playQueue.size().toLong())
    }

    if (updatePlayStateOnly(control)) {
      mediaSession.setMetadata(builder.build())
    } else {
      object : RemoteUriRequest(getSearchRequestWithAlbumType(currentSong),
              RequestConfig.Builder(400, 400).build()) {
        override fun onError(throwable: Throwable) {
          setMediaSessionData(null)
        }

        override fun onSuccess(result: Bitmap?) {
          setMediaSessionData(result)
        }

        private fun setMediaSessionData(result: Bitmap?) {
          val bitmap = copy(result)
          builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
          mediaSession.setMetadata(builder.build())
        }
      }.load()
    }

    updatePlaybackState()
  }

  private fun updatePlaybackState() {
    mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setActiveQueueItemId(currentSong.id.toLong())
            .setState(if (isPlay) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED, progress.toLong(), speed)
            .setActions(MEDIA_SESSION_ACTIONS).build())
  }

  /**
   * 재생준비
   *
   * @param path 음악재생경로
   */
  private fun prepare(path: String, requestFocus: Boolean = true) {
    tryLaunch(
            block = {
              Timber.v("재생준비: %s, isPlaying: %s", path, isPlaying)
              if (TextUtils.isEmpty(path)) {
                ToastUtil.show(service, getString(R.string.path_empty))
                return@tryLaunch
              }

              val exist = withContext(Dispatchers.IO) {
                File(path).exists()
              }
              if (!exist) {
                ToastUtil.show(service, getString(R.string.file_not_exist))
                return@tryLaunch
              }
              if (requestFocus) {
                audioFocus = audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                if (!audioFocus) {
                  ToastUtil.show(service, getString(R.string.cant_request_audio_focus))
                  return@tryLaunch
                }
              }
              if (isPlaying) {
                pause(true)
              }

              prepared = false
              isLove = withContext(Dispatchers.IO) {
                repository.isMyLove(playQueue.song.id)
                        .onErrorReturn {
                          false
                        }
                        .blockingGet()
              }
              mediaPlayer.reset()
              withContext(Dispatchers.IO) {
                mediaPlayer.setDataSource(path)
              }
              mediaPlayer.prepareAsync()
              prepared = true
              Timber.v("prepare finish: $path")
            },
            catch = {
              ToastUtil.show(service, getString(R.string.play_failed) + it.toString())
              prepared = false
            })
  }

  /**
   * 현재 재생방식에 따라 이전 또는 다음 노래로 전환
   *
   * @param isNext 다음 노래 재생 여부
   */
  fun playNextOrPrev(isNext: Boolean) {
    if (playQueue.size() == 0) {
      ToastUtil.show(service, getString(R.string.list_is_empty))
      return
    }
    Timber.v("play next song")
    if (isNext) {
      playQueue.next()
    } else {
      playQueue.previous()
    }

    if (playQueue.song == EMPTY_SONG) {
      ToastUtil.show(service, R.string.song_lose_effect)
      return
    }
    setPlay(true)
    prepare(playQueue.song.url)
  }

  /**
   * MediaPlayer 재생 진행률 설정
   */
  fun setProgress(current: Int) {
    if (prepared) {
      mediaPlayer.seekTo(current)
      updatePlaybackState()
    }
  }

  /**
   * 반복재생조종
   */
  fun handleLoop() {
    abLoopHandler.removeCallbacks(abLoop)
    abLoopHandler.postDelayed(abLoop, 100)
  }

  /**
   * 재생속도설정
   */
  private fun setSpeed(speed: Float) {
    if (prepared && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer.isPlaying) {
      try {
        mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(speed)
      } catch (e: Exception) {
        Timber.w(e)
      }
    }
  }

  /**
   * 노래 Id 목록 및 재생대기렬 읽기
   */
  private fun loadSync() {
    launch(context = Dispatchers.IO) {
      load()
    }
  }

  /**
   * 자료읽기
   */
  @WorkerThread
  @Synchronized
  private fun load() {
    val isFirst = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, true)
    SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, false)
    //처음으로 App 시작
    if (isFirst) {
      //즐겨찾기항목 playList table에 넣기
      repository.insertPlayList(getString(R.string.db_my_favorite)).subscribe(LogObserver())

      //알림창 style
      SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_STYLE_CLASSIC,
              Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
    }

    //떨림
    if (SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.SHAKE, false)) {
      ShakeDetector.getInstance().beginListen()
    }

    //사용자설정
    lockScreen = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN, APLAYER_LOCKSCREEN)
    playModel = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.PLAY_MODEL, MODE_LOOP)
    showDesktopLyric = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
    speed = java.lang.Float.parseFloat(SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.SPEED, "1.0"))
    playAtBreakPoint = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.PLAY_AT_BREAKPOINT, false)
    lastProgress = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.LAST_PLAY_PROGRESS, 0)

    //재생목록읽기
    val fromOther = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.PLAY_FROM_OTHER_APP, false)
    if (!fromOther) {
      playQueue.restoreIfNecessary()
      prepare(playQueue.song.url)
    } else {
      SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.PLAY_FROM_OTHER_APP, false)
      firstPrepared = false
    }
    loadFinished = true

    uiHandler.postDelayed({ sendLocalBroadcast(Intent(META_CHANGE)) }, 400)
  }

  fun deleteSongFromService(deleteSongs: List<Song>?) {
    if (deleteSongs != null && deleteSongs.isNotEmpty()) {
      playQueue.removeAll(deleteSongs)
    }
  }

  /**
   * 화면잠금해제
   */
  private fun releaseWakeLock() {
    if (wakeLock.isHeld) {
      wakeLock.release()
    }
  }

  /**
   * 화면잠금 받기
   */
  private fun acquireWakeLock() {
    wakeLock.acquire(if (playQueue.song != EMPTY_SONG) playQueue.song.getDuration() else 30000L)
  }


  /**
   * Desktop 가사 갱신
   */
  private fun updateDesktopLyric(force: Boolean) {
    Timber.v("updateDesktopLyric, showDesktopLyric: $showDesktopLyric")
    if (!showDesktopLyric) {
      return
    }
    if (checkNoPermission()) { //권한이 거부된 상태
      return
    }
    if (!isPlaying) {
      stopUpdateLyric()
    } else {
      //화면이 켜질때만 갱신
      if (screenOn) {
        //가사 갱신
        desktopLyricTask?.force = force
        startUpdateLyric()
      }
    }
  }

  /**
   * Floating Window 허용 여부, 가사 닫기 허용 여부 판단
   */
  private fun checkNoPermission(): Boolean {
    try {
      if (!FloatWindowManager.getInstance().checkPermission(service)) {
        closeDesktopLyric()
        return true
      }
      return false
    } catch (e: Exception) {
      Timber.v(e)
    }

    return true
  }

  /**
   * Desktop Widget 갱신
   */
  private fun updateAppwidget() {
    //진행률 표시줄 및 시간 갱신을 중지하려면 일시중지
    if (!isPlaying) {
      //일시 중지후 다시 갱신하지 않함
      //그러므로 Desktop Widget 조종의 재생 및 일시중지단추상태가 옳바른지 확인하려면 중지하기전에 한번 갱신해야 한다
      desktopWidgetTask?.run()
      stopUpdateAppWidget()
    } else {
      if (screenOn) {
        appWidgets.forEach {
          it.value.updateWidget(this, null, true)
        }
        //재생시작후 진행률 표시줄 및 시간 갱신
        startUpdateAppWidget()
      }
    }
  }

  /**
   * Desktop Widget 갱신중지
   */
  private fun stopUpdateAppWidget() {
    desktopWidgetTask?.cancel()
    desktopWidgetTask = null
    appWidgets.forEach {
      it.value.updateWidget(this, null, true)
    }
  }

  /**
   * 재생시작후 진행률 표시줄 및 시간 갱신
   */
  private fun startUpdateAppWidget() {
    if (desktopWidgetTask != null) {
      return
    }
    desktopWidgetTask = WidgetTask()
    timer.schedule(desktopWidgetTask, INTERVAL_APPWIDGET, INTERVAL_APPWIDGET)
  }


  /**
   * 가사 갱신
   */
  private fun startUpdateLyric() {
    if (desktopLyricTask != null) {
      return
    }
    desktopLyricTask = LyricTask()
    timer.schedule(desktopLyricTask, LYRIC_FIND_INTERVAL, LYRIC_FIND_INTERVAL)
  }

  /**
   * 가사 갱신중지
   */
  private fun stopUpdateLyric() {
    desktopLyricTask?.cancel()
    desktopLyricTask = null
  }

  private inner class WidgetTask : TimerTask() {
    private val tag: String = WidgetTask::class.java.simpleName

    override fun run() {
      val isAppOnForeground = isAppOnForeground()
      // App 이 Foreground 상태인 경우 갱신할 필요가 없음
      if (!isAppOnForeground) {
        appWidgets.forEach {
          uiHandler.post {
            it.value.partiallyUpdateWidget(service)
          }
        }
      }
    }

    override fun cancel(): Boolean {
      return super.cancel()
    }
  }

  fun setLyricOffset(offset: Int) {
    desktopLyricTask?.lyricFetcher?.offset = offset
  }

  private inner class LyricTask : TimerTask() {
    private var songInLyricTask = EMPTY_SONG
    val lyricFetcher = LyricFetcher(this@MusicService)
    var force = false

    override fun run() {
      if (!showDesktopLyric) {
        uiHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
        return
      }
      if (songInLyricTask != playQueue.song) {
        songInLyricTask = playQueue.song
        lyricFetcher.updateLyricRows(songInLyricTask)
        return
      }
      if (force) {
        force = false
        lyricFetcher.updateLyricRows(songInLyricTask)
        return
      }

      if (checkNoPermission()) {
        return
      }
      if (stop) {
        uiHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
        return
      }
      //현재 App 이 Foreground 상태에 있음
      if (isAppOnForeground()) {
        if (isDesktopLyricShowing) {
          uiHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
        }
      } else {
        if (!isDesktopLyricShowing) {
          uiHandler.removeMessages(CREATE_DESKTOP_LRC)
          uiHandler.sendEmptyMessageDelayed(CREATE_DESKTOP_LRC, 50)
        } else {
          uiHandler.obtainMessage(UPDATE_DESKTOP_LRC_CONTENT, lyricFetcher.findCurrentLyric()).sendToTarget()
        }
      }
    }

    override fun cancel(): Boolean {
      lyricFetcher.dispose()
      return super.cancel()
    }

    fun cancelByNotification() {
      needShowDesktopLyric = true
      showDesktopLyric = false
      uiHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
      cancel()
    }
  }

  /**
   * Desktop 가사 창조
   */
  private fun createDesktopLyric() {
    if (checkNoPermission()) {
      return
    }
    if (isDesktopLyricInitializing) {
      return
    }
    isDesktopLyricInitializing = true

    val param = WindowManager.LayoutParams()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      param.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      param.type = WindowManager.LayoutParams.TYPE_PHONE
    }

    param.format = PixelFormat.RGBA_8888
    param.gravity = Gravity.TOP
    param.width = resources.displayMetrics.widthPixels
    param.height = ViewGroup.LayoutParams.WRAP_CONTENT
    param.x = 0
    param.y = SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_Y, 0)

    if (desktopLyricView != null) {
      windowManager.removeView(desktopLyricView)
      desktopLyricView = null
    }

    desktopLyricView = DesktopLyricView(service)
    windowManager.addView(desktopLyricView, param)
    isDesktopLyricInitializing = false
  }

  /**
   * Desktop 가사 제거
   */
  private fun removeDesktopLyric() {
    if (desktopLyricView != null) {
      //      desktopLyricView.cancelNotify();
      windowManager.removeView(desktopLyricView)
      desktopLyricView = null
    }
  }

  /**
   * Desktop 가사 끄기
   */
  private fun closeDesktopLyric() {
    SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
    showDesktopLyric = false
    stopUpdateLyric()
    uiHandler.removeMessages(CREATE_DESKTOP_LRC)
    uiHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC)
  }

  /**
   * 현재 재생상태 보관
   */
  private fun startSaveProgress() {
    if (progressTask != null) {
      return
    }
    progressTask = ProgressTask()
    timer.schedule(progressTask, 100, LYRIC_FIND_INTERVAL)
  }

  /**
   * 현재 재생상태 보관취소
   */
  private fun stopSaveProgress() {
    SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.LAST_PLAY_PROGRESS, 0)
    progressTask?.cancel()
    progressTask = null
  }


  /**
   * 현재 재생 진행상황 저장
   */
  private inner class ProgressTask : TimerTask() {
    override fun run() {
      val progress = progress
      if (progress > 0) {
        SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.LAST_PLAY_PROGRESS, progress)
      }
    }

  }


  private inner class AudioFocusChangeListener : AudioManager.OnAudioFocusChangeListener {

    //Focus 변경을 기록하기전에 현재 재생중인지 확인;
    private var needContinue = false

    override fun onAudioFocusChange(focusChange: Int) {
      when (focusChange) {
        //AudioFocus 받기
        AudioManager.AUDIOFOCUS_GAIN -> {
          audioFocus = true
          if (!prepared) {
            setUpPlayer()
          } else if (needContinue) {
            play(true)
            needContinue = false
            operation = Command.TOGGLE
          }
          volumeController.directTo(1f)
        }
        //짧은 멈춤
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
          needContinue = isPlay
          if (isPlay && prepared) {
            operation = Command.TOGGLE
            pause(false)
          }
        }
        //음량 낮추기
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          volumeController.directTo(.1f)
        }
        //시간초과
        AudioManager.AUDIOFOCUS_LOSS -> {
          val ignoreFocus = SPUtil.getValue(this@MusicService, SETTING_KEY.NAME, SETTING_KEY.AUDIO_FOCUS, false)
          if (ignoreFocus) {
            Timber.v("Ignore AudioFocus without pause")
            return
          }
          audioFocus = false
          if (isPlay && prepared) {
            operation = Command.TOGGLE
            pause(false)
          }
        }
      }
    }
  }

  /**
   * Activity 와의 련동을 위한 Handler
   */
  private class PlaybackHandler internal constructor(
          service: MusicService,
          private val ref: WeakReference<MusicService> = WeakReference(service))
    : Handler() {

    override fun handleMessage(msg: Message) {
      if (ref.get() == null) {
        return
      }
      val musicService = ref.get() ?: return
      when (msg.what) {
        UPDATE_PLAY_STATE -> musicService.handlePlayStateChange()
        UPDATE_META_DATA -> {
          musicService.handleMetaChange()
        }
        UPDATE_DESKTOP_LRC_CONTENT -> {
          if (msg.obj is LyricRowWrapper) {
            val wrapper = msg.obj as LyricRowWrapper
            musicService.desktopLyricView?.setText(wrapper.lineOne, wrapper.lineTwo)
          }
        }
        REMOVE_DESKTOP_LRC -> {
          musicService.removeDesktopLyric()
        }
        CREATE_DESKTOP_LRC -> {
          musicService.createDesktopLyric()
        }
      }
    }
  }

  /**
   * 잠금화면 관련 Receiver
   */
  private inner class ScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action
      Timber.tag("ScreenReceiver").v(action)
      if (Intent.ACTION_SCREEN_ON == action) {
        screenOn = true
        //Desktop 가사 재 표시
        updateDesktopLyric(false)
        //App Widget 갱신 다시시작
        updateAppwidget()
      } else {
        screenOn = false
        //Desktop Widget 갱신 중지
        stopUpdateAppWidget()
        //Desktop 가사 끄기
        stopUpdateLyric()
      }
    }
  }

  companion object {
    const val TAG_DESKTOP_LYRIC = "LyricTask"
    const val TAG_LIFECYCLE = "ServiceLifeCycle"
    const val EXTRA_DESKTOP_LYRIC = "DesktopLyric"
    const val EXTRA_SONG = "Song"
    const val EXTRA_POSITION = "Position"

    //Desktop Widget 갱신
    const val UPDATE_APPWIDGET = 1000
    //현재 재생중인 노래 갱신
    const val UPDATE_META_DATA = 1002
    //재생 상태 갱신
    const val UPDATE_PLAY_STATE = 1003
    //Desktop 가사 내용 갱신
    const val UPDATE_DESKTOP_LRC_CONTENT = 1004
    //Desktop 가사 제거
    const val REMOVE_DESKTOP_LRC = 1005
    //Desktop 가사 추가
    const val CREATE_DESKTOP_LRC = 1006

    private const val APLAYER_PACKAGE_NAME = "com.kr.myplayer"
    //Media 자료기지 변경
    const val MEDIA_STORE_CHANGE = "$APLAYER_PACKAGE_NAME.media_store.change"
    //읽기 및 쓰기 권한 변경
    const val PERMISSION_CHANGE = "$APLAYER_PACKAGE_NAME.permission.change"
    //재생목록 변경
    const val PLAYLIST_CHANGE = "$APLAYER_PACKAGE_NAME.playlist.change"
    //재생자료 변경
    const val META_CHANGE = "$APLAYER_PACKAGE_NAME.meta.change"
    const val LOAD_FINISHED = "$APLAYER_PACKAGE_NAME.load.finished"
    //재생상태 변경
    const val PLAY_STATE_CHANGE = "$APLAYER_PACKAGE_NAME.play_state.change"
    //노래 tag 변경
    const val TAG_CHANGE = "$APLAYER_PACKAGE_NAME.tag_change"

    const val EXTRA_CONTROL = "Control"
    const val EXTRA_SHUFFLE = "shuffle"
    const val ACTION_APPWIDGET_OPERATE = "$APLAYER_PACKAGE_NAME.appwidget.operate"
    const val ACTION_SHORTCUT_SHUFFLE = "$APLAYER_PACKAGE_NAME.shortcut.shuffle"
    const val ACTION_SHORTCUT_MYLOVE = "$APLAYER_PACKAGE_NAME.shortcut.my_love"
    const val ACTION_SHORTCUT_LASTADDED = "$APLAYER_PACKAGE_NAME.shortcut.last_added"
    const val ACTION_SHORTCUT_CONTINUE_PLAY = "$APLAYER_PACKAGE_NAME.shortcut.continue_play"
    const val ACTION_LOAD_FINISH = "$APLAYER_PACKAGE_NAME.load.finish"
    const val ACTION_CMD = "$APLAYER_PACKAGE_NAME.cmd"
    const val ACTION_WIDGET_UPDATE = "$APLAYER_PACKAGE_NAME.widget_update"
    const val ACTION_TOGGLE_TIMER = "$APLAYER_PACKAGE_NAME.toggle_timer"

    private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
        or PlaybackStateCompat.ACTION_PAUSE
        or PlaybackStateCompat.ACTION_PLAY_PAUSE
        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        or PlaybackStateCompat.ACTION_STOP
        or PlaybackStateCompat.ACTION_SEEK_TO)

    private const val APPWIDGET_BIG = "AppWidgetBig"
    private const val APPWIDGET_MEDIUM = "AppWidgetMedium"
    private const val APPWIDGET_SMALL = "AppWidgetSmall"
    private const val APPWIDGET_MEDIUM_TRANSPARENT = "AppWidgetMediumTransparent"
    private const val APPWIDGET_SMALL_TRANSPARENT = "AppWidgetSmallTransparent"
    private const val APPWIDGET_EXTRA_TRANSPARENT = "AppWidgetExtra"

    private const val INTERVAL_APPWIDGET = 1000L


    /**
     * Bitmap 복사
     */
    @JvmStatic
    fun copy(bitmap: Bitmap?): Bitmap? {
      if (bitmap == null || bitmap.isRecycled) {
        return null
      }
      var config: Bitmap.Config? = bitmap.config
      if (config == null) {
        config = Bitmap.Config.RGB_565
      }
      return try {
        bitmap.copy(config, false)
      } catch (e: OutOfMemoryError) {
        e.printStackTrace()
        null
      }

    }
  }
}

