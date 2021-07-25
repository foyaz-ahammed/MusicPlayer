package com.kr.musicplayer.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Folder;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.ui.misc.MultipleChoice;

/**
 * 등록부보기를 위한 Adapter
 */
public class FolderAdapter extends BaseAdapter<Folder, FolderAdapter.FolderHolder> {

  private MultipleChoice<Folder> mChoice;

  public FolderAdapter(int layoutId, MultipleChoice<Folder> multiChoice) {
    super(layoutId, DIFF_CALLBACK);
    mChoice = multiChoice;
  }

  @Override
  public void onBindViewHolder(@NonNull FolderAdapter.FolderHolder holder, int position) {
    convert(holder, getItem(position), position);
  }

  @SuppressLint({"DefaultLocale", "RestrictedApi"})
  @Override
  protected void convert(final FolderHolder holder, Folder folder, int position) {
    //등록부 이름, 경로 이름, 노래수 설정
    holder.mName.setText(folder.getName());
    holder.mPath.setText(folder.getPath());
    holder.mCount.setText(String.format("%d", folder.getCount()));

    if (mOnItemClickListener != null && holder.mContainer != null) {
      holder.mContainer.setOnClickListener(
          v -> mOnItemClickListener.onItemClick(v, position));
      holder.mContainer.setOnLongClickListener(v -> {
        mOnItemClickListener.onItemLongClick(v, position);
        return true;
      });
    }


    if (MultipleChoice.isActiveSomeWhere()) {
      holder.checkBox.setVisibility(View.VISIBLE);
      holder.mImg.setVisibility(View.GONE);
    } else {
      holder.checkBox.setVisibility(View.GONE);
      holder.mImg.setVisibility(View.VISIBLE);
    }

    boolean flag = mChoice.isPositionCheck(position);
    holder.checkBox.setChecked(flag);

  }

  /**
   * 개별적인 등록부를 위한 viewHolder
   */
  static class FolderHolder extends BaseViewHolder {

    View mContainer;
    @BindView(R.id.folder_image)
    ImageView mImg;
    @BindView(R.id.folder_name)
    TextView mName;
    @BindView(R.id.folder_path)
    TextView mPath;
    @BindView(R.id.folder_num)
    TextView mCount;
    @BindView(R.id.folder_button)
    ImageButton mButton;
    @BindView(R.id.check)
    CheckBox checkBox;

    public FolderHolder(View itemView) {
      super(itemView);
      mContainer = itemView;
    }
  }

  /**
   * 자료가 갱신되였을때 개별적인 항목들의 이전자료와 현재자료를 비교하는 diffCallback
   */
  public static final DiffUtil.ItemCallback<Folder> DIFF_CALLBACK =
          new DiffUtil.ItemCallback<Folder>() {
            @Override
            public boolean areItemsTheSame(@NonNull Folder oldItem, @NonNull Folder newItem) {
              return oldItem.getParentId() == newItem.getParentId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Folder oldItem, @NonNull Folder newItem) {
              return oldItem.equals(newItem);
            }
          };

}
