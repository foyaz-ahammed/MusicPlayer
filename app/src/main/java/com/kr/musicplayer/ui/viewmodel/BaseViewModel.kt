package com.kr.musicplayer.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.kr.musicplayer.App
import com.kr.musicplayer.bean.mp3.Artist
import com.kr.musicplayer.bean.mp3.Folder
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.db.room.DatabaseRepository.Companion.getInstance
import com.kr.musicplayer.helper.SortOrder
import com.kr.musicplayer.util.Constants
import com.kr.musicplayer.util.MediaStoreUtil
import com.kr.musicplayer.util.SPUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 노래변경 감지를 위한 viewModel
 */
class BaseViewModel(application: Application) : AndroidViewModel(application) {
    //MediaStore 로부터 얻은 모든 노래목록
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> get() = _songs
    // 모든 등록부
    private val _folders = MutableLiveData<List<Folder>>()
    val folders: LiveData<List<Folder>> get() = _folders
    // 모든 예술가
    private val _artists = MutableLiveData<List<Artist>>()
    val artists: LiveData<List<Artist>> get() = _artists

    private var contentObserver: ContentObserver? = null

    /**
     * 모든 노래를 얻는 함수
     */
    fun loadSongs(type: Int, id: Int = -1) {
        loadSongs(type, id, false, -1);
    }

    /**
     * 모든 노래를 얻는 함수
     * @param type 얻으려는 노래 형태 (PlayList, Artist, Song, Folder)
     * @param id PlayList, Artist, Folder 의 개별적인 항목에 대한 id
     * @param isPlayList 얻으려는 노래목록이 PlayList 에 관한 노래목록인지를 판별
     * @param playListId 얻으려는 노래목록이 PlayList 에 관한 노래목록이면 여러개의 PlayList 중 어느 항목인지 식별
     */
    fun loadSongs(type: Int, id: Int = -1, isPlayList: Boolean = false, playListId: Int = -1) {
        viewModelScope.launch {

            val songs = querySongs(type, id)
            //이미 음악들이 playlist에 존재하면 제거
            if (isPlayList) {
                val gainedSongs = songs.toMutableList()
                val playListSongIds = getInstance().getPlayListById(playListId)?.audioIds
                if (playListSongIds != null) {
                    for (id in playListSongIds) {
                        for (song in gainedSongs) {
                            if (song.id == id) {
                                gainedSongs.removeAt(gainedSongs.indexOf(song))
                                break;
                            }
                        }
                    }
                    _songs.postValue(gainedSongs)
                }
            } else {
                _songs.postValue(songs)
            }

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadSongs(type, id)
                }
            }
        }
    }

    /**
     * 주어진 노래형태에 대한 노래얻기
     * @param type PlayList, Song, Artist, Folder 중 하나
     * @param id 노래형태에 관한 id
     * @return 얻어진 노래목록
     */
    private suspend fun querySongs(type: Int, id: Int) : List<Song> {
        var songs = listOf<Song>()
        withContext(Dispatchers.IO) {
            when (type) {
                Constants.SONG -> songs = MediaStoreUtil.getAllSong()
                Constants.ALBUM, Constants.ARTIST -> if (id > 0) songs = MediaStoreUtil.getSongsByArtistIdOrAlbumId(id, type)
                Constants.FOLDER -> if (id > 0) songs = MediaStoreUtil.getSongsByParentId(id)
                Constants.PLAYLIST -> if (id > 0) {
                    songs = MediaStoreUtil.getSongsByIds(getInstance().getPlayListById(id)?.audioIds?.toList())
                }
            }
            val sortType = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
                    SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z)
            when (sortType) {
                SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z -> Collections.sort(songs) { o1, o2 -> o1.displayName.compareTo(o2.displayName) }
                SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A -> Collections.sort(songs) { o1, o2 -> o1.displayName.compareTo(o2.displayName) * -1 }
                SortOrder.SongSortOrder.SONG_DATE -> Collections.sort(songs) { o1, o2 -> (o1.addTime - o2.addTime).toInt() }
                SortOrder.SongSortOrder.SONG_DATE_DESC -> Collections.sort(songs) { o1, o2 -> (o1.addTime - o2.addTime).toInt() * -1 }
            }
        }

        return songs
    }

    /**
     * 주어진 keyword 에 관한 노래얻기
     * @param key 검색 Keyword
     */
    fun searchSongs(key: String) {
        viewModelScope.launch {

            val songs = search(key)
            _songs.postValue(songs)

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                ) {
                    searchSongs(key)
                }
            }
        }
    }

    /**
     * MediaStore 에서 주어진 keyword 에 관한 노래 얻기
     */
    private suspend fun search(key: String): List<Song> {
        var songs: List<Song>
        withContext(Dispatchers.IO) {
            songs = MediaStoreUtil.searchSong(key)
        }
        return songs;
    }

    /**
     * 노래를 포함하고 있는 등록부목록 얻기
     */
    fun loadFolders() {
        viewModelScope.launch {
            val folders = MediaStoreUtil.getAllFolder()
            _folders.postValue(folders)

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadFolders()
                }
            }
        }
    }

    /**
     * 예술가목록 얻기
     */
    fun loadArtists() {
        viewModelScope.launch {
            val artists = MediaStoreUtil.getAllArtist()
            _artists.postValue(artists)
            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadArtists()
                }
            }
        }
    }

    // Occur when viewmodel is destroyed
    /**
     * viewModel 이 Destroy 될때 contentResolver 등록취소
     */
    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }
}

/**
 * contentResolver Observer 등록
 */
fun ContentResolver.registerObserver(
        uri: Uri,
        observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}