package com.kr.musicplayer.ui.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kr.musicplayer.R;
import com.kr.musicplayer.helper.SleepTimer;
import com.kr.musicplayer.ui.dialog.TimeDialog;

/**
 * Sleep Timer 를 위한 Adapter
 */
public class SleepAdapter extends RecyclerView.Adapter<SleepAdapter.SleepViewHolder> {
    private Context mContext;
    private String[] times; // SleepTimer 사용자설정에 표시할 시간목록
    private MaterialDialog mDialog;

    public SleepAdapter(Context context, MaterialDialog dialog) {
        mContext = context;
        mDialog = dialog;
        times = context.getResources().getStringArray(R.array.sleep_times);
    }

    @NonNull
    @Override
    public SleepViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new SleepViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_times, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SleepViewHolder sleepViewHolder, int i) {
        sleepViewHolder.tv.setText(times[i]);
        TimeDialog td = TimeDialog.newInstance(mContext);

        sleepViewHolder.tv.setOnClickListener(v -> {
            // 초단위로 sleep time 계산
            int millis = (i + 1) % 6 * 15 * 60 * 1000;
            if (i == 4) {
                // Sleep Timer 대화창 현시
                td.show();
            } else if (millis > 0) {
                // 현재 sleep timer 가 진행중이면 취소
                if (SleepTimer.isTicking()) {
                    SleepTimer.cancelTimer();
                }
                SleepTimer.toggleTimer(millis);
            } else {
                SleepTimer.cancelTimer();
            }
            mDialog.dismiss();
        });
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    /**
     * Sleep Timer 대화창의 개별적인 시간항목들을 위한 viewHolder
     */
    static class SleepViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        public SleepViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.title);
        }
    }
}
