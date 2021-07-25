package com.kr.musicplayer.lyric

import android.provider.MediaStore
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import org.jaudiotagger.tag.FieldKey
import com.kr.musicplayer.App
import com.kr.musicplayer.bean.misc.LyricPriority
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.lyric.bean.LrcRow
import com.kr.musicplayer.misc.cache.DiskCache
import com.kr.musicplayer.misc.tageditor.TagEditor
import com.kr.musicplayer.request.network.RxUtil
import com.kr.musicplayer.util.ImageUriUtil
import com.kr.musicplayer.util.LyricUtil
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.Util
import timber.log.Timber
import java.io.*
import java.nio.charset.Charset

/**
 * 노래이름과 예술가이름을 기준으로 가사를 검색하고 고정형식으로 구문분석
 */
class LyricSearcher {
  private var song: Song = Song.EMPTY_SONG // 노래
  private val lrcParser: ILrcParser
  private var displayName: String? = null // 노래이름
  private var cacheKey: String? = null
  private var searchKey: String? = null

  init {
    lrcParser = DefaultLrcParser()
  }

  /**
   * 구문분석
   */
  private fun parse() {
    try {
      if (!TextUtils.isEmpty(song.displayName)) {
        val temp = song.displayName
        displayName = if (temp.indexOf('.') > 0) temp.substring(0, temp.lastIndexOf('.')) else temp
      }
      searchKey = getLyricSearchKey(song)
    } catch (e: Exception) {
      Timber.v(e)
      displayName = song.title
    }
  }

  /**
   * 검색할 노래 설정
   */
  fun setSong(song: Song): LyricSearcher {
    this.song = song
    parse()
    return this
  }

  /**
   * 요청보내기 및 가사 구문분석
   *
   * @return 가사
   */
  fun getLyricObservable(manualPath: String, clearCache: Boolean): Observable<List<LrcRow>> {
    if (song == Song.EMPTY_SONG) {
      return Observable.error(Throwable("empty song"))
    }
    val type = SPUtil.getValue(App.getContext(), SPUtil.LYRIC_KEY.NAME, song.id, SPUtil.LYRIC_KEY.LYRIC_DEFAULT)

    val observable = when (type) {
      SPUtil.LYRIC_KEY.LYRIC_IGNORE -> {
        Timber.v("ignore lyric")
        Observable.error(Throwable("ignore lyric"))
      }
      SPUtil.LYRIC_KEY.LYRIC_EMBEDDED -> {
        getEmbeddedObservable()
      }
      SPUtil.LYRIC_KEY.LYRIC_LOCAL -> {
        getLocalObservable()
      }
      SPUtil.LYRIC_KEY.LYRIC_MANUAL -> {
        getManualObservable(manualPath)
      }
      SPUtil.LYRIC_KEY.LYRIC_DEFAULT -> {
        //기본우선순위정렬
        val priority = Gson().fromJson<List<LyricPriority>>(SPUtil.getValue(App.getContext(), SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.PRIORITY_LYRIC, SPUtil.LYRIC_KEY.DEFAULT_PRIORITY),
            object : TypeToken<List<LyricPriority>>() {}.type)

        val observables = mutableListOf<Observable<List<LrcRow>>>()
        priority.forEach {
          when (it.priority) {
            LyricPriority.LOCAL.priority -> observables.add(getLocalObservable())
            LyricPriority.EMBEDED.priority -> observables.add(getEmbeddedObservable())
          }
        }
        Observable.concat(observables).firstOrError().toObservable()
      }
      else -> {
        Observable.error(Throwable("unknown type"))
      }
    }

    return if (isTypeAvailable(type)) Observable.concat(getCacheObservable(), observable)
        .firstOrError()
        .toObservable()
        .doOnSubscribe {
          cacheKey = Util.hashKeyForDisk(song.id.toString() + "-" +
              (if (!TextUtils.isEmpty(song.artist)) song.artist else "") + "-" +
              if (!TextUtils.isEmpty(song.title)) song.title else "")
          Timber.v("CacheKey: $cacheKey SearchKey: $searchKey")
          if (clearCache) {
            Timber.v("clearCache")
            DiskCache.getLrcDiskCache().remove(cacheKey)
          }
        }.compose(RxUtil.applyScheduler())
    else
      observable
  }

  private fun isTypeAvailable(type: Int): Boolean {
    return type != SPUtil.LYRIC_KEY.LYRIC_IGNORE
  }

  /**
   * 가사 ID 에 따라 요청을 보내고 가사를 구문분석
   *
   * @return 가사
   */
  fun getLyricObservable(): Observable<List<LrcRow>> {
    return getLyricObservable("", false)
  }

