package com.kr.musicplayer.service

import android.content.Intent
import androidx.annotation.WorkerThread
import com.kr.musicplayer.R
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.db.room.DatabaseRepository
import com.kr.musicplayer.misc.log.LogObserver
import com.kr.musicplayer.request.network.RxUtil
import com.kr.musicplayer.ui.activity.PlayerActivity
import com.kr.musicplayer.util.Constants.MODE_SHUFFLE
import com.kr.musicplayer.util.MediaStoreUtil
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.ToastUtil
import com.kr.musicplayer.util.Util
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * 재생대기렬 클라스
 */
class PlayQueue(service: MusicService) {
  private val service = WeakReference(service)
  private val repository = DatabaseRepository.getInstance()

  private var loaded = false

  // 현재 재생 대기렬
  private val _playingQueue = ArrayList<Song>()
  val playingQueue: List<Song>
    get() = _playingQueue


  // 본래 대기렬
  private val _originalQueue = ArrayList<Song>()
  val originalQueue: List<Song>
    get() = _originalQueue


  // 다음 노래의 위치
  private var nextPosition = 0

  // 현재 재생 위치
  var position = 0
    private set

  // 현재 재생중인 노래
  var song = Song.EMPTY_SONG

  // 다음 노래
  var nextSong = Song.EMPTY_SONG

  /**
   * 재생 대기렬 생성
   */
  fun makeList() {
    val service = service.get() ?: return
    synchronized(this) {
      if (service.playModel == MODE_SHUFFLE) {
        makeShuffleList()
      } else {
        makeNormalList()
      }
    }
    Timber.v("makeList, size: ${_playingQueue.size}")
  }

  /**
   * 일반 재생 대기렬 생성
   */
  private fun makeNormalList() {
    if (_originalQueue.isEmpty()) {
      return
    }
    _playingQueue.clear()
    _playingQueue.addAll(_originalQueue)
    Timber.v("makeNormalList, queue: ${_playingQueue.size}")
  }

  /**
   * 임의로 재생을 위한 대기렬 생성
   */
  private fun makeShuffleList() {
    if (_originalQueue.isEmpty()) {
      return
    }

    _playingQueue.clear()
    _playingQueue.addAll(_originalQueue)

    if (position >= 0) {
      _playingQueue.shuffle()
      if (position < _playingQueue.size) {
        val removeSong = _playingQueue.removeAt(position)
        _playingQueue.add(0, removeSong)
      }
    } else {
      _playingQueue.shuffle()
    }

    Timber.v("makeShuffleList, queue: ${_playingQueue.size}")
  }

  /**
   * 재생대기렬 읽기
   */
  @WorkerThread
  @Synchronized
  fun restoreIfNecessary() {
    Timber.v("PlayQueue restoreIfNecessary is called")
    if (!loaded && _playingQueue.isEmpty()) {
      val queue = repository.getPlayQueueSongs().blockingGet()
      if (queue.isNotEmpty()) {
        _originalQueue.addAll(queue)
        _playingQueue.addAll(_originalQueue)
        makeList()
      } else {
        //모든 노래 재생대기렬에 추가
        setPlayQueue(MediaStoreUtil.getAllSong())
      }

      restoreLastSong()
      loaded = true
    }
  }

  /**
   * 마지막으로 끝냈을때 재생중이던 노래 초기화
   */
  private fun restoreLastSong() {
    if (_originalQueue.isEmpty()) {
      return
    }
    //마지막으로 끝냈을때 재생중이던 노래의 Id를 읽는다
    val service = service.get() ?: return
    val lastId = SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, -1)
    //마지막으로 끝냈을때 재생중이던 노래가 여전히 존재하는지 확인여부
    var isLastSongExist = false
    //마지막으로 끝냈을때 재생중이던 노래의 위치
    var pos = 0
    //마지막노래가 존재하는지 확인
    if (lastId != -1) {
      for (i in _originalQueue.indices) {
        if (lastId == _originalQueue[i].id) {
          isLastSongExist = true
          pos = i
          break
        }
      }
    }

