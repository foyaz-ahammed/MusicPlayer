package com.kr.musicplayer.util;

import timber.log.Timber;

/**
 * 가사 관련 함수들
 */
public class LyricUtil {

  private static final String TAG = "LyricUtil";

  private LyricUtil() {
  }

  public static String getCharset(final String filePath) {
    try {
      return EncodingDetect.getJavaEncode(filePath);
    } catch (Exception e) {
      Timber.w(e);
      return "UTF-8";
    }
  }
}
