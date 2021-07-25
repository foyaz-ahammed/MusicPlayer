package com.kr.musicplayer.ui.widget;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;

/**
 * 재생화면의 중간 세 페지를 위한 viewPager
 */
public class AudioViewPager extends ViewPager {

  boolean mIntercept = false;

  public AudioViewPager(Context context) {
    super(context);
  }

  public AudioViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setIntercept(boolean value) {
    mIntercept = value;
  }


}
