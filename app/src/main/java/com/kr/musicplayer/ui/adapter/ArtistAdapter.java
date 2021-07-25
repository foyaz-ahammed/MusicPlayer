package com.kr.musicplayer.ui.adapter;

import static com.kr.musicplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Artist;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import com.kr.musicplayer.util.ImageUriUtil;
import com.kr.musicplayer.util.ToastUtil;

/**
 * Artist Adapter
 */
public class ArtistAdapter extends HeaderAdapter<Artist, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  public ArtistAdapter(int layoutId, MultipleChoice multiChoice,
      FastScrollRecyclerView recyclerView) {
    super(layoutId, multiChoice, recyclerView, DIFF_CALLBACK);
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ArtistAdapter.ArtistListHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist_recycle_list, parent, false));
  }

  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @SuppressLint({"RestrictedApi", "CheckResult"})
  @Override
  protected void convert(final BaseViewHolder baseHolder, final Artist artist, final int position) {

    if (!(baseHolder instanceof ArtistHolder)) {
      return;
    }

    final Context context = baseHolder.itemView.getContext();
    final ArtistHolder holder = (ArtistHolder) baseHolder;
    //가수이름 설정
    holder.mText1.setText(artist.getArtist());
    final int artistId = artist.getArtistID();
    if (holder instanceof ArtistListHolder && holder.mText2 != null) {
      if (artist.getCount() > 0) {
        holder.mText2.setText(context.getString(R.string.song_count_1, artist.getCount()));
      } else {
        holder.mText2.setText(App.getContext().getString(R.string.song_count_1, artist.getCount()));
      }
    }
    //덮개 설정
    final int imageSize = SMALL_IMAGE_SIZE; /*mMode == LIST_MODE ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;*/
    holder.mImage.setTag(setImage(holder.mImage, ImageUriUtil.getSearchRequest(artist), imageSize, position));

    holder.mContainer.setOnClickListener(v -> {
      if (holder.getAdapterPosition() < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return;
      }
      mOnItemClickListener.onItemClick(holder.mContainer, position);
    });
    //다중선택 메뉴
    holder.mContainer.setOnLongClickListener(v -> {
      if (position < 0) {
        ToastUtil.show(context, R.string.illegal_arg);
        return true;
      }
      mOnItemClickListener.onItemLongClick(holder.mContainer, position);
      return true;
    });

    if (MultipleChoice.isActiveSomeWhere()) {
      holder.mCheckBox.setVisibility(View.VISIBLE);
    } else {
      holder.mCheckBox.setVisibility(View.GONE);
    }

    //선택표시
    if (mMode == HeaderAdapter.GRID_MODE)
      holder.mContainer.setSelected(mChoice.isPositionCheck(position));
    holder.mCheckBox.setChecked(mChoice.isPositionCheck(position));

    //padding
    setMarginForGridLayout(holder, position);
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (getCurrentList() != null && position - 1 < getItemCount()) {
      String artist = getItem(position - 1).getArtist();
      return !TextUtils.isEmpty(artist) ? (Pinyin.toPinyin(artist.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  /**
   * 예술가 항목을 위한 viewHolder
   */
  static class ArtistHolder extends BaseViewHolder {

    @BindView(R.id.item_text1)
    TextView mText1;
    @BindView(R.id.item_text2)
    @Nullable
    TextView mText2;
    @BindView(R.id.item_simpleiview)
    SimpleDraweeView mImage;
    @BindView(R.id.item_button)
    ImageButton mButton;
    @BindView(R.id.item_container)
    ViewGroup mContainer;
    @BindView(R.id.item_checkbox)
    CheckBox mCheckBox;

    ArtistHolder(View v) {
      super(v);
    }
  }

  static class ArtistListHolder extends ArtistHolder {

    ArtistListHolder(View v) {
      super(v);
    }
  }

  /**
   * 자료가 갱신되였을때 개별적인 항목들의 이전자료와 현재자료를 비교하는 diffCallback
   */
  public static final DiffUtil.ItemCallback<Artist> DIFF_CALLBACK =
          new DiffUtil.ItemCallback<Artist>() {
            @Override
            public boolean areItemsTheSame(@NonNull Artist oldItem, @NonNull Artist newItem) {
              return oldItem.getArtistID() == newItem.getArtistID();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Artist oldItem, @NonNull Artist newItem) {
              return oldItem.equals(newItem);
            }
          };
}
