package com.kr.musicplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.db.room.DatabaseRepository
import com.kr.musicplayer.db.room.model.PlayQueue
import org.jetbrains.anko.doAsync

/**
 * 재생대기렬감지를 위한 viewModel
 */
class PlayQueueSongsViewModel(application: Application): AndroidViewModel(application) {
    private val queueList: LiveData<List<PlayQueue>> = DatabaseRepository.getInstance().getAllPlayQueue()
    val queueSongs = MediatorLiveData<List<Song>>()
    
    fun getAllQueueSongs() {
        queueSongs.addSource(queueList) {
            doAsync {
                var songs = arrayListOf<Song>()
                songs = DatabaseRepository.getInstance().getPlayQueueSongs().blockingGet() as ArrayList<Song>
                queueSongs.postValue(songs)
            }
        }
    }
}
