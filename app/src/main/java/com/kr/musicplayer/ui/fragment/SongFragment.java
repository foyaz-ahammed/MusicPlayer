package com.kr.musicplayer.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.helper.CloseEvent;
import com.kr.musicplayer.helper.SortOrder;
import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.ui.adapter.SongAdapter;
import com.kr.musicplayer.ui.adapter.holder.ItemMoveCallback;
import com.kr.musicplayer.ui.viewmodel.BaseViewModel;
import com.kr.musicplayer.ui.viewmodel.PlayQueueSongsViewModel;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.LocationRecyclerView;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.DensityUtil;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.MusicUtil;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.ToastUtil;

import java.util.List;
import java.util.Random;

import butterknife.BindView;

import static com.kr.musicplayer.helper.MusicServiceRemote.setPlayQueue;
import static com.kr.musicplayer.service.MusicService.EXTRA_POSITION;
import static com.kr.musicplayer.util.MusicUtil.makeCmdIntent;

/**
 * 모든 노래 및 재생대기렬화면
 */
public class SongFragment extends LibraryFragment<Song, SongAdapter> {

    @BindView(R.id.location_recyclerView)
    LocationRecyclerView mRecyclerView;
    @BindView(R.id.textView_dialog)
    TextView textView_dialog;
    @BindView(R.id.sort_alphabetical)
    LinearLayout sort;
    @BindView(R.id.play_random)
    LinearLayout random;

    private CloseEvent event;
    private boolean fromPlayerActivity = false; // 재생화면에서 호출되였는지 판별
    public PlayQueueSongsViewModel queueSongsViewModel;

    public SongFragment() {
        this.fromPlayerActivity = false;
    }

