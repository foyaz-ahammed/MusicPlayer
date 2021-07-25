package com.kr.musicplayer.misc.menu;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.kr.musicplayer.R;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.service.MusicService;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.ToastUtil;
import com.kr.musicplayer.util.Util;

/**
 * 음악조종 단추들을 위한 Listener
 */
public class CtrlButtonListener implements View.OnClickListener, View.OnTouchListener {

  private Context context;

  public CtrlButtonListener(Context context) {
    this.context = context;
  }

  @Override
  public void onClick(View v) {
    Intent intent = new Intent(MusicService.ACTION_CMD);
    switch (v.getId()) {
      case R.id.playbar_prev:
        if (MaterialDialogHelper.timeA >= 0) {
          ToastUtil.show(context, R.string.stop_repeat);
          return;
        }
        intent.putExtra("Control", Command.PREV);
        break;
      case R.id.playbar_next:
        if (MaterialDialogHelper.timeA >= 0) {
          ToastUtil.show(context, R.string.stop_repeat);
          return;
        }
        intent.putExtra("Control", Command.NEXT);
        break;
      case R.id.playbar_play:
        intent.putExtra("Control", Command.TOGGLE);
        break;
      case R.id.fast_forward:
        intent.putExtra("Control", Command.FAST_FORWARD);
        break;
      case R.id.fast_back:
        intent.putExtra("Control", Command.FAST_BACK);
        break;
    }
    Util.sendLocalBroadcast(intent);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (v.getId() != R.id.playbar_play) {
      ImageView imageView = (ImageView) v;
      ColorStateList c;
      if (event.getAction() == MotionEvent.ACTION_UP) {
        c = ContextCompat.getColorStateList(
                context,
                R.color.light_text_color_primary
        );
      } else {
        c = ContextCompat.getColorStateList(
                context,
                R.color.md_blue_primary
        );
      }
      imageView.setImageTintList(c);
    }
    return false;
  }
}
