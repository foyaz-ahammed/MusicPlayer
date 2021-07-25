package com.kr.musicplayer.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import butterknife.BindView;
import java.util.Arrays;
import java.util.List;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.theme.GradientDrawableMaker;
import com.kr.musicplayer.theme.ThemeStore;
import com.kr.musicplayer.ui.adapter.holder.BaseViewHolder;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.DensityUtil;

/**
 * Desktop 가사색상 변경을 위한 adapter
 */

public class DesktopLyricColorAdapter extends BaseAdapter<Integer, DesktopLyricColorAdapter.FloatColorHolder> {

  //현재 Desktop 가사의 서체색상은 기본적으로 현재 Theme 색상으로 설정된다
  private int mCurrentColor;
  private int mItemWidth;

  private static final int SIZE = DensityUtil.dip2px(18);

  public static final List<Integer> COLORS = Arrays.asList(
      R.color.md_red_primary, R.color.md_brown_primary, R.color.md_navy_primary,
      R.color.md_green_primary, R.color.md_yellow_primary, R.color.md_purple_primary,
      R.color.md_indigo_primary, R.color.md_plum_primary, R.color.md_blue_primary,
      R.color.md_white_primary, R.color.md_pink_primary
  );

  public DesktopLyricColorAdapter(Context context, int layoutId, int width) {
    super(layoutId, DIFF_CALLBACK);
    submitList(COLORS);
    mItemWidth = width / COLORS.size();
    //너비가 너무 작으면 20dp 로 설정
    if (mItemWidth < DensityUtil.dip2px(context, 20)) {
      mItemWidth = DensityUtil.dip2px(context, 20);
    }
    mCurrentColor = ThemeStore.getFloatLyricTextColor();

  }

  /**
   * 선택한 색상인지 확인
   */
  private boolean isColorChoose(Context context, int colorRes) {
    return context.getResources().getColor(colorRes) == mCurrentColor;
  }

  /**
   * 현재 색상 설정
   * @param color 설정하려는 색
   */
  public void setCurrentColor(int color) {
    mCurrentColor = color;
    ThemeStore.saveFloatLyricTextColor(color);
  }


  @NonNull
  @Override
  public FloatColorHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    final Context context = parent.getContext();
    FloatColorHolder holder = new FloatColorHolder(
        LayoutInflater.from(context).inflate(R.layout.item_float_lrc_color, parent, false));

    RelativeLayout.LayoutParams imgLayoutParam = new RelativeLayout.LayoutParams(
        DensityUtil.dip2px(context, 18), DensityUtil.dip2px(context, 18));
    imgLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT);
    holder.mColor.setLayoutParams(imgLayoutParam);

    RecyclerView.LayoutParams rootLayoutParam = new RecyclerView.LayoutParams(mItemWidth,
        ViewGroup.LayoutParams.MATCH_PARENT);
    holder.mRoot.setLayoutParams(rootLayoutParam);

    return holder;
  }


  @Override
  protected void convert(FloatColorHolder holder, Integer colorRes, final int position) {
    final int color = colorRes != R.color.md_white_primary ?
        ColorUtil.getColor(colorRes) : Color.parseColor("#F9F9F9");

    if (isColorChoose(holder.itemView.getContext(), colorRes)) {
      holder.mColor.setBackground(new GradientDrawableMaker()
          .shape(GradientDrawable.OVAL)
          .color(color)
          .strokeSize(DensityUtil.dip2px(1))
          .strokeColor(Color.BLACK)
          .width(SIZE)
          .height(SIZE)
          .make()
      );
    } else {
      holder.mColor.setBackground(new GradientDrawableMaker()
          .shape(GradientDrawable.OVAL)
          .color(color)
          .width(SIZE)
          .height(SIZE)
          .make());
    }
    holder.mRoot.setOnClickListener(v -> mOnItemClickListener.onItemClick(v, position));
  }

  static class FloatColorHolder extends BaseViewHolder {

    @BindView(R.id.item_color)
    ImageView mColor;

    public FloatColorHolder(View itemView) {
      super(itemView);
    }
  }

  /**
   * 자료가 갱신되였을때 개별적인 항목들의 이전자료와 현재자료를 비교하는 diffCallback
   */
  public static final DiffUtil.ItemCallback<Integer> DIFF_CALLBACK =
          new DiffUtil.ItemCallback<Integer>() {
            @Override
            public boolean areItemsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
              return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Integer oldItem, @NonNull Integer newItem) {
              return oldItem.equals(newItem);
            }
          };
}
