package com.kr.musicplayer.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kr.musicplayer.R;
import com.kr.musicplayer.ui.adapter.SleepAdapter;
import com.kr.musicplayer.ui.dialog.base.BaseDialog;
import com.kr.musicplayer.util.MaterialDialogHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 잠자기 설정을 위한 대화창
 */
public class SleepDialog extends BaseDialog {

    @BindView(R.id.times)
    RecyclerView times;

    public static SleepDialog newInstance() {
        SleepDialog timerDialog = new SleepDialog();
        return timerDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getContext())
                .customView(R.layout.dialog_sleep, false)
                .build();
        MaterialDialogHelper.adjustAlertDialog(dialog, getResources().getDrawable(R.drawable.round_gray_top));
        View root = dialog.getCustomView();
        ButterKnife.bind(this, root);

        SleepAdapter adapter = new SleepAdapter(getContext(), dialog);
        times.setLayoutManager(new LinearLayoutManager(getContext()));
        times.setAdapter(adapter);
        return dialog;
    }
}
