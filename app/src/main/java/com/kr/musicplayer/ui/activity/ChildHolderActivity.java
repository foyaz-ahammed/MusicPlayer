package com.kr.musicplayer.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.db.room.DatabaseRepository;
import com.kr.musicplayer.db.room.model.PlayList;
import com.kr.musicplayer.helper.SortOrder;
import com.kr.musicplayer.misc.handler.MsgHandler;
import com.kr.musicplayer.misc.handler.OnHandleMessage;
import com.kr.musicplayer.misc.interfaces.LoaderIds;
import com.kr.musicplayer.misc.interfaces.OnItemClickListener;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.service.MusicService;
import com.kr.musicplayer.theme.ThemeStore;
import com.kr.musicplayer.ui.adapter.ChildHolderAdapter;
import com.kr.musicplayer.ui.misc.MultipleChoice;
import com.kr.musicplayer.ui.viewmodel.BaseViewModel;
import com.kr.musicplayer.ui.viewmodel.PlayListSongsViewModel;
import com.kr.musicplayer.ui.viewmodel.PlayListSongsViewModelFactory;
import com.kr.musicplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import com.kr.musicplayer.util.ColorUtil;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.MediaStoreUtil;
import com.kr.musicplayer.util.MusicUtil;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.SPUtil.SETTING_KEY;
import com.kr.musicplayer.util.ToastUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.kr.musicplayer.helper.MusicServiceRemote.setPlayQueue;
import static com.kr.musicplayer.request.network.RxUtil.applySingleScheduler;
import static com.kr.musicplayer.service.MusicService.EXTRA_POSITION;
import static com.kr.musicplayer.util.MusicUtil.makeCmdIntent;

/**
 * 자식등록부보기화면 및 재생목록화면
 */
public class ChildHolderActivity extends LibraryActivity<Song, ChildHolderAdapter> {

    public final static String TAG = ChildHolderActivity.class.getSimpleName();
    //노래 정보 목록의 매개 변수 가져 오기
    private int mId; // playList 혹은 등록부의 id
    private int mType; // 현재 화면이 Folder, Artist 혹은 PlayList 인지 확인할수 있는 형태
    private String mArg;

