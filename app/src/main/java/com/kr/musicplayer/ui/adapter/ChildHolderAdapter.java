package com.kr.musicplayer.ui.adapter;

import static com.kr.musicplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static com.kr.musicplayer.theme.ThemeStore.getTextColorPrimary;
import static com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

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
import android.widget.ImageButton;
import android.widget.TextView;
import butterknife.BindView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import com.kr.musicplayer.R;
import com.kr.musicplayer.appwidgets.MusicVisualizer;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.ToastUtil;

/**
 * 자식등록부 adapter
 */
@SuppressLint("RestrictedApi")
public class ChildHolderAdapter extends HeaderAdapter<Song, BaseViewHolder> implements
    FastScroller.SectionIndexer {

  private int mType; // 현재 화면이 Folder, Artist 혹은 PlayList 인지 확인할수 있는 형태
  private String mArg;

  private Song mLastPlaySong = MusicServiceRemote.getCurrentSong(); // 마지막으로 재생된 노래

  public ChildHolderAdapter(int layoutId, int type, String arg,
      MultipleChoice multiChoice, RecyclerView recyclerView) {
    super(layoutId, multiChoice, recyclerView, DIFF_CALLBACK);
    this.mType = type;
    this.mArg = arg;
  }

  @NonNull
  @Override
  public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ChildHolderViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_recycle, parent, false));
  }

  @Override
  public void onViewRecycled(@NonNull BaseViewHolder holder) {
    super.onViewRecycled(holder);
    disposeLoad(holder);
  }

  @Override
  protected void convert(final BaseViewHolder baseHolder, final Song song, int position) {
    final Context context = baseHolder.itemView.getContext();
    if (song == null || getItemCount() == 0) {
      return;
    }

    final ChildHolderViewHolder holder = (ChildHolderViewHolder) baseHolder;
    if (song == null || song.getId() < 0 || song.getDisplayName()
        .equals(context.getString(R.string.song_lose_effect))) {
      holder.mName.setText(R.string.song_lose_effect);
      holder.mButton.setVisibility(View.INVISIBLE);
    } else {
      holder.mButton.setVisibility(View.VISIBLE);

      //덮개
      holder.mImage.setTag(
          setImage(holder.mImage, getSearchRequestWithAlbumType(song), SMALL_IMAGE_SIZE, position));

      //가장 밝은 부분
      if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
        mLastPlaySong = song;
        holder.mIndicator.setVisibility(View.VISIBLE);
        holder.mVisualizer.setVisibility(View.VISIBLE);
      } else {
        holder.mIndicator.setVisibility(View.GONE);
        holder.mVisualizer.setVisibility(View.GONE);
      }
      holder.mIndicator.setBackgroundColor(ColorUtil.getColor(R.color.md_red_primary));

      //제목 설정
      holder.mName.setText(song.getDisplayName());

      //Artist & Album
      holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
    }

    if (holder.mContainer != null && mOnItemClickListener != null) {
      holder.mContainer.setOnClickListener(v -> {
        if (holder.getAdapterPosition() < 0) {
          ToastUtil.show(context, R.string.illegal_arg);
          return;
        }
        if (song != null && song.getId() > 0) {
          mOnItemClickListener.onItemClick(v, holder.getAdapterPosition());
          SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE, mChoice.getExtra());
          SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE_TYPE, mType);
        }
      });
      holder.mContainer.setOnLongClickListener(v -> {
        if (holder.getAdapterPosition() < 0) {
          ToastUtil.show(context, R.string.illegal_arg);
          return true;
        }
        mOnItemClickListener.onItemLongClick(v, holder.getAdapterPosition());
        return true;
      });
    }

    holder.mDuration.setText(MaterialDialogHelper.formatSeconds(song.getDuration() / 1000));

    if (MultipleChoice.isActiveSomeWhere()) {
      holder.mCheckBox.setVisibility(View.VISIBLE);
      holder.mButton.setVisibility(View.GONE);
      holder.mVisualizer.setVisibility(View.INVISIBLE);
      holder.mImage.setVisibility(View.GONE);
      holder.mIndicator.setVisibility(View.INVISIBLE);
    } else {
      holder.mCheckBox.setVisibility(View.GONE);
      holder.mButton.setVisibility(View.VISIBLE);
      holder.mImage.setVisibility(View.VISIBLE);
    }

    holder.mCheckBox.setChecked(mChoice.isPositionCheck(position));
  }

  @Override
  public String getSectionText(int position) {
    if (position == 0) {
      return "";
    }
    if (getCurrentList() != null && getItemCount() > 0 && position < getItemCount()
        && getItem(position - 1) != null) {
      String title = getItem(position - 1).getDisplayName();
      return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
          .substring(0, 1) : "";
    }
    return "";
  }

  /**
   * 현재 재생되는 노래 갱신
   */
  public void updatePlayingSong() {
    final Song currentSong = MusicServiceRemote.getCurrentSong();
    if (currentSong.getId() == -1 || currentSong.getId() == mLastPlaySong.getId()) {
      return;
    }

    if (getCurrentList() != null && getCurrentList().indexOf(currentSong) >= 0) {
      // 새로 강조 표시된 노래찾기
      final int index = getCurrentList().indexOf(currentSong);
      final int lastIndex = getCurrentList().indexOf(mLastPlaySong);

      ChildHolderViewHolder newHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(index) instanceof ChildHolderViewHolder) {
        newHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
      } else {
        notifyItemChanged(index);
      }
      ChildHolderViewHolder oldHolder = null;
      if (mRecyclerView.findViewHolderForAdapterPosition(lastIndex) instanceof ChildHolderViewHolder) {
        oldHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(lastIndex);
      } else {
        if (lastIndex >= 0)
          notifyItemChanged(lastIndex);
      }

      if (newHolder != null) {
        newHolder.mIndicator.setVisibility(View.VISIBLE);
        newHolder.mVisualizer.setVisibility(View.VISIBLE);
      }

      if (oldHolder != null) {
        oldHolder.mName.setTextColor(getTextColorPrimary());
        oldHolder.mIndicator.setVisibility(View.GONE);
        oldHolder.mVisualizer.setVisibility(View.GONE);
      }
      mLastPlaySong = currentSong;
    } else {
      final int lastIndex = getCurrentList().indexOf(mLastPlaySong);
      if (lastIndex >= 0) {
        ChildHolderViewHolder oldHolder = null;
        if (mRecyclerView.findViewHolderForAdapterPosition(lastIndex) instanceof ChildHolderViewHolder) {
          oldHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(lastIndex);
        } else {
          notifyItemChanged(lastIndex);
        }

        if (oldHolder != null) {
          oldHolder.mName.setTextColor(getTextColorPrimary());
          oldHolder.mIndicator.setVisibility(View.GONE);
          oldHolder.mVisualizer.setVisibility(View.GONE);
        }

        mLastPlaySong = currentSong;
      }
    }
  }

  /**
   * 자식등록부항목을 위한 viewHolder
   */
  static class ChildHolderViewHolder extends BaseViewHolder {

    @BindView(R.id.song_head_image)
    SimpleDraweeView mImage;
    @BindView(R.id.song_title)
    TextView mName;
    @BindView(R.id.song_other)
    TextView mOther;
    @BindView(R.id.song_button)
    public ImageButton mButton;
    public View mContainer;
    @BindView(R.id.indicator)
    View mIndicator;
    @BindView(R.id.visualizer)
    MusicVisualizer mVisualizer;
    @BindView(R.id.item_checkbox)
    CheckBox mCheckBox;
    @BindView(R.id.duration)
    TextView mDuration;

    ChildHolderViewHolder(View itemView) {
      super(itemView);
      mContainer = itemView;
    }
  }

  /**
   * 자료가 갱신되였을때 개별적인 항목들의 이전자료와 현재자료를 비교하는 diffCallback
   */
  public static final DiffUtil.ItemCallback<Song> DIFF_CALLBACK =
          new DiffUtil.ItemCallback<Song>() {
            @Override
            public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
              return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
              return oldItem.equals(newItem);
            }
          };
}