  /**
   * 삽입된 가사 얻기
   *
   * @return
   */
  private fun getEmbeddedObservable(): Observable<List<LrcRow>> {
    val tagEditor = TagEditor(song.url)
    return Observable.create { e ->
      val lyric = tagEditor.getFieldValueSingle(FieldKey.LYRICS).blockingGet()
      if (!lyric.isNullOrEmpty()) {
        e.onNext(lrcParser.getLrcRows(getBufferReader(lyric.toByteArray(UTF_8)),
            true, cacheKey, searchKey))
        Timber.v("EmbeddedLyric")
      }
      e.onComplete()
    }
  }

  /**
   * Cache 자료 얻기
   */
  private fun getCacheObservable(): Observable<List<LrcRow>> {
    return Observable.create { e ->
      DiskCache.getLrcDiskCache().get(cacheKey)?.run {
        BufferedReader(InputStreamReader(getInputStream(0))).use {
          it.readLine().run {
            e.onNext(Gson().fromJson(this, object : TypeToken<List<LrcRow>>() {}.type))
            Timber.v("CacheLyric")
          }
        }
      }
      e.onComplete()
    }
  }

  /**
   * Local 가사 파일 검색하여 얻기
   */
  private fun getLocalLyricPath(): String? {
    var path = ""
    //가사경로가 설정되지 않음, 가능한 모든 가사 파일 검색
    App.getContext().contentResolver.query(MediaStore.Files.getContentUri("external"), null,
        MediaStore.Files.FileColumns.DATA + " like ? or " +
            MediaStore.Files.FileColumns.DATA + " like ? or " +
            MediaStore.Files.FileColumns.DATA + " like ? or " +
            MediaStore.Files.FileColumns.DATA + " like ? or " +
            MediaStore.Files.FileColumns.DATA + " like ? or " +
            MediaStore.Files.FileColumns.DATA + " like ?",
        getLocalSearchKey(),
        null)
        .use { filesCursor ->
          while (filesCursor.moveToNext()) {
            val file = File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)))
            Timber.v("file: %s", file.absolutePath)
            if (file.exists() && file.isFile && file.canRead()) {
              path = file.absolutePath
              break
            }
          }
          return path
        }
  }

  /**
   * Local 가사 검색을 위한 keyword 얻기
   * @param searchPath Local 가사 검색경로설정
   * displayName.lrc
   * title.lrc
   * title-artist.lrc
   * displayname-artist.lrc
   * artist-title.lrc
   * aritst-displayName.lrc
   */
  private fun getLocalSearchKey(searchPath: String? = null): Array<String> {
    return arrayOf("%$displayName$SUFFIX_LYRIC",
        "%${song.title}$SUFFIX_LYRIC",
        "%${song.title}%${song.artist}$SUFFIX_LYRIC",
        "%${song.displayName}%${song.artist}$SUFFIX_LYRIC",
        "%${song.artist}%${song.title}$SUFFIX_LYRIC",
        "%${song.artist}%$displayName$SUFFIX_LYRIC")
  }

  /**
   * 수동으로 가사 설정
   */
  private fun getManualObservable(manualPath: String): Observable<List<LrcRow>> {
    return Observable.create { e ->
      //手动设置的歌词
      if (!TextUtils.isEmpty(manualPath)) {
        Timber.v("ManualLyric")
        e.onNext(lrcParser.getLrcRows(getBufferReader(manualPath), true, cacheKey, searchKey))
      }
      e.onComplete()
    }
  }


  /**
   * Local 가사 얻기
   */
  private fun getLocalObservable(): Observable<List<LrcRow>> {
    return Observable
        .create { emitter ->
          val path = getLocalLyricPath()
          if (path != null && path.isNotEmpty()) {
            Timber.v("LocalLyric")
            emitter.onNext(lrcParser.getLrcRows(getBufferReader(path), true, cacheKey, searchKey))
          }
          emitter.onComplete()
        }
  }


  /**
   * 가사검색을 위한 keyword 얻기
   *
   * @param song 가사 관련 노래
   * @return keyword
   */
  private fun getLyricSearchKey(song: Song?): String {
    if (song == null)
      return ""
    val isTitleAvailable = !ImageUriUtil.isSongNameUnknownOrEmpty(song.title)
    val isAlbumAvailable = !ImageUriUtil.isAlbumNameUnknownOrEmpty(song.album)
    val isArtistAvailable = !ImageUriUtil.isArtistNameUnknownOrEmpty(song.artist)

    //합법적인 노래 이름
    return if (isTitleAvailable) {
      when {
        isArtistAvailable -> song.artist + "-" + song.title
        isAlbumAvailable ->
          song.album + "-" + song.title
        else -> song.title
      }
    } else ""
  }

  @Throws(FileNotFoundException::class, UnsupportedEncodingException::class)
  private fun getBufferReader(path: String): BufferedReader {
    return BufferedReader(InputStreamReader(FileInputStream(path), LyricUtil.getCharset(path)))
  }

  private fun getBufferReader(bytes: ByteArray): BufferedReader {
    return BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), UTF_8))
  }

  companion object {
    private const val TAG = "LyricSearcher"
    private const val SUFFIX_LYRIC = ".lrc"
    private val UTF_8 = Charset.forName("UTF-8")
  }
}
