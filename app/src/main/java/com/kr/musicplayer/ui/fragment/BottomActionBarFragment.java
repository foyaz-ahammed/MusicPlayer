package com.kr.musicplayer.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.RoundedCornerTreatment;
import com.google.android.material.shape.ShapePathModel;
import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.appwidgets.MusicVisualizer;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.misc.menu.CtrlButtonListener;
import com.kr.musicplayer.request.LibraryUriRequest;
import com.kr.musicplayer.request.RequestConfig;
import com.kr.musicplayer.service.MusicService;
import com.kr.musicplayer.ui.activity.PlayerActivity;
import com.kr.musicplayer.ui.fragment.base.BaseMusicFragment;
import com.kr.musicplayer.util.DensityUtil;
import com.kr.musicplayer.util.MaterialDialogHelper;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import static com.kr.musicplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * 작은 재생화면
 */
public class BottomActionBarFragment extends BaseMusicFragment {

    //재생단추
    @BindView(R.id.playbar_play)
    FloatingActionButton mPlayButton;
    @BindView(R.id.playbar_next)
    ImageView mPlayNext;
    @BindView(R.id.playbar_prev)
    ImageView mPlayPrev;
    @BindView(R.id.fast_forward)
    ImageView mFastForward;
    @BindView(R.id.fast_back)
    ImageView mFastBack;
    //노래 이름
    @BindView(R.id.bottom_title)
    TextView mTitle;
    @BindView(R.id.bottom_artist)
    TextView mArtist;
    @BindView(R.id.bottom_action_bar)
    BottomAppBar mBottomActionBar;
    @BindView(R.id.bottom_actionbar_root)
    CoordinatorLayout mRootView;
    @BindView(R.id.bottom_action_bar_cover)
    SimpleDraweeView mCover;
    @BindView(R.id.container_playbar_play)
    View container;

