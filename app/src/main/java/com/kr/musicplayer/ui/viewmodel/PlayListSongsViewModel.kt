package com.kr.musicplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.kr.musicplayer.db.room.DatabaseRepository
import com.kr.musicplayer.db.room.model.PlayList

/**
 * Local DB 의 특정 재생목록감지를 위한 viewModel
 */
class PlayListSongsViewModel(application: Application, id: Int): AndroidViewModel(application) {
    private val _ids: LiveData<PlayList> = DatabaseRepository.getInstance().getPlaylistSongId(id)
    val ids: LiveData<PlayList> get() = _ids
}

/**
 * 주어진 파라메터에 관한 PlayListSongsViewModel 창조
 */
class PlayListSongsViewModelFactory(private val mApplication: Application, private val mParam: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PlayListSongsViewModel(mApplication, mParam) as T
    }
}