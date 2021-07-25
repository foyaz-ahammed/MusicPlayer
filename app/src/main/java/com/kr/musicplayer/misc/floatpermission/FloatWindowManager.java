package com.kr.musicplayer.misc.floatpermission;

import android.content.Context;
import android.provider.Settings;

import java.lang.reflect.Method;

/**
 * FloatingWindow 관리
 */

public class FloatWindowManager {

  private static volatile FloatWindowManager instance;

  public static FloatWindowManager getInstance() {
    if (instance == null) {
      synchronized (FloatWindowManager.class) {
        if (instance == null) {
          instance = new FloatWindowManager();
        }
      }
    }
    return instance;
  }

  public boolean checkPermission(Context context) {
    return commonROMPermissionCheck(context);
  }

  private boolean commonROMPermissionCheck(Context context) {
      Boolean result = true;
      try {
        Class clazz = Settings.class;
        Method canDrawOverlays = clazz.getDeclaredMethod("canDrawOverlays", Context.class);
        result = (Boolean) canDrawOverlays.invoke(null, context);
      } catch (Exception e) {

      }
      return result;
  }

}
