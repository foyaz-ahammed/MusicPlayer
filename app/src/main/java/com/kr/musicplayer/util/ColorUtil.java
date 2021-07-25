package com.kr.musicplayer.util;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.core.content.ContextCompat;

import com.kr.musicplayer.App;

/**
 * 색상설정과 관련된 함수들
 */
public class ColorUtil {

  private ColorUtil() {
  }

  /**
   * 주어진 resource 에 관한 색상을 얻는 함수
   * @param colorRes resourceId
   * @return 색상
   */
  @ColorInt
  public static int getColor(@ColorRes int colorRes) {
    return ContextCompat.getColor(App.getContext(), colorRes);
  }

  /**
   * 색상 투명도 조절함수
   * @param paramInt 색상
   * @param  paramFloat 투명도 값
   */
  @ColorInt
  public static int adjustAlpha(@ColorInt int paramInt,
      @FloatRange(from = 0.0D, to = 1.0D) float paramFloat) {
    return Color.argb(Math.round(Color.alpha(paramInt) * paramFloat), Color.red(paramInt),
        Color.green(paramInt), Color.blue(paramInt));
  }

  /**
   * 주어진 색이 밝은 색인지 판별
   * @param color 판별할 색
   */
  public static boolean isColorLight(@ColorInt int color) {
    double darkness = 1.0D -
        (0.299D * (double) Color.red(color) + 0.587D * (double) Color.green(color)
            + 0.114D * (double) Color.blue(color)) / 255.0D;
    return darkness < 0.4D;
  }

  /**
   * 색상변환
   */
  @ColorInt
  public static int shiftColor(@ColorInt int paramInt,
      @FloatRange(from = 0.0D, to = 2.0D) float paramFloat) {
    if (paramFloat == 1.0F) {
      return paramInt;
    }
    int i = Color.alpha(paramInt);
    float[] arrayOfFloat = new float[3];
    Color.colorToHSV(paramInt, arrayOfFloat);
    arrayOfFloat[2] *= paramFloat;
    return (i << 24) + (Color.HSVToColor(arrayOfFloat) & 0xFFFFFF);
  }

  public static int stripAlpha(@ColorInt int paramInt) {
    return 0xFF000000 | paramInt;
  }

  /**
   * 주어진 색이 하얀색에 가까운 색인지 판별
   * @param color 판별하려는 색
   * @return 하얀색에 가까우면 true 아니면 false
   */
  public static boolean isColorCloseToWhite(@ColorInt int color) {
    return StatusBarUtil.MeizuStatusbar.toGrey(color) >= 254;
  }
}