    //노래 및 제목수
    @BindView(R.id.favourite)
    ImageView mIcon;
    @BindView(R.id.childholder_item_num)
    TextView mNum;
    @BindView(R.id.child_holder_recyclerView)
    FastScrollRecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.textView_dialog)
    TextView textViewDialog;
    @BindView(R.id.search)
    EditText mSearch;
    @BindView(R.id.toggle_select)
    ImageView toggleSelect;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.sort_alphabetical)
    LinearLayout sort;
    @BindView(R.id.play_random)
    LinearLayout random;

    private String Title;

    //현재정렬순서
    private String mSortOrder;
    private MsgHandler mRefreshHandler;
    private boolean flagSelectAll = false;

    private BaseViewModel viewModel;
    private PlayListSongsViewModel listSongsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        ((TextView) random.findViewById(R.id.tv_random_count)).setText(getString(R.string.play_randomly, 0));

        mRefreshHandler = new MsgHandler(this);

        //재생목록 혹은 등록부의 mId 인지 얻기
        mId = getIntent().getIntExtra(EXTRA_ID, -1);
        // 현재 화면이 Folder, Artist 혹은 PlayList 인지 확인할수 있는 형태
        mType = getIntent().getIntExtra(EXTRA_TYPE, -1);
        mArg = getIntent().getStringExtra(EXTRA_TITLE);

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

        if (mType != Constants.PLAYLIST) {
            // 현재 화면이 재생목록화면이 아니면 MediaStore 감지
            viewModel = ViewModelProviders.of(this).get(BaseViewModel.class);
            viewModel.getSongs().observe(this, songs -> {
                mAdapter.submitList(songs);
                ((TextView) random.findViewById(R.id.tv_random_count)).setText(getString(R.string.play_randomly, songs.size()));
                togglePlaylistState(songs.size());
            });
            viewModel.loadSongs(mType, mId);
        }

        if (mType == Constants.PLAYLIST) {
            //현재 화면이 재생목록화면이면 Local DB 의 PlayList table 감지
            listSongsViewModel = ViewModelProviders.of(this, new PlayListSongsViewModelFactory(getApplication(), mId)).get(PlayListSongsViewModel.class);
            listSongsViewModel.getIds().observe(this, ids -> {
                String sortTemp = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
                        SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z);
                List<Song> songs = MediaStoreUtil.getSongsByIds(new ArrayList<>(ids.getAudioIds()));
                switch (sortTemp) {
                    case SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z:
                        songs.sort((o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()));
                        break;
                    case SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A:
                        songs.sort((o1, o2) -> o1.getDisplayName().compareTo(o2.getDisplayName()) * -1);
                        break;
                    case SortOrder.SongSortOrder.SONG_DATE:
                        songs.sort((o1, o2) -> (int) (o1.getAddTime() - o2.getAddTime()));
                        break;
                    case SortOrder.SongSortOrder.SONG_DATE_DESC:
                        songs.sort((o1, o2) -> (int) (o1.getAddTime() - o2.getAddTime()) * -1);
                        break;
                }
                mAdapter.submitList(songs);
                ((TextView) random.findViewById(R.id.tv_random_count)).setText(getString(R.string.play_randomly, songs.size()));
                togglePlaylistState(mAdapter.getItemCount());
            });
        }

        if (mId == -1 || mType == -1 || TextUtils.isEmpty(mArg)) {
            ToastUtil.show(this, R.string.illegal_arg);
            finish();
            return;
        }

        mChoice = new MultipleChoice<>(this,
                mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);

        mAdapter = new ChildHolderAdapter(R.layout.item_song_recycle, mType, mArg, mChoice,
                mRecyclerView);
        mChoice.setAdapter(mAdapter);
        mChoice.setExtra(mId);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (!mChoice.click(position, mAdapter.getItem(position))) {
                    final List<Song> songs = mAdapter.getCurrentList();
                    if (songs.size() == 0) {
                        return;
                    }
                    // RecyclerView 의 자료를 대기렬에 저장하고 선택한 노래를 재생
                    setPlayQueue(songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
                            .putExtra(EXTRA_POSITION, position));
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                mChoice.longClick(position, mAdapter.getItem(position));
                mAdapter.notifyDataSetChanged();
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        final int accentColor = ThemeStore.getAccentColor();
        mRecyclerView.setBubbleColor(accentColor);
        mRecyclerView.setHandleColor(accentColor);
        mRecyclerView.setBubbleTextColor(ColorUtil.getColor(R.color.light_text_color_primary));

        if (mType == Constants.PLAYLIST) {
            toggleSelect.setImageDrawable(getDrawable(R.drawable.ic_playlist_add));
        }

        //Album Cover
        if (mType != Constants.FOLDER) {
            if (mArg.contains("unknown")) {
                if (mType == Constants.ARTIST) {
                    Title = getString(R.string.unknown_artist);
                } else if (mType == Constants.ALBUM) {
                    Title = getString(R.string.unknown_album);
                }
            } else {
                Title = mArg;
            }
        } else {
            Title = mArg.substring(mArg.lastIndexOf("/") + 1);
        }
        //toolbar 초기화
        setUpToolbar(Title);
        title.setText(Title);
        mSearch.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startActivity(new Intent(getApplicationContext(), SearchActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
            return true;
        });

        // 임의로 재생
        random.setOnClickListener(v -> {
            if (mChoice.isActive()) mChoice.close();
            MusicUtil.makeCmdIntent(Command.NEXT, true);
            List<Song> mDatas = mAdapter.getCurrentList();
            if (mDatas == null || mDatas.isEmpty()) {
                ToastUtil.show(this, R.string.no_song);
                return;
            }

            setPlayQueue(mDatas, makeCmdIntent(Command.PLAYSELECTEDSONG)
                    .putExtra(EXTRA_POSITION, new Random().nextInt(mDatas.size())));
        });

        // 노래 정렬
        sort.setOnClickListener(v -> {
            Context wrapper = new ContextThemeWrapper(this, R.style.PopupMenu);
            PopupMenu popupMenu = new PopupMenu(wrapper, sort);
            popupMenu.inflate(R.menu.menu_sort);
            popupMenu.setOnMenuItemClickListener(item -> {
                String sortOrder = null;
                switch (item.getItemId()) {
                    case R.id.action_sort_order_title_asc: {
                        sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z;
                        ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically);
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getDrawable(R.drawable.ic_sort_white_24dp));
                        break;
                    }
                    case R.id.action_sort_order_date_asc: {
                        sortOrder = SortOrder.SongSortOrder.SONG_DATE;
                        ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date);
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getDrawable(R.drawable.ic_timer_white_24dp));
                        break;
                    }
                    case R.id.action_sort_order_title_desc: {
                        sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A;
                        ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.alphabetically_desc);
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getDrawable(R.drawable.ic_sort_white_24dp));
                        break;
                    }
                    case R.id.action_sort_order_date_desc: {
                        sortOrder = SortOrder.SongSortOrder.SONG_DATE_DESC;
                        ((TextView) sort.findViewById(R.id.sort_title)).setText(R.string.date_desc);
                        ((ImageView) sort.findViewById(R.id.sort_icon)).setImageDrawable(getDrawable(R.drawable.ic_timer_white_24dp));
                        break;
                    }
                }
                if (!TextUtils.isEmpty(sortOrder)) {
                    SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, sortOrder);
                    if (mType != Constants.PLAYLIST) {
                        viewModel.loadSongs(mType, mId);
                    } else {
                        // 노래가 재정렬되였다는것을 반영하기 위해 재생목록의 날자 변경
                        DatabaseRepository.getInstance()
                                .updatePlayListAudiosDate(mId, System.currentTimeMillis())
                                .compose(applySingleScheduler())
                                .subscribe();
                    }
                }
                return true;
            });
            popupMenu.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MaterialDialogHelper.setBackground(this, R.id.container_background);
    }

    @OnClick({R.id.toggle_select})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.toggle_select:
                if (mType != Constants.PLAYLIST) {
                    MultipleChoice<Song> multipleChoice = getMultipleChoice();
                    List<Song> data = mAdapter.getCurrentList();
                    if (!flagSelectAll) {
                        multipleChoice.selectAll(data);
                        toggleSelect.setImageDrawable(getDrawable(R.drawable.btn_cancel_normal));
                        flagSelectAll = true;
                    } else {
                        multipleChoice.close();
                        toggleSelect.setImageDrawable(getDrawable(R.drawable.ic_check_circle_24));
                        flagSelectAll = false;
                    }
                } else {
                    SongChooseActivity.start(this, mId, mArg);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mType == Constants.PLAYLIST) {
            mSortOrder = SPUtil
                    .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
                            SortOrder.PlayListSongSortOrder.SONG_A_Z);
        } else if (mType == Constants.ALBUM) {
            mSortOrder = SPUtil
                    .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER,
                            SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER);
        } else if (mType == Constants.ARTIST) {
            mSortOrder = SPUtil
                    .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
                            SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        } else {
            mSortOrder = SPUtil
                    .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
                            SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        }
        if (TextUtils.isEmpty(mSortOrder)) {
            return true;
        }
        setUpMenuItem(menu, mSortOrder);
        return true;
    }

    /**
     * 정렬순서보관
     * @param sortOrder 보관할 정렬순서
     */
    @Override
    protected void saveSortOrder(String sortOrder) {
        boolean update = false;
        if (mType == Constants.PLAYLIST) {
            //수동 정렬 또는 정렬 변경
            if (sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM) ||
                    !mSortOrder.equalsIgnoreCase(sortOrder)) {
                //수동정렬 선택됨
                if (!sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM)) {
                    update = true;
                }
            }
        } else {
            if (!mSortOrder.equalsIgnoreCase(sortOrder)) {
                update = true;
            }
        }
        if (mType == Constants.PLAYLIST) {
            SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
                    sortOrder);
        } else if (mType == Constants.ALBUM) {
            SPUtil
                    .putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, sortOrder);
        } else if (mType == Constants.ARTIST) {
            SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
                    sortOrder);
        } else {
            SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
                    sortOrder);
        }
        mSortOrder = sortOrder;
        if (update) {
            onMediaStoreChanged();
        }

    }

    /**
     * Menu Layout Id 를 얻는 함수
     * @return Menu Layout Id
     */
    @Override
    public int getMenuLayoutId() {
        return mType == Constants.PLAYLIST ? R.menu.menu_child_for_playlist :
                mType == Constants.ALBUM ? R.menu.menu_child_for_album :
                        mType == Constants.ARTIST ? R.menu.menu_child_for_artist : R.menu.menu_child_for_folder;
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.CHILDHOLDER_ACTIVITY;
    }

    /**
     * Service 에 접속되면 호출되는 Callback
     * @param service 접속된 Service
     */
    @Override
    public void onServiceConnected(@NotNull MusicService service) {
        super.onServiceConnected(service);
    }

    /**
     * 재생목록 혹은 재생대기렬이 변경되였을때 호출되는 Callback
     * @param name 재생목록 혹은 재생대기렬
     */
    @Override
    public void onPlayListChanged(String name) {
        super.onPlayListChanged(name);
        if (name.equals(PlayList.TABLE_NAME)) {
            onMediaStoreChanged();
        }
    }

    /**
     * MediaStore 가 갱신되였을때 호출되는 Callback
     */
    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();
    }

    @Override
    public void onTagChanged(@NotNull Song oldSong, @NotNull Song newSong) {
        super.onTagChanged(oldSong, newSong);
    }

    /**
     * 뒤로 가기 단추를 눌렀을때 현재 상태가 선택상태이면 선택화상 교체
     */
    @Override
    public void onBackPressed() {
        if (mChoice.isActive()) {
            mChoice.close();
            toggleSelect.setImageDrawable(getDrawable(R.drawable.ic_check_circle_24));
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRefreshHandler.remove();
    }

    /**
     * 노래가 없는 경우 노래없음 표시, 노래가 존재하면 노래없음 표시 없애기
     * @param size 노래개수
     */
    private void togglePlaylistState(int size) {
        if (size != 0) {
            mNum.setVisibility(View.GONE);
            mIcon.setVisibility(View.GONE);
            mNum.setText(getString(R.string.song_count, size));
        } else {
            mNum.setVisibility(View.VISIBLE);
            mIcon.setVisibility(View.VISIBLE);
            mNum.setText(getString(R.string.no_song));
        }
    }

    @OnHandleMessage
    public void handleInternal(Message msg) {
        switch (msg.what) {
            case MSG_RESET_MULTI:
                mAdapter.notifyDataSetChanged();
                break;
        }
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();
        if (mAdapter != null) {
            mAdapter.updatePlayingSong();
        }
    }

    private static final String EXTRA_ID = "id";
    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_TITLE = "title";

    public static void start(Context context, int type, int id, String title) {
        context.startActivity(new Intent(context, ChildHolderActivity.class)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TYPE, type)
                .putExtra(EXTRA_TITLE, title));
    }

}