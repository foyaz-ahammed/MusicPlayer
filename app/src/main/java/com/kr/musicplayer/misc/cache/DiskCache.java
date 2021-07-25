package com.kr.musicplayer.misc.cache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import com.facebook.common.util.ByteConstants;
import java.io.File;
import java.io.IOException;

/**
 * DiskCache 관련 클라스
 */
public class DiskCache {

  private static DiskLruCache mLrcCache;

  /**
   * 초기화
   * @param context The application context
   */
  public static void init(Context context) {
    try {
      File lrcCacheDir = getDiskCacheDir(context, "lyric");
      if (!lrcCacheDir.exists()) {
        lrcCacheDir.mkdir();
      }
      mLrcCache = DiskLruCache.open(lrcCacheDir, getAppVersion(context), 1, 10 * ByteConstants.MB);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * 가사 cache 얻기
   * @return 가사 cache
   */
  public static DiskLruCache getLrcDiskCache() {
    return mLrcCache;
  }

  public static File getDiskCacheDir(Context context, String uniqueName) {
    String cachePath = "";
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
        || !Environment.isExternalStorageRemovable()) {
      File file = context.getExternalCacheDir();
      if (file != null) {
        cachePath = file.getPath();
      }
    } else {
      cachePath = context.getCacheDir().getPath();
    }
    return new File(cachePath + File.separator + uniqueName);
  }

  public static int getAppVersion(Context context) {
    try {
      PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      return info.versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
    return 1;
  }

}
