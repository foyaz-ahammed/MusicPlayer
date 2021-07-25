package com.kr.musicplayer.ui.adapter.holder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import butterknife.ButterKnife;

/**
 * 기초 viewHolder
 */
public class BaseViewHolder extends RecyclerView.ViewHolder {

  public View mRoot;

  public BaseViewHolder(View itemView) {
    super(itemView);
    ButterKnife.bind(this, itemView);
    mRoot = itemView;
  }
}
