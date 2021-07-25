package com.kr.musicplayer.ui.adapter;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;

import com.kr.musicplayer.R;
import com.kr.musicplayer.db.room.model.PlayList;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.ui.dialog.MenuDialog;
import com.kr.musicplayer.ui.dialog.SleepDialog;

import butterknife.BindView;

/**
 * 기본화면의 재생목록들을 위한 Adapter
 */
public class DrawerAdapter extends BaseAdapter<PlayList, DrawerAdapter.DrawerHolder> {

    private int[] IMAGES = new int[]{R.drawable.ic_favorites, R.drawable.ic_add_24, R.drawable.ic_playlists};
    private int[] TITLES = new int[]{R.string.drawer_favorites, R.string.new_list, R.string.drawer_playlists};
    private Context mContext;

    public DrawerAdapter(int layoutId) {
        super(layoutId, DIFF_CALLBACK);
    }

    public void setContext(Context context) {
        mContext = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void convert(final DrawerHolder holder, PlayList playlistRes, int position) {
        if (position < 2) {
            holder.mImg.setImageResource(IMAGES[position]);
            holder.mText.setText(TITLES[position]);
            holder.mPopup.setImageResource(R.drawable.ic_arrow);
        } else {
            holder.mImg.setImageResource(IMAGES[2]);
            holder.mText.setText(playlistRes.getName());
            holder.mPopup.setImageResource(R.drawable.ic_player_more);
        }
        if (playlistRes.getId() > 0) {
            holder.mCountText.setText(playlistRes.getAudioIds().size() + "");
        } else {
            holder.mCountText.setText("");
        }
        holder.mRoot
                .setOnClickListener(v -> mOnItemClickListener.onItemClick(v, position));
        holder.mPopup.setOnClickListener(v -> {
            if (position != 1) {
                new MenuDialog(playlistRes).show(((FragmentActivity) mContext).getSupportFragmentManager(), SleepDialog.class.getSimpleName());
            }
        });
    }

    static class DrawerHolder extends BaseViewHolder {

        @BindView(R.id.item_img)
        ImageView mImg;
        @BindView(R.id.item_text)
        TextView mText;
        @BindView(R.id.item_count)
        TextView mCountText;
        @BindView(R.id.item_root)
        RelativeLayout mRoot;
        @BindView(R.id.popup)
        ImageView mPopup;

        DrawerHolder(View itemView) {
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
                    return oldItem.getName().equals(newItem.getName()) && oldItem.getAudioIds().size() == newItem.getAudioIds().size();
                }
            };
}
