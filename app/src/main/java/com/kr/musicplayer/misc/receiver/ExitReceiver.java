package com.kr.musicplayer.misc.receiver;

import static com.kr.musicplayer.service.MusicService.TAG_LIFECYCLE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.kr.musicplayer.helper.ShakeDetector;
import com.kr.musicplayer.misc.manager.ActivityManager;
import com.kr.musicplayer.misc.manager.ServiceManager;
import timber.log.Timber;

/**
 * 프로그람종료 관련 BroadcastReceiver
 */
public class ExitReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    //모든 Service 중지
    ServiceManager.StopAll();
    //흔들기 감지 취소
        ShakeDetector.getInstance().stopListen();
    //모든 Activity 닫기
    ActivityManager.FinishAll();
    new Handler().postDelayed(() -> {
      Timber.tag(TAG_LIFECYCLE).v("Close the app");
      System.exit(0);
    }, 1000);
  }
}
