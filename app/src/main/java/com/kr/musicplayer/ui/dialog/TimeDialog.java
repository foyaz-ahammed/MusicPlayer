package com.kr.musicplayer.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.kr.musicplayer.R;
import com.kr.musicplayer.helper.SleepTimer;
import com.kr.musicplayer.ui.widget.HourMinutePicker;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 잠자기시간 설정을 위한 대화창
 */
public class TimeDialog extends Dialog {
    @BindView(R.id.time)
    HourMinutePicker timePicker;
    @BindView(R.id.text_remain)
    TextView remainTime;
    @BindView(R.id.cancel)
    TextView cancel;
    @BindView(R.id.confirm)
    TextView confirm;

    private Context context;
    private ToneGenerator toneGen1; // 시간설정에서 시간이 바뀌였을때 내는 소리
    private long sleepTime = 0; // 잠자기 시간
    private boolean isFirst = true;

    public TimeDialog(@NonNull Context context) {
        super(context);
        this.context = context;
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    }

    public static TimeDialog newInstance(Context context) {
        return new TimeDialog(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_time);
        ButterKnife.bind(this);
        getWindow().setBackgroundDrawable(context.getDrawable(R.drawable.round_gray_top));
        getWindow().setLayout(-1, -2);
        getWindow().setGravity(Gravity.BOTTOM);
        toggleConfirm();
        timePicker.setIs24HourView(true);
        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            String h, m;
            if (hourOfDay < 10) {
                h = "0" + hourOfDay;
            } else {
                h = String.valueOf(hourOfDay);
            }
            if (minute < 10) {
                m = "0" + minute;
            } else {
                m = String.valueOf(minute);
            }
            remainTime.setText(h + ":" + m);
            sleepTime = hourOfDay * 3600 + minute * 60;
            if(!isFirst) toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
        });

        int time = (int) (SleepTimer.getMillisUntilFinish() / 1000);
        int hour = time / 3600;
        int min = (time - hour * 3600) / 60;
        timePicker.setCurrentHour(hour);
        timePicker.setCurrentMinute(min);
        isFirst = false;
    }

    @OnClick({R.id.confirm, R.id.cancel})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirm:
                if (sleepTime <= 0) return;
                SleepTimer.toggleTimer(sleepTime * 1000);
                toggleConfirm();
                break;
            case R.id.cancel:
                break;
        }
        dismiss();
    }

    /**
     * SleepTimer 상태에 따라 문자렬 변경
     */
    private void toggleConfirm() {
        if (SleepTimer.isTicking()) {
            confirm.setText(R.string.stop);
        } else {
            confirm.setText(R.string.confirm);
        }
    }

}