    private GestureDetector mGestureDetector;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = BottomActionBarFragment.class.getSimpleName();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        Timber.v("BottomActionBarFragment onResume is called");
        super.onResume();
        updatePlayStatus();
        mBottomActionBar.setClickable(true);
    }
    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.bottom_actionbar, container, false);
        mUnBinder = ButterKnife.bind(this, rootView);

        mBottomActionBar.setOnClickListener(new DebouncedOnClickListener(500) {
            @Override
            public void onDebouncedClick(View v) {
                if (!(getActivity() instanceof PlayerActivity)) {
                    startPlayerActivity();
                    mBottomActionBar.setClickable(false);
                }
            }
        });

        MaterialShapeDrawable bottomBarBackground = (MaterialShapeDrawable) mBottomActionBar.getBackground();
        ShapePathModel shapePathModel = bottomBarBackground.getShapedViewModel();
        shapePathModel.setTopLeftCorner(new RoundedCornerTreatment(56));
        shapePathModel.setTopRightCorner(new RoundedCornerTreatment(56));
        bottomBarBackground.setShapedViewModel(shapePathModel);

        mGestureDetector = new GestureDetector(mContext, new GestureListener(this));
        if (!(getActivity() instanceof PlayerActivity)) {
            mBottomActionBar.setOnTouchListener((v, event) -> mGestureDetector.onTouchEvent(event));
        }

        //조종단추
        CtrlButtonListener listener = new CtrlButtonListener(App.getContext());
        mPlayButton.setOnClickListener(listener);
        mPlayNext.setOnClickListener(listener);
        mPlayPrev.setOnClickListener(listener);
        mFastBack.setOnClickListener(listener);
        mFastForward.setOnClickListener(listener);

        mPlayNext.setOnTouchListener(listener);
        mPlayPrev.setOnTouchListener(listener);
        mFastBack.setOnTouchListener(listener);
        mFastForward.setOnTouchListener(listener);

        mTitle.setSelected(true);

        return rootView;
    }

    /**
     * 재생 및 중지상태가 변경되였을때 호출되는 callback
     */
    @Override
    public void onMetaChanged() {
        Timber.v("BottomActionBarFragment onMetaChanged is called");
        super.onMetaChanged();
        Log.d("--------------BottomActionBarFragment", "onMetaChanged");
        updateSong();
    }

    /**
     * MediaStore 가 변경되였을때 호출되는 callback
     */
    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();
        Log.d("--------------BottomActionBarFragment", "onMediaStoreChanged");
        onMetaChanged();
    }

    /**
     * 재생상태가 변경되였을때 호출되는 callback
     */
    @Override
    public void onPlayStateChange() {
        super.onPlayStateChange();
        updatePlayStatus();
    }

    /**
     * Service에 접속되였을때 호출되는 callback
     * @param service 접속된 service
     */
    @Override
    public void onServiceConnected(@NotNull MusicService service) {
        super.onServiceConnected(service);
        Log.d("--------------BottomActionBarFragment", "onServiceConnected");
        onMetaChanged();
        onPlayStateChange();
    }

    /**
     * 재생상태갱신
     */
    public void updatePlayStatus() {
        //단추색상설정
        if (mPlayButton == null) {
            return;
        }

        if (MusicServiceRemote.isPlaying()) {
            mPlayButton.setBackgroundTintList(ContextCompat.getColorStateList(
                    getContext(),
                    R.color.md_red_dark
            ));
            container.setBackgroundTintList(ContextCompat.getColorStateList(
                    getContext(),
                    R.color.md_red_light
            ));

            //재생단추를 누르면 Music Visualizer 상태 재개
            MusicVisualizer.resumeVisualizer();
        } else {
            mPlayButton.setBackgroundTintList(ContextCompat.getColorStateList(
                    getContext(),
                    R.color.md_green_primary
            ));
            container.setBackgroundTintList(ContextCompat.getColorStateList(
                    getContext(),
                    R.color.md_green_light
            ));

            //일시중지 단추를 누르면 Music Visualizer 상태를 일시중지
            MusicVisualizer.pauseVisualizer();
        }
    }



    //Interface 갱신
    public void updateSong() {
        Song song = MusicServiceRemote.getCurrentSong();
        Timber.v("BottomActionBarFragment updateSong is called and song empty status is %s", song == Song.getEMPTY_SONG());
        //노래 이름 및 예술가
        if (mTitle != null) {
            mTitle.setText(song.getDisplayName());
        }
        if (mArtist != null) {
            mArtist.setText(song.getArtist());
        }
        //Album Cover
        if (mCover != null) {
            new LibraryUriRequest(mCover,
                    getSearchRequestWithAlbumType(song),
                    new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()) {
                @Override
                public void onSuccess(String result) {
                    super.onSuccess(result);
                }

                @Override
                public void onError(Throwable throwable) {
                    super.onError(throwable);
                }
            }.load();
        }

    }

    /**
     * 재생화면 시작
     */
    public void startPlayerActivity() {
        if (MusicServiceRemote.getCurrentSong().getId() < 0) {
            return;
        }
        Intent intent = new Intent(mContext, PlayerActivity.class);

        Activity activity = getActivity();
        if (activity != null) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity);
            startActivity(intent, options.toBundle());
            MaterialDialogHelper.favoriteCount++;
        }
    }

    /**
     * 재생화면을 Fling Gesture 로 열기 위한 Listener
     */
    static class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private final WeakReference<BottomActionBarFragment> mReference;

        GestureListener(BottomActionBarFragment fragment) {
            super();
            mReference = new WeakReference<>(fragment);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mReference.get() != null) {
                mReference.get().startPlayerActivity();
            }
            return true;
        }

        @Override
        public boolean onContextClick(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        private static final int Y_THRESHOLD = DensityUtil.dip2px(App.getContext(), 10);

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mReference.get() != null && velocityY < 0 && e1.getY() - e2.getY() > Y_THRESHOLD) {
                mReference.get().startPlayerActivity();
            }
            return true;
        }
    }

    public abstract class DebouncedOnClickListener implements View.OnClickListener {

        private final long minimumIntervalMillis;
        private Map<View, Long> lastClickMap;

        /**
         * subClass 에서 정의
         * @param v 누르기한 view
         */
        public abstract void onDebouncedClick(View v);

        /**
         * 구성자
         * @param minimumIntervalMillis click 사이에 허용되는 최소시간 - 이전 click 이후 이보다 빠른 click은 거부
         */
        public DebouncedOnClickListener(long minimumIntervalMillis) {
            this.minimumIntervalMillis = minimumIntervalMillis;
            this.lastClickMap = new WeakHashMap<>();
        }

        @Override
        public void onClick(View clickedView) {
            Long previousClickTimestamp = lastClickMap.get(clickedView);
            long currentTimestamp = SystemClock.uptimeMillis();

            lastClickMap.put(clickedView, currentTimestamp);
            if(previousClickTimestamp == null || Math.abs(currentTimestamp - previousClickTimestamp) > minimumIntervalMillis) {
                onDebouncedClick(clickedView);
            }
        }
    }
}