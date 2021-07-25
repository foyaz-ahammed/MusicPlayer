package com.kr.musicplayer.ui.adapter;

import static com.kr.musicplayer.misc.ExtKt.isPortraitOrientation;

import android.content.Context;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.drawee.view.SimpleDraweeView;
import io.reactivex.disposables.Disposable;
import com.kr.musicplayer.R;
import com.kr.musicplayer.request.LibraryUriRequest;
import com.kr.musicplayer.request.RequestConfig;
import com.kr.musicplayer.request.UriRequest;
import com.kr.musicplayer.theme.Theme;
import com.kr.musicplayer.theme.ThemeStore;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.ui.adapter.holder.HeaderHolder;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.DensityUtil;
import com.kr.musicplayer.util.SPUtil;

/**
 * Header 부분을 포함하는 기초 Adapter
 */

public abstract class HeaderAdapter<M, B extends RecyclerView.ViewHolder> extends
    BaseAdapter<M, BaseViewHolder> {

  //현시방식 1: 목록 2: 격자
  public final static int LIST_MODE = 1;
  public final static int GRID_MODE = 2;

  //격자방식에서 수평 및 수직간격
  private static final int GRID_MARGIN_VERTICAL = DensityUtil.dip2px(4);
  private static final int GRID_MARGIN_HORIZONTAL = DensityUtil.dip2px(6);

  static final int TYPE_HEADER = 0;
  static final int TYPE_NORMAL = 1;

  protected MultipleChoice mChoice;
  protected RecyclerView mRecyclerView;

  //현재 목록방식 1: 목록 2: 격자
  int mMode = GRID_MODE;

  HeaderAdapter(int layoutId, MultipleChoice multiChoice,
                RecyclerView recyclerView, DiffUtil.ItemCallback<M> diff_callback) {
    super(layoutId, diff_callback);
    this.mChoice = multiChoice;
    this.mRecyclerView = recyclerView;
    String key =
        this instanceof ArtistAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ARTIST :
            this instanceof PlayListAdapter ? SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST :
                null;
    this.mMode =
        key != null ? SPUtil
            .getValue(recyclerView.getContext(), SPUtil.SETTING_KEY.NAME, key, GRID_MODE)
            : LIST_MODE;
  }

  @Override
  public int getItemViewType(int position) {
    return mMode;
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
    if (manager instanceof GridLayoutManager) {
      final GridLayoutManager gridManager = ((GridLayoutManager) manager);
      gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
          return getItemViewType(position) == TYPE_HEADER ? gridManager.getSpanCount() : 1;
        }
      });
    }
  }


  /**
   * 목록방식전환 초기화 단추
   */
  void setUpModeButton(HeaderHolder headerHolder) {
    if (getCurrentList() == null || getItemCount() == 0) {
      headerHolder.mRoot.setVisibility(View.GONE);
      return;
    }

    headerHolder.mDivider
        .setVisibility(mMode == HeaderAdapter.LIST_MODE ? View.VISIBLE : View.GONE);
    headerHolder.mGridModeBtn.setOnClickListener(v -> switchMode(headerHolder, v));
    headerHolder.mListModeBtn.setOnClickListener(v -> switchMode(headerHolder, v));
    headerHolder.mDivider.setVisibility(mMode == LIST_MODE ? View.VISIBLE : View.GONE);
    tintModeButton(headerHolder);
  }

  /**
   * 현시방식 변경
   */
  private void switchMode(HeaderHolder headerHolder, View v) {
    int newModel = v.getId() == R.id.list_model ? LIST_MODE : GRID_MODE;
    if (newModel == mMode) {
      return;
    }
    mMode = newModel;
    setUpModeButton(headerHolder);
    //LayoutManager 와 Adapter 재설정 및 목록 새로 고침
    mRecyclerView.setLayoutManager(
        mMode == LIST_MODE ? new LinearLayoutManager(headerHolder.itemView.getContext())
            : new GridLayoutManager(headerHolder.itemView.getContext(), 2));
    mRecyclerView.setAdapter(this);
    //현재방식저장
    saveMode(headerHolder.itemView.getContext());
  }

  /**
   * 현시방식단추설정
   * @param headerHolder 현시방식단추를 설정할 viewHolder
   */
  private void tintModeButton(HeaderHolder headerHolder) {
    headerHolder.mListModeBtn.setImageDrawable(
        Theme.tintVectorDrawable(headerHolder.itemView.getContext(),
            R.drawable.ic_format_list_bulleted_white_24dp,
            mMode == LIST_MODE ? ThemeStore.getAccentColor()
                : ColorUtil.getColor(R.color.default_model_button_color))
    );

    headerHolder.mGridModeBtn.setImageDrawable(
        Theme.tintVectorDrawable(headerHolder.itemView.getContext(), R.drawable.ic_apps_white_24dp,
            mMode == GRID_MODE ? ThemeStore.getAccentColor()
                : ColorUtil.getColor(R.color.default_model_button_color))
    );

  }

  private void saveMode(Context context) {
    String key =
        this instanceof ArtistAdapter ? SPUtil.SETTING_KEY.MODE_FOR_ARTIST :
            SPUtil.SETTING_KEY.MODE_FOR_PLAYLIST;
    SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, key, mMode);
  }

  void setMarginForGridLayout(BaseViewHolder holder, int position) {
    //Margin 을 리용하여 Divider 로 설정
    if (mMode == GRID_MODE && holder.mRoot != null) {
      ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.mRoot
          .getLayoutParams();
      if (isPortraitOrientation(holder.itemView.getContext())) { //수직화면
        if (position % 2 == 1) {
          lp.setMargins(GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL);
        } else {
          lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL,
              GRID_MARGIN_HORIZONTAL, GRID_MARGIN_VERTICAL);
        }
      } else { //수평화면
        lp.setMargins(GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2,
            GRID_MARGIN_HORIZONTAL / 2, GRID_MARGIN_VERTICAL / 2);
      }

      holder.mRoot.setLayoutParams(lp);
    }
  }

  Disposable setImage(final SimpleDraweeView simpleDraweeView,
      final UriRequest uriRequest,
      final int imageSize,
      final int position) {
    return new LibraryUriRequest(simpleDraweeView,
        uriRequest,
        new RequestConfig.Builder(imageSize, imageSize).build()) {
    }.load();
  }

  void disposeLoad(final ViewHolder holder) {
    final ViewGroup parent =
        holder.itemView instanceof ViewGroup ? (ViewGroup) holder.itemView : null;
    if (parent != null) {
      for (int i = 0; i < parent.getChildCount(); i++) {
        final View childView = parent.getChildAt(i);
        if (childView instanceof SimpleDraweeView) {
          final Object tag = childView.getTag();
          if (tag instanceof Disposable) {
            final Disposable disposable = (Disposable) tag;
            if (!disposable.isDisposed()) {
              disposable.dispose();
            }
            childView.setTag(null);
          }
        }
      }
    }
  }

}
