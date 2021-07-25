package com.kr.musicplayer.ui.activity;

import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kr.musicplayer.R;
import com.kr.musicplayer.helper.EQHelper;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.ui.activity.base.BaseActivity;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.ToastUtil;

import me.khrystal.library.widget.CircleRecyclerView;
import me.khrystal.library.widget.ScaleXViewMode;

import static com.kr.musicplayer.util.SPUtil.SETTING_KEY.ENABLE_EQ;

/**
 * 장치 및 효과설정화면
 */
public class EffectActivity extends BaseActivity {
    CircleRecyclerView mEffect;
    CircleRecyclerView mPreset;
    SwitchCompat mToggleEffect;
    SwitchCompat mTogglePreset;

    ScaleXViewMode mItemViewMode;
    LinearLayoutManager mLayoutManagerEffect;
    LinearLayoutManager mLayoutManagerPreset;
    private Equalizer mEqualizer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_effect);

        //현재 Audio Effect Id 얻기
        int sessionId = MusicServiceRemote.getMediaPlayer().getAudioSessionId();
        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            Toast.makeText(this, this.getResources().getString(R.string.no_audio_ID), Toast.LENGTH_LONG).show();
        } else {
            mEqualizer = new Equalizer(0, sessionId);
        }

        // 음효과를 위한 CircleRecyclerView
        mEffect = findViewById(R.id.circleRecyclerviewEffect);
        // 장치를 위한 CircleRecyclerView
        mPreset = findViewById(R.id.circleRecyclerviewEqualizerPreset);
        mToggleEffect = findViewById(R.id.effect);
        mTogglePreset = findViewById(R.id.preset);

        if (mEqualizer != null) {
            boolean enabled = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, ENABLE_EQ, false);
            mEqualizer.setEnabled(enabled);
            mTogglePreset.setChecked(enabled);
        }

        mTogglePreset.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) mEqualizer.usePreset((short) 0);
            mEqualizer.setEnabled(isChecked);
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, ENABLE_EQ, isChecked);
        });

        mToggleEffect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String state;
            if (isChecked) {
                state = "Enabled";
            } else {
                state = "Disabled";
            }
            ToastUtil.show(this, "DTS is " + state);
        });

        mItemViewMode = new ScaleXViewMode();
        mLayoutManagerEffect = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        mLayoutManagerPreset = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);

        mEffect.setLayoutManager(mLayoutManagerEffect);
        mEffect.setViewMode(mItemViewMode);
        mEffect.setNeedCenterForce(true);
        mEffect.setNeedLoop(true);
        mEffect.setAdapter(new EffectAdapter(0));

        mPreset.setLayoutManager(mLayoutManagerPreset);
        mPreset.setViewMode(mItemViewMode);
        mPreset.setNeedCenterForce(true);
        mPreset.setNeedLoop(true);
        mPreset.setAdapter(new EffectAdapter(1));
        mPreset.setOnCenterItemClickListener(v -> {
            short pos = Short.parseShort(((TextView) v.findViewById(R.id.item_text)).getText().toString());
            if (pos == 10) {
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                startActivityForResult(audioEffectIntent, EQHelper.REQUEST_EQ);
                return;
            }
            if (mEqualizer != null) {
                if (!SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, ENABLE_EQ, false))
                    ToastUtil.show(this, R.string.enable_preset_recommend);
                else {
                    mEqualizer.usePreset(pos);
                    ToastUtil.show(this, "Success set " + mEqualizer.getPresetName(pos));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MaterialDialogHelper.setBackground(this, R.id.container_background);
    }

    class EffectAdapter extends RecyclerView.Adapter<VH> {
        int[][] resIds = {
                { R.drawable.device1, R.drawable.device2, R.drawable.device3, R.drawable.device4,
                        R.drawable.device5, R.drawable.device6 },
                { R.drawable.effect1, R.drawable.effect2, R.drawable.effect3, R.drawable.effect4,
                R.drawable.effect5, R.drawable.effect6, R.drawable.effect1, R.drawable.effect2,
                R.drawable.effect3, R.drawable.effect4, R.drawable.effect5 }
        };

        int[][] stringIds = {
                { R.string.standard_earphone, R.string.standard_headset, R.string.headset, R.string.earphone,
                        R.string.speaker, R.string.adjust },
                { R.string.normal, R.string.classical,
                        R.string.dance, R.string.flat, R.string.folk, R.string.heavy_metal,
                        R.string.hip_hop, R.string.jazz, R.string.pop, R.string.rock, R.string.effect_custom }
        };
        int mType;

        //장치 혹은 음효과 결정
        public EffectAdapter(int type) {
            mType = type % 2;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            VH h;
            h = new VH(LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.carousel_item_effect, parent, false));

            return h;
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            int pos = position % resIds[mType].length;
            int resId = resIds[mType][pos];
            holder.tv.setText(String.valueOf(pos));
            holder.iv.setImageDrawable(getDrawable(resId));
            holder.tvTitle.setText(stringIds[mType][pos]);
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        TextView tvTitle;
        ImageView iv;

        public VH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.item_text);
            tvTitle = itemView.findViewById(R.id.title);
            tvTitle.setVisibility(View.VISIBLE);

            iv = itemView.findViewById(R.id.item_img);
        }
    }
}
