package com.kr.musicplayer.ui.widget.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Message
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.internal.MDTintHelper
import com.kr.musicplayer.R
import com.kr.musicplayer.lyric.bean.LrcRow
import com.kr.musicplayer.misc.handler.MsgHandler
import com.kr.musicplayer.misc.handler.OnHandleMessage
import com.kr.musicplayer.misc.interfaces.OnItemClickListener
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.service.MusicService.Companion.EXTRA_DESKTOP_LYRIC
import com.kr.musicplayer.theme.ThemeStore.getFloatLyricTextColor
import com.kr.musicplayer.theme.ThemeStore.saveFloatLyricTextColor
import com.kr.musicplayer.ui.adapter.DesktopLyricColorAdapter
import com.kr.musicplayer.ui.adapter.DesktopLyricColorAdapter.COLORS
import com.kr.musicplayer.util.ColorUtil
import com.kr.musicplayer.util.MusicUtil.makeCmdIntent
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.SPUtil.SETTING_KEY
import com.kr.musicplayer.util.ToastUtil
import com.kr.musicplayer.util.Util
import com.kr.musicplayer.util.Util.sendLocalBroadcast
import timber.log.Timber
import kotlin.math.abs


/**
 * 가사현시를 위한 view
 */

class DesktopLyricView(service: MusicService) : RelativeLayout(service) {

  private lateinit var mWindowManager: WindowManager
  private lateinit var mService: MusicService
  private val mLastPoint = PointF()
  var isLocked = false // Desktop 가사 잠금판별
    private set
  private val mUIHandler = MsgHandler(this)
  @BindView(R.id.widget_line1)
  lateinit var mText1: DesktopLyricTextView
  @BindView(R.id.widget_line2)
  lateinit var mText2: TextView
  @BindView(R.id.widget_pannel)
  lateinit var mPanel: ViewGroup
  @BindView(R.id.widget_lock)
  lateinit var mLock: ImageView
  @BindView(R.id.widget_close)
  lateinit var mClose: ImageView
  @BindView(R.id.widget_next)
  lateinit var mNext: ImageView
  @BindView(R.id.widget_play)
  lateinit var mPlay: ImageView
  @BindView(R.id.widget_prev)
  lateinit var mPrev: ImageView
  @BindView(R.id.widget_color_recyclerview)
  lateinit var mColorRecyclerView: androidx.recyclerview.widget.RecyclerView
  @BindView(R.id.widget_control_container)
  lateinit var mControlContainer: View
  @BindView(R.id.widget_lrc_container)
  lateinit var mLrcSettingContainer: View
  @BindView(R.id.widget_root)
  lateinit var mRoot: ViewGroup
  @BindView(R.id.widget_seekbar_r)
  lateinit var mSeekBarR: SeekBar
  @BindView(R.id.widget_seekbar_g)
  lateinit var mSeekBarG: SeekBar
  @BindView(R.id.widget_seekbar_b)
  lateinit var mSeekBarB: SeekBar
  @BindView(R.id.widget_text_r)
  lateinit var mTextR: TextView
  @BindView(R.id.widget_text_g)
  lateinit var mTextG: TextView
  @BindView(R.id.widget_text_b)
  lateinit var mTextB: TextView


  private lateinit var mColorAdapter: DesktopLyricColorAdapter

  private var mTextSizeType = MEDIUM
  private val mHideRunnable = Runnable {
    mPanel.visibility = View.GONE
    mLrcSettingContainer.visibility = View.GONE
  }

  private val mOnSeekBarChangeListener = object : OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
      val temp = Color.rgb(mSeekBarR.progress, mSeekBarG.progress, mSeekBarB.progress)
      val color = if (ColorUtil.isColorCloseToWhite(temp)) Color.parseColor("#F9F9F9") else temp
      mText1.setTextColor(color)
      MDTintHelper.setTint(mSeekBarR, color)
      MDTintHelper.setTint(mSeekBarG, color)
      MDTintHelper.setTint(mSeekBarB, color)
      mTextR.setTextColor(color)
      mTextG.setTextColor(color)
      mTextB.setTextColor(color)
      resetHide()

