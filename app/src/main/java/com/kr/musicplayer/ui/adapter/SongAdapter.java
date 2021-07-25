package com.kr.musicplayer.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;
import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.appwidgets.MusicVisualizer;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.helper.SortOrder;
import com.kr.musicplayer.service.PlayQueue;
import com.kr.musicplayer.ui.activity.PlayerActivity;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.ui.adapter.holder.ItemMoveCallback;
import com.kr.musicplayer.ui.fragment.LibraryFragment;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScroller;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.ToastUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import timber.log.Timber;

import static com.kr.musicplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * 모든 노래 및 재생페지를 위한 Adapter
 */

public class SongAdapter extends HeaderAdapter<Song, BaseViewHolder> implements
        FastScroller.SectionIndexer, ItemMoveCallback.ItemTouchHelpers {

    private Song mLastPlaySong = MusicServiceRemote.getCurrentSong(); // 마지막으로 재생된 노래
    private LibraryFragment mFragment;
    private ItemTouchHelper mTouchHelper = null;

    public SongAdapter(int layoutId, MultipleChoice multiChoice, RecyclerView recyclerView, LibraryFragment fragment) {
        super(layoutId, multiChoice, recyclerView, DIFF_CALLBACK);
        mRecyclerView = recyclerView;
        mFragment = fragment;
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SongViewHolder holder = new SongViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_song_recycle, parent, false));

        // For items exchange in one click not long click
        holder.mHandle.setOnTouchListener((v, event) -> {
            if (mTouchHelper != null && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mTouchHelper.startDrag(holder);
            }
            return true;
        });
        return holder;
    }

    @Override
    public void onViewRecycled(@NonNull BaseViewHolder holder) {
        super.onViewRecycled(holder);
        disposeLoad(holder);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(BaseViewHolder baseHolder, final Song song, int position) {
        final Context context = baseHolder.itemView.getContext();
        if (!(baseHolder instanceof SongViewHolder)) {
            return;
        }
        final SongViewHolder holder = (SongViewHolder) baseHolder;

        //Cover
        holder.mImage.setTag(
                setImage(holder.mImage, getSearchRequestWithAlbumType(song), SMALL_IMAGE_SIZE, position + 1));

        if (MusicServiceRemote.getCurrentSong().getId() == song.getId()) {
            mLastPlaySong = song;
            holder.mIndicator.setVisibility(View.VISIBLE);
            holder.mVisualizer.setVisibility(View.VISIBLE);
        } else {
            holder.mIndicator.setVisibility(View.GONE);
            holder.mVisualizer.setVisibility(View.GONE);
        }
        holder.mIndicator.setBackgroundColor(ColorUtil.getColor(R.color.md_red_primary)); // getHighLightTextColor()

        //표제
        holder.mName.setText(song.getDisplayName());
        holder.mDuration.setText(MaterialDialogHelper.formatSeconds(song.getDuration() / 1000));

        //Artist & Album 표시
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));

        if (MultipleChoice.isActiveSomeWhere()) {
            holder.mImage.setVisibility(View.GONE);
            holder.mCheckBox.setVisibility(View.VISIBLE);
            holder.mButton.setVisibility(View.GONE);
            holder.mVisualizer.setVisibility(View.GONE);
            holder.mIndicator.setVisibility(View.INVISIBLE);
        } else {
            holder.mImage.setVisibility(View.VISIBLE);
            holder.mCheckBox.setVisibility(View.GONE);
            holder.mButton.setVisibility(View.VISIBLE);
        }

        holder.mContainer.setOnClickListener(v -> {
            if (position < 0) {
                ToastUtil.show(context, R.string.illegal_arg);
                return;
            }
            mOnItemClickListener.onItemClick(v, holder.getAdapterPosition());
        });

        if (mFragment.getContext() instanceof PlayerActivity) {
            holder.mHandle.setVisibility(View.VISIBLE);
            return;
        }

        holder.mContainer.setOnLongClickListener(v -> {
            if (position < 0) {
                ToastUtil.show(context, R.string.illegal_arg);
                return true;
            }
            mOnItemClickListener.onItemLongClick(v, holder.getAdapterPosition());
            return true;
        });

        holder.mCheckBox.setChecked(mChoice.isPositionCheck(position));
    }

    /**
     * ItemTouchHelper 설정
     * @param callback 설정할 ItemTouchHelper
     */
    public void setItemTouchHelper(ItemTouchHelper callback) {
        mTouchHelper = callback;
    }

    @Override
    public String getSectionText(int position) {
        if (position == 0) {
            return "";
        }
        if (getCurrentList() != null && position - 1 < getItemCount()) {
            String title = getItem(position - 1).getDisplayName();
            return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase()
                    .substring(0, 1) : "";
        }
        return "";
    }

    /**
     * 재생노래 갱신
     */
    public void updatePlayingSong() {
        final Song currentSong = MusicServiceRemote.getCurrentSong();
        if (currentSong.getId() < 0 || currentSong.getId() == mLastPlaySong.getId()) {
            return;
        }

        if (getCurrentList() != null && getCurrentList().contains(currentSong)) {
            // 새로 강조 표시된 노래 찾기
            final int index = getCurrentList().indexOf(currentSong);
            final int lastIndex = getCurrentList().indexOf(mLastPlaySong);

            SongViewHolder newHolder = null;
            if (mRecyclerView.findViewHolderForAdapterPosition(index) instanceof SongViewHolder) {
                newHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
            } else {
                notifyItemChanged(index);
            }
            SongViewHolder oldHolder = null;
            if (mRecyclerView.findViewHolderForAdapterPosition(lastIndex) instanceof SongViewHolder) {
                oldHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(lastIndex);
            } else {
                notifyItemChanged(lastIndex);
            }

            if (newHolder != null) {
                newHolder.mIndicator.setVisibility(View.VISIBLE);
                newHolder.mVisualizer.setVisibility(View.VISIBLE);
            }

            if (oldHolder != null) {
                oldHolder.mIndicator.setVisibility(View.GONE);
                oldHolder.mVisualizer.setVisibility(View.GONE);
            }
            mLastPlaySong = currentSong;
        }
    }

    /**
     * 이전 위치로부터 새로운 위치로 이동
     * @param fromPosition 이전 위치
     * @param toPosition 새 위치
     */
    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        List<Song> songs = new ArrayList<>();
        songs.addAll(getCurrentList());
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(songs, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(songs, i, i - 1);
            }
        }
        submitList(songs);
    }

    @Override
    public void onRowSelected(SongViewHolder myViewHolder) {

    }

    /**
     * 끌기가 끝나면 재생대기렬 갱신
     * @param myViewHolder 이동한 viewHolder
     */
    @Override
    public void onRowClear(SongViewHolder myViewHolder) {
        List<Song> songs = new ArrayList<>();
        songs.addAll(getCurrentList());
        PlayQueue playQueue = MusicServiceRemote.getPlayQueues();
        if (playQueue != null) {
            playQueue.addAll(songs);
            int pos = playQueue.getPlayingQueue().indexOf(MusicServiceRemote.getCurrentSong());
            playQueue.updateNextSong(pos);
        }
    }

    public static class SongViewHolder extends BaseViewHolder {

        @BindView(R.id.song_title)
        TextView mName;
        @BindView(R.id.song_other)
        TextView mOther;
        @BindView(R.id.song_head_image)
        SimpleDraweeView mImage;
        @BindView(R.id.song_button)
        ImageButton mButton;
        @BindView(R.id.item_root)
        View mContainer;
        @BindView(R.id.indicator)
        View mIndicator;
        @BindView(R.id.visualizer)
        MusicVisualizer mVisualizer;
        @BindView(R.id.item_checkbox)
        CheckBox mCheckBox;
        @BindView(R.id.duration)
        TextView mDuration;
        @BindView(R.id.handle)
        ImageView mHandle;

        SongViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class HeaderHolder extends BaseViewHolder {

        View mRoot;
        @BindView(R.id.divider)
        View mDivider;
        @BindView(R.id.play_random_button)
        ImageView mIvRandom;
        @BindView(R.id.tv_random_count)
        TextView mTvRandom;
        @BindView(R.id.check_all)
        ImageView mCheckAll;
        @BindView(R.id.play_random)
        LinearLayout mPlayRandom;
        @BindView(R.id.sort_alphabetical)
        LinearLayout mSort;
        @BindView(R.id.sort_title)
        TextView mSortTitle;
        @BindView(R.id.sort_icon)
        ImageView mSortIcon;

        HeaderHolder(View itemView) {
            super(itemView);
            String sortType = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
                    SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z);
            if (sortType.equals(SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z)) {
                mSortTitle.setText(R.string.alphabetically);
                mSortIcon.setImageDrawable(App.getContext().getDrawable(R.drawable.ic_sort_white_24dp));
            } else {
                mSortTitle.setText(R.string.date);
                mSortIcon.setImageDrawable(App.getContext().getDrawable(R.drawable.ic_timer_white_24dp));
            }
            mRoot = itemView;

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
