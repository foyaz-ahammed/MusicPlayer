package com.kr.musicplayer.theme;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.SPUtil;

public class ThemeStore {

  public static final String NAME = "aplayer-theme";

  public static final String LIGHT = "light";
  public static final String DARK = "dark";
  public static final String BLACK = "black";
  public static final String KEY_THEME = "theme";
  public static final String KEY_PRIMARY_COLOR = "primary_color";
  public static final String KEY_PRIMARY_DARK_COLOR = "primary_dark_color";
  public static final String KEY_ACCENT_COLOR = "accent_color";
  public static final String KEY_FLOAT_LYRIC_TEXT_COLOR = "float_lyric_text_color";

  public static int STATUS_BAR_ALPHA = 150;

  public static boolean sColoredNavigation = false;
  public static boolean sImmersiveMode = false;
  public static String sTheme = LIGHT;

  /**
   * Theme resourceId 얻기
   */
  @StyleRes
  public static int getThemeRes() {
    return R.style.Theme_APlayer;
  }

  /**
   * MaterialPrimary 색 얻기
   */
  @ColorInt
  public static int getMaterialPrimaryColor() {
    return ColorUtil.getColor(R.color.light_background_color_main);

  }

  /**
   * Accent 색 얻기
   */
  @ColorInt
  public static int getAccentColor() {
    return ColorUtil.getColor(R.color.default_accent_color);
  }

  /**
   * NavigationBar 색 얻기
   */
  @ColorInt
  public static int getNavigationBarColor() {
    return getMaterialPrimaryColor();
  }

  /**
   * Text Primary 색 얻기
   */
  @ColorInt
  public static int getTextColorPrimary() {
    return ColorUtil.getColor(R.color.light_text_color_primary);
  }

  /**
   * 가사 본문색상 얻기
   */
  @ColorInt
  public static int getFloatLyricTextColor() {
    final int temp = SPUtil
        .getValue(App.getContext(), NAME, KEY_FLOAT_LYRIC_TEXT_COLOR, getMaterialPrimaryColor());

    return ColorUtil.isColorCloseToWhite(temp) ? Color.parseColor("#F9F9F9") : temp;
  }

  /**
   * 가사 본문색상 Preference 에 보관
   * @param color 보관하려는 색상
   */
  public static void saveFloatLyricTextColor(@ColorInt int color) {
    SPUtil.putValue(App.getContext(), NAME, KEY_FLOAT_LYRIC_TEXT_COLOR, color);
  }
}
