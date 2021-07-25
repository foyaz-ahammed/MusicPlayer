package com.kr.musicplayer.ui.activity;

import static com.kr.musicplayer.service.MusicService.EXTRA_SONG;
import static com.kr.musicplayer.util.MusicUtil.makeCmdIntent;
import static com.kr.musicplayer.util.Util.sendLocalBroadcast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;

import java.util.ArrayList;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.misc.interfaces.LoaderIds;
import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.ui.adapter.SearchAdapter;
import com.kr.musicplayer.ui.viewmodel.BaseViewModel;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.ToastUtil;

/**
 * 검색화면
 */
public class SearchActivity extends LibraryActivity<Song, SearchAdapter> {

  protected BaseViewModel viewModel;
  //검색할 keyword
  private String mkey;
  //검색결과 RecyclerView
  @BindView(R.id.search_result_native)
  RecyclerView mSearchResRecyclerView;
  //검색결과없음 상태표시 view
  @BindView(R.id.search_result_blank)
  TextView mSearchResBlank;
  @BindView(R.id.search_result_container)
  FrameLayout mSearchResContainer;
  @BindView(R.id.confirm)
  ImageView mConfirm;

  private ArrayList<Integer> mCheckSongIdList = new ArrayList<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = ViewModelProviders.of(this).get(BaseViewModel.class);
    viewModel.getSongs().observe(this, songs -> {
      mAdapter.submitList(songs);
      updateUI(songs.size() > 0);
      mSearchResRecyclerView.smoothScrollToPosition(0);
    });
    setContentView(R.layout.activity_search);
    ButterKnife.bind(this);
    setUpToolbar("");

    boolean isSearch = getIntent().getBooleanExtra("search", false);
    if (isSearch) mConfirm.setVisibility(View.VISIBLE);
    mAdapter = new SearchAdapter(R.layout.item_search_reulst, isSearch);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        if (mAdapter != null && mAdapter.getCurrentList() != null && position >= 0 && position < mAdapter.getItemCount()) {
          if (!isSearch) sendLocalBroadcast(makeCmdIntent(Command.PLAY_TEMP)
              .putExtra(EXTRA_SONG, mAdapter.getItem(position)));
          else {
            int id = mAdapter.getItem(position).getId();
            if (mCheckSongIdList.contains(id)) {
              mCheckSongIdList.remove((Object) id);
            } else {
              mCheckSongIdList.add(id);
            }
          }
        } else {
          ToastUtil.show(mContext, R.string.illegal_arg);
        }
        if (!isSearch) finish();
      }

      @Override
      public void onItemLongClick(View view, int position) {
      }
    });
    mSearchResRecyclerView.setAdapter(mAdapter);
    mSearchResRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mSearchResRecyclerView.setItemAnimator(new DefaultItemAnimator());

    EditText search = findViewById(R.id.searcher);
    search.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        search(s.toString());
      }

      @Override
      public void afterTextChanged(Editable s) {

      }
    });

    mConfirm.setOnClickListener(v -> {
      Intent intent = new Intent();
      intent.putIntegerArrayListExtra("selected", mCheckSongIdList);
      setResult(Activity.RESULT_OK, intent);
      finish();
    });

    updateUI(false);

    search.setFocusable(true);
    search.requestFocus();
    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
  }

  @Override
  protected void onResume() {
    super.onResume();
    MaterialDialogHelper.setBackground(this, R.id.container_background);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    return false;
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.SEARCH_ACTIVITY;
  }


  /**
   * 검색
   * @param key 검색할 keyword
   */
  private void search(String key) {
    mkey = key;
    MaterialDialogHelper.searchKey = key;
    viewModel.searchSongs(mkey);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    MaterialDialogHelper.searchKey = "";
  }

  /**
   * UI 갱신
   */
  private void updateUI(boolean hasSearchResult) {
    mSearchResRecyclerView.setVisibility(hasSearchResult ? View.VISIBLE : View.GONE);
    mSearchResBlank.setVisibility(hasSearchResult ? View.GONE : View.VISIBLE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }
}
