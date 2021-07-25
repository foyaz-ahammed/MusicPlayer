package com.kr.musicplayer.ui.fragment

import android.os.Bundle
import android.os.Message
import androidx.viewpager.widget.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import com.kr.musicplayer.App
import com.kr.musicplayer.R
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.helper.MusicServiceRemote
import com.kr.musicplayer.lyric.LrcView
import com.kr.musicplayer.lyric.LyricSearcher
import com.kr.musicplayer.misc.handler.MsgHandler
import com.kr.musicplayer.misc.handler.OnHandleMessage
import com.kr.musicplayer.misc.interfaces.OnInflateFinishListener
import com.kr.musicplayer.ui.fragment.base.BaseMusicFragment
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.ToastUtil
import timber.log.Timber
import java.util.*

/**
 * 가사 현시를 위한 Fragment
 */
class LyricFragment : BaseMusicFragment {
  private var onFindListener: OnInflateFinishListener? = null
  private var info: Song? = null
  @BindView(R.id.lrcView)
  lateinit var lrcView: LrcView
  @BindView(R.id.offsetContainer)
  lateinit var offsetContainer: View
  @BindView(R.id.container)
  lateinit var container: FrameLayout

  private var mViewPager: androidx.viewpager.widget.ViewPager? = null

  private var disposable: Disposable? = null
  private val msgHandler = MsgHandler(this)

  private val lyricSearcher = LyricSearcher()

  fun setOnInflateFinishListener(l: OnInflateFinishListener) {
    onFindListener = l
  }

  constructor():super() {  }

  constructor(viewPager: androidx.viewpager.widget.ViewPager) {
    mViewPager = viewPager
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mPageName = LyricFragment::class.java.simpleName
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.fragment_lrc, container, false)
    mUnBinder = ButterKnife.bind(this, rootView)

    onFindListener?.onViewInflateFinish(lrcView)

    if(mViewPager != null) lrcView.setViewPager(mViewPager)

    return rootView
  }

  override fun onDestroyView() {
    msgHandler.remove()
    disposable?.dispose()
    onFindListener = null
    super.onDestroyView()
  }

  /**
   * 가사갱신
   * @param song 얻을려는 가사를 가진 노래
   * @param clearCache Cache 초기화 여부
   */
  @JvmOverloads
  fun updateLrc(song: Song, clearCache: Boolean = false) {
    info = song
    getLrc("", clearCache)
  }

  /**
   * 가사갱신함수
   */
  private fun getLrc(manualPath: String, clearCache: Boolean) {
    if (!isVisible)
      return
    if (info == null) {
      lrcView.setText(getStringSafely(R.string.no_lrc))
      return
    }
    if (clearCache) {
      // offset 초기화
      SPUtil.putValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0)
      lrcView.setOffset(0)
    }
    val id = info?.id

    disposable?.dispose()
    disposable = lyricSearcher.setSong(info ?: return)
        .getLyricObservable(manualPath, clearCache)
        .doOnSubscribe { lrcView.setText(getStringSafely(R.string.searching)) }
        .subscribe(Consumer {
          if (id == info?.id) {
            if (it == null || it.isEmpty()) {
              lrcView.setText(getStringSafely(R.string.no_lrc))
              return@Consumer
            }
            lrcView.setOffset(SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0))
            lrcView.lrcRows = it
          }
        }, Consumer {
          if (id == info?.id) {
            lrcView.lrcRows = null
            lrcView.setText(getStringSafely(R.string.no_lrc))
          }
        })

  }

  @OnClick(R.id.offsetReduce, R.id.offsetAdd, R.id.offsetReset)
  internal fun onClick(view: View) {
    msgHandler.removeMessages(MESSAGE_HIDE)
    msgHandler.sendEmptyMessageDelayed(MESSAGE_HIDE, DELAY_HIDE)

    val originalOffset = SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0)
    var newOffset = originalOffset
    when (view.id) {
      R.id.offsetReset -> {
        newOffset = 0
        ToastUtil.show(mContext, R.string.lyric_offset_reset)
      }
      R.id.offsetAdd -> newOffset += 500
      R.id.offsetReduce -> newOffset -= 500
    }
    if (originalOffset != newOffset) {
      SPUtil.putValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", newOffset)
      val toastMsg = msgHandler.obtainMessage(MESSAGE_SHOW_TOAST)
      toastMsg.arg1 = newOffset
      msgHandler.removeMessages(MESSAGE_SHOW_TOAST)
      msgHandler.sendMessageDelayed(toastMsg, DELAY_SHOW_TOAST)
      lrcView.setOffset(newOffset)
      MusicServiceRemote.setLyricOffset(newOffset)
    }

  }

  @OnHandleMessage
  fun handleInternal(msg: Message) {
    when (msg.what) {
      MESSAGE_HIDE -> {
        offsetContainer.visibility = View.GONE
      }
      MESSAGE_SHOW_TOAST -> {
        val newOffset = msg.arg1
        if (newOffset != 0 && Math.abs(newOffset) <= 60000) {//最大偏移60s
          ToastUtil.show(mContext, if (newOffset > 0) R.string.lyric_advance_x_second else R.string.lyric_delay_x_second,
              String.format(Locale.getDefault(), "%.1f", newOffset / 1000f))
        }
      }
    }
  }


  companion object {
    private const val DELAY_HIDE = 5000L
    private const val DELAY_SHOW_TOAST = 500L

    private const val MESSAGE_HIDE = 1
    private const val MESSAGE_SHOW_TOAST = 2
  }

}
