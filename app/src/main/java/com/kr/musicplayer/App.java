package com.kr.musicplayer;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.facebook.common.util.ByteConstants;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import com.kr.musicplayer.appshortcuts.Controller;
import com.kr.musicplayer.helper.LanguageHelper;
import com.kr.musicplayer.misc.cache.DiskCache;

import timber.log.Timber;

/**
 * App 이 기동될때 호출되는 클라스
 */

public class App extends MultiDexApplication implements ActivityLifecycleCallbacks {

  private static App mContext;

  // 현재 Foreground 상태에 있는 Activity 의 개수, App 이 Foreground 상태인지 판별하는데 리용된다.
  private int mForegroundActivityCount = 0;

  @Override
  protected void attachBaseContext(Context base) {
    LanguageHelper.saveSystemCurrentLanguage();
    super.attachBaseContext(LanguageHelper.setLocal(base));
    MultiDex.install(this);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mContext = this;

    setUp();

    //App이 기동할때 shortcut들을 추가한다.
    Controller controller = Controller.Companion.getController();
    controller.setContext(getApplicationContext());
    controller.setupShortcuts();

    // Third-party library 읽기
    loadLibrary();

    registerActivityLifecycleCallbacks(this);
  }

  /**
   * DiskCache 초기화
   */
  private void setUp() {
    DiskCache.init(this);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    LanguageHelper.onConfigurationChanged(getApplicationContext());
  }

  /**
   * Context 얻기
   * @return 얻어진 context
   */
  public static App getContext() {
    return mContext;
  }

  /**
   * Fresco library 읽기
   */
  private void loadLibrary() {
    // fresco
    final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
    ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
        .setBitmapMemoryCacheParamsSupplier(
            () -> new MemoryCacheParams(cacheSize, Integer.MAX_VALUE, cacheSize, Integer.MAX_VALUE,
                2 * ByteConstants.MB))
        .setBitmapsConfig(Bitmap.Config.RGB_565)
        .setDownsampleEnabled(true)
        .build();
    Fresco.initialize(this, config);

  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    Timber.v("onLowMemory");
    Completable
        .fromAction(() -> Fresco.getImagePipeline().clearMemoryCaches())
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
    Timber.v("onTrimMemory, %s", level);

    Completable
        .fromAction(() -> {
          switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:
              // Release UI
              break;
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
              // 불필요한 resource 해제
              Fresco.getImagePipeline().clearMemoryCaches();
              break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
              // 가능한 한 많은 resource 확보
              Timber.v("");
              Fresco.getImagePipeline().clearMemoryCaches();
              break;
            default:
              break;
          }
        })
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  public boolean isAppForeground() {
    return mForegroundActivityCount > 0;
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

  }

  @Override
  public void onActivityStarted(Activity activity) {
    mForegroundActivityCount++;
  }

  @Override
  public void onActivityResumed(Activity activity) {

  }

  @Override
  public void onActivityPaused(Activity activity) {

  }

  @Override
  public void onActivityStopped(Activity activity) {
    mForegroundActivityCount--;
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

  }

  @Override
  public void onActivityDestroyed(Activity activity) {

  }
}
