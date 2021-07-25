package com.kr.musicplayer.util;

import android.content.Context;

/**
 * StatusBar 관련 함수들
 */
public class StatusBarUtil {

  /**
   * 상태띠의 높이 얻기
   *
   * @param context The application context
   * @return 상태띠 높이
   */
  public static int getStatusBarHeight(Context context) {
    // 상태띠의 높이 가져오기
    int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    return context.getResources().getDimensionPixelSize(resourceId);
  }

  public static class MeizuStatusbar {

    /**
     * 색상을 회색으로 변환
     *
     * @param rgb 색갈
     * @return 회색값
     */
    public static int toGrey(int rgb) {
      int blue = rgb & 0x000000FF;
      int green = (rgb & 0x0000FF00) >> 8;
      int red = (rgb & 0x00FF0000) >> 16;
      return (red * 38 + green * 75 + blue * 15) >> 7;
    }
  }
}
