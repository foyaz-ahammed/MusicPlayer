package com.kr.musicplayer.ui.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.kr.musicplayer.R;
import com.kr.musicplayer.db.room.DatabaseRepository;
import com.kr.musicplayer.db.room.model.PlayList;
import com.kr.musicplayer.helper.CircleRecyclerViewEvent;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.ui.adapter.DrawerAdapter;
import com.kr.musicplayer.ui.viewmodel.PlaylistViewModel;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.ToastUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import me.khrystal.library.widget.CircleRecyclerView;
import me.khrystal.library.widget.ItemViewMode;
import me.khrystal.library.widget.ScaleXViewMode;

import static com.kr.musicplayer.request.network.RxUtil.applySingleScheduler;

/**
 * 기본화면
 */
public class MusicPlayer extends LibraryActivity<PlayList, DrawerAdapter> implements CircleRecyclerViewEvent {

    private CircleRecyclerView mCircleRecyclerView;
    ItemViewMode mItemViewMode;
    LinearLayoutManager mLayoutManager;
    EditText mSearch;
    ImageView mTheme;

    RecyclerView mRecyclerView;

    PlaylistViewModel viewModel;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 자료기지의 재생목록 table 을 감지하여 변경되면 자료 얻기
        viewModel = ViewModelProviders.of(this).get(PlaylistViewModel.class);

        viewModel.getPlayList().observe(this, playlists -> {
            List<PlayList> playListItems = new ArrayList<>(playlists);
            int pos = 1;
            //얻은 Playlist 목록이 Favorite 하나만 가지고 있는지 검사
            if (playListItems.size() < 1) {
                return;
            }
            // 목록에 '새 재생목록 추가...'항목을 위한 빈 재생목록 추가
            playListItems.add(pos, new PlayList(-1, "", new LinkedHashSet<>(), -1));
            mAdapter.submitList(playListItems);
        });

