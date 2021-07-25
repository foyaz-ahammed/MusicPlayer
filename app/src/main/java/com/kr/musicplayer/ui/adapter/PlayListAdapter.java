package com.kr.musicplayer.ui.adapter;

import static com.kr.musicplayer.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static com.kr.musicplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;

import com.kr.musicplayer.R;
import com.kr.musicplayer.db.room.model.PlayList;
import com.kr.musicplayer.request.ImageUriRequest;
import com.kr.musicplayer.request.PlayListUriRequest;
import com.kr.musicplayer.request.RequestConfig;
import com.kr.musicplayer.request.UriRequest;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.ui.adapter.holder.HeaderHolder;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import com.kr.musicplayer.util.ToastUtil;

/**
 * PlayList Adapter
 */
public class PlayListAdapter extends HeaderAdapter<PlayList, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  public PlayListAdapter(int layoutId, MultipleChoice multiChoice,
      RecyclerView recyclerView) {
    super(layoutId, multiChoice, recyclerView, DIFF_CALLBACK);
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_HEADER) {
      return new HeaderHolder(
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.layout_header_2, parent, false));
    }
    return viewType == HeaderAdapter.LIST_MODE ?
        new PlayListListHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist_recycle_list, parent, false)) :
        new PlayListGridHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist_recycle_grid, parent, false));
  }

  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @SuppressLint("RestrictedApi")
  @Override
  protected void convert(BaseViewHolder baseHolder, final PlayList info, int position) {
    if (position == 0) {
      final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
      setUpModeButton(headerHolder);
      return;
    }

    if (!(baseHolder instanceof PlayListHolder)) {
      return;
    }
    final PlayListHolder holder = (PlayListHolder) baseHolder;
    if (info == null) {
      return;
    }

    final Context context = baseHolder.itemView.getContext();
    holder.mName.setText(info.getName());
    holder.mOther.setText(context.getString(R.string.song_count, info.getAudioIds().size()));

    final int imageSize = mMode == LIST_MODE ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;

    new PlayListUriRequest(holder.mImage,
        new UriRequest(info.getId(), ImageUriRequest.URL_PLAYLIST, UriRequest.TYPE_NETEASE_SONG),
        new RequestConfig.Builder(imageSize, imageSize).build()) {
    }.load();

    holder.mContainer.setOnClickListener(v -> {
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return;
      }
      mOnItemClickListener.onItemClick(holder.mContainer, position - 1);
    });

    holder.mContainer.setOnLongClickListener(v -> {
      if (position - 1 < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return true;
      }
      mOnItemClickListener.onItemLongClick(holder.mContainer, position - 1);
      return true;
    });

    if (MultipleChoice.isActiveSomeWhere()) {
      holder.mCheckBox.setVisibility(View.VISIBLE);
    } else {
      holder.mCheckBox.setVisibility(View.GONE);
    }

    //선택표시
    if (mMode == HeaderAdapter.GRID_MODE)
      holder.mContainer.setSelected(mChoice.isPositionCheck(position - 1));
    holder.mCheckBox.setChecked(mChoice.isPositionCheck(position - 1));

    setMarginForGridLayout(holder, position);
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (getCurrentList() != null && position - 1 < getItemCount()) {
      String title = getItem(position - 1).getName();
      return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  /**
   * 개별적인 재생목록항목을 위한 viewHolder
   */
  static class PlayListHolder extends BaseViewHolder {

    @BindView(R.id.item_text1)
    TextView mName;
    @BindView(R.id.item_text2)
    TextView mOther;
    @BindView(R.id.item_simpleiview)
    SimpleDraweeView mImage;
    @BindView(R.id.item_button)
    ImageView mButton;
    @BindView(R.id.item_container)
    ViewGroup mContainer;
    @BindView(R.id.item_checkbox)
    CheckBox mCheckBox;

    PlayListHolder(View itemView) {
      super(itemView);
    }
  }

  static class PlayListListHolder extends PlayListHolder {

    PlayListListHolder(View itemView) {
      super(itemView);
    }
  }

  static class PlayListGridHolder extends PlayListHolder {

    PlayListGridHolder(View itemView) {
      super(itemView);
    }
  }

  /**
   * 자료가 갱신되였을때 개별적인 항목들의 이전자료와 현재자료를 비교하는 diffCallback
   */
  public static final DiffUtil.ItemCallback<PlayList> DIFF_CALLBACK =
          new DiffUtil.ItemCallback<PlayList>() {
            @Override
            public boolean areItemsTheSame(@NonNull PlayList oldItem, @NonNull PlayList newItem) {
              return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull PlayList oldItem, @NonNull PlayList newItem) {
              return oldItem.getName().equals(newItem.getName());
            }
          };
}
