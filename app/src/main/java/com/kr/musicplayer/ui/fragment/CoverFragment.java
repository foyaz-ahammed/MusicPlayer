package com.kr.musicplayer.ui.fragment;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;

import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.ui.fragment.base.BaseMusicFragment;

/**
 * Album Cover 현시를 위한 Fragment
 */
public class CoverFragment extends BaseMusicFragment {

  @BindView(R.id.cover_image)
  SimpleDraweeView mImage;
  @BindView(R.id.cover_container)
  View mCoverContainer;

  ViewPager mViewPager;

  private int mWidth;
  private Uri mUri = Uri.EMPTY; // Album Cover 화상관련 uri

  public CoverFragment() {
  }

  public CoverFragment(ViewPager viewPager) {
    mViewPager = viewPager;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = CoverFragment.class.getSimpleName();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mWidth = getResources().getDisplayMetrics().widthPixels;
    View rootView = inflater.inflate(R.layout.fragment_cover, container, false);
    mUnBinder = ButterKnife.bind(this, rootView);

    mCoverContainer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
          mViewPager.setCurrentItem(1, true);
        }
      }
    });

    return rootView;
  }

  /**
   * 작업이 이전곡인 경우 왼쪽으로 사라지는 Animation이 표시되고 다음 곡의 경우 오른쪽으로 사라지는 Animation 표시
   *
   * @param info 갱신해야 하는 노래
   * @param withAnim Animation 표시 여부
   */
  public void updateCover(Song info, Uri uri, boolean withAnim) {
    if (!isAdded()) {
      return;
    }
    if (mImage == null || info == null) {
      return;
    }
    mUri = uri;
    if (withAnim) {
      int operation = MusicServiceRemote.getOperation();

      int offsetX = (mWidth + mImage.getWidth()) >> 1;
      final double startValue = 0;
      final double endValue = operation == Command.PREV ? offsetX : -offsetX;

      //Cover 를 움직이는 Animation
      final Spring outAnim = SpringSystem.create().createSpring();
      outAnim.addListener(new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
          if (mCoverContainer == null || spring == null) {
            return;
          }
          mCoverContainer.setTranslationX((float) spring.getCurrentValue());
        }

        @Override
        public void onSpringAtRest(Spring spring) {
          //Cover 를 보여주는 Animation
          if (mImage == null || spring == null) {
            return;
          }
          mCoverContainer.setTranslationX((float) startValue);
          setImageUriInternal();

          float endVal = 1;
          final Spring inAnim = SpringSystem.create().createSpring();
          inAnim.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
              if (mImage == null || spring == null) {
                return;
              }
              mCoverContainer.setScaleX((float) spring.getCurrentValue());
              mCoverContainer.setScaleY((float) spring.getCurrentValue());
            }

            @Override
            public void onSpringActivate(Spring spring) {

            }

          });
          inAnim.setCurrentValue(0.85);
          inAnim.setEndValue(endVal);
        }
      });
      outAnim.setOvershootClampingEnabled(true);
      outAnim.setCurrentValue(startValue);
      outAnim.setEndValue(endValue);
    } else {
      setImageUriInternal();
    }
  }

  private void setImageUriInternal() {
    mImage.setImageURI(mUri);
  }

}
