package com.kr.musicplayer.ui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kr.musicplayer.R;
import com.kr.musicplayer.misc.MediaScanner;

/**
 * 노래검색정형 현시를 위한 대화창
 */
public class LoadingDialog extends Dialog {
  ProgressBar mProgressbar;
  TextView mTextView;
  TextView mCount;

  MediaScanner mediaScanner;

  private LoadingPauseCallback loadingPauseCallback;

  public LoadingDialog(@NonNull Context context, MediaScanner mediaScanner) {
    super(context);
    this.mediaScanner = mediaScanner;
  }

  @SuppressLint("CheckResult")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.dialog_loading);
    setCanceledOnTouchOutside(false);

    mProgressbar = findViewById(R.id.progressBar);
    mTextView = findViewById(R.id.percent);
    mCount = findViewById(R.id.count_songs);
  }

  /**
   * 뒤로가기 단추를 눌렀을때 현재 검색중인 상태이면 중지
   */
  @Override
  public void onBackPressed() {
    if (mediaScanner != null && mediaScanner.isStarted()) {
      mediaScanner.setPause(!mediaScanner.isPause());
      loadingPauseCallback.onLoadingPause();
    } else {
      super.onBackPressed();
    }
  }

  public interface LoadingPauseCallback {
    void onLoadingPause();
  }

  /**
   * 검색진행률 현시
   * @param percent 진행률
   */
  public void setPercent(int percent) {
    mTextView.setText(percent + "%");
  }

  /**
   * 검색진행률 현시
   * @param progress 진행률
   */
  public void setProgress(int progress) {
    mProgressbar.setProgress(progress);
  }

  /**
   * 검색개수 현시
   * @param count 검색한 개수
   */
  public void setCount(int count) {
    mCount.setText(getContext().getString(R.string.song_count_found, count));
  }

  /**
   * 검색중지를 위한 callback 설정
   * @param loadingPauseCallback 설정할 callback
   */
  public void setLoadingPauseCallback(LoadingPauseCallback loadingPauseCallback) {
    this.loadingPauseCallback = loadingPauseCallback;
  }
}
