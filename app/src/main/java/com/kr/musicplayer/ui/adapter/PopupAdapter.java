package com.kr.musicplayer.ui.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.db.room.DatabaseRepository;
import com.kr.musicplayer.db.room.model.PlayList;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.service.MusicService;
import com.kr.musicplayer.ui.activity.ChildHolderActivity;
import com.kr.musicplayer.ui.activity.MusicPlayer;
import com.kr.musicplayer.ui.activity.PlayerActivity;
import com.kr.musicplayer.ui.activity.SongChooseActivity;
import com.kr.musicplayer.util.Constants;
import com.kr.musicplayer.util.MediaStoreUtil;
import com.kr.musicplayer.util.MusicUtil;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import static com.kr.musicplayer.request.network.RxUtil.applySingleScheduler;

/**
 * 재생목록항목의 popup 창을 위한 Adapter
 */
public class PopupAdapter extends RecyclerView.Adapter<PopupAdapter.MenuViewHolder> {
    private Context mContext;
    private int[] times = {R.string.play, R.string.add, R.string.rename, R.string.delete};
    private int[] icons = {R.drawable.ic_play, R.drawable.ic_playlist_add, R.drawable.ic_edit, R.drawable.ic_delete};
    private PlayList mPlayList; // 현재 현시된 popup 창과 관련된 재생목록 Object
    private MaterialDialog mDialog;

    public PopupAdapter(Context context, PlayList playList, MaterialDialog dialog) {
        mContext = context;
        mPlayList = playList;
        mDialog = dialog;
    }

    @NonNull
    @Override
    public MenuViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MenuViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_menu, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MenuViewHolder holder, int position) {
        if (mPlayList.getId() == 1 && (position > 1)) {
            holder.tv.setVisibility(View.GONE);
            return;
        }
        holder.iv.setImageResource(icons[position]);
        holder.tv.setText(times[position]);
        holder.root.setOnClickListener(v0 -> {
            switch (position) {
                case 0:
                    DatabaseRepository
                            .getInstance()
                            .getPlayList(mPlayList.getId())
                            .compose(applySingleScheduler())
                            .subscribe(
                                    playlist -> {
                                        List<Song> songs = MediaStoreUtil.getSongsByIds(
                                                new ArrayList<>(playlist.getAudioIds())
                                        );
                                        if (songs.size() < 1) {
                                            ToastUtil.show(mContext, mContext.getString(R.string.no_song));
                                            return;
                                        }
                                        // Save Selected Playlist's id and type for observe Queue
                                        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE, mPlayList.getId());
                                        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_QUEUE_TYPE, Constants.PLAYLIST);
                                        MusicServiceRemote
                                                .setPlayQueue(
                                                        songs,
                                                        MusicUtil
                                                                .makeCmdIntent(Command.PLAYSELECTEDSONG)
                                                                .putExtra(MusicService.EXTRA_POSITION, 0)
                                                );
                                        mContext.startActivity(new Intent(mContext, PlayerActivity.class));
                                    }
                            );
                    break;
                case 1:
                    SongChooseActivity.start((Activity) mContext, mPlayList.getId(), mPlayList.getName());
                    break;
                case 2:
                    DatabaseRepository
                            .getInstance()
                            .getAllPlaylist()
                            .compose(applySingleScheduler())
                            .subscribe(playLists -> {
                                // Set custom view for dialog
                                View v = ((Activity) mContext).getLayoutInflater().inflate(R.layout.dialog_new_playlist, null);
                                TextView p = v.findViewById(R.id.confirm);
                                TextView n = v.findViewById(R.id.cancel);
                                EditText et = v.findViewById(R.id.title);
                                et.setText(mPlayList.getName());
                                et.setFocusable(true);
                                et.requestFocus();

                                AlertDialog mdialog = new AlertDialog.Builder(mContext, R.style.CustomDialog)
                                        .setView(v)
                                        .create();
                                n.setOnClickListener(v1 -> {
                                    InputMethodManager imm = (InputMethodManager) ((Activity) mContext).getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                                    mdialog.dismiss();
                                });
                                p.setOnClickListener(v1 -> {
                                    if (!TextUtils.isEmpty(et.getText())) {
                                        DatabaseRepository.getInstance()
                                                .updatePlayListName(mPlayList.getId(), et.getText().toString())
                                                .compose(applySingleScheduler())
                                                .subscribe(id -> ToastUtil.show(mContext, R.string.add_playlist_success), throwable -> {
                                                    ToastUtil.show(mContext, R.string.create_playlist_fail, throwable.toString());
                                                });
                                    }
                                    InputMethodManager imm = (InputMethodManager) ((Activity) mContext).getSystemService(Context.INPUT_METHOD_SERVICE);
                                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                                    mdialog.dismiss();
                                });

                                // 대화창을 보여줄때 입력건반도 함께 현시
                                mdialog.getWindow().setSoftInputMode(
                                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                                mdialog.show();

                                //대화창의 gravity 를 bottom 으로 설정하고 배경을 웃부분이 아로지게 설정
                                mdialog.getWindow().setGravity(Gravity.BOTTOM);
                                mdialog.getWindow().setBackgroundDrawable(mContext.getDrawable(R.drawable.round_gray_top));
                                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                                layoutParams.copyFrom(mdialog.getWindow().getAttributes());
                                layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                                layoutParams.height = -2;
                                mdialog.getWindow().setAttributes(layoutParams);
                            });
                    break;
                case 3:
                    View v = ((Activity) mContext).getLayoutInflater().inflate(R.layout.dialog_delete, null);
                    TextView p = v.findViewById(R.id.confirm);
                    TextView n = v.findViewById(R.id.cancel);
                    TextView title = v.findViewById(R.id.title);
                    title.setText(R.string.confirm_delete_playlist);
                    AlertDialog mdialog = new AlertDialog.Builder(mContext, R.style.CustomDialog)
                            .setView(v)
                            .create();
                    n.setOnClickListener(v1 -> {
                        mdialog.dismiss();
                    });
                    p.setOnClickListener(v1 -> {
                        new DeletePlayListAsyncTask().execute(mPlayList.getId());
                        mdialog.dismiss();
                    });
                    mdialog.show();

                    //대화창의 gravity 를 bottom 으로 설정하고 배경을 웃부분이 아로지게 설정
                    mdialog.getWindow().setGravity(Gravity.BOTTOM);
                    mdialog.getWindow().setBackgroundDrawable(mContext.getDrawable(R.drawable.round_gray_top));
                    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                    layoutParams.copyFrom(mdialog.getWindow().getAttributes());
                    layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT;
                    layoutParams.height = -2;
                    mdialog.getWindow().setAttributes(layoutParams);
                    break;
                default:
                    ChildHolderActivity.start(mContext, Constants.PLAYLIST, mPlayList.getId(), mPlayList.getName());
            }
            mDialog.dismiss();
        });
    }

    @Override
    public int getItemCount() {
        return times.length;
    }

    /**
     * 재생목록항목을 삭제하기 위한 AsyncTask
     */
    private class DeletePlayListAsyncTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... integers) {
            DatabaseRepository.getInstance().deletePlayList(integers[0]).subscribe();
            return null;
        }
    }

    /**
     * Popup 창의 개별적인 항목을 위한 viewHolder
     */
    static class MenuViewHolder extends RecyclerView.ViewHolder {
        View root;
        TextView tv;
        ImageView iv;

        public MenuViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            tv = itemView.findViewById(R.id.title);
            iv = itemView.findViewById(R.id.icon);
        }
    }
}
