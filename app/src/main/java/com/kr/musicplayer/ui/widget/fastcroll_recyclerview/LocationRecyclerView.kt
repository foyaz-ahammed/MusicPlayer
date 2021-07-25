package com.kr.musicplayer.ui.widget.fastcroll_recyclerview

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.helper.MusicServiceRemote
import com.kr.musicplayer.ui.activity.AllSongsActivity

class LocationRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FastScrollRecyclerView(context, attrs, defStyleAttr) {
  private var move = false
  private var pos = -1

  init {
    addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
      override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (move) {
          move = false
          //현재화면에서 고정할 항목의 위치를 가져온다.
          val n = pos - (layoutManager as androidx.recyclerview.widget.LinearLayoutManager).findFirstVisibleItemPosition()
          if (n in 0 until childCount) {
            //RecyclerView 의 맨 웃부분의 위치얻기
            val top = getChildAt(n).top
            //맨우로 이동
            scrollBy(0, top)
          }
        }
      }
    })
  }


  fun smoothScrollToCurrentSong(data: List<Song>) {
    val currentId = MusicServiceRemote.getCurrentSong().id
    if (currentId < 0)
      return
    smoothScrollTo(data, currentId)
  }

  /**
   * 지정된 위치로 Scroll 진행
   *
   * @param data Scroll 할수있는 자료목록
   * @param currentId 현재위치
   */
  private fun smoothScrollTo(data: List<Song>, currentId: Int) {
    for (i in data.indices) {
      if (data[i].id == currentId) {
        pos = i
        break
      }
    }
    // 첫번째 항목은 임의로 재생이다.
    if(context is AllSongsActivity){
      pos += 1
    }
    val layoutManager = layoutManager as androidx.recyclerview.widget.LinearLayoutManager
    val firstItem = layoutManager.findFirstVisibleItemPosition()
    val lastItem = layoutManager.findLastVisibleItemPosition()
    when {
      pos <= firstItem -> // 고정할 항목이 현재 표시된 첫번째 항목앞에 있는 경우
        scrollToPosition(pos)
      pos <= lastItem -> {
        //고정할 항목이 이미 화면에 표시된 경우
        val top = getChildAt(pos - firstItem).top
        scrollBy(0, top)
      }
      else -> {
        // 맨우로 될 항목이 현재 표시된 마지막 항목 뒤에 있는 경우
        scrollToPosition(pos)
        move = true
      }
    }
    if (pos >= 0) {
      getLayoutManager()?.scrollToPosition(pos)
    }
  }
}