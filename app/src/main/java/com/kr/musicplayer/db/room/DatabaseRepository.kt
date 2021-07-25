package com.kr.musicplayer.db.room

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.gson.Gson
import com.kr.musicplayer.App
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.db.room.model.History
import com.kr.musicplayer.db.room.model.PlayList
import com.kr.musicplayer.db.room.model.PlayQueue
import com.kr.musicplayer.helper.SortOrder
import com.kr.musicplayer.util.MediaStoreUtil
import com.kr.musicplayer.util.SPUtil
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Single
import org.jetbrains.anko.doAsync
import timber.log.Timber
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet

/**
 * Local DB 관련 repository
 */
class DatabaseRepository private constructor() {

  private val db = AppDatabase.getInstance(App.getContext().applicationContext)

  private val executors = Executors.newSingleThreadExecutor()

    //DB 의 playlist 목록에서 즐겨찾기에 해당하는 id
  private var myLoveId: Int = 0
    get() {
      if (field <= 0) {
        field = db.playListDao().selectAll().getOrNull(0)?.id ?: 0
      }
      return field
    }

  fun runInTransaction(block: () -> Unit) {
    executors.execute {
      db.runInTransaction(block)
    }
  }

    fun getAllPlayList(): LiveData<List<PlayList>> {
        return db.playListDao().selectAllPlayList()
    }

    fun getPlaylistSongId(id: Int): LiveData<PlayList> {
        return db.playListDao().selectSongIdsById(id)
    }

    suspend fun getPlayListById(id: Int): PlayList? {
        return db.playListDaoVM().selectPlayListById(id)
    }

    fun getPlayListWithId(id: Int): PlayList? {
        return db.playListDaoVM().selectPlayListWithId(id)
    }

    suspend fun getPlayQueueSong(): List<PlayQueue> {
        return db.playQueueDaoVM().selectAllPlayQueueSongs()
    }

  /**
   * 재생대기렬에 여러 노래 삽입
   */
  fun insertToPlayQueue(audioIds: List<Int>): Single<Int> {
    val actual = audioIds.toMutableList()
    return getPlayQueue()
        .map {
          //반복해서 추가하지 않는다.
          actual.removeAll(it)

          db.playQueueDao().insertPlayQueue(convertAudioIdsToPlayQueues(actual))

          actual.size
        }
  }

    suspend fun insertIdsToQueue(audioIds: List<Int>) {
        db.playQueueDao().insertPlayQueue(convertAudioIdsToPlayQueues(audioIds))
    }

  private fun deleteFromPlayQueueInternal(audioIds: List<Int>): Int {
    if (audioIds.isEmpty()) {
      return 0
    }
    return db.runInTransaction(Callable {
      var count = 0
      val length = audioIds.size / MAX_ARGUMENT_COUNT + 1
      for (i in 0 until length) {
        val lastIndex = if ((i + 1) * MAX_ARGUMENT_COUNT < audioIds.size) (i + 1) * MAX_ARGUMENT_COUNT else 1
        try {
          count += db.playQueueDao().deleteSongs(audioIds.subList(i * MAX_ARGUMENT_COUNT, lastIndex))
        } catch (e: Exception) {
          Timber.e(e)
//          CrashReport.postCatchedException(e)
        }
      }
      Timber.v("deleteFromPlayQueueInternal, count: $count")
      return@Callable count
    })
  }

    fun getAllPlayQueue(): LiveData<List<PlayQueue>> {
        return db.playQueueDao().getPlayQueue()
    }

