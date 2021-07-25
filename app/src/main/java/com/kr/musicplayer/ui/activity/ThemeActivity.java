package com.kr.musicplayer.ui.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.kr.musicplayer.R;
import com.kr.musicplayer.util.MaterialDialogHelper;
import com.kr.musicplayer.util.SPUtil;
import com.kr.musicplayer.util.Util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import me.khrystal.library.widget.CircleRecyclerView;
import me.khrystal.library.widget.ItemViewMode;
import me.khrystal.library.widget.ScaleXViewMode;

import static com.kr.musicplayer.util.MaterialDialogHelper.copyFile;

/**
 * 화면양상설정화면
 */
public class ThemeActivity extends AppCompatActivity {
    public static int CAMERA = 1;
    public static int GALLERY = 2;
    private Uri cameraDestination;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme);

        CircleRecyclerView mCircleRecyclerView = findViewById(R.id.circleRecyclerview);
        mCircleRecyclerView.setAdapter(new CircleRecyclerViewAdapter(this));
        ItemViewMode mItemViewMode = new ScaleXViewMode();
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false);
        mCircleRecyclerView.setLayoutManager(mLayoutManager);
        mCircleRecyclerView.setViewMode(mItemViewMode);
        mCircleRecyclerView.setNeedCenterForce(true);
        mCircleRecyclerView.setNeedLoop(true);
        mCircleRecyclerView.setOnCenterItemClickListener(v -> {
            int index = Integer.parseInt(((TextView) v.findViewById(R.id.item_text)).getText().toString());
            Log.d("------------------", index + "");

            if (index == 9) {
                finish();
                return;
            }

            // 내부화상을 배경으로 리용하면 true로 설정
            SPUtil.putValue(getApplicationContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BACKGROUND_INTERNAL, true);
            // 배경으로 리용하려는 내부화상의 id 보관
            SPUtil.putValue(getApplicationContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BACKGROUND_INTERNAL_ID, index);
            // 배경으로 리용하려는 외부화상의 url
            SPUtil.putValue(getApplicationContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BACKGROUND_EXTERNAL, "");
            finish();
        });

        LinearLayout camera = findViewById(R.id.camera);
        camera.setOnClickListener(v -> {
            // 카메라로부터 화상 얻기
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis());
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.TITLE, timestamp);
            cameraDestination = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, cameraDestination), CAMERA);
        });

        LinearLayout gallery = findViewById(R.id.gallery);
        gallery.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, GALLERY);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String imageLocation = "";
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA) {
                imageLocation = cameraDestination.toString();
            } else if (requestCode == GALLERY) {
                imageLocation = data.getData().toString();
            }
            MaterialDialogHelper.deleteCache(this);
            File file = new File(MaterialDialogHelper.getPath(this, Uri.parse(imageLocation)));
            File dest = new File(String.format("%s/" + file.getName(), Glide.getPhotoCacheDir(this, "temp").getAbsolutePath(), file.getName()));
            try {
                copyFile(file, dest);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 외부화상이 배경으로 리용되므로 false 로 설정
            SPUtil.putValue(getApplicationContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BACKGROUND_INTERNAL, false);
            // 외부화상이 배경으로 리용되므로 -1 로 설정
            SPUtil.putValue(getApplicationContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BACKGROUND_INTERNAL_ID, -1);
            // 배경으로 리용되는 외부화상의 url 을 preference 에 보관
            SPUtil.putValue(getApplicationContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BACKGROUND_EXTERNAL, dest.getAbsolutePath());
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MaterialDialogHelper.setBackground(this, R.id.container_background);
    }

    class CircleRecyclerViewAdapter extends RecyclerView.Adapter<VH> {
        Activity activity;
        boolean flag;
        String path;
        public CircleRecyclerViewAdapter(Activity activity) {
            this.activity = activity;
            flag = SPUtil.getValue(
                    activity.getApplicationContext(),
                    SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.BACKGROUND_INTERNAL,
                    true);
            path = SPUtil.getValue(
                    activity.getApplicationContext(),
                    SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.BACKGROUND_EXTERNAL,
                    "");
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            VH h;
            h = new VH(LayoutInflater.from(getApplicationContext())
                    .inflate(R.layout.item_theme, parent, false));

            return h;
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            int extraPos; // 현재 설정된 배경까지의 offset
            int divider; // 배경 개수. 9 혹은 10. 10번째는 외부화상이다

            if (flag) {
                // 내부화상이 배경으로 리용될때
                extraPos = SPUtil.getValue(
                        activity.getApplicationContext(),
                        SPUtil.SETTING_KEY.NAME,
                        SPUtil.SETTING_KEY.BACKGROUND_INTERNAL_ID,
                        0);
                divider = 9;
            } else {
                // 외부화상이 배경으로 리용될때 (카메라 혹은 사진첩보기에서부터)
                extraPos = 6;
                divider = 10;
            }

            // 현재 화상의 위치 계산
            int pos = (position + extraPos + 2) % divider;
            if (pos < 9) {
                int resId = Util.mBks[pos];
                if (resId != Util.mBks[0]) {
                    // 선택한 화상을 배경으로 설정
                    Glide.with(activity)
                            .load(resId)
                            .apply(new RequestOptions().centerCrop())
                            .into(holder.iv);
                } else {
                    // 표준 검은 배경 설정
                    Glide.with(activity)
                            .load(R.drawable.bk_default)
                            .into(holder.iv);
                }
            } else {
                // 주어진 경로로부터 외부화상을 배경으로 설정
                Glide.with(activity)
                        .load(path)
                        .apply(new RequestOptions().centerCrop())
                        .into(holder.iv);
            }
            int visibility;
            int visibilityRandom;
            boolean isRandom = pos % 9 == 8; // 임의의 화상인지 결정
            boolean extra = pos == 9; // 배경이 외부로부터 불러들인것인지 결정

            // "표준양상"제목 현시 여부
            //
            if (pos == 0) {
                holder.tvDefault.setVisibility(View.VISIBLE);
            } else {
                holder.tvDefault.setVisibility(View.GONE);
            }

            // 화상이 배경으로 설정되였는지 나타내는 checkbox 상태 표시여부
            if (divider > 9) {
                if (extra) {
                    visibility = View.VISIBLE;
                } else {
                    visibility = View.GONE;
                }
            } else {
                if (extraPos == pos) {
                    visibility = View.VISIBLE;
                } else {
                    visibility = View.GONE;
                }
            }

            // Show or not 'Set random image for background' title
            //"자동으로 배경바꾸기" 제목 현시여부
            if (isRandom) {
                visibilityRandom = View.VISIBLE;
            } else {
                visibilityRandom = View.GONE;
            }

            holder.cvTitle.setVisibility(visibility);
            holder.tvRandom.setVisibility(visibilityRandom);
            holder.tv.setText(String.valueOf(pos));
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv;
        ImageView iv;
        CheckBox cvTitle;
        TextView tvRandom;
        TextView tvDefault;

        public VH(View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.item_text);
            cvTitle = itemView.findViewById(R.id.item_text_select);
            tvRandom = itemView.findViewById(R.id.item_text_random);
            tvDefault = itemView.findViewById(R.id.item_text_default);
            iv = itemView.findViewById(R.id.item_image);
        }
    }
}