    //마지막으로 끝냈을때 보관된 노래 설정
    if (isLastSongExist) {
      setUpDataSource(_originalQueue[pos], pos)
    } else {
      //다시 노래 Id 찾기
      setUpDataSource(_originalQueue[0], 0)
    }
  }

  /**
   * MediaPlayer 초기화
   */
  private fun setUpDataSource(lastSong: Song?, pos: Int) {
    Timber.v("PlayQueue setUpDataSource is called and lastSong is %s", lastSong)
    if (lastSong == null) {
      return
    }
    //현재 재생중인 노래 초기화
    song = lastSong
    position = pos
    updateNextSong()
  }

  /**
   * 재생 대기렬 설정
   */
  fun setPlayQueue(songs: List<Song>) {
    synchronized(this) {
      _originalQueue.clear()
      _originalQueue.addAll(songs)
    }

    makeList()
    saveQueue()
  }

  /**
   * 다음 노래로 추가
   */
  fun addNextSong(nextSong: Song) {
    //재생 대기렬에 추가
    if (nextSong == this.nextSong) {
      ToastUtil.show(service.get() ?: return, R.string.already_add_to_next_song)
      return
    }

    synchronized(this) {
      if (_playingQueue.contains(nextSong)) {
        _playingQueue.remove(nextSong)
        _playingQueue.add(if (position + 1 < playingQueue.size) position + 1 else 0, nextSong)
      } else {
        _playingQueue.add(_playingQueue.indexOf(song) + 1, nextSong)
      }
      if (_originalQueue.contains(nextSong)) {
        _originalQueue.remove(nextSong)
        _originalQueue.add(if (position + 1 < _originalQueue.size) position + 1 else 0, nextSong)
      } else {
        _originalQueue.add(_originalQueue.indexOf(song) + 1, nextSong)
      }
    }
    updateNextSong()
    saveQueue()
  }

  fun addSong(song: Song) {
    synchronized(this) {
      _playingQueue.add(song)
      _originalQueue.add(song)
    }
    saveQueue()
  }

  fun addSong(position: Int, song: Song) {
    synchronized(this) {
      _playingQueue.add(position, song)
      _originalQueue.add(position, song)
    }
    saveQueue()
  }

  fun remove(song: Song) {
    synchronized(this) {
      _playingQueue.remove(song)
      _originalQueue.remove(song)
    }
    saveQueue()
  }

  /**
   * 재생 대기렬 모두 삭제
   */
  fun removeAll(deleteSongs: List<Song>) {
    synchronized(this) {
      _playingQueue.removeAll(deleteSongs)
      _originalQueue.removeAll(deleteSongs)
    }
    saveQueue()
  }

  /**
   * 재생대기렬에 노래 추가
   * @param addSongs 추가할 노래 목록
   */
  fun addAll(addSongs: List<Song>) {
    synchronized(this) {
      _playingQueue.clear()
      _originalQueue.clear()
      _playingQueue.addAll(addSongs)
      _originalQueue.addAll(addSongs)
    }
    reArrangeQueue()
  }

  /**
   * 직접 위치 설정
   * @param pos 설정할 위치
   */
  fun setPosition(pos: Int) {
    position = pos
    // 재생방식이 임의로 재생방식이면 그에 해당한 재생대기렬 생성
    if (service.get()?.playModel == MODE_SHUFFLE) {
      makeShuffleList()
    }
    song = _originalQueue[position]
  }

  /**
   * 현재 재생중인 노래에 따라 위치 변경
   */
  fun rePosition() {
    val newPosition = _originalQueue.indexOf(song)
    if (newPosition >= 0) {
      position = newPosition
    }
  }

  /**
   * 다음 노래를 현재 노래로 설정
   */
  fun next() {
    position = nextPosition
    song = nextSong.copy()
    updateNextSong()
  }

  /**
   * 이전 노래를 현재 노래로 설정
   */
  fun previous() {
    if (--position < 0) {
      position = _playingQueue.size - 1
    }
    if (position == -1 || position > _playingQueue.size - 1) {
      return
    }
    song = _playingQueue[position]
    updateNextSong()
  }

  /**
   * 자료기지의 재생대기렬 삭제
   */
  private fun saveQueue() {
    repository.clearPlayQueue()
        .flatMap {
          repository.insertToPlayQueue(_originalQueue.map { it.id })
        }
        .compose(RxUtil.applySingleScheduler())
        .subscribe(LogObserver())
  }

  /**
   * 재생대기렬목록 재정렬
   */
  private fun reArrangeQueue() {
    repository.reArrangeQueueList(_originalQueue.map { it.id })
  }

  /**
   * 다음 노래 갱신
   * @param pos 노래위치
   */
  fun updateNextSong(pos: Int? = null) {
    if (_playingQueue.isEmpty()) {
      return
    }
    if (pos != null) {
      position = pos
    }

    synchronized(this) {
      nextPosition = position + 1
      if (nextPosition >= _playingQueue.size) {
        nextPosition = 0
      }
      nextSong = _playingQueue[nextPosition]
    }

    Util.sendLocalBroadcast(Intent(PlayerActivity.ACTION_UPDATE_NEXT))
  }

  /**
   * 재생대기렬 개수
   * @return 개수
   */
  fun size(): Int {
    return _playingQueue.size
  }

  /**
   * 재생대기렬이 비여있는지 확인
   * @return 비여있으면 true, 아니면 false
   */
  fun isEmpty(): Boolean {
    return _playingQueue.isEmpty()
  }
}