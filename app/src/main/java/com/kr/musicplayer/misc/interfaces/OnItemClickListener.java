package com.kr.musicplayer.misc.interfaces;

import android.view.View;

/**
 * 항목선택관련 interface
 */
public interface OnItemClickListener {

  void onItemClick(View view, int position);

  void onItemLongClick(View view, int position);
}
