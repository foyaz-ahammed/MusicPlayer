package com.kr.musicplayer.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.kr.musicplayer.R;
import com.kr.musicplayer.ui.activity.base.BaseMusicActivity;

/**
 * Toolbar 를 포함하는 Activity
 */
@SuppressLint("Registered")
public class ToolbarActivity extends BaseMusicActivity {

  protected Toolbar toolbar;

  /**
   * Toolbar 를 생성하는 함수
   * @param title Toolbar 제목
   * @param iconRes Back 단추 아이콘
   */
  protected void setUpToolbar(String title, @DrawableRes int iconRes) {
    if (toolbar == null) {
      toolbar = findViewById(R.id.toolbar);
    }
    toolbar.setTitle(title);

    setSupportActionBar(toolbar);
    toolbar.setNavigationIcon(iconRes);
    toolbar.setNavigationOnClickListener(v -> onClickNavigation());
  }

  /**
   * Toolbar 를 생성하는 함수
   * @param title Toolbar 제목
   */
  protected void setUpToolbar(String title) {
    setUpToolbar(title, R.drawable.ic_arrow_back_white_24dp);
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    return super.onCreateOptionsMenu(menu);
  }

  public boolean onPrepareOptionsMenu(Menu menu) {
    return super.onPrepareOptionsMenu(menu);
  }

  public void setSupportActionBar(@Nullable Toolbar toolbar) {
    this.toolbar = toolbar;
    super.setSupportActionBar(toolbar);
  }

  /**
   * Toolbar 의 Navigation click 사건처리함수
   */
  protected void onClickNavigation() {
    InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
    //현재 focus 된 view 를 찾아서 옳바른 창의 token 을 가져올수 있다
    View view = getCurrentFocus();
    //현재 focus 된 view 가 없으면 새 view 를 창조하여 token 을 가져올수 있다
    if (view == null) {
      view = new View(this);
    }
    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    finish();
  }

}
