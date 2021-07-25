package com.kr.musicplayer.util;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.StringRes;
import android.widget.Toast;

/**
 * Toast 관련 함수들
 */
public class ToastUtil {

  private ToastUtil() {
    /* cannot be instantiated */
    throw new UnsupportedOperationException("cannot be instantiated");
  }

  private static Handler mainHandler = new Handler(Looper.getMainLooper());
  public static boolean isShow = true;

  /**
   *사용자 지정 시간동안 Toast 현시
   * @param context The application context
   * @param message 현시하려는 문자렬
   * @param duration 현시할 시간
   */
  public static void show(Context context, CharSequence message, int duration) {
    if (isShow) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        showInternal(context,message,duration);
      } else {
        mainHandler.post(() -> {
          showInternal(context,message,duration);
        });
      }
    }
  }

  private static void showInternal(Context context, CharSequence message, int duration){
    if(context instanceof Activity){
      if(((Activity) context).isFinishing() || ((Activity) context).isDestroyed()){
        return;
      }
    }
    Toast toast = Toast.makeText(context, message, duration);
    toast.show();
  }

  /**
   * 사용자 지정 시간동안 Toast 현시
   * @param context The application context
   * @param message 현시하려는 문자렬의 id
   * @param duration 현시하려는 시간
   */
  public static void show(Context context, @StringRes int message, int duration) {
    show(context, context.getString(message), duration);
  }

  public static void show(Context context, @StringRes int message) {
    show(context, context.getString(message));
  }

  public static void show(Context context, CharSequence message) {
    show(context, message, Toast.LENGTH_LONG);
  }

  public static void show(Context context, @StringRes int resId, Object... formatArgs) {
    show(context, context.getString(resId, formatArgs));
  }

}
