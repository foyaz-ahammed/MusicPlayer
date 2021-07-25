package com.kr.musicplayer.ui.adapter.holder;

import android.view.View;
import android.widget.ImageView;
import butterknife.BindView;
import com.kr.musicplayer.R;

/**
 * 머리부 viewHolder
 */
public class HeaderHolder extends BaseViewHolder {

  //목록 표시와 격자표시간 전환
  @BindView(R.id.list_model)
  public ImageView mListModeBtn;
  @BindView(R.id.grid_model)
  public ImageView mGridModeBtn;
  @BindView(R.id.divider)
  public View mDivider;

  public HeaderHolder(View itemView) {
    super(itemView);
  }
}
