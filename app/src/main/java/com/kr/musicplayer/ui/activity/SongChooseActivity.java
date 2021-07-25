package com.kr.musicplayer.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.db.room.DatabaseRepository;
import com.kr.musicplayer.misc.interfaces.LoaderIds;
import com.kr.musicplayer.request.network.RxUtil;
import com.kr.musicplayer.ui.adapter.SongChooseAdapter;
import com.kr.musicplayer.ui.viewmodel.BaseViewModel;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.ToastUtil;

/**
 * 노래선택화면
 */

public class SongChooseActivity extends LibraryActivity<Song, SongChooseAdapter> {

  public static final String TAG = SongChooseActivity.class.getSimpleName();
  public static final String EXTRA_NAME = "PlayListName";
  public static final String EXTRA_ID = "PlayListID";
  public static final String EXTRA_SONG_IDS = "PlayListSongIds";

  private int mPlayListID;
  private String mPlayListName;
  private int[] mPlayListSongIds;
  @BindView(R.id.confirm)
  LinearLayout mConfirm;
  @BindView(R.id.cancel)
  LinearLayout mCancel;
  @BindView(R.id.selected)
  TextView mSelected;
  @BindView(R.id.toggle_select)
  ImageView mToggle;
  @BindView(R.id.recyclerview)
  RecyclerView mRecyclerView;
  @BindView(R.id.search)
  EditText mSearch;
  @BindView(R.id.select_all)
  TextView mSelectAll;

  protected BaseViewModel viewModel;

  public static void start(Activity activity, int playListId, String playListName) {
    Intent intent = new Intent(activity, SongChooseActivity.class);
    intent.putExtra(EXTRA_ID, playListId);
    intent.putExtra(EXTRA_NAME, playListName);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_song_choose);
    ButterKnife.bind(this);
    mPlayListID = getIntent().getIntExtra(EXTRA_ID, -1);
    mPlayListSongIds = getIntent().getIntArrayExtra(EXTRA_SONG_IDS);

    viewModel = ViewModelProviders.of(this).get(BaseViewModel.class);
    viewModel.getSongs().observe(this, songs -> {
      mAdapter.submitList(songs);
    });

    viewModel.loadSongs(Constants.SONG, -1, true, mPlayListID);

    if (mPlayListID <= 0) {
      ToastUtil.show(this, R.string.add_error, Toast.LENGTH_SHORT);
      return;
    }
    mPlayListName = getIntent().getStringExtra(EXTRA_NAME);

    mAdapter = new SongChooseAdapter(R.layout.item_song_choose, isValid -> {
      mConfirm.setAlpha(isValid ? 1.0f : 0.4f);
      mConfirm.setClickable(isValid);
      if (mAdapter.getCheckedSong().size() == mAdapter.getItemCount()) {
        mSelectAll.setText(R.string.select_none);
      } else {
        mSelectAll.setText(R.string.select_all);
      }
      mSelected.setText(getString(R.string.multi_top_text, mAdapter.getCheckedSong().size()));
    });

    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mConfirm.setAlpha(0.4f);
    mSelected.setText(getString(R.string.multi_top_text, mAdapter.getCheckedSong().size()));

    mSearch.setOnTouchListener((v, event) -> {
      if (event.getAction() == MotionEvent.ACTION_DOWN)
        // FLAG_ACTIVITY_REORDER_TO_FRONT for prevent multiple activity start with several click
        startActivityForResult(new Intent(getApplicationContext(), SearchActivity.class).putExtra("search", true).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT), 1);
      return true;
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    MaterialDialogHelper.setBackground(this, R.id.container_background);
  }

  @OnClick({R.id.confirm, R.id.cancel, R.id.select_all, R.id.toggle_select})
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.cancel:
        finish();
        break;
      case R.id.confirm:
        if (mAdapter.getCheckedSong() == null || mAdapter.getCheckedSong().size() == 0) {
          ToastUtil.show(this, R.string.choose_no_song);
          return;
        }
        DatabaseRepository.getInstance()
            .insertToPlayList(mAdapter.getCheckedSong(), mPlayListID)
            .compose(RxUtil.applySingleScheduler())
            .subscribe(num -> {
              ToastUtil.show(mContext, getString(R.string.add_song_playlist_success, num, mPlayListName));
              finish();
            }, throwable -> finish());
        break;
      case R.id.select_all:
      case R.id.toggle_select:
        if (mAdapter.getCheckedSong().size() == mAdapter.getItemCount()) {
          mSelectAll.setText(R.string.select_all);
          selectNone();
        } else {
          mSelectAll.setText(R.string.select_none);
          selectAll();
        }
        break;
    }
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.SONGCHOOSE_ACTIVITY;
  }

  @Override
  public void saveSortOrder(@Nullable String sortOrder) {

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK && requestCode == 1) {
      if (data != null) {
        ArrayList<Integer> datas = mAdapter.getCheckedSong();
        mAdapter.getCheckedSong().addAll(data.getParcelableArrayListExtra("selected"));
        mSelected.setText(getString(R.string.multi_top_text, mAdapter.getCheckedSong().size()));
        if (datas.size() > 0) {
          mConfirm.setAlpha(1.0f);
          mConfirm.setClickable(true);
        } else {
          mConfirm.setAlpha(0.4f);
          mConfirm.setClickable(false);
        }
        mAdapter.notifyDataSetChanged();
      }
    }
  }

  /**
   * 모두 선택
   */
  private void selectAll() {
    mAdapter.getCheckedSong().clear();
    mAdapter.getCheckedSong().addAll(mAdapter.getCurrentList().stream().map(Song::getId).collect(Collectors.toList()));
    mSelected.setText(getString(R.string.multi_top_text, mAdapter.getCheckedSong().size()));
    mAdapter.notifyDataSetChanged();
    mConfirm.setAlpha(1.0f);
    mConfirm.setClickable(true);
  }

  /**
   * 모두 선택 취소
   */
  private void selectNone() {
    mAdapter.getCheckedSong().clear();
    mSelected.setText(getString(R.string.multi_top_text, mAdapter.getCheckedSong().size()));
    mAdapter.notifyDataSetChanged();
    mConfirm.setAlpha(0.4f);
    mConfirm.setClickable(false);
  }

}
