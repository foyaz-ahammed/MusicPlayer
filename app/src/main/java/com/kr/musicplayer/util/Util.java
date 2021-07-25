package com.kr.musicplayer.util;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Vibrator;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.kr.musicplayer.App;
import com.kr.musicplayer.R;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import timber.log.Timber;

/**
 * 일반 함수들
 */
public class Util {

  public static int[] mBks = {
          R.drawable.bk1, R.drawable.bk2, R.drawable.bk3,
          R.drawable.bk4, R.drawable.bk5, R.drawable.bk6,
          R.drawable.bk7, R.drawable.bk8, R.drawable.bk9
  };

  /**
   * Local Receiver 등록
   */
  public static void registerLocalReceiver(BroadcastReceiver receiver, IntentFilter filter) {
    LocalBroadcastManager.getInstance(App.getContext()).registerReceiver(receiver, filter);
  }

  /**
   * Local Receiver 등록해제
   */
  public static void unregisterLocalReceiver(BroadcastReceiver receiver) {
    LocalBroadcastManager.getInstance(App.getContext()).unregisterReceiver(receiver);
  }

  /**
   * Local Broadcast 보내기
   * @param intent 보내려는 intent
   */
  public static void sendLocalBroadcast(Intent intent) {
    LocalBroadcastManager.getInstance(App.getContext()).sendBroadcast(intent);
  }

  /**
   *
   * @param cmd
   */
  public static void sendCMDLocalBroadcast(int cmd) {
    LocalBroadcastManager.getInstance(App.getContext()).sendBroadcast(MusicUtil.makeCmdIntent(cmd));
  }

  /**
   * Receiver 해제
   */
  public static void unregisterReceiver(Context context, BroadcastReceiver receiver) {
    try {
      if (context != null) {
        context.unregisterReceiver(receiver);
      }
    } catch (Exception e) {
    }
  }

  /**
   * App 이 Foreground 에서 실행중인지 확인
   * @return Foreground 에서 실행중이면 true 아니면 false
   */
  public static boolean isAppOnForeground() {
    try {
      ActivityManager activityManager = (ActivityManager) App.getContext()
          .getSystemService(Context.ACTIVITY_SERVICE);
      String packageName = App.getContext().getPackageName();

      List<ActivityManager.RunningAppProcessInfo> appProcesses = null;
      if (activityManager != null) {
        appProcesses = activityManager.getRunningAppProcesses();
      }
      if (appProcesses == null) {
        return false;
      }

      for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
        if (appProcess.processName.equals(packageName) &&
            appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
          return true;
        }
      }
    } catch (Exception e) {
      Timber.w("isAppOnForeground(), ex: %s", e.getMessage());
      return App.getContext().isAppForeground();
    }
    return false;
  }


  /**
   * 흔들림효과
   */
  public static void vibrate(final Context context, final long milliseconds) {
    if (context == null) {
      return;
    }
    try {
      Vibrator vibrator = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
      vibrator.vibrate(milliseconds);
    } catch (Exception ignore) {

    }

  }

  /**
   * 파일을 안전하게 삭제
   */
  public static boolean deleteFileSafely(File file) {
    if (file != null) {
      String tmpPath = file.getParent() + File.separator + System.currentTimeMillis();
      File tmp = new File(tmpPath);
      return file.renameTo(tmp) && tmp.delete();
    }
    return false;
  }

  /**
   * 변환시간
   *
   * @return 00:00 format 시간
   */
  public static String getTime(long duration) {
    int minute = (int) duration / 1000 / 60;
    int second = (int) (duration / 1000) % 60;
    //시간이 10분미만인 경우
    if (minute < 10) {
      if (second < 10) {
        return "0" + minute + ":0" + second;
      } else {
        return "0" + minute + ":" + second;
      }
    } else {
      if (second < 10) {
        return minute + ":0" + second;
      } else {
        return minute + ":" + second;
      }
    }
  }

  /**
   * Intent 에 응답하는 Activity 가 있는지 감지
   */
  public static boolean isIntentAvailable(Context context, Intent intent) {
    final PackageManager packageManager = context.getPackageManager();
    List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
        PackageManager.MATCH_DEFAULT_ONLY);
    return list != null && list.size() > 0;
  }

  /**
   * 노래이름, 예술가이름 또는 Album 이름처리
   *
   * @param origin 원시자료
   * @param type 처리류형 0: 노래이름 1: 예술가이름 2: Album 이름 3: 파일이름
   * @return
   */
  public static final int TYPE_SONG = 0;
  public static final int TYPE_ARTIST = 1;
  public static final int TYPE_ALBUM = 2;
  public static final int TYPE_DISPLAYNAME = 3;

  public static String processInfo(String origin, int type) {
    if (type == TYPE_SONG) {
      if (origin == null || origin.equals("")) {
        return App.getContext().getString(R.string.unknown_song);
      } else {
//                return origin.lastIndexOf(".") > 0 ? origin.substring(0, origin.lastIndexOf(".")) : origin;
        return origin;
      }
    } else if (type == TYPE_DISPLAYNAME) {
      if (origin == null || origin.equals("")) {
        return App.getContext().getString(R.string.unknown_song);
      } else {
        return origin.lastIndexOf(".") > 0 ? origin.substring(0, origin.lastIndexOf(".")) : origin;
      }
    } else {
      if (origin == null || origin.equals("")) {
        return App.getContext()
            .getString(type == TYPE_ARTIST ? R.string.unknown_artist : R.string.unknown_album);
      } else {
        return origin;
      }
    }
  }

  /**
   * 주어진 keyword 의 MD 값 반환
   *
   */
  public static String hashKeyForDisk(String key) {
    String cacheKey;
    try {
      final MessageDigest mDigest = MessageDigest.getInstance("MD5");
      mDigest.update(key.getBytes());
      cacheKey = bytesToHexString(mDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      cacheKey = String.valueOf(key.hashCode());
    }
    return cacheKey;
  }

  /**
   * byte 를 hex 로 전환
   * @param bytes 변환하려는 bytes
   * @return hex 값
   */
  public static String bytesToHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte aByte : bytes) {
      String hex = Integer.toHexString(0xFF & aByte);
      if (hex.length() == 1) {
        sb.append('0');
      }
      sb.append(hex);
    }
    return sb.toString();
  }

  /**
   * 허가 여부 확인
   */
  public static boolean hasPermissions(String[] permissions) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
      for (String permission : permissions) {
        if (App.getContext().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * 읽기 및 쓰기권한
   */
  private static final String[] PERMISSION_STORAGE = new String[]{
      Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

  public static boolean hasStoragePermissions() {
    return hasPermissions(PERMISSION_STORAGE);
  }
}
