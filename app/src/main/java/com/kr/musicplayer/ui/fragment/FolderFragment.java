package com.kr.musicplayer.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import butterknife.BindView;

import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Folder;
import com.kr.musicplayer.helper.CloseEvent;
import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.ui.activity.ChildHolderActivity;
import com.kr.musicplayer.ui.adapter.FolderAdapter;
import com.kr.musicplayer.ui.viewmodel.BaseViewModel;
import com.kr.musicplayer.util.Constants;

/**
 * 등록부보기화면
 */
public class FolderFragment extends LibraryFragment<Folder, FolderAdapter> {

  @BindView(R.id.recyclerView)
  RecyclerView mRecyclerView;

  public static final String TAG = FolderFragment.class.getSimpleName();

  private CloseEvent event;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    if (activity instanceof CloseEvent) event = (CloseEvent) activity;
  }

  @Override
  protected int getLayoutID() {
    return R.layout.fragment_folder;
  }

  /**
   * Adapter 초기화
   */
  @Override
  protected void initAdapter() {
    mAdapter = new FolderAdapter(R.layout.item_folder_recycle, mChoice);
    getMultipleChoice().setEvent(event);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        Folder folder = mAdapter.getItem(position);
        String path = folder.getPath();
        if (getUserVisibleHint() && !TextUtils.isEmpty(path) &&
            !mChoice.click(position, folder)) {
          ChildHolderActivity.start(mContext, Constants.FOLDER, folder.getParentId(), path);
        }
        if (event != null) event.closeListener();
      }

      @Override
      public void onItemLongClick(View view, int position) {
        Folder folder = mAdapter.getItem(position);
        String path = mAdapter.getItem(position).getPath();
        if (getUserVisibleHint() && !TextUtils.isEmpty(path)) {
          mChoice.longClick(position, folder);
        }
        mAdapter.notifyDataSetChanged();
        if (event != null) event.closeListener();
      }
    });
  }

  /**
   * RecyclerView 초기화
   */
  @Override
  protected void initView() {
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    mRecyclerView.setHasFixedSize(true);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = ViewModelProviders.of(this).get(BaseViewModel.class);
    viewModel.getFolders().observe(this, folders -> {
      mAdapter.submitList(folders);
      Log.d("--------------------", "FOLDERS" + mAdapter.getItemCount());
    });
    viewModel.loadFolders();
    mPageName = TAG;
  }

  @Override
  public FolderAdapter getAdapter() {
    return mAdapter;
  }
}
