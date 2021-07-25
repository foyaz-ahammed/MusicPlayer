package com.kr.musicplayer.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Artist;
import com.kr.musicplayer.bean.mp3.Folder;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.db.room.DatabaseRepository;
import com.kr.musicplayer.helper.MusicServiceRemote;
import com.kr.musicplayer.helper.SortOrder;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import static com.kr.musicplayer.util.Util.TYPE_ALBUM;
import static com.kr.musicplayer.util.Util.TYPE_ARTIST;
import static com.kr.musicplayer.util.Util.TYPE_SONG;
import static com.kr.musicplayer.util.Util.hasStoragePermissions;
import static com.kr.musicplayer.util.Util.processInfo;

/**
 * MediaStore 자료기지 관련 함수들
 */
public class MediaStoreUtil {

  //검색파일기본크기설정
  public static int SCAN_SIZE;
  @SuppressLint("StaticFieldLeak")
  private static Context mContext;

  static {
    mContext = App.getContext();
  }

  private MediaStoreUtil() {
  }

  private static String BASE_SELECTION = " ";


  static {
    SCAN_SIZE = SPUtil
        .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCAN_SIZE,
            0);
  }

  /**
   * 모든 예술가목록을 얻는 함수
   * @return 얻어진 예술가목록
   */
  public static List<Artist> getAllArtist() {
    if (!hasStoragePermissions()) {
      return new ArrayList<>();
    }
    ArrayList<Artist> artists = new ArrayList<>();
    try (Cursor cursor = mContext.getContentResolver()
        .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            new String[]{MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST,
                "count(" + Media.ARTIST_ID + ")"},
            MediaStoreUtil.getBaseSelection() + ")" + " GROUP BY ("
                + MediaStore.Audio.Media.ARTIST_ID,
            null,
            SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
                SortOrder.ArtistSortOrder.ARTIST_A_Z))) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          try {
            artists.add(new Artist(cursor.getInt(0),
                cursor.getString(1),
                cursor.getInt(2)));
          } catch (Exception ignored) {

          }
        }
      }
    }

    return artists;
  }

  /**
   * 모든 노래 얻기
   * @return 얻어진 노래 목록
   */
  public static List<Song> getAllSong() {
    return getSongs(null,
        null,
        SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
            SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z));
  }

  /**
   * 검색문자렬에 관한 노래목록 얻기
   * @param key 검색문자렬
   * @return 얻어진 노래목록
   */
  public static List<Song> searchSong(String key) {
    if (TextUtils.isEmpty(key)) {
      return new ArrayList<>();
    }
    Cursor cursor = null;
    List<Song> songs = new ArrayList<>();
    try {
      String selection =
              MediaStore.Audio.Media.TITLE + " like ? " + "or " + MediaStore.Audio.Media.DISPLAY_NAME
                      + " like ? "
                      + " and " + MediaStoreUtil
                      .getBaseSelection();
      cursor = mContext.getContentResolver()
              .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                      null,
                      selection,
                      new String[]{"%" + key + "%", "%" + key + "%"}, null);

      if (cursor != null && cursor.getCount() > 0) {
        while (cursor.moveToNext()) {
          songs.add(MediaStoreUtil.getSongInfo(cursor));
        }
      }
    } finally {
      if (cursor != null && !cursor.isClosed()) {
        cursor.close();
      }
    }
    return songs;
  }

  /**
   * 최근에 추가된 노래목록 얻기
   * @return 얻어진 노래목록
   */
  public static List<Song> getLastAddedSong() {
    Calendar today = Calendar.getInstance();
    today.setTime(new Date());
    return getSongs(MediaStore.Audio.Media.DATE_ADDED + " >= ?",
        new String[]{String.valueOf((today.getTimeInMillis() / 1000 - (3600 * 24 * 7)))},
        MediaStore.Audio.Media.DATE_ADDED);
  }

  /**
   * 노래를 포함하는 모든 등록부 얻기
   * @return 얻어진 등록부목록
   */
  public static List<Folder> getAllFolder() {
    List<Folder> folders = new ArrayList<>();
    if (!hasStoragePermissions()) {
      return folders;
    }
    Map<Integer, List<String>> folderMap = new LinkedHashMap<>();

    try (Cursor cursor = mContext.getContentResolver()
        .query(MediaStore.Files.getContentUri("external"),
            null, getBaseSelection() + " and media_type = 2", null, null)) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          final String data = cursor
              .getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
          final int parentId = cursor
              .getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT));
          final String parentPath = data.substring(0, data.lastIndexOf("/"));

          if (!folderMap.containsKey(parentId)) {
            folderMap.put(parentId, new ArrayList<>(Collections.singletonList(parentPath)));
          } else {
            folderMap.get(parentId).add(parentPath);
          }
        }

        //转换
        for (Map.Entry<Integer, List<String>> entry : folderMap.entrySet()) {
          final String parentPath = entry.getValue().get(0);
          Folder folder = new Folder(
              parentPath.substring(parentPath.lastIndexOf("/") + 1),
              folderMap.get(entry.getKey()).size(),
              parentPath,
              entry.getKey());
          folders.add(folder);
        }

      }
    } catch (Exception e) {
      Timber.v(e);
    }

    return folders;
  }


  /**
   * 예술가 또는 Album Id를 기준으로 모든 노래 가져 오기
   *
   * @param id 예술가 Id 혹은 Album Id
   * @param type 1: Album 2: 예술가
   * @return 얻어진 노래 목록
   */
  public static List<Song> getSongsByArtistIdOrAlbumId(int id, int type) {
    String selection = null;
    String sortOrder = null;
    String[] selectionValues = null;
    if (type == Constants.ALBUM) {
      selection = MediaStore.Audio.Media.ALBUM_ID + "=?";
      selectionValues = new String[]{id + ""};
      sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_DISPLAY_TITLE_A_Z);
    }
    if (type == Constants.ARTIST) {
      selection = MediaStore.Audio.Media.ARTIST_ID + "=?";
      selectionValues = new String[]{id + ""};
      sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_DISPLAY_TITLE_A_Z);
    }
    return getSongs(selection, selectionValues, sortOrder);
  }

  /**
   * 주어진 Cursor 에 관한 노래정보 얻기
   *
   * @param cursor Cursor
   * @return 얻어진 노래 정보
   */
  @WorkerThread
  public static Song getSongInfo(Cursor cursor) {
    if (cursor == null || cursor.getColumnCount() <= 0) {
      return Song.Companion.getEMPTY_SONG();
    }

    long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
    final String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
    String[] tmp = data.split("/");
    final int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));

    Song song = new Song(
        id,
            tmp[tmp.length - 1],
        processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
            TYPE_SONG),
        processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
            TYPE_ALBUM),
        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)),
        processInfo(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
            TYPE_ARTIST),
        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)),
        duration,
        Util.getTime(duration),
        data,
        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)),
        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)),
        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE_KEY)),
        cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)),
            false);
    return song;
  }

  /**
   * 등록부에 속하는 노래 id 목록 얻기
   * @param parentId 등록부 Id
   * @return 노래 Id 목록
   */
  public static List<Integer> getSongIdsByParentId(int parentId) {
    List<Integer> ids = new ArrayList<>();
    try (Cursor cursor = mContext.getContentResolver()
        .query(MediaStore.Files.getContentUri("external"),
            new String[]{"_id"}, "parent = " + parentId, null, null)) {
      if (cursor != null) {
        while (cursor.moveToNext()) {
          ids.add(cursor.getInt(0));
        }
      }
    }
    return ids;
  }

  /**
   * 등록부에 속하는 노래목록 얻기
   * @param parentId 등록부 Id
   * @return 노래목록
   */
  public static List<Song> getSongsByParentId(int parentId) {
    List<Integer> ids = getSongIdsByParentId(parentId);

    if (ids.size() == 0) {
      return new ArrayList<>();
    }
    StringBuilder selection = new StringBuilder(127);
    selection.append(MediaStore.Audio.Media._ID + " in (");
    for (int i = 0; i < ids.size(); i++) {
      selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
    }
    return getSongs(selection.toString(), null, SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME,
        SPUtil.SETTING_KEY.SONG_SORT_ORDER,
        SortOrder.ChildHolderSongSortOrder.SONG_DISPLAY_TITLE_A_Z));
  }


  /**
   * Album Id 에 따라 노래 세부정보 얻기
   * @param albumId 기초로 되는 album id
   * @return 노래세부정보
   */
  public static Song getSongByAlbumId(int albumId) {
    return getSong(MediaStore.Audio.Media.ALBUM_ID + "=?", new String[]{albumId + ""});
  }

  /**
   * 노래  Id 에 따라 노래 세부정보 얻기
   *
   * @param id 노래 Id
   * @return 노래세부정보
   */
  public static Song getSongById(int id) {
    return getSong(MediaStore.Audio.Media._ID + "=?", new String[]{id + ""});
  }

  /**
   * 노래 id 목록에 관한 노래 목록을 얻는 함수
   * @param ids 얻으려는 노래 목록의 id 목록
   * @return 얻어진 노래 목록
   */
  public static List<Song> getSongsByIds(List<Integer> ids) {
    List<Song> songs = new ArrayList<>();
    if (ids == null || ids.isEmpty()) {
      return songs;
    }
    StringBuilder selection = new StringBuilder(127);
    selection.append(MediaStore.Audio.Media._ID + " in (");
    for (int i = 0; i < ids.size(); i++) {
      selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
    }

    return getSongs(selection.toString(), null);
  }

  /**
   * AlbumArt 삽입
   * @param context The application context
   * @param albumId 삽입하려는 album Id
   * @param path 경로
   */
  private static void insertAlbumArt(@NonNull Context context, int albumId, String path) {
    ContentResolver contentResolver = context.getContentResolver();

    Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
    contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null);

    ContentValues values = new ContentValues();
    values.put("album_id", albumId);
    values.put("_data", path);

    contentResolver.insert(artworkUri, values);
  }

  @WorkerThread
  public static void saveArtwork(Context context, int albumId, File artFile)
      throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException, CannotWriteException {
    Song song = MediaStoreUtil.getSongByAlbumId(albumId);
    if (song == null) {
      return;
    }
    AudioFile audioFile = AudioFileIO.read(new File(song.getUrl()));
    Tag tag = audioFile.getTagOrCreateAndSetDefault();
    Artwork artwork = ArtworkFactory.createArtworkFromFile(artFile);
    tag.deleteArtworkField();
    tag.setField(artwork);
    audioFile.commit();
    insertAlbumArt(context, albumId, artFile.getAbsolutePath());
  }

  /**
   * 노래삭제
   *
   * @param data 노래 Id 혹은 Album Id, 예술가 Id, 재생 목록 Id, parentId를 포함한 매개 변수 삭제
   * @param type 노래, Album, 예술가, 등록부, 재생목록을 포함한 류형삭제
   * @return 노래수
   */
  @Deprecated
  public static int delete(int data, int type, boolean deleteSource) {
    String where = null;
    String[] arg = null;

    //拼接参数
    switch (type) {
      case Constants.SONG:
        where = MediaStore.Audio.Media._ID + "=?";
        arg = new String[]{data + ""};
        break;
      case Constants.ALBUM:
      case Constants.ARTIST:
        if (type == Constants.ALBUM) {
          where = MediaStore.Audio.Media.ALBUM_ID + "=?";
          arg = new String[]{data + ""};
        } else {
          where = MediaStore.Audio.Media.ARTIST_ID + "=?";
          arg = new String[]{data + ""};
        }
        break;
      case Constants.FOLDER:
        List<Integer> ids = getSongIdsByParentId(data);
        StringBuilder selection = new StringBuilder(127);
//                for(int i = 0 ; i < ids.size();i++){
//                    selection.append(MediaStore.Audio.Media._ID).append(" = ").append(ids.get(i)).append(i != ids.size() - 1 ? " or " : " ");
//                }
        selection.append(MediaStore.Audio.Media._ID + " in (");
        for (int i = 0; i < ids.size(); i++) {
          selection.append(ids.get(i)).append(i == ids.size() - 1 ? ") " : ",");
        }
        where = selection.toString();
        arg = null;
        break;
    }

    return delete(getSongs(where, arg), deleteSource);
  }

  /**
   * 지정된 노래 삭제
   * @param songs 삭제할려는 노래 목록
   * @param deleteSource 원천삭제여부
   */
  @WorkerThread
  public static int delete(List<Song> songs, boolean deleteSource) {
    //원천 파일 저장 또는 삭제
    SPUtil.putValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE,
        deleteSource);
    if (songs == null || songs.size() == 0) {
      return 0;
    }

    //전에 보관된 노래 id 삭제
    Set<String> deleteId = new HashSet<>(
        SPUtil.getStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG));
    //保存到sp
    for (Song temp : songs) {
      if (temp != null) {
        deleteId.add(temp.getId() + "");
      }
    }
    SPUtil.putStringSet(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.BLACKLIST_SONG,
        deleteId);
    //재생대기렬과 모든 노래에서 삭제
    MusicServiceRemote.deleteFromService(songs);

    DatabaseRepository.getInstance().deleteFromAllPlayList(songs).subscribe();

    //원천 삭제
    if (deleteSource) {
      deleteSource(songs);
    }

    //Interface 갱신
    mContext.getContentResolver().notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null);

    return songs.size();
  }


  /**
   * 원천 삭제
   */
  public static void deleteSource(List<Song> songs) {
    if (songs == null || songs.size() == 0) {
      return;
    }
    for (Song song : songs) {
      mContext.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          MediaStore.Audio.Media._ID + "=?", new String[]{song.getId() + ""});
      Util.deleteFileSafely(new File(song.getUrl()));
    }
  }

  /**
   * 기초 조건문 얻기
   */
  public static String getBaseSelection() {
    return MediaStore.Audio.Media.SIZE + ">" + SCAN_SIZE;
  }

  /**
   * 주어진 url 에 관한 노래 얻기
   * @param url 얻으려는 노래의 url
   * @return 노래정보
   */
  public static Song getSongByUrl(String url) {
    return getSong(MediaStore.Audio.Media.DATA + " = ?", new String[]{url});
  }

  /**
   * 착신음 설정
   * @param context The application context
   * @param audioId 착신으로 설정할 노래 id
   */
  public static void setRing(Context context, int audioId) {
    try {
      ContentValues cv = new ContentValues();
      cv.put(MediaStore.Audio.Media.IS_RINGTONE, true);
      cv.put(MediaStore.Audio.Media.IS_NOTIFICATION, false);
      cv.put(MediaStore.Audio.Media.IS_ALARM, false);
      cv.put(MediaStore.Audio.Media.IS_MUSIC, false);
      // 착신음으로 설정할 노래로 착신음 library 갱신
      if (mContext.getContentResolver().update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cv,
          MediaStore.MediaColumns._ID + "=?", new String[]{audioId + ""}) >= 0) {
        Uri newUri = ContentUris
            .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId);
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri);
        ToastUtil.show(context, R.string.set_ringtone_success);
      } else {
        ToastUtil.show(context, R.string.set_ringtone_error);
      }
    } catch (Exception e) {
      //권한거부
      if (e instanceof SecurityException) {
        ToastUtil.show(context, R.string.please_give_write_settings_permission);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (!Settings.System.canWrite(mContext)) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + mContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Util.isIntentAvailable(mContext, intent)) {
              mContext.startActivity(intent);
            }
          }
        }
      }

    }
  }

  /**
   * 노래관련 cursor 생성
   * @param selection 조건문
   * @param selectionValues 선택할 마당들
   * @param sortOrder 순서
   * @return Cursor
   */
  @Nullable
  public static Cursor makeSongCursor(@Nullable String selection, final String[] selectionValues,
      final String sortOrder) {
    if (selection != null && !selection.trim().equals("")) {
      selection = getBaseSelection() + " AND " + selection;
    } else {
      selection = getBaseSelection();
    }
    String so = sortOrder;
    if (sortOrder != null && sortOrder.equals(SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z)) {
      so = sortOrder + " COLLATE NOCASE ASC";
      Log.d("---------------------------", sortOrder);
    }
    try {
      return mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          null, selection, selectionValues, so);
    } catch (SecurityException e) {
      return null;
    }
  }

  /**
   * 노래 Id 목록 얻기
   * @param selection 조건문
   * @param selectionValues 선택할 마당들
   * @return 노래 id 목록
   */
  public static List<Integer> getSongIds(@Nullable String selection, String[] selectionValues) {
    return getSongIds(selection, selectionValues, null);
  }

  /**
   * 노래 Id 목록 얻기
   * @param selection 조건문
   * @param selectionValues 선택할 마당들
   * @param sortOrder 순서
   * @return 노래 id 목록
   */
  public static List<Integer> getSongIds(@Nullable String selection, String[] selectionValues,
      String sortOrder) {
    if (!hasStoragePermissions()) {
      return new ArrayList<>();
    }

    List<Integer> ids = new ArrayList<>();
    try (Cursor cursor = makeSongCursor(selection, selectionValues, sortOrder)) {
      if (cursor != null && cursor.getCount() > 0) {
        while (cursor.moveToNext()) {
          ids.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
        }
      }
    } catch (Exception ignore) {

    }
    return ids;
  }

  /**
   * 노래 얻기
   * @param selection 조건문
   * @param selectionValues 선택할 마당들
   * @return 노래정보
   */
  public static Song getSong(@Nullable String selection, String[] selectionValues) {
    List<Song> songs = getSongs(selection, selectionValues, null);
    return songs != null && songs.size() > 0 ? songs.get(0) : Song.getEMPTY_SONG();
  }

  /**
   * 노래목록 얻는 함수
   * @param selection 조건문
   * @param selectionValues 선택할 마당들
   * @param sortOrder 순서
   * @return 노래목록
   */
  public static List<Song> getSongs(@Nullable String selection, String[] selectionValues,
      final String sortOrder) {
    if (!hasStoragePermissions()) {
      return new ArrayList<>();
    }

    List<Song> songs = new ArrayList<>();

    try (Cursor cursor = makeSongCursor(selection, selectionValues, sortOrder)) {
      if (cursor != null && cursor.getCount() > 0) {
        while (cursor.moveToNext()) {
          songs.add(getSongInfo(cursor));
        }
      }
    } catch (Exception ignore) {
      Timber.v(ignore);
    }
    return songs;
  }

  /**
   * 노래목록 얻는 함수
   * @param selection 조건문
   * @param selectionValues 선택할 마당들
   * @return 노래목록
   */
  public static List<Song> getSongs(@Nullable String selection, String[] selectionValues) {
    return getSongs(selection, selectionValues, SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z));
  }

  /**
   * 노래 개수 얻기
   */
  public static int getCount() {
    try (Cursor cursor = mContext.getContentResolver()
        .query(Media.EXTERNAL_CONTENT_URI, new String[]{Media._ID}, getBaseSelection(), null, null)) {
      if (cursor != null) {
        return cursor.getCount();
      }
    } catch (Exception e) {
      Timber.v(e);
    }

    return 0;
  }
}
