package com.kr.musicplayer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.kr.musicplayer.db.room.DatabaseRepository.Companion.getInstance
import com.kr.musicplayer.db.room.model.PlayList

/**
 * Local DB 의 재생목록 table 에서 모든 재생목록들을 감지하기 위한 viewModel
 */
class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val _playList: LiveData<List<PlayList>> = getInstance().getAllPlayList()
    val playList: LiveData<List<PlayList>> get() = _playList
}