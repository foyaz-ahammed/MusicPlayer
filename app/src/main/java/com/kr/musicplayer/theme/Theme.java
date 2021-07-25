package com.kr.musicplayer.theme;

import static android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.view.View;
import android.widget.ImageView;
import com.afollestad.materialdialogs.MaterialDialog;
import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.DensityUtil;


/**
 * Theme 관련 class
 */
public class Theme {

  /**
   * Drawable 을 얻는 함수
   * @param res resource
   * @param resId Drawable Id
   * @param theme Theme
   * @return Drawable
   */
  public static Drawable getVectorDrawable(@NonNull Resources res, @DrawableRes int resId,
      @Nullable Resources.Theme theme) {
    if (Build.VERSION.SDK_INT >= 21) {
      return res.getDrawable(resId, theme);
    }
    return VectorDrawableCompat.create(res, resId, theme);
  }

  public static Drawable tintVectorDrawable(@NonNull Context context, @DrawableRes int id,
      @ColorInt int color) {
    return tintDrawable(getVectorDrawable(context.getResources(), id, context.getTheme()), color);
  }

  /**
   * 색상 Drawable 얻기
   * @param oriDrawable 본래 drawable
   * @param color 색상
   * @param alpha 투명도값
   */
  public static Drawable tintDrawable(Drawable oriDrawable, @ColorInt int color,
      @FloatRange(from = 0.0D, to = 1.0D) float alpha) {

    final Drawable wrappedDrawable = DrawableCompat.wrap(oriDrawable.mutate());
    DrawableCompat
        .setTintList(wrappedDrawable, ColorStateList.valueOf(ColorUtil.adjustAlpha(color, alpha)));
    return wrappedDrawable;
  }

  /**
   * 색상 Drawable 얻기
   */
  public static Drawable tintDrawable(Drawable oriDrawable, @ColorInt int color) {
    return tintDrawable(oriDrawable, color, 1.0f);
  }

  /**
   * 주어진 id 에 해당하는 drawable 얻는 함수
   * @param context The application context
   * @param id Drawable Id
   */
  public static Drawable getDrawable(Context context, int id) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      return context.getResources().getDrawable(id);
    } else {
      return context.getDrawable(id);
    }
  }

  /**
   * Loading 대화창 얻기
   * @param context The application context
   * @param content 제목
   * @return 대화창
   */
  public static MaterialDialog.Builder getLoadingDialog(Context context,String content) {
    return Theme.getBaseDialog(context)
        .title(content)
        .content(R.string.please_wait)
        .canceledOnTouchOutside(false)
        .progress(true, 0)
        .progressIndeterminateStyle(false);
  }

  /**
   * 기초 대화창 얻기
   * @param context The application context
   * @return
   */
  public static MaterialDialog.Builder getBaseDialog(Context context) {
    return new MaterialDialog.Builder(context)
        .contentColor(context.getColor(R.color.light_text_color_primary))
        .titleColor(context.getColor(R.color.light_text_color_primary))
        .positiveColor(context.getColor(R.color.light_text_color_primary))
        .negativeColor(context.getColor(R.color.light_text_color_primary))
        .neutralColor(context.getColor(R.color.light_text_color_primary))
        .buttonRippleColor(context.getColor(R.color.light_ripple_color))
        .backgroundColor(context.getColor(R.color.light_background_color_main))
        .itemsColor(context.getColor(R.color.light_text_color_primary))
        .widgetColor(ThemeStore.getAccentColor());
  }

  /**
   * Navigation bar 색상 밝게 설정
   * @param activity 설정하려는 Activity
   * @param enabled
   */
  public static void setLightNavigationbarAuto(Activity activity, boolean enabled) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      final View decorView = activity.getWindow().getDecorView();
      int systemUiVisibility = decorView.getSystemUiVisibility();
      if (enabled) {
        systemUiVisibility |= SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      } else {
        systemUiVisibility &= ~SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
      }
      decorView.setSystemUiVisibility(systemUiVisibility);
    }

  }
}
