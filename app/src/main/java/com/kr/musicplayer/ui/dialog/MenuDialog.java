package com.kr.musicplayer.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kr.musicplayer.R;
import com.kr.musicplayer.db.room.model.PlayList;
import com.kr.musicplayer.ui.adapter.PopupAdapter;
import com.kr.musicplayer.ui.adapter.SleepAdapter;
import com.kr.musicplayer.ui.dialog.base.BaseDialog;
import com.kr.musicplayer.util.MaterialDialogHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 재생목록 편집을 위한 대화창
 */
public class MenuDialog extends BaseDialog {
    @BindView(R.id.times)
    RecyclerView times;

    PlayList mPlayList;

    public MenuDialog(PlayList playList) {
        mPlayList = playList;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .customView(R.layout.dialog_menu, false)
                .build();
        MaterialDialogHelper.adjustAlertDialog(dialog, getResources().getDrawable(R.drawable.round_gray_top));
        View root = dialog.getCustomView();
        ButterKnife.bind(this, root);

        RecyclerView.Adapter adapter;
        TextView tvTitle = root.findViewById(R.id.title);
        TextView tvCount = root.findViewById(R.id.count);
        tvTitle.setText(mPlayList.getName());
        int size = mPlayList.getAudioIds().size();
        String str = getString(R.string.total_count, size);
        tvCount.setText(str);
        adapter = new PopupAdapter(getContext(), mPlayList, dialog);
        times.setLayoutManager(new LinearLayoutManager(getContext()));
        times.setAdapter(adapter);
        return dialog;
    }
}
