package com.kr.musicplayer.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.View;
import android.widget.RelativeLayout;

import butterknife.BindView;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Artist;
import com.kr.musicplayer.helper.CloseEvent;
import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.ui.activity.ChildHolderActivity;
import com.kr.musicplayer.ui.adapter.ArtistAdapter;
import com.kr.musicplayer.ui.adapter.HeaderAdapter;
import com.kr.musicplayer.ui.viewmodel.BaseViewModel;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.SPUtil;

/**
 * 예술가화면
 */
public class ArtistFragment extends LibraryFragment<Artist, ArtistAdapter> {

  @BindView(R.id.recyclerView)
  FastScrollRecyclerView mRecyclerView;
  @BindView(R.id.sort_container)
  View toolbar;

  public static final String TAG = ArtistFragment.class.getSimpleName();

  private CloseEvent event;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof CloseEvent) event = (CloseEvent) activity;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = ViewModelProviders.of(this).get(BaseViewModel.class);
    viewModel.getArtists().observe(this, artists -> {
      mAdapter.submitList(artists);
    });
    viewModel.loadArtists();
    mPageName = TAG;
  }

  @Override
  protected int getLayoutID() {
    return R.layout.fragment_artist;
  }

  /**
   * Adapter 초기화
   */
  @Override
  protected void initAdapter() {
    mAdapter = new ArtistAdapter(R.layout.item_artist_recycle_grid, mChoice, mRecyclerView);
    getMultipleChoice().setEvent(event);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        final Artist artist = mAdapter.getItem(position);
        boolean flag = !mChoice.click(position, artist);
        if (getUserVisibleHint() && artist != null &&
            flag) {
          if (mAdapter.getItem(position) != null) {
            ChildHolderActivity
                .start(mContext, Constants.ARTIST, artist.getArtistID(), artist.getArtist());
          }
        }
        if (event != null && !flag) event.closeListener();
      }

      @Override
      public void onItemLongClick(View view, int position) {
        if (getUserVisibleHint()) {
          mChoice.longClick(position, mAdapter.getItem(position));
          mAdapter.notifyDataSetChanged();
          if (event != null) event.closeListener();
        }
      }
    });
  }

  /**
   * RecyclerView 초기화
   */
  @Override
  protected void initView() {
    int model = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MODE_FOR_ARTIST,
            HeaderAdapter.GRID_MODE);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setHasFixedSize(true);

    toolbar.setVisibility(View.GONE);
    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mRecyclerView.getLayoutParams();
    layoutParams.topMargin = 0;
    mRecyclerView.setLayoutParams(layoutParams);
  }

  /**
   * Adapter 를 얻는 함수
   * @return 얻어진 Adapter
   */
  @Override
  public ArtistAdapter getAdapter() {
    return mAdapter;
  }
}
