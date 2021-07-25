package com.kr.musicplayer.util;

import android.content.Context;
import com.kr.musicplayer.App;

/**
 * 화면밀도와 관련된 함수들
 */
public class DensityUtil {

  /**
   * dp 를 pixel 로 변환하는 함수
   * @param context The application context
   * @param dpValue 변환하려는 dp 값
   * @return 변환된 pixel 값
   */
  public static int dip2px(Context context, float dpValue) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dpValue * scale + 0.5f);
  }

  /**
   * dp 를 pixel 로 변환하는 함수
   * @param dpValue 변환하려는 dp 값
   * @return 변환된 pixel 값
   */
  public static int dip2px(float dpValue) {
    return dip2px(App.getContext(), dpValue);
  }
}