      mUIHandler.removeMessages(MESSAGE_SAVE_COLOR)
      mUIHandler.sendMessageDelayed(Message.obtain(mUIHandler, MESSAGE_SAVE_COLOR, color, 0), 100)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

    }
  }
  /**
   * 현재 끌기상태
   */
  private var mIsDragging = false

  init {
    init(service)
  }

  /**
   * 초기화
   */
  private fun init(context: Context) {
    mService = context as MusicService
    //    mNotify = new UnLockNotify();
    mWindowManager = mService.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    ButterKnife.bind(this, View.inflate(mService, R.layout.layout_desktop_lyric, this))
    setUpView()
  }

  /**
   * 색상설정
   */
  private fun setUpColor() {
    mColorRecyclerView.viewTreeObserver
        .addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
          override fun onPreDraw(): Boolean {
            mColorRecyclerView.viewTreeObserver.removeOnPreDrawListener(this)
            mColorAdapter = DesktopLyricColorAdapter(mService, R.layout.item_float_lrc_color, mColorRecyclerView.measuredWidth)
            mColorAdapter.setOnItemClickListener(object : OnItemClickListener {
              override fun onItemClick(view: View, position: Int) {
                val color = ColorUtil.getColor(COLORS[position])
                mText1.setTextColor(color)
                mColorAdapter.setCurrentColor(color)
                mColorAdapter.notifyDataSetChanged()
                resetHide()
              }

              override fun onItemLongClick(view: View, position: Int) {

              }
            })
            mColorRecyclerView.layoutManager = LinearLayoutManager(mService, LinearLayoutManager.HORIZONTAL, false)
            mColorRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
            mColorRecyclerView.adapter = mColorAdapter
            return true
          }
        })
  }

  /**
   * 가사를 현시할 view 설정
   */
  private fun setUpView() {
    val temp = getFloatLyricTextColor()
    val color = if (ColorUtil.isColorCloseToWhite(temp)) Color.parseColor("#F9F9F9") else temp
    val red = color and 0xff0000 shr 16
    val green = color and 0x00ff00 shr 8
    val blue = color and 0x0000ff
    mSeekBarR.max = 255
    mSeekBarR.progress = red
    mSeekBarG.max = 255
    mSeekBarG.progress = green
    mSeekBarB.max = 255
    mSeekBarB.progress = blue
    mTextR.setTextColor(color)
    mTextG.setTextColor(color)
    mTextB.setTextColor(color)
    mSeekBarR.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    mSeekBarG.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    mSeekBarB.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    MDTintHelper.setTint(mSeekBarR, color)
    MDTintHelper.setTint(mSeekBarG, color)
    MDTintHelper.setTint(mSeekBarB, color)

    mTextSizeType = SPUtil.getValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, MEDIUM)
    mText1.setTextColor(color)
    mText1.textSize = getTextSize(TYPE_TEXT_SIZE_FIRST_LINE)
    mText2.textSize = getTextSize(TYPE_TEXT_SIZE_SECOND_LINE)
    isLocked = SPUtil.getValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, false)

    setPlayIcon(mService.isPlaying)

    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
      override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)
        saveLock(isLocked, false)
        return true
      }
    })
  }

  /**
   * 가사의 길이를 얻는 함수
   * @param type 0: 첫번째줄 가사 1: 두번째줄 가사
   * @return 가사의 길이
   */
  private fun getTextSize(type: Int): Float {
    return when (type) {
      TYPE_TEXT_SIZE_FIRST_LINE -> {
        (when (mTextSizeType) {
          TINY -> FIRST_LINE_TINY
          SMALL -> FIRST_LINE_SMALL
          MEDIUM -> FIRST_LINE_MEDIUM
          BIG -> FIRST_LINE_BIG
          else -> FIRST_LINE_HUGE
        }).toFloat()
      }
      TYPE_TEXT_SIZE_SECOND_LINE -> {
        (when (mTextSizeType) {
          TINY -> SECOND_LINE_TINY
          SMALL -> SECOND_LINE_SMALL
          MEDIUM -> SECOND_LINE_MEDIUM
          BIG -> SECOND_LINE_BIG
          else -> SECOND_LINE_HUGE
        }).toFloat()
      }
      else -> throw IllegalArgumentException("unknown textSize type")
    }
  }

  /**
   * 가사 설정함수
   * @param lrc1 첫번째줄 가사
   * @param lrc2 두번째줄 가사
   */
  fun setText(lrc1: LrcRow?, lrc2: LrcRow?) {
    if (lrc1 != null) {
      if (TextUtils.isEmpty(lrc1.content)) {
        lrc1.content = "......"
      }
      mText1.setLrcRow(lrc1)
    }
    if (lrc2 != null) {
      if (TextUtils.isEmpty(lrc2.content)) {
        lrc2.content = "....."
      }
      mText2.text = lrc2.content
    }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> if (!isLocked) {
        mIsDragging = false
        mLastPoint.set(event.rawX, event.rawY)
        mUIHandler.removeCallbacks(mHideRunnable)
      } else {
//        mUIHandler.postDelayed(mLongClickRunnable, LONGCLICK_THRESHOLD);
      }
      MotionEvent.ACTION_MOVE -> if (!isLocked) {
        val params = layoutParams as WindowManager.LayoutParams

        if (abs(event.rawY - mLastPoint.y) > DISTANCE_THRESHOLD) {
          params.y += (event.rawY - mLastPoint.y).toInt()
          mIsDragging = true
          if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && isAttachedToWindow) {
            mWindowManager.updateViewLayout(this, params)
          }
        }
        mLastPoint.set(event.rawX, event.rawY)
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (!isLocked) {
        if (!mIsDragging) {
          //view 를 click 후 panel 을 숨기기 및 보여주기
          if (mPanel.isShown) {
            mPanel.visibility = View.INVISIBLE
          } else {
            mPanel.visibility = View.VISIBLE
            mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD.toLong())
          }
        } else {
          if (mPanel.isShown) {
            mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD.toLong())
          }
          mIsDragging = false
        }
        //y 좌표 저장
        val params = layoutParams as WindowManager.LayoutParams
        SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_Y, params.y)
      } else {
//        mUIHandler.removeCallbacks(mLongClickRunnable)
      }
    }
    return true
  }

  /**
   * 재생 및 중지단추 설정
   * @param play 현재상태가 재생상태인지 아니면 중지상태인지 판별
   */
  fun setPlayIcon(play: Boolean) {
    mPlay.setImageResource(
        if (play) R.drawable.widget_btn_stop_normal else R.drawable.widget_btn_play_normal)
  }

  @OnClick(R.id.widget_close, R.id.widget_lock, R.id.widget_next, R.id.widget_play, R.id.widget_prev, R.id.widget_lrc_bigger, R.id.widget_lrc_smaller, R.id.widget_setting)
  fun onViewClicked(view: View) {
    when (view.id) {
      //Desktop 가사 끄기
      R.id.widget_close -> {
        SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_SHOW,
            false)
        sendLocalBroadcast(
            makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC).putExtra(EXTRA_DESKTOP_LYRIC, false))
      }
      //잠금
      R.id.widget_lock -> {
        saveLock(lock = true, toast = true)
        mUIHandler.postDelayed(mHideRunnable, 0)
        Util.sendCMDLocalBroadcast(Command.LOCK_DESKTOP_LYRIC)
      }
      //가사 서체 및 크기설정
      R.id.widget_setting -> {
        mLrcSettingContainer.visibility = if (mLrcSettingContainer.isShown) View.GONE else View.VISIBLE
        setUpColor()
        //Panel 구역이 없어지는 시간 재설정
        resetHide()
      }
      R.id.widget_next, R.id.widget_play, R.id.widget_prev -> {
        sendLocalBroadcast(makeCmdIntent(when {
          view.id == R.id.widget_next -> Command.NEXT
          view.id == R.id.widget_prev -> Command.PREV
          else -> Command.TOGGLE
        }))
        mUIHandler.postDelayed({
          mPlay.setImageResource(
              if (mService.isPlaying)
                R.drawable.widget_btn_stop_normal
              else
                R.drawable.widget_btn_play_normal)
        }, 100)
        //Panel 구역이 없어지는 시간 재설정
        resetHide()
      }
      //서체 확대 및 축소
      R.id.widget_lrc_bigger, R.id.widget_lrc_smaller -> {
        var needRefresh = false
        if (view.id == R.id.widget_lrc_bigger) {
          //현재 가장 큰 서체인지 확인
          if (mTextSizeType == HUGE) {
            return
          }
          mTextSizeType++
          needRefresh = true
        }
        if (view.id == R.id.widget_lrc_smaller) {
          //현재 가장 작은 서체인지 확인
          if (mTextSizeType == TINY) {
            return
          }
          mTextSizeType--
          needRefresh = true
        }
        if (needRefresh) {
          mText1.textSize = getTextSize(TYPE_TEXT_SIZE_FIRST_LINE)
          mText2.textSize = getTextSize(TYPE_TEXT_SIZE_SECOND_LINE)
          SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, mTextSizeType)
          //Panel 구역이 없어지는 시간 재설정
          resetHide()
        }
      }
    }
  }

  /**
   * Desktop 가사 잠금 및 잠금해제
   * @param lock 잠금여부
   * @param toast 잠금 및 잠금해제에 관한 알림 표시여부
   */
  fun saveLock(lock: Boolean, toast: Boolean) {
    isLocked = lock
    SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, isLocked)
    if (toast) {
      ToastUtil.show(mService, if (!isLocked) R.string.desktop_lyric__unlock else R.string.desktop_lyric_lock)
    }
    val params = layoutParams as WindowManager.LayoutParams?
    if (params != null) {
      if (lock) {
        //锁定后点击通知栏解锁
        //        mNotify.notifyToUnlock();
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
      } else {
        //        mNotify.cancel();
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      }
      if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && isAttachedToWindow) {
        mWindowManager.updateViewLayout(this, params)
      }
    }
  }

  /**
   * Panel 구역이 없어지는 시간 재설정
   */
  private fun resetHide() {
    mUIHandler.removeCallbacks(mHideRunnable)
    mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD.toLong())
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    mUIHandler.removeCallbacksAndMessages(null)
    mSeekBarR.setOnSeekBarChangeListener(null)
    mSeekBarG.setOnSeekBarChangeListener(null)
    mSeekBarB.setOnSeekBarChangeListener(null)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
  }

  @OnHandleMessage
  fun handleMsg(msg: Message) {
    when (msg.what) {
      MESSAGE_SAVE_COLOR -> saveFloatLyricTextColor(msg.arg1)
    }
  }

  companion object {

    //서체크기종류
    private const val TINY = 0
    private const val SMALL = 1
    private const val MEDIUM = 2
    private const val BIG = 3
    private const val HUGE = 4

    //첫줄 가사의 서체크기종류
    private const val FIRST_LINE_HUGE = 20
    private const val FIRST_LINE_BIG = 19
    private const val FIRST_LINE_MEDIUM = 18
    private const val FIRST_LINE_SMALL = 17
    private const val FIRST_LINE_TINY = 16
    //두번째줄 가사의 서체크기종류
    private const val SECOND_LINE_HUGE = 18
    private const val SECOND_LINE_BIG = 17
    private const val SECOND_LINE_MEDIUM = 16
    private const val SECOND_LINE_SMALL = 15
    private const val SECOND_LINE_TINY = 14

    private const val TYPE_TEXT_SIZE_FIRST_LINE = 0
    private const val TYPE_TEXT_SIZE_SECOND_LINE = 1

    private const val DISTANCE_THRESHOLD = 10
    private const val DISMISS_THRESHOLD = 4500
    private const val LONGCLICK_THRESHOLD = 1000


    private const val MESSAGE_SAVE_COLOR = 1
  }
}
