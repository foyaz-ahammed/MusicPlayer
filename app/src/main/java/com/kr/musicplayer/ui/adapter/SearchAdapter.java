package com.kr.musicplayer.ui.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.util.MaterialDialogHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;

/**
 * 검색결과를 위한 Adapter
 */
public class SearchAdapter extends BaseAdapter<Song, SearchAdapter.SearchResHolder> {
    private boolean isSearch = false;

    public SearchAdapter(int layoutId, boolean isSearch) {
        super(layoutId, DIFF_CALLBACK);
        this.isSearch = isSearch;
    }

    @Override
    public void onViewRecycled(@NonNull SearchAdapter.SearchResHolder holder) {
        super.onViewRecycled(holder);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(final SearchResHolder holder, Song song, int position) {
        String name = song.getDisplayName();
        SpannableStringBuilder sb = new SpannableStringBuilder(name);
        // Search words in the item and set color white
        Pattern p = Pattern.compile(MaterialDialogHelper.searchKey, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(name);
        while (m.find()) {
            sb.setSpan(new ForegroundColorSpan(Color.WHITE), m.start(), m.end(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        holder.mName.setText(sb);
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
        holder.mDuration.setText(MaterialDialogHelper.formatSeconds(song.getDuration() / 1000));
        if (isSearch) {
            holder.mDuration.setVisibility(View.GONE);
            holder.mCheck.setVisibility(View.VISIBLE);
        }

        if (mOnItemClickListener != null && holder.mRooView != null) {
            holder.mRooView.setOnClickListener(
                    v -> {
                        mOnItemClickListener.onItemClick(v, holder.getAdapterPosition());
                        holder.mCheck.setChecked(!holder.mCheck.isChecked());
                    });
        }
    }

    /**
     * 개별적인 검색결과항목을 위한 viewHolder
     */
    static class SearchResHolder extends BaseViewHolder {

        @BindView(R.id.reslist_item)
        RelativeLayout mRooView;
        @BindView(R.id.search_image)
        ImageView mImage;
        @BindView(R.id.search_name)
        TextView mName;
        @BindView(R.id.search_detail)
        TextView mOther;
        @BindView(R.id.search_button)
        ImageButton mButton;
        @BindView(R.id.duration)
        TextView mDuration;
        @BindView(R.id.check)
        CheckBox mCheck;

        SearchResHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 자료가 갱신되였을때 개별적인 항목들의 이전자료와 현재자료를 비교하는 diffCallback
     */
    public static final DiffUtil.ItemCallback<Song> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Song>() {
                @Override
                public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
