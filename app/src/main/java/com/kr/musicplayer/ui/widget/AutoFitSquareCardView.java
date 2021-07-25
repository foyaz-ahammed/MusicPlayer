package com.kr.musicplayer.ui.widget;

import android.content.Context;
import androidx.cardview.widget.CardView;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import com.kr.musicplayer.App;
import com.kr.musicplayer.util.DensityUtil;

/**
 * 재생화면에서 Cover Fragment 를 위한 view
 */
public class AutoFitSquareCardView extends CardView {

  public AutoFitSquareCardView(Context context) {
    super(context);
  }

  public AutoFitSquareCardView(Context context, AttributeSet attributeSet) {
    super(context, attributeSet);
  }

  public AutoFitSquareCardView(Context context, AttributeSet attributeSet, int i) {
    super(context, attributeSet, i);
  }

  private static final int THRESHOLD = DensityUtil.dip2px(App.getContext(), 40);

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    final int sizeMeasureSpec = MeasureSpec
        .makeMeasureSpec(Math.min(widthSize, heightSize), MeasureSpec.EXACTLY);
    super.onMeasure(sizeMeasureSpec, sizeMeasureSpec);
    //비률에 따라 layout 조절
    if (heightSize * 1f / widthSize > 1.2f) {
      RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
      lp.addRule(RelativeLayout.CENTER_VERTICAL);
      lp.topMargin = 0;
      lp.bottomMargin = 0;
    }
  }
}
