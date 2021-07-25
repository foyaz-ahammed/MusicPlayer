package com.kr.musicplayer.lyric;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;
import androidx.viewpager.widget.ViewPager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Scroller;
import java.util.List;
import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.lyric.bean.LrcRow;
import com.kr.musicplayer.theme.Theme;
import com.kr.musicplayer.util.DensityUtil;
import timber.log.Timber;

/**
 * 가사 view
 */

public class LrcView extends View implements ILrcView {

  /**
   * 모든 가사
   ***/
  private List<LrcRow> mLrcRows;
  /**
   * 모든 가사의 총 높이
   */
  private int mTotalHeight;
  /**
   * 가사 강조를 위한 paint
   ***/
  private TextPaint mPaintForHighLightLrc;
  /**
   * 가사사이의 기본 줄 간격
   **/
  public static final float DEFAULT_PADDING = DensityUtil.dip2px(App.getContext(), 10);
  /**
   * 줄간 사이의 추가 줄 간격
   */
  public static final float DEFAULT_SPACING_PADDING = 0/** DensityUtil.dip2px(App.getContext(),5)*/
      ;
  /**
   * 줄간사이의 여러 줄 간격
   */
  public static final float DEFAULT_SPACING_MULTI = 1f;
  /**
   * 가사의 현재 서체크기 강조
   ***/
  private float mSizeForHighLightLrc;
  /**
   * 강조표시된 가사의 기본 서체색상
   **/
  private static final int DEFAULT_COLOR_FOR_HIGH_LIGHT_LRC = Color.BLACK;
  /**
   * 가사의 현재 서체색상강조
   **/
  private int mColorForHighLightLrc = DEFAULT_COLOR_FOR_HIGH_LIGHT_LRC;

  /**
   * 다른 가사를 위한 Paint
   ***/
  private TextPaint mPaintForOtherLrc;
  /**
   * 다른 가사의 현재 서체 크기
   ***/
  private float mSizeForOtherLrc;
  /**
   * 다른 가사의 기본 서체색상
   **/
  private static final int DEFAULT_COLOR_FOR_OTHER_LRC = Color.GRAY;
  /**
   * 가사의 현재 서체색상강조
   **/
  private int mColorForOtherLrc = DEFAULT_COLOR_FOR_OTHER_LRC;


  /**
   * TimeLine Paint
   ***/
  private TextPaint mPaintForTimeLine;
  /***TimeLine 색상**/
  private int mTimeLineColor = Color.GRAY;
  /**
   * 시간 text 크기
   **/
  private float mSizeForTimeLine;
  /**
   * TimeLine 그리기여부
   **/
  private boolean mIsDrawTimeLine = false;

  /**
   * 가사사이의 줄 간격
   **/
  private float mLinePadding = DEFAULT_PADDING;

  /**
   * 기본 확대비률
   **/
  private static final float DEFAULT_SCALING_FACTOR = 1.0f;
  /**
   * 가사의 현재 확대/축소 비률
   **/
  private float mCurScalingFactor = DEFAULT_SCALING_FACTOR;
  /**
   * 가사의 부드러운 수직 Scrolling 을 위한 보조 객체
   **/
  private Scroller mScroller;
  private Interpolator DEFAULT_INTERPOLATOR = new DecelerateInterpolator();
  /***문장의 길이이동시간**/
  private static final int DURATION_FOR_LRC_SCROLL = 800;
  /***다치기가 중지되면 view 를 Scroll 해야 하는 기간**/
  private static final int DURATION_FOR_ACTION_UP = 400;
  /**
   * Sliding 후 Timeline 애 표시되는 시간
   */
  private static final int DURATION_TIME_LINE = 3000;
  /**
   * TimeLine 아이콘
   */
  private static final Drawable TIMELINE_DRAWABLE = Theme
      .getDrawable(App.getContext(), R.drawable.icon_lyric_timeline);
  /**
   * 초기상태 Timeline 아이콘의 위치
   */
  private static Rect TIMELINE_DRAWABLE_RECT;
  /**
   * 본문크기조절을 조절하는 요소
   **/
  private float mCurFraction = 0;
  private int mTouchSlop;

  /**
   * 가사 없음
   */
  private String mText = App.getContext().getString(R.string.no_lrc);
  /**
   * 현재 좌표
   */
  private float mRowY;

