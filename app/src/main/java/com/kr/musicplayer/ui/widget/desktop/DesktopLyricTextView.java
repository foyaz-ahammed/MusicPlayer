package com.kr.musicplayer.ui.widget.desktop;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import com.kr.musicplayer.lyric.bean.LrcRow;

/**
 * Desktop 가사현시를 위한 TextView
 */

public class DesktopLyricTextView extends androidx.appcompat.widget.AppCompatTextView {

  private static final int DELAY_MAX = 100;

  private float mCurTextXForHighLightLrc;
  /**
   * 현재 가사
   */
  private LrcRow mCurLrcRow;
  /**
   * 현재 가사의 문자렬이 차지하는 령역
   */
  private Rect mTextRect = new Rect();
  /***
   * Animation 의 변화를 위한 Listener
   */
  ValueAnimator.AnimatorUpdateListener mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
      mCurTextXForHighLightLrc = (Float) animation.getAnimatedValue();
      invalidate();
    }
  };

  public DesktopLyricTextView(Context context) {
    this(context, null);
  }

  public DesktopLyricTextView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DesktopLyricTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
  }

  /**
   * 가사의 가로 Scroll 을 조종하는 Animation
   ***/
  private ValueAnimator mAnimator;

  /**
   * 가사를 가로로 Scroll 시작
   *
   * @param endX 가사의 첫 단어의 마지막 x 좌표
   * @param duration Scroll 진행시간
   */
  private void startScrollLrc(float endX, long duration) {
    if (mAnimator == null) {
      mAnimator = ValueAnimator.ofFloat(0, endX);
      mAnimator.addUpdateListener(mUpdateListener);
    } else {
      mCurTextXForHighLightLrc = 0;
      mAnimator.cancel();
      mAnimator.setFloatValues(0, endX);
    }
    mAnimator.setDuration(duration);
    long delay = (long) (duration * 0.1);
    mAnimator.setStartDelay(delay > DELAY_MAX ? DELAY_MAX : delay); //Animation 실행 지연
    mAnimator.start();
  }

  @Override
  public void setTextColor(ColorStateList colors) {
    super.setTextColor(colors);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mCurLrcRow == null) {
      return;
    }
    canvas.drawText(mCurLrcRow.getContent(),
        mCurTextXForHighLightLrc,
        (getHeight() - getPaint().getFontMetrics().top - getPaint().getFontMetrics().bottom) / 2,
        getPaint());
  }

  /**
   * 가사현시
   * @param lrcRow 현시하려는 가사줄
   */
  public void setLrcRow(@NonNull LrcRow lrcRow) {
    if (lrcRow.getTime() != 0 && mCurLrcRow != null && mCurLrcRow.getTime() == lrcRow.getTime()) {
      return;
    }
    mCurLrcRow = lrcRow;
    stopAnimation();

    TextPaint paint = getPaint();
    if (paint == null) {
      return;
    }
    String text = mCurLrcRow.getContent();
    paint.getTextBounds(text, 0, text.length(), mTextRect);
    float textWidth = mTextRect.width();
    if (textWidth > getWidth()) {
      //가사의 너비가 view 의 너비보다 크면 시작 x 좌표를 동적으로 설정하여 수평 Scroll 을 한다
      startScrollLrc(getWidth() - textWidth, (long) (mCurLrcRow.getTotalTime() * 0.85));
    } else {
      //가사의 너비가 view 의 너비보다 작으면 가사를 view 의 중심에 현시하도록 한다
      mCurTextXForHighLightLrc = (getWidth() - textWidth) / 2;
      invalidate();
    }

  }

  /**
   * Animation 중지
   */
  public void stopAnimation() {
    if (mAnimator != null && mAnimator.isRunning()) {
      mAnimator.cancel();
    }
    invalidate();
  }
}
