package com.kr.musicplayer.misc.manager;

import android.app.Activity;
import java.util.ArrayList;

/**
 * Activity 관리가 종료되면 모든 Activity 끝내기
 */
public class ActivityManager {

  private static ArrayList<Activity> mActivityList = new ArrayList<>();

  /**
   * ActivityManager에 Activity 추가
   * @param activity 추가할 Activity
   */
  public static void AddActivity(Activity activity) {
    mActivityList.add(activity);
  }

  /**
   * ActivityManager 에서 지정된 Activity 없애기
   * @param activity 없앨 Activity
   */
  public static void RemoveActivity(Activity activity) {
    mActivityList.remove(activity);
  }

  /**
   * 모든 Activity 를 끝내는 함수
   */
  public static void FinishAll() {
    for (Activity activity : mActivityList) {
      if (activity != null && !activity.isFinishing()) {
        activity.finish();
      }
    }
  }
}