    public SongFragment(boolean flag) {
        this.fromPlayerActivity = flag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof CloseEvent) event = (CloseEvent) activity;
    }

    public static final String TAG = SongFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!fromPlayerActivity) {
            viewModel = ViewModelProviders.of(this).get(BaseViewModel.class);
            viewModel.getSongs().observe(this, songs -> {
                mAdapter.submitList(songs);
                ((TextView) random.findViewById(R.id.tv_random_count)).setText(getString(R.string.play_randomly, songs.size()));
            });
            viewModel.loadSongs(Constants.SONG, -1);
        } else {
            queueSongsViewModel = new ViewModelProvider(this).get(PlayQueueSongsViewModel.class);
            queueSongsViewModel.getAllQueueSongs();
            queueSongsViewModel.getQueueSongs().observe(this, songs -> mAdapter.submitList(songs));
        }
        mPageName = TAG;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (!fromPlayerActivity) {
            mRecyclerView.setClipToPadding(false);
            mRecyclerView.setPadding(0, 0, 0, DensityUtil.dip2px(60f));
            ((TextView) random.findViewById(R.id.tv_random_count)).setText(getString(R.string.play_randomly, 0));

            // 정렬방식에 따라 제목과 아이콘 변경
            String sortType = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
                    SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z);
            switch (sortType) {
                case SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z:
                    ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically);
                    ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(App.getContext().getDrawable(R.drawable.ic_sort_white_24dp));
                    break;
                case SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A:
                    ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically_desc);
                    ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(App.getContext().getDrawable(R.drawable.ic_sort_white_24dp));
                    break;
                case SortOrder.SongSortOrder.SONG_DATE:
                    ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date);
                    ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(App.getContext().getDrawable(R.drawable.ic_timer_white_24dp));
                    break;
                case SortOrder.SongSortOrder.SONG_DATE_DESC:
                    ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date_desc);
                    ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(App.getContext().getDrawable(R.drawable.ic_timer_white_24dp));
                    break;
            }

            random.setOnClickListener(v -> {
                if (mChoice.isActive()) mChoice.close();
                MusicUtil.makeCmdIntent(Command.NEXT, true);
                List<Song> mDatas = mAdapter.getCurrentList();
                if (mDatas == null || mDatas.isEmpty()) {
                    ToastUtil.show(getContext(), R.string.no_song);
                    return;
                }

                setPlayQueue(mDatas, makeCmdIntent(Command.PLAYSELECTEDSONG)
                        .putExtra(EXTRA_POSITION, new Random().nextInt(mDatas.size())));
            });

            sort.setOnClickListener(v -> {
                Context wrapper = new ContextThemeWrapper(getActivity(), R.style.PopupMenu);
                PopupMenu popupMenu = new PopupMenu(wrapper, sort);
                popupMenu.inflate(R.menu.menu_sort);
                popupMenu.setOnMenuItemClickListener(item -> {
                    String sortOrder = null;
                    switch (item.getItemId()) {
                        case R.id.action_sort_order_title_asc: {
                            sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z;
                            ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically);
                            ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_sort_white_24dp));
                            break;
                        }
                        case R.id.action_sort_order_date_asc: {
                            sortOrder = SortOrder.SongSortOrder.SONG_DATE;
                            ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date);
                            ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_timer_white_24dp));
                            break;
                        }
                        case R.id.action_sort_order_title_desc: {
                            sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A;
                            ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically_desc);
                            ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_sort_white_24dp));
                            break;
                        }
                        case R.id.action_sort_order_date_desc: {
                            sortOrder = SortOrder.SongSortOrder.SONG_DATE_DESC;
                            ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date_desc);
                            ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_timer_white_24dp));
                            break;
                        }
                    }
                    // 순서가 변경되였을때 viewModel 로부터 자료 다시 얻기
                    if (!TextUtils.isEmpty(sortOrder)) {
                        SPUtil.putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, sortOrder);
                        if (!fromPlayerActivity) {
                            viewModel.loadSongs(Constants.SONG, -1);
                        } else {
                            queueSongsViewModel.getAllQueueSongs();
                        }
                    }
                    return true;
                });
                popupMenu.show();
            });
        } else {
            view.findViewById(R.id.sort_container).setVisibility(View.GONE);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mRecyclerView.getLayoutParams();
            layoutParams.topMargin = 0;
            mRecyclerView.setLayoutParams(layoutParams);
        }
        return view;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_song;
    }

    /**
     * Adapter 초기화
     */
    @Override
    protected void initAdapter() {
        mAdapter = new SongAdapter(R.layout.item_song_recycle, mChoice, mRecyclerView, this);

        if (fromPlayerActivity) {
            ItemTouchHelper.Callback callback = new ItemMoveCallback(mAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(mRecyclerView);
            mAdapter.setItemTouchHelper(touchHelper);
        }

        getMultipleChoice().setEvent(event);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                boolean flagClick = !mChoice.click(position, mAdapter.getItem(position));
                if (getUserVisibleHint() && flagClick) {
                    //재생대기렬설정
                    final List<Song> songs = mAdapter.getCurrentList();
                    if (songs == null || songs.isEmpty()) {
                        return;
                    }
                    if (MaterialDialogHelper.timeA > -1) {
                        ToastUtil.show(getContext(), R.string.stop_repeat);
                        return;
                    }
                    // 이 Fragment가 재생화면의 재생대기렬을 위한 Fragment 인 경우 대기렬형태(PlayList, Artist, Folder) 및 id 보관
                    if (!SongFragment.this.fromPlayerActivity) {
                        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE_TYPE, 0);
                        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE, -1);
                    }
                    // 선택한 노래 재생
                    setPlayQueue(songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
                            .putExtra(EXTRA_POSITION, position));
                }
                if (event != null && !flagClick) event.closeListener();
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (getUserVisibleHint()) {
                    mChoice.longClick(position, mAdapter.getItem(position));
                    mAdapter.notifyDataSetChanged();
                }
                if (event != null) event.closeListener();
            }
        });
    }

    /**
     * RecyclerView 초기화
     */
    @Override
    protected void initView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.getItemAnimator().setMoveDuration(100);
    }

    /**
     * Adapter 얻기
     * @return 얻어진 Adapter
     */
    @Override
    public SongAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();
        Log.d("-----------", "SongFragment:onMetaChanged");
        if (mAdapter != null) {
            mAdapter.updatePlayingSong();
        }
    }

    /**
     * 현재 재생되는 노래의 위치로 이동
     */
    public void scrollToCurrent() {
        mRecyclerView.smoothScrollToCurrentSong(mAdapter.getCurrentList());
    }
}
