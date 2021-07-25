package com.kr.musicplayer.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Telephony;
import androidx.appcompat.widget.LinearLayoutCompat;

import android.view.Gravity;
import android.view.View;

import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 공유를 위한 대화창
 */
public class ShareDialog extends Dialog {

    @BindView(R.id.message)
    LinearLayoutCompat message;
    @BindView(R.id.bluetooth)
    LinearLayoutCompat bluetooth;

    Song song[] = null;
    Activity activity = null;

    public ShareDialog(Activity activity, Song[] song) {
        super(activity);
        this.activity = activity;
        this.song = song;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_transfer);
        getWindow().setBackgroundDrawable(activity.getDrawable(R.drawable.round_gray_top));
        getWindow().setLayout(-1, -2);
        getWindow().setGravity(Gravity.BOTTOM);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.message, R.id.bluetooth})
    public void onClick(View v) {
        if (song == null) return;

        ArrayList<Uri> files = new ArrayList<>();
        for(Song s : song) {
            Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI , Integer.toString(s.getId()));
            files.add(uri);
        }

        switch (v.getId()) {
            case R.id.message:
                // Set message app for use musics in it
                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                intent.setPackage(Telephony.Sms.getDefaultSmsPackage(getContext()));
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(intent);
                }
                break;
            case R.id.bluetooth:

                Intent sharingIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                sharingIntent.setType("*/*");
                // Set bluetooth app for use musics in it
                sharingIntent.setPackage("com.android.bluetooth");
                sharingIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sharingIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
                activity.startActivity(sharingIntent);
                break;
        }
        dismiss();
    }
}
