package com.kr.musicplayer.ui.activity;

import android.os.Bundle;

import java.util.List;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.ui.adapter.BaseAdapter;
import com.kr.musicplayer.util.Constants;

/**
 * 기본화면, 노래선택화면, 자식등록부화면, 검색화면들의 기초 Activity
 * @param <D> Song 혹은 Playlist Object
 * @param <A> Adapter
 */

public abstract class LibraryActivity<D, A extends BaseAdapter> extends MenuActivity {

  protected A mAdapter;
  protected MultipleChoice<D> mChoice = new MultipleChoice<>(this, Constants.SONG);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  protected abstract int getLoaderId();

  /**
   * MultipleChoice instance 를 얻는 함수
   * @return MultipleChoice instance
   */
  public MultipleChoice<D> getMultipleChoice() {
    return mChoice;
  }

  /**
   * MediaStore 가 변경되였을때 호출되는 callback
   */
  @Override
  public void onMediaStoreChanged() {
    super.onMediaStoreChanged();
    if (!mHasPermission) {
      if (mAdapter != null) {
        mAdapter.submitList(null);
      }
    }
  }

  /**
   * 저장공간 권한여부가 변경되였을때 호출되는 callback
   * @param has true 이면 권한설정됨, false 이면 권한설정안됨
   */
  @Override
  public void onPermissionChanged(boolean has) {
    if (has != mHasPermission) {
      mHasPermission = has;
      onMediaStoreChanged();
    }
  }

  @Override
  public void onPlayListChanged(String name) {

  }

  @Override
  public void onBackPressed() {
    if (mChoice.isActive()) {
      mChoice.close();
    } else {
      finish();
    }
  }
}
