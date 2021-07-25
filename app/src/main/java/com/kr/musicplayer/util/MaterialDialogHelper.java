package com.kr.musicplayer.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import jp.wasabeef.glide.transformations.SupportRSBlurTransformation;

public class MaterialDialogHelper {
    public static int timeA = -1;// 반복시작 시간
    public static int timeB = -1;// 반복마감시간
    public static int favoriteCount = 0;
    public static String searchKey = "";// 검색칸에 입력한 검색문자렬
    public static int randomBackgroundId = -1;// 임의의 배경 id
    public static void adjustAlertDialog(MaterialDialog dialog, Drawable drawable) {
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setBackgroundDrawable(drawable);

        dialog.getWindow().setLayout(-1, -2);
    }

    /**
     * 00:00 형식의 초단위 시간 얻기
     * @param timeInSeconds long 형의 시간
     * @return 초단위 시간
     */
    public static String formatSeconds(long timeInSeconds) {
        long hours = timeInSeconds / 3600;
        long secondsLeft = timeInSeconds - hours * 3600;
        long minutes = secondsLeft / 60;
        long seconds = secondsLeft - minutes * 60;

        String formattedTime = "";
        if (hours < 10)
            formattedTime += "0";
        formattedTime += hours + ":";

        if (minutes < 10)
            formattedTime += "0";
        formattedTime += minutes + ":";

        if (seconds < 10)
            formattedTime += "0";
        formattedTime += seconds ;

        return formattedTime;
    }

    /**
     * 배경설정함수
     * @param activity 배경을 설정할 activity
     * @param resId 설정할 배경의 id
     */
    public static void setBackground(AppCompatActivity activity, int resId) {
        ImageView view = activity.findViewById(resId);
        RequestOptions ro = RequestOptions.bitmapTransform(new SupportRSBlurTransformation(15, 5));
        if (SPUtil.getValue(activity.getApplicationContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BACKGROUND_INTERNAL, true)) {
            // get internal image id from preference
            int bk = Util.mBks[SPUtil.getValue(
                    activity.getApplicationContext(),
                    SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.BACKGROUND_INTERNAL_ID,
                    0)];
            if (bk == Util.mBks[8]) {
                if (randomBackgroundId < 0) randomBackgroundId = (int) (Math.random() * 8);
                bk = Util.mBks[randomBackgroundId];
            }
            Glide.with(activity)
                    .load(bk)
                    .thumbnail(0.05f)
                    .apply(ro)
                    .into(view);
        } else {
            // get external image path from preference
            String path = SPUtil.getValue(
                    activity.getApplicationContext(),
                    SPUtil.SETTING_KEY.NAME,
                    SPUtil.SETTING_KEY.BACKGROUND_EXTERNAL,
                    "");
            if (path.isEmpty()) {
                Glide.with(activity)
                        .load(Util.mBks[0])
                        .thumbnail(0.05f)
                        .apply(ro)
                        .into(view);
                return;
            }
            Glide.with(activity)
                    .load(path)
                    .apply(ro)
                    .into(view);
        }
    }

    /**
     * 파일복사
     * @param sourceFile 원천파일
     * @param destFile 복사될 파일
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile, false).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    /**
     * Uri 로부터 파일경로를 얻는 함수
     * @param context The application context
     * @param uri 얻으려는 파일경로의 기초 uri
     * @return 파일경로
     */
    public static String getPath(final Context context, final Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    // get Data column's content from uri

    /**
     * 자료의 Column 값을 얻는 함수
     * @param context The application context
     * @param uri 얻으려는 값의 기초로되는 uri
     * @param selection 얻으려는 Column 값이 관한 selection
     * @param selectionArgs 얻으려는 Column 값이 관한 selectionArgs
     * @return Column 값
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * cache 삭제 함수
     * @param context The application context
     */
    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) { e.printStackTrace();}
    }

    // delete folder

    /**
     * 등록부 삭제 함수
     * @param dir 삭제하려는 등록부
     * @return 성과적으로 삭제되였으면 true 아니면 false
     */
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