        mTheme = findViewById(R.id.theme);
        mTheme.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ThemeActivity.class)));

        mCircleRecyclerView = findViewById(R.id.circleRecyclerview);
        mItemViewMode = new ScaleXViewMode();
        mLayoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        // Recyclerview 에서 항목들을 움직이기 위한 snapHelper
        SnapHelper snapHelper = new SnapHelperOneByOne();
        mCircleRecyclerView.setLayoutManager(mLayoutManager);
        mCircleRecyclerView.setViewMode(mItemViewMode);
        //초점을 중심으로 설정
        mCircleRecyclerView.setNeedCenterForce(true);
        // Force to infinite loop
        mCircleRecyclerView.setNeedLoop(true);
        mCircleRecyclerView.setAdapter(new CarouselAdapter(this));
        snapHelper.attachToRecyclerView(mCircleRecyclerView);

        mSearch = findViewById(R.id.search);
        mSearch.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                startActivity(new Intent(getApplicationContext(), SearchActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            return true;
        });

        mRecyclerView = findViewById(R.id.recyclerview);
        mAdapter = new DrawerAdapter(R.layout.item_drawer);
        mAdapter.setContext(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @SuppressLint("CheckResult")
            @Override
            public void onItemClick(View view, int position) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(mContext, AllSongsActivity.class).putExtra("favourite", true));
                        break;
                    case 1:
                        DatabaseRepository
                                .getInstance()
                                .getAllPlaylist()
                                .compose(applySingleScheduler())
                                .subscribe(playLists -> {
                                    // 새 재생목록 생성을 위한 AlertDialog view 창조
                                    View v = getLayoutInflater().inflate(R.layout.dialog_new_playlist, null);
                                    TextView p = v.findViewById(R.id.confirm);
                                    TextView n = v.findViewById(R.id.cancel);
                                    EditText et = v.findViewById(R.id.title);
                                    et.setText(getString(R.string.local_list) + playLists.size());
                                    et.setFocusable(true);
                                    et.requestFocus();

                                    // 새 재생목록 생성을 위한 view 를 AlertDialog 에 설정
                                    AlertDialog mdialog = new AlertDialog.Builder(mContext, R.style.CustomDialog)
                                            .setView(v)
                                            .create();
                                    n.setOnClickListener(v1 -> {
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                                        mdialog.dismiss();
                                    });
                                    p.setOnClickListener(v1 -> {
                                        if (!TextUtils.isEmpty(et.getText())) {
                                            DatabaseRepository.getInstance()
                                                    .insertPlayList(et.getText().toString())
                                                    .compose(applySingleScheduler())
                                                    .subscribe(id -> SongChooseActivity.start(MusicPlayer.this, id, et.getText().toString()), throwable -> {
                                                        ToastUtil.show(mContext, R.string.create_playlist_fail, throwable.toString());
                                                    });
                                        }
                                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                                        mdialog.dismiss();
                                    });

                                    mdialog.getWindow().setSoftInputMode(
                                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                                    mdialog.show();

                                    //대화창의 gravity 를 bottom 으로 설정
                                    mdialog.getWindow().setGravity(Gravity.BOTTOM);
                                    // 대화창의 우 량쪽끝을 아로지게 변경
                                    mdialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.round_gray_top));
                                    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                                    layoutParams.copyFrom(mdialog.getWindow().getAttributes());
                                    layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                    // height 를 WRAP_CONTENT 로 설정
                                    layoutParams.height = -2;
                                    mdialog.getWindow().setAttributes(layoutParams);
                                });
                        break;
                    default:
                        PlayList playList = mAdapter.getItem(position);
                        ChildHolderActivity.start(mContext, Constants.PLAYLIST, playList.getId(), playList.getName());
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });

        if (SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false))
            MusicServiceRemote.setProgress(0);
    }

    @Override
    protected int getLoaderId() {
        return 0;
    }

    /**
     * 자료기지의 Playlist table 이 변경되였을때 호출되는 callback
     * @param name 변경된 playList table 이름
     */
    @Override
    public void onPlayListChanged(String name) {
        if (name.equals(PlayList.TABLE_NAME)) {
            onMediaStoreChanged();
        }
    }

    /**
     * 배경화면 갱신
     */
    @Override
    protected void onResume() {
        super.onResume();
        MaterialDialogHelper.setBackground(this, R.id.container);
    }

    /**
     * 중심에 배치된 view 를 얻는 함수
     * @return
     */
    @Override
    public View getCenterView() {
        return mCircleRecyclerView.findViewAtCenter();
    }

    /**
     * CircleRecyclerView instance 를 얻는 함수
     * @return CircleRecyclerView instance
     */
    @Override
    public CircleRecyclerView getCircleRecyclerView() {
        return mCircleRecyclerView;
    }

    /**
     * SnapHelper class
     */
    public class SnapHelperOneByOne extends LinearSnapHelper {

        @Override
        public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {

            if (!(layoutManager instanceof RecyclerView.SmoothScroller.ScrollVectorProvider)) {
                return RecyclerView.NO_POSITION;
            }

            final View currentView = findSnapView(layoutManager);

            if (currentView == null) {
                return RecyclerView.NO_POSITION;
            }

            return layoutManager.getPosition(currentView);
        }
    }

    /**
     * CircleRecyclerView 를 위한 Adapter
     */
    class CarouselAdapter extends RecyclerView.Adapter<VH> {
        private int[] resIds = {
                R.drawable.icon_setting,
                R.drawable.icon_allsongs,
                R.drawable.icon_folder,
                R.drawable.icon_effect
        };
        CircleRecyclerViewEvent event;

        public CarouselAdapter(CircleRecyclerViewEvent crve) {
            event = crve;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            VH h = new VH(LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.carousel_item, parent, false));
            return h;
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            int pos = position % 4;
            int resId = resIds[pos];
            holder.tv.setText(String.valueOf(pos));
            holder.iv.setImageDrawable(getDrawable(resId));

            holder.iv.setOnClickListener(v -> {
                int centerPos = Integer.parseInt(((TextView) event.getCenterView().findViewById(R.id.item_text)).getText().toString());
                int sidePos = Integer.parseInt(holder.tv.getText().toString());
                if (centerPos == sidePos) {
                    // FLAG_ACTIVITY_REORDER_TO_FRONT is for prevent to start multiple same activity for multiple click item
                    switch (pos) {
                        case 0:
                            startActivity(new Intent(getApplicationContext(), SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                            break;
                        case 1:
                            startActivity(new Intent(getApplicationContext(), AllSongsActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                            break;
                        case 2:
                            startActivity(new Intent(getApplicationContext(), AllSongsActivity.class).putExtra("position", 1).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                            break;
                        case 3:
                            startActivity(new Intent(getApplicationContext(), EffectActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                            break;
                    }
                    return;
                }

                event.getCircleRecyclerView().smoothScrollToView(holder.itemView);
            });
        }

        @Override
        public int getItemCount() {
            // For infinite loop
            return Integer.MAX_VALUE;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        ImageView iv;

        public VH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.item_text);
            iv = itemView.findViewById(R.id.item_img);
        }
    }
}
