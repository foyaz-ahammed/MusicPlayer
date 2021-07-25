package com.kr.musicplayer.ui.fragment;

import static com.kr.musicplayer.misc.ExtKt.isPortraitOrientation;
import static com.kr.musicplayer.util.ColorUtil.getColor;
import static com.kr.musicplayer.util.ColorUtil.isColorLight;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import java.util.List;
import com.kr.musicplayer.R;
import com.kr.musicplayer.helper.MusicEventCallback;
import com.kr.musicplayer.theme.ThemeStore;
import com.kr.musicplayer.ui.adapter.BaseAdapter;
import com.kr.musicplayer.ui.fragment.base.BaseMusicFragment;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.ui.viewmodel.BaseViewModel;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.DensityUtil;

/**
 * 화면들의 기초 Fragment
 */
public abstract class LibraryFragment<D, A extends BaseAdapter> extends BaseMusicFragment implements
    MusicEventCallback {

  protected A mAdapter;
  protected MultipleChoice<D> mChoice;
  protected BaseViewModel viewModel;

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
  }

  public MultipleChoice getChoice() {
    return mChoice;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View rootView = inflater.inflate(getLayoutID(), container, false);
    mUnBinder = ButterKnife.bind(this, rootView);

    int type = this instanceof SongFragment ? Constants.SONG :
            this instanceof ArtistFragment ? Constants.ARTIST :
              this instanceof FavouriteFragment ? Constants.PLAYLISTSONG:
                Constants.FOLDER;
    mChoice = new MultipleChoice<>(requireActivity(), type);
    initAdapter();
    initView();

    //RecyclerView 의 Scrollbar
    final int accentColor = ThemeStore.getAccentColor();
    final RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
    if (recyclerView instanceof FastScrollRecyclerView) {
      ((FastScrollRecyclerView) recyclerView).setBubbleColor(accentColor);
      ((FastScrollRecyclerView) recyclerView).setHandleColor(accentColor);
      ((FastScrollRecyclerView) recyclerView)
          .setBubbleTextColor(getColor(isColorLight(accentColor) ?
              R.color.light_text_color_primary : R.color.dark_text_color_primary));
    }

    mChoice.setAdapter(mAdapter);
    return rootView;
  }


  protected abstract int getLayoutID();

  protected abstract void initAdapter();

  protected abstract void initView();

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mAdapter != null) {
      mAdapter.submitList(null);
    }
  }

  /**
   * MediaStore 가 변경되였을때 호출되는 callback
   */
  @Override
  public void onMediaStoreChanged() {
    Log.d("--------------onMediaStoreChanged", "LibraryFragment");
    if (mAdapter != null && !mHasPermission) {
      mAdapter.submitList(null);
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

  /**
   * Adapter 얻기
   * @return 얻어진 Adapter
   */
  @Override
  public RecyclerView.Adapter getAdapter() {
    return mAdapter;
  }

  /**
   * MultipleChoice 얻기
   * @return 얻어진 MultipleChoice
   */
  public MultipleChoice<D> getMultipleChoice() {
    return mChoice;
  }

  /**
   * Adapter 의 자료얻기
   * @return 얻어진 자료목록
   */
  public List getDatas() {
    return mAdapter.getCurrentList();
  }

  private static final int PORTRAIT_ORIENTATION_COUNT = 2;
  private static final int LANDSCAPE_ORIENTATION_ITEM_WIDTH = DensityUtil.dip2px(180);
  private static final int PORTRAIT_ORIENTATION_MAX_ITEM_COUNT = 6;

  /**
   * Grid 에서 Column 개수 얻기
   * @return 얻어진 Column 개수
   */
  protected int getSpanCount() {
    final boolean portraitOrientation = isPortraitOrientation(requireContext());
    if (portraitOrientation) {
      return PORTRAIT_ORIENTATION_COUNT;
    } else {
      int count = getResources().getDisplayMetrics().widthPixels / LANDSCAPE_ORIENTATION_ITEM_WIDTH;
      return count > PORTRAIT_ORIENTATION_MAX_ITEM_COUNT ? PORTRAIT_ORIENTATION_MAX_ITEM_COUNT : count;
    }
  }
}
