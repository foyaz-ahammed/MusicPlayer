package com.kr.musicplayer.ui.adapter;

import android.view.View;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.facebook.drawee.view.SimpleDraweeView;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.misc.interfaces.OnSongChooseListener;
import com.kr.musicplayer.request.LibraryUriRequest;
import com.kr.musicplayer.request.RequestConfig;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;

import java.util.ArrayList;

import butterknife.BindView;

import static com.kr.musicplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * 노래선택화면을 위한 Adapter
 */

public class SongChooseAdapter extends BaseAdapter<Song, SongChooseAdapter.SongChooseHolder> {

  private OnSongChooseListener mCheckListener;
  private ArrayList<Integer> mCheckSongIdList = new ArrayList<>(); // 선택된 노래 id 목록

  public SongChooseAdapter(int layoutID, OnSongChooseListener l) {
    super(layoutID, DIFF_CALLBACK);
    mCheckListener = l;
  }

  public ArrayList<Integer> getCheckedSong() {
    return mCheckSongIdList;
  }

  @Override
  protected void convert(final SongChooseHolder holder, Song song, int position) {
    //노래제목
    holder.mSong.setText(song.getDisplayName());
    //예술가
    holder.mArtist.setText(song.getArtist());

    new LibraryUriRequest(holder.mImage,
        getSearchRequestWithAlbumType(song),
        new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
    //선택한 노래
    holder.mRoot.setOnClickListener(v -> {
      holder.mCheck.setChecked(!holder.mCheck.isChecked());
      mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
    });

    final int audioId = song.getId();
    holder.mCheck.setOnCheckedChangeListener(null);
    holder.mCheck.setChecked(mCheckSongIdList != null && mCheckSongIdList.contains(audioId));
    holder.mCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked && !mCheckSongIdList.contains(audioId)) {
        mCheckSongIdList.add(audioId);
      } else if (!isChecked) {
        mCheckSongIdList.remove(Integer.valueOf(audioId));
      }
      mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
    });
  }

  /**
   * 개별적인 노래항목을 위한 viewHolder
   */
  static class SongChooseHolder extends BaseViewHolder {

    @BindView(R.id.checkbox)
    CheckBox mCheck;
    @BindView(R.id.item_img)
    SimpleDraweeView mImage;
    @BindView(R.id.item_song)
    TextView mSong;
    @BindView(R.id.item_album)
    TextView mArtist;
    @BindView(R.id.item_root)
    RelativeLayout mRoot;

    SongChooseHolder(View itemView) {
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
              return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
              return oldItem.equals(newItem);
            }
          };
}