  private ViewPager mViewPager;

  public LrcView(Context context) {
    super(context);
    init();
  }

  public LrcView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  /**
   * 초기화
   */
  @Override
  public void init() {
    mScroller = new Scroller(getContext(), DEFAULT_INTERPOLATOR);
    mPaintForHighLightLrc = new TextPaint();
    mPaintForHighLightLrc.setAntiAlias(true);
    mPaintForHighLightLrc.setColor(mColorForHighLightLrc);
    float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25,
        getContext().getResources().getDisplayMetrics());

    mSizeForHighLightLrc = size;
    mPaintForHighLightLrc.setTextSize(mSizeForHighLightLrc);
    mPaintForHighLightLrc.setFakeBoldText(true);

    mPaintForOtherLrc = new TextPaint();
    mPaintForOtherLrc.setAntiAlias(true);
    mPaintForOtherLrc.setColor(mColorForOtherLrc);

    mSizeForOtherLrc = size;
    mPaintForOtherLrc.setTextSize(mSizeForOtherLrc);

    mPaintForTimeLine = new TextPaint();
    mPaintForTimeLine.setAntiAlias(true);
    mSizeForTimeLine = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11,
        getContext().getResources().getDisplayMetrics());
    mPaintForTimeLine.setTextSize(mSizeForTimeLine);
    mPaintForTimeLine.setColor(mTimeLineColor);

    mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (TIMELINE_DRAWABLE_RECT == null)
    //누름령역확장
    {
      TIMELINE_DRAWABLE_RECT = new Rect(-TIMELINE_DRAWABLE.getIntrinsicWidth() / 2,
          (getHeight() / 2 - TIMELINE_DRAWABLE.getIntrinsicHeight()),
          TIMELINE_DRAWABLE.getIntrinsicWidth() + TIMELINE_DRAWABLE.getIntrinsicWidth() / 2,
          (getHeight() / 2 + TIMELINE_DRAWABLE.getIntrinsicHeight()));
    }
  }

  private int mTotalRow;

  @SuppressLint("DrawAllocation")
  @Override
  protected void onDraw(Canvas canvas) {
    if (mLrcRows == null || mLrcRows.size() == 0) {
      //기본표시본문그리기
      float textWidth = mPaintForOtherLrc.measureText(mText);
      float textX = (getWidth() - textWidth) / 2;
      mPaintForOtherLrc.setAlpha(0xff);
      canvas.drawText(mText, textX, getHeight() / 2, mPaintForOtherLrc);
      return;
    }

    final int availableWidth = getWidth() - (getPaddingLeft() + getPaddingRight());

    mRowY = getHeight() / 2;
    for (int i = 0; i < mLrcRows.size(); i++) {
      if (i == mCurRow) {   //Highlight 가사 그리기
        drawLrcRow(canvas, mPaintForHighLightLrc, availableWidth, mLrcRows.get(i));
      } else {  //일반가사
        drawLrcRow(canvas, mPaintForOtherLrc, availableWidth, mLrcRows.get(i));
      }
    }
    //Timeline 및 시간그리기
    if (mIsDrawTimeLine) {
      float y = getHeight() / 2 + getScrollY() + DEFAULT_SPACING_PADDING;

      canvas.drawText(mLrcRows.get(mCurRow).getTimeStr(),
          getWidth() - mPaintForTimeLine.measureText(mLrcRows.get(mCurRow).getTimeStr()) - 5,
          y - 10, mPaintForTimeLine);
      canvas.drawLine(TIMELINE_DRAWABLE.getIntrinsicWidth() + 10, y, getWidth(), y,
          mPaintForTimeLine);
      TIMELINE_DRAWABLE.setBounds(0,
          (int) y - TIMELINE_DRAWABLE.getIntrinsicHeight() / 2,
          TIMELINE_DRAWABLE.getIntrinsicWidth(),
          (int) y + TIMELINE_DRAWABLE.getIntrinsicHeight() / 2);
      TIMELINE_DRAWABLE.draw(canvas);

    }
  }

  public void setViewPager(ViewPager vp) {
    mViewPager = vp;
  }

  /**
   * 가사분할 및 그리기
   */
  private void drawLrcRow(Canvas canvas, TextPaint textPaint, int availableWidth, LrcRow lrcRow) {
    drawText(canvas, textPaint, availableWidth, lrcRow.getContent());
    if (lrcRow.hasTranslate()) {
      drawText(canvas, textPaint, availableWidth, lrcRow.getTranslate());
    }
    mRowY += mLinePadding;
  }

  /**
   * 가사분할 및 그리기
   */
  private void drawText(Canvas canvas, TextPaint textPaint, int availableWidth, String text) {
    StaticLayout staticLayout = new StaticLayout(text, textPaint, availableWidth,
        Layout.Alignment.ALIGN_CENTER,
        DEFAULT_SPACING_MULTI, 0, true);
    final int extra = staticLayout.getLineCount() > 1 ? DensityUtil.dip2px(getContext(), 10) : 0;
    canvas.save();
    canvas.translate(getPaddingLeft(), mRowY - staticLayout.getHeight() / 2 + extra);
    staticLayout.draw(canvas);
    canvas.restore();
    mRowY += staticLayout.getHeight();
  }

  /**
   * 가사 끌기 여부
   **/
  private boolean mCanDrag = false;
  /**
   * 첫번째 y좌표
   **/
  private float mFirstY;
  /**
   * 마지막 y좌표
   **/
  private float mLastY;
  private float mLastX;
  /**
   * TimeLine 대기
   */
  private boolean mTimeLineWaiting;
  /**
   * LongPress Runnable
   */
  private Runnable mLongPressRunnable = new LongPressRunnable();
  private Runnable mTimeLineDisableRunnable = new TimeLineRunnable();
  private Handler mHandler = new Handler();

  private class LongPressRunnable implements Runnable {

    @Override
    public void run() {
      if (mOnLrcClickListener != null) {
        mOnLrcClickListener.onLongClick();
      }
    }
  }

  private class TimeLineRunnable implements Runnable {

    @Override
    public void run() {
      mTimeLineWaiting = false;
      mIsDrawTimeLine = false;
      invalidate();
    }
  }

  public List<LrcRow> getLrcRows() {
    return mLrcRows;
  }

  private boolean hasLrc() {
    return mLrcRows != null && mLrcRows.size() > 0;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        //가사없음
        if (hasLrc()) {
          mFirstY = event.getRawY();
          mLastX = event.getRawX();
          if (mTimeLineWaiting) {
            if (TIMELINE_DRAWABLE_RECT.contains((int) event.getX(), (int) event.getY())
                && onSeekToListener != null && mCurRow != -1) {
              mHandler.removeCallbacks(mTimeLineDisableRunnable);
              mHandler.post(mTimeLineDisableRunnable);
              onSeekToListener.onSeekTo(mLrcRows.get(mCurRow).getTime());
              return false;
            }
          }
        }
        mLongPressRunnable = new LongPressRunnable();
        mHandler.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
        break;
      case MotionEvent.ACTION_MOVE:
        if (hasLrc()) {
          if (!mCanDrag) {
            if (Math.abs(event.getRawY() - mFirstY) > mTouchSlop
                && Math.abs(event.getRawY() - mFirstY) > Math.abs(event.getRawX() - mLastX)) {
              mCanDrag = true;
              mIsDrawTimeLine = true;
              mScroller.forceFinished(true);
              mCurFraction = 1;
            }
            mLastY = event.getRawY();
          }
          if (mCanDrag) {
            mTimeLineWaiting = false;
            mHandler.removeCallbacks(mLongPressRunnable);
            float offset = event.getRawY() - mLastY;//offset
            if (getScrollY() - offset < 0) {
              if (offset > 0) {
//                                offset = offset / 3;
              }
            } else if (getScrollY() - offset > mTotalHeight) {
              if (offset < 0) {
//                                offset = offset / 3;
              }
            }
            scrollBy(getScrollX(), -(int) offset);
            mLastY = event.getRawY();
            //Scroll 후 거리에 따라 가사 찾기
            int currentRow = getRowByScrollY();
            currentRow = Math.min(currentRow, mLrcRows.size() - 1);
            currentRow = Math.max(currentRow, 0);
            seekTo(mLrcRows.get(currentRow).getTime(), false, false);
            return true;
          }
          mLastY = event.getRawY();
        } else {
          mHandler.removeCallbacks(mLongPressRunnable);
        }
        break;
      case MotionEvent.ACTION_UP:
        if (!mCanDrag) {
          if (mLongPressRunnable == null && mOnLrcClickListener != null) {
            mOnLrcClickListener.onClick();
          }
          mHandler.removeCallbacks(mLongPressRunnable);
          mLongPressRunnable = null;
        } else {
          //3초동안 TimeLine 표시
          mHandler.removeCallbacks(mTimeLineDisableRunnable);
          mHandler.postDelayed(mTimeLineDisableRunnable, DURATION_TIME_LINE);
          mTimeLineWaiting = true;
          if (getScrollY() < 0) {
            smoothScrollTo(0, DURATION_FOR_ACTION_UP);
          } else if (getScrollY() > getScrollYByRow(mCurRow)) {
            smoothScrollTo(getScrollYByRow(mCurRow), DURATION_FOR_ACTION_UP);
          }
          mCanDrag = false;
          invalidate();
        }
        break;
      case MotionEvent.ACTION_CANCEL:
        mHandler.removeCallbacks(mLongPressRunnable);
        mLongPressRunnable = null;
        break;
    }
    return true;
  }

  /**
   * 가사목록자료설정
   */
  @Override
  public void setLrcRows(List<LrcRow> lrcRows) {
    reset();

    mLrcRows = lrcRows;
    if (mLrcRows != null) {
      for (LrcRow lrcRow : mLrcRows) {
        lrcRow.setContentHeight(getSingleLineHeight(lrcRow.getContent()));
        if (lrcRow.hasTranslate()) {
          lrcRow.setTranslateHeight(getSingleLineHeight(lrcRow.getTranslate()));
        }
        lrcRow.setTotalHeight(lrcRow.getTranslateHeight() + lrcRow.getContentHeight());
        mTotalHeight += lrcRow.getTotalHeight();
      }
    }
    invalidate();
  }

  /**
   * 가사의 한 문장높이 얻기
   */
  private int getSingleLineHeight(String text) {
    StaticLayout staticLayout = new StaticLayout(text, mPaintForOtherLrc,
        getWidth() - getPaddingLeft() - getPaddingRight(), Layout.Alignment.ALIGN_CENTER,
        DEFAULT_SPACING_MULTI, DEFAULT_SPACING_PADDING, true);
    return staticLayout.getHeight();
  }

  /**
   * 현재 강조표시된 가사의 줄 번호
   **/
  private int mCurRow = -1;
  /**
   * 마지막으로 강조 표시된 가사의 줄 번호
   **/
  private int mLastRow = -1;

  /**
   * 주어진 행까지의 Scroll 거리
   * @param row 주어진 행
   */
  private int getScrollYByRow(int row) {
    if (mLrcRows == null) {
      return 0;
    }
    int scrollY = 0;
    for (int i = 0; i < mLrcRows.size() && i < row; i++) {
      scrollY += mLrcRows.get(i).getTotalHeight() + mLinePadding;
    }
    return scrollY;
  }

  /**
   * 현재 행수를 기준으로 sliding 거리 계산
   */
  private int getRowByScrollY() {
    if (mLrcRows == null) {
      return 0;
    }
    int totalY = 0;
    int line;

    for (line = 0; line < mLrcRows.size(); line++) {
      totalY += mLinePadding + mLrcRows.get(line).getTotalHeight();
      if (totalY >= getScrollY()) {
        return line;
      }
    }
    return line - 1;
  }

  private int mOffset;

  public void setOffset(int offset) {
    mOffset = offset;
    invalidate();
  }

  @Override
  public void seekTo(int progress, boolean fromSeekBar, boolean fromSeekBarByUser) {
    if (progress != 0) {
      progress += mOffset;
    }
    if (mLrcRows == null || mLrcRows.size() == 0) {
      return;
    }
    //SeekBar 의 진행률정도와 끌기하는것에 의해 trigger 되면 return
    if (fromSeekBar && mCanDrag) {
      return;
    }
    //TimeLine 대기
    if (mTimeLineWaiting) {
      return;
    }
    for (int i = mLrcRows.size() - 1; i >= 0; i--) {
      if (progress >= mLrcRows.get(i).getTime()) {
        if (mCurRow != i) {
          mLastRow = mCurRow;
          mCurRow = i;
          if (fromSeekBarByUser) {
            if (!mScroller.isFinished()) {
              mScroller.forceFinished(true);
            }
            scrollTo(getScrollX(), getScrollYByRow(mCurRow));
          } else {
            smoothScrollTo(getScrollYByRow(mCurRow), DURATION_FOR_LRC_SCROLL);
          }
          invalidate();
        }
        break;
      }
    }

  }

  /**
   * 가사의 확대비률설정
   */
  @Override
  public void setLrcScalingFactor(float scalingFactor) {
    mCurScalingFactor = scalingFactor;
    mSizeForHighLightLrc *= mCurScalingFactor;
    mSizeForOtherLrc *= mCurScalingFactor;
    mLinePadding = DEFAULT_PADDING * mCurScalingFactor;
    mTotalRow = (int) (getHeight() / (mSizeForOtherLrc + mLinePadding)) + 3;
    scrollTo(getScrollX(), (int) (mCurRow * (mSizeForOtherLrc + mLinePadding)));
    invalidate();
    mScroller.forceFinished(true);
  }

  /**
   * 재설정
   */
  @Override
  public void reset() {
    if (!mScroller.isFinished()) {
      mScroller.forceFinished(true);
    }
    mCurRow = 0;
    mTotalRow = 0;
    mLrcRows = null;
    mHandler.removeCallbacks(mLongPressRunnable);
    mHandler.removeCallbacks(mTimeLineDisableRunnable);
    mHandler.post(mTimeLineDisableRunnable);
    scrollTo(getScrollX(), 0);
    invalidate();
  }


  /**
   * 유연하게 이동
   */
  private void smoothScrollTo(int dstY, int duration) {
    int oldScrollY = getScrollY();
    int offset = dstY - oldScrollY;
    mScroller.startScroll(getScrollX(), oldScrollY, getScrollX(), offset, duration);
    invalidate();
  }

  @Override
  public void computeScroll() {
    if (!mScroller.isFinished()) {
      if (mScroller.computeScrollOffset()) {
        int oldY = getScrollY();
        int y = mScroller.getCurrY();
        if (oldY != y && !mCanDrag) {
          scrollTo(getScrollX(), y);
        }
        mCurFraction = mScroller.timePassed() * 3f / DURATION_FOR_LRC_SCROLL;
        mCurFraction = Math.min(mCurFraction, 1F);
        invalidate();
      }
    }
  }

  private OnSeekToListener onSeekToListener;

  public void setOnSeekToListener(OnSeekToListener onSeekToListener) {
    this.onSeekToListener = onSeekToListener;
  }

  public void setText(String text) {
    mText = text;
    reset();
  }

  public void setText(@StringRes int res) {
    setText(getResources().getString(res));
  }

  public interface OnSeekToListener {

    void onSeekTo(int progress);
  }

  private OnLrcClickListener mOnLrcClickListener;

  public void setOnLrcClickListener(OnLrcClickListener mOnLrcClickListener) {
    this.mOnLrcClickListener = mOnLrcClickListener;
  }

  public interface OnLrcClickListener {

    void onClick();

    void onLongClick();
  }

  public void log(Object o) {
    Timber.v("%s", o);
  }

  /**
   * 강조표시된 가사의 색상 설정
   */
  public void setHighLightColor(@ColorInt int color) {
    mColorForHighLightLrc = color;
    if (mPaintForHighLightLrc != null) {
      mPaintForHighLightLrc.setColor(mColorForHighLightLrc);
    }
  }

  /**
   * 강조표시되지 않은 가사의 색상 설정
   */
  public void setOtherColor(@ColorInt int color) {
    mColorForOtherLrc = color;
    if (mPaintForOtherLrc != null) {
      mPaintForOtherLrc.setColor(mColorForOtherLrc);
    }
  }

  /**
   * TimeLine 색상설정
   */
  public void setTimeLineColor(@ColorInt int color) {
    if (mTimeLineColor != color) {
      mTimeLineColor = color;
      Theme.tintDrawable(TIMELINE_DRAWABLE, color);
      mPaintForTimeLine.setColor(color);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (mHandler != null) {
      mHandler.removeCallbacksAndMessages(null);
    }
  }


}
