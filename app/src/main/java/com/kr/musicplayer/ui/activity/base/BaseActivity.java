package com.kr.musicplayer.ui.activity.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Toast;

import com.kr.musicplayer.helper.LanguageHelper;
import com.kr.musicplayer.misc.manager.ActivityManager;
import com.kr.musicplayer.service.MusicService;
import com.kr.musicplayer.theme.Theme;
import com.kr.musicplayer.theme.ThemeStore;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.StatusBarUtil;
import com.kr.musicplayer.util.Util;

import static com.kr.musicplayer.theme.ThemeStore.sColoredNavigation;
import static com.kr.musicplayer.util.Util.sendLocalBroadcast;

/**
 * 기초 Activity
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

  private static final int REQUEST_STORAGE = 47;
  protected Context mContext;
  protected boolean mIsDestroyed; // Activity 가 파괴되였는지 판별
  protected boolean mIsForeground; // Activity 가 Foreground 상태에 있는지 판별
  protected boolean mHasPermission; // Storage 접근권한을 가졌는지 판별
  public static final String[] EXTERNAL_STORAGE_PERMISSIONS = new String[]{
      Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

  /**
   * Theme 설정하는 함수
   */
  protected void setUpTheme() {
    setTheme(ThemeStore.getThemeRes());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    mContext = this;
    mHasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONS);

    setUpTheme();
    super.onCreate(savedInstanceState);
    //프로그람을 끝낼때 닫을 Activity 를 ActivityManager 에 추가
    ActivityManager.AddActivity(this);
    if(!Util.hasPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE})) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
    }
  }

  @Override
  public void setContentView(int layoutResID) {
    super.setContentView(layoutResID);
  }

  /**
   * NavigationBar 색상설정
   */
  protected void setNavigationBarColor() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sColoredNavigation) {
      final int navigationColor = ThemeStore.getNavigationBarColor();
      getWindow().setNavigationBarColor(navigationColor);
      Theme.setLightNavigationbarAuto(this, ColorUtil.isColorLight(navigationColor));
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    ActivityManager.RemoveActivity(this);
    mIsDestroyed = true;
  }

  /**
   * Activity 가 파괴되였는지 확인하는 함수
   * @return true 이면 Activity 가 파괴된것이고 false 이면 아직 파괴되지 않음
   */
  @Override
  public boolean isDestroyed() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? super.isDestroyed()
        : mIsDestroyed;
  }

  /**
   * 권한요청결과에 대한 callback
   * @param requestCode ActivityCompat 에 전달된 requestCode
   * @param permissions 요청된 권한들
   * @param grantResults 허용된 권한들
   */
  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    Toast toast = Toast.makeText(getApplicationContext(), "Storage permission is required", Toast.LENGTH_LONG);
    switch (requestCode) {
      case REQUEST_STORAGE: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          if(!mHasPermission) {
            Intent intent = new Intent(MusicService.PERMISSION_CHANGE);
            intent.putExtra(BaseMusicActivity.EXTRA_PERMISSION, true);
            sendLocalBroadcast(intent);
          }
        } else {
          if (! shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            toast.show();
            BaseActivity.this.finish();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 1);
          }
          else {
            toast.show();
            BaseActivity.this.finish();
          }
        }
      }
    }
  }

  @SuppressLint("CheckResult")
  @Override
  protected void onResume() {
    super.onResume();
    mIsForeground = true;
  }

  @Override
  protected void onPause() {
    super.onPause();
    mIsForeground = false;
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(LanguageHelper.setLocal(newBase));

  }

}
