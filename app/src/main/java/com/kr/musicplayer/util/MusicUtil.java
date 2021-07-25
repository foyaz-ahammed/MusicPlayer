package com.kr.musicplayer.util;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.kr.musicplayer.App;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.service.Command;
import com.kr.musicplayer.service.MusicService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.kr.musicplayer.helper.MusicServiceRemote.setPlayQueue;
import static com.kr.musicplayer.service.MusicService.EXTRA_CONTROL;
import static com.kr.musicplayer.service.MusicService.EXTRA_POSITION;
import static com.kr.musicplayer.service.MusicService.EXTRA_SHUFFLE;

public class MusicUtil {

  private static final String TAG = MusicUtil.class.getSimpleName();

  private MusicUtil() {
  }

  /**
   * 주어진 uri 에 관한 노래를 얻어 재생
   * @param uri 노래 관련 uri
   */
  public static void playFromUri(Uri uri) {
    List<Song> songs = null;
    if (uri.getScheme() != null && uri.getAuthority() != null) {
      if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
        String songId = null;
        switch (uri.getAuthority()) {
          case "com.android.providers.media.documents":
            songId = getSongIdFromMediaProvider(uri);
            break;
          case "com.android.browser.fileprovider":
            String fileProviderPath = uri.getPath();
            if (fileProviderPath != null && !TextUtils.isEmpty(fileProviderPath)) {
              songs = new ArrayList<>();
              if (fileProviderPath.startsWith("///")) {
                fileProviderPath = fileProviderPath.substring(2, fileProviderPath.length());
              }
              songs.add(MediaStoreUtil.getSongByUrl(fileProviderPath));
            }
            break;
          case "media":
            songId = uri.getLastPathSegment();
            break;
        }
        if (songId != null && TextUtils.isDigitsOnly(songId)) {
          songs = new ArrayList<>();
          songs.add(MediaStoreUtil.getSongById(Integer.valueOf(songId)));
        }
      }
    }

    if (songs == null) {
      File songFile = null;
      if (uri.getAuthority() != null && uri.getAuthority()
          .equals("com.android.externalstorage.documents")) {
        songFile = new File(Environment.getExternalStorageDirectory(),
            uri.getPath().split(":", 2)[1]);
      }
      if (songFile == null) {
        String path = getFilePathFromUri(App.getContext(), uri);
        if (path != null) {
          songFile = new File(path);
        }
      }
      if (songFile == null && uri.getPath() != null) {
        songFile = new File(uri.getPath());
      }
      if (songFile != null) {
        final Song song = MediaStoreUtil.getSongByUrl(songFile.getAbsolutePath());
        if (song != Song.getEMPTY_SONG()) {
          songs = new ArrayList<>();
          songs.add(song);
        }
      }
    }

    if (songs != null && !songs.isEmpty()) {
      setPlayQueue(songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
          .putExtra(EXTRA_POSITION, 0));
    } else {
      Timber.v("unknown uri");
    }
  }

  /**
   * 노래 Id 얻기
   * @param uri 노래 관련 uri
   * @return 얻어진 노래 Id
   */
  @TargetApi(Build.VERSION_CODES.KITKAT)
  private static String getSongIdFromMediaProvider(Uri uri) {
    return DocumentsContract.getDocumentId(uri).split(":")[1];
  }

  /**
   * 주어진 Uri 로부터 노래경로 얻기
   * @param context The application context
   * @param uri 노래 관련 uri
   * @return 노래경로
   */
  @Nullable
  private static String getFilePathFromUri(Context context, Uri uri) {
    final String column = "_data";
    final String[] projection = {
        column
    };

    try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null,
        null)) {
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndex(column);
        return cursor.getString(column_index);
      }
    } catch (Exception e) {
      Timber.v(e);
    }
    return null;
  }

  /**
   * Intent 생성
   */
  public static Intent makeCmdIntent(int cmd, boolean shuffle) {
    return new Intent(MusicService.ACTION_CMD).putExtra(EXTRA_CONTROL, cmd)
        .putExtra(EXTRA_SHUFFLE, shuffle);
  }

  /**
   * Intent 생성
   */
  public static Intent makeCmdIntent(int cmd) {
    return makeCmdIntent(cmd, false);
  }
}
