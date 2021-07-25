package com.kr.musicplayer.ui.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.db.room.DatabaseRepository;
import com.kr.musicplayer.helper.CloseEvent;
import com.kr.musicplayer.helper.SortOrder;
import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.ui.adapter.ChildHolderAdapter;
import com.kr.musicplayer.ui.viewmodel.PlayListSongsViewModel;
import com.kr.musicplayer.ui.viewmodel.PlayListSongsViewModelFactory;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.MediaStoreUtil;
import com.kr.musicplayer.util.MusicUtil;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;

import static com.kr.musicplayer.helper.MusicServiceRemote.setPlayQueue;
import static com.kr.musicplayer.request.network.RxUtil.applySingleScheduler;
import static com.kr.musicplayer.service.MusicService.EXTRA_POSITION;
import static com.kr.musicplayer.util.MusicUtil.makeCmdIntent;

/**
 * 즐겨찾기화면
 */
public class FavouriteFragment extends LibraryFragment<Song, ChildHolderAdapter> {

    @BindView(R.id.recyclerView)
    FastScrollRecyclerView mRecyclerView;
    @BindView(R.id.sort_alphabetical)
    LinearLayout sort;
    @BindView(R.id.play_random)
    LinearLayout random;
    @BindView(R.id.status)
    ConstraintLayout status;

    private CloseEvent event;
    private PlayListSongsViewModel listSongsViewModel;

    public static final String TAG = ArtistFragment.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listSongsViewModel = ViewModelProviders.of(this,
                new PlayListSongsViewModelFactory(
                        this.getActivity().getApplication(),
                        1))
                .get(PlayListSongsViewModel.class);
        listSongsViewModel.getIds().observe(this, ids -> {
            List<Song> songs = MediaStoreUtil.getSongsByIds(new ArrayList<>(ids.getAudioIds()));
            mAdapter.submitList(songs);
            ((TextView) random.findViewById(R.id.tv_random_count)).setText(getString(R.string.play_randomly, songs.size()));
            if (songs.size() > 0) {
                status.setVisibility(View.GONE);
            } else {
                status.setVisibility(View.VISIBLE);
            }
        });
        mPageName = TAG;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ((TextView) random.findViewById(R.id.tv_random_count)).setText(getString(R.string.play_randomly, 0));

        String sortType = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
                SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z);
        switch (sortType) {
            case SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z:
                ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically);
                ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_sort_white_24dp));
                break;
            case SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A:
                ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically_desc);
                ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_sort_white_24dp));
                break;
            case SortOrder.SongSortOrder.SONG_DATE:
                ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date);
                ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_timer_white_24dp));
                break;
            case SortOrder.SongSortOrder.SONG_DATE_DESC:
                ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date_desc);
                ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getContext().getDrawable(R.drawable.ic_timer_white_24dp));
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
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getActivity().getDrawable(R.drawable.ic_sort_white_24dp));
                        break;
                    }
                    case R.id.action_sort_order_date_asc: {
                        sortOrder = SortOrder.SongSortOrder.SONG_DATE;
                        ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date);
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getActivity().getDrawable(R.drawable.ic_timer_white_24dp));
                        break;
                    }
                    case R.id.action_sort_order_title_desc: {
                        sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A;
                        ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically_desc);
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getActivity().getDrawable(R.drawable.ic_sort_white_24dp));
                        break;
                    }
                    case R.id.action_sort_order_date_desc: {
                        sortOrder = SortOrder.SongSortOrder.SONG_DATE_DESC;
                        ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date_desc);
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getActivity().getDrawable(R.drawable.ic_timer_white_24dp));
                        break;
                    }
                }
                if (!TextUtils.isEmpty(sortOrder)) {
                    SPUtil.putValue(getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, sortOrder);
                    DatabaseRepository.getInstance()
                            .updatePlayListAudiosDate(1, System.currentTimeMillis())
                            .compose(applySingleScheduler())
                            .subscribe();
                }
                return true;
            });
            popupMenu.show();
        });
        mChoice.setExtra(1);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof CloseEvent) event = (CloseEvent) activity;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_artist;
    }

    /**
     * Adapter 초기화
     */
    @Override
    protected void initAdapter() {
        mAdapter = new ChildHolderAdapter(R.layout.item_song_recycle, Constants.PLAYLIST, getString(R.string.my_favorite), mChoice,
                mRecyclerView);
        getMultipleChoice().setEvent(event);

        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                boolean flag = !mChoice.click(position, mAdapter.getItem(position));
                if (getUserVisibleHint() && flag) {
                    //재생대기렬설정
                    final List<Song> songs = mAdapter.getCurrentList();
                    if (songs == null || songs.isEmpty()) {
                        return;
                    }
                    setPlayQueue(songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
                            .putExtra(EXTRA_POSITION, position));
                    mAdapter.notifyDataSetChanged();
                }
                if (event != null && !flag) event.closeListener();
            }

            @Override
            public void onItemLongClick(View view, int position) {
                if (getUserVisibleHint()) {
                    mChoice.longClick(position, mAdapter.getItem(position));
                    mAdapter.notifyDataSetChanged();
                    if (event != null) event.closeListener();
                }
            }
        });
    }

    /**
     * RecyclerView 초기화
     */
    @Override
    protected void initView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * 재생 및 중지상태가 변경되였을때 재생중인 노래 UI 에 반영
     */
    @Override
    public void onMetaChanged() {
        super.onMetaChanged();
        if (mAdapter != null) {
            mAdapter.updatePlayingSong();
        }
    }

    @Override
    public ChildHolderAdapter getAdapter() {
        return mAdapter;
    }

}