  /**
   * 재생목록에 여러노래 삽입
   */
  fun insertToPlayList(audioIds: List<Int>, playlistId: Int = -1): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectById(playlistId)
              ?: throw IllegalArgumentException("No Playlist Found")
        }
        .map {
          //不重复添加
          val old = it.audioIds.size
          it.audioIds.addAll(audioIds)
          val count = it.audioIds.size - old
          db.playListDao().update(it)
          count
        }
  }

  /**
   * 재생목록에 여러노래 삽입
   */
  fun insertToPlayList(audioIds: List<Int>, name: String): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(name) ?: throw IllegalArgumentException("No Playlist Found")
        }
        .map {
          //不重复添加
          val old = it.audioIds.size
          it.audioIds.addAll(audioIds)
          val count = it.audioIds.size - old
          db.playListDao().update(it)
          count
        }
  }

  /**
   * 재생목록에서 여러노래 삭제
   */
  fun deleteFromPlayList(audioIds: List<Int>, name: String): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(name)
          /**?: db.playListDao().selectById(playlistId)*/
              ?: throw IllegalArgumentException()
        }
        .map {
          val old = it.audioIds.size
          it.audioIds.removeAll(audioIds)
          val count = old - it.audioIds.size
          db.playListDao().update(it)
          count
        }
  }

    suspend fun deleteFromPlayLists(audioIds: List<Int>, name: Int) {
        val playlist = db.playListDaoVM().selectById(name)
        playlist?.audioIds?.removeAll(audioIds)
        db.playListDaoVM().update(playlist!!)
    }

    suspend fun deleteIdsFromQueue(audioIds: List<Int>) {
        db.playQueueDao().deleteSongs(audioIds)
    }

  /**
   * 재생목록에서 여러노래 삭제
   */
  fun deleteFromPlayList(audioIds: List<Int>, playlistId: Int): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectById(playlistId) ?: throw IllegalArgumentException()
        }
        .map {
          val old = it.audioIds.size
          it.audioIds.removeAll(audioIds)
          val count = old - it.audioIds.size
          db.playListDao().update(it)
          count
        }
  }


  /**
   * 재생목록에서 모두 삭제
   */
  fun deleteFromAllPlayList(songs: List<Song>): Completable {
    return getAllPlaylist()
        .flatMapCompletable { playLists ->
          CompletableSource {
            val audioIds = songs.map { song -> song.id }
            playLists.forEach { playList ->
              deleteFromPlayList(audioIds, playList.name).subscribe()
            }
            it.onComplete()
          }
        }

  }

  /**
   * 재생목록삽입
   */
  fun insertPlayList(name: String): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().insertPlayList(PlayList(0, name, LinkedHashSet(), Date().time)).toInt()
        }

  }

  /**
   * 즐겨찾기 노래목록 삭제
   */
  fun getMyLoveList(): Single<List<Int>> {
    return Single
        .fromCallable {
          db.playListDao().selectById(myLoveId)
        }
        .map {
          it.audioIds.toList()
        }
  }

  /**
   * 재생목록 가져오기
   */
  fun getPlayList(id: Int): Single<PlayList> {
    return Single
        .fromCallable {
          db.playListDao().selectById(id)
        }
  }

  /**
   * 모든 재생목록 가져오기
   */
  fun getAllPlaylist(): Single<List<PlayList>> {
    return Single
        .fromCallable {
          db.playListDao().selectAll()
        }
  }

    suspend fun getAllPlaylists(): List<PlayList> {
        return db.playListDaoVM().selectAll()
    }

    fun updatePlayListName(playlistId: Int, name: String): Single<Int> {
        return Single
                .fromCallable {
                    db.playListDao().updateName(playlistId, name)
                }
    }

    fun updatePlayListAudiosDate(playlistId: Int, time: Long): Single<Int> {
        return Single
                .fromCallable {
                    db.playListDao().updateDate(playlistId, time)
                }
    }

  /**
   * 즐겨찾기에 있는 노래인지 확인
   */
  fun isMyLove(audioId: Int): Single<Boolean> {
    return getPlayList(myLoveId)
        .map { playList ->
          playList.audioIds.contains(audioId)
        }
  }

  /**
   * 재생대기렬가져오기
   */
  fun getPlayQueue(): Single<List<Int>> {
    return Single
        .fromCallable {
          db.playQueueDao().selectAll()
              .map {
                it.audio_id
              }
        }
  }

  /**
   * 재생대기렬에 해당하는 노래 가져오기
   */
  fun getPlayQueueSongs(): Single<List<Song>> {
    val idsInQueue = ArrayList<Int>()
    return Single
        .fromCallable {
          db.playQueueDao().selectAll()
              .map {
                it.audio_id
              }
        }
        .doOnSuccess {
          idsInQueue.addAll(it)
        }
        .flatMap {
          getSongsWithSort(CUSTOMSORT, it)
        }
        .doOnSuccess { songs ->
          //删除不存在的歌曲
          if (songs.size < idsInQueue.size) {
            val deleteIds = ArrayList<Int>()
            val existIds = songs.map { it.id }

            for (audioId in idsInQueue) {
              if (!existIds.contains(audioId)) {
                deleteIds.add(audioId)
              }
            }

            if (deleteIds.isNotEmpty()) {
              deleteFromPlayQueueInternal(deleteIds)
            }
          }
        }
  }

  /**
   * 재생대기렬 지우기
   */
  fun clearPlayQueue(): Single<Int> {
    return Single
        .fromCallable {
          db.playQueueDao().clear()
        }
  }

    /**
     * play queue list 재정렬
     */
    fun reArrangeQueueList(queueList : List<Int>) {
        doAsync {
            val actual = queueList.toMutableList()
            db.playQueueDao().reArrangeQueueList(convertAudioIdsToPlayQueues(actual))
        }
    }


  private fun convertAudioIdsToPlayQueues(audioIds: List<Int>): List<PlayQueue> {
    val playQueues = ArrayList<PlayQueue>()
    for (audioId in audioIds) {
      playQueues.add(PlayQueue(0, audioId))
    }
    return playQueues
  }

  /**
   * 재생목록삭제
   */
  fun deletePlayList(playListId: Int): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().deletePlayList(playListId)
        }
  }


  /**
   * 재생목록 별 노래목록 가져오기
   */
  fun getPlayListSongs(context: Context, playList: PlayList, force: Boolean = false): Single<List<Song>> {

    return Single
        .just(playList)
        .flatMap {
          val sort = SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME,
              SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
              SortOrder.PlayListSongSortOrder.SONG_DISPLAY_TITLE_A_Z)
          val actualSort = if (force || sort == SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM)
            CUSTOMSORT else sort

          return@flatMap getSongsWithSort(actualSort, it.audioIds.toList())
        }
        .doOnSuccess { songs ->
          //존재하지 않는 노래 제거
          if (songs.size < playList.audioIds.size) {
            val deleteIds = ArrayList<Int>()
            val existIds = songs.map { it.id }

            for (audioId in playList.audioIds) {
              if (!existIds.contains(audioId)) {
                deleteIds.add(audioId)
              }
            }

            if (deleteIds.isNotEmpty()) {
              deleteFromPlayList(deleteIds, playList.name).subscribe()
            }
          }
        }
  }


    /**
     * 정렬된 노래 목록 얻기
     */
  private fun getSongsWithSort(sort: String, ids: List<Int>): Single<List<Song>> {
    return Single
        .fromCallable {
          val customSort = sort == CUSTOMSORT
          val inStr = makeInStr(ids)
          val songs = MediaStoreUtil.getSongs(
              MediaStore.Audio.Media._ID + " in(" + inStr + ")",
              null,
              if (customSort) null else sort)
          val tempArray = Array<Song>(ids.size) { Song.EMPTY_SONG }

          songs.forEachIndexed { index, song ->
            tempArray[if (CUSTOMSORT == sort) ids.indexOf(song.id) else index] = song
          }
          tempArray
              .filter { it.id != Song.EMPTY_SONG.id }
        }
  }

  private fun makeInStr(audioIds: List<Int>): String {
    val inStrBuilder = StringBuilder(127)

    for (i in audioIds.indices) {
      inStrBuilder.append(audioIds[i]).append(if (i != audioIds.size - 1) "," else " ")
    }

    return inStrBuilder.toString()
  }

    /**
     * 기록갱신
     */
  fun updateHistory(song: Song): Single<Int> {
    //존재여부를 먼저 판단
    return Single
        .fromCallable {
          db.historyDao().selectByAudioId(song.id)
        }
        //그렇지 않으면 새로 추가
        .onErrorResumeNext(Single.fromCallable {
          val newHistory = History(0, song.id, 0, 0)
          val id = db.historyDao().insertHistory(newHistory)
          newHistory.copy(id = id.toInt())
        })
        .map {
          db.historyDao().update(it.copy(play_count = it.play_count + 1, last_play = Date().time))
        }

  }

  companion object {
    @Volatile
    private var INSTANCE: DatabaseRepository? = null

    @JvmStatic
    fun getInstance(): DatabaseRepository =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: DatabaseRepository()
        }

    private const val CUSTOMSORT = "CUSTOMSORT"
    private const val MAX_ARGUMENT_COUNT = 300
  }
}