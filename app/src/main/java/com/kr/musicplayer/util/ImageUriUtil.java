package com.kr.musicplayer.util;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.kr.musicplayer.App;
import com.kr.musicplayer.bean.mp3.Artist;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.misc.cache.DiskCache;
import com.kr.musicplayer.request.ImageUriRequest;
import com.kr.musicplayer.request.UriRequest;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * 화상관련 함수들
 */

public class ImageUriUtil {

  private ImageUriUtil() {
  }

  private static final String TAG = "ImageUriUtil";

  /**
   * Album Cover 가 MediaCache 에 있는지 확인
   */
  public static boolean isAlbumThumbExistInMediaCache(Uri uri) {
    boolean exist = false;
    try (InputStream ignored = App.getContext().getContentResolver().openInputStream(uri)) {
      if (ignored != null)
        exist = true;
    } catch (Exception ignored) {
    }
    return exist;
  }

  /**
   * Album Cover 화상 얻기
   */
  public static File getCustomThumbIfExist(int id, int type) {
    File img = type == ImageUriRequest.URL_ALBUM ? new File(
        DiskCache.getDiskCacheDir(App.getContext(), "thumbnail/album") + "/" + Util
            .hashKeyForDisk(id + ""))
        : type == ImageUriRequest.URL_ARTIST ? new File(
            DiskCache.getDiskCacheDir(App.getContext(), "thumbnail/artist") + "/" + Util
                .hashKeyForDisk(id + ""))
            : new File(
                DiskCache.getDiskCacheDir(App.getContext(), "thumbnail/playlist") + "/" + Util
                    .hashKeyForDisk(id + ""));
    if (img.exists()) {
      return img;
    }
    return null;
  }

  /**
   * 예술가정보를 기초로 하여 매개 변수 구성
   */
  public static UriRequest getSearchRequest(Artist artist) {
    if (artist == null) {
      return UriRequest.DEFAULT_REQUEST;
    }
    return new UriRequest(artist.getArtistID(), ImageUriRequest.URL_ARTIST,
        UriRequest.TYPE_NETEASE_ARTIST, artist.getArtist());
  }

  /**
   * 노래정보를 기초로 하여 매개 변수 구성
   * @param song 기초로 되는 노래
   * @return UriRequest
   */
  public static UriRequest getSearchRequestWithAlbumType(Song song) {
    return new UriRequest(song.getAlbumId(),
        song.getId(),
        ImageUriRequest.URL_ALBUM,
        UriRequest.TYPE_NETEASE_SONG,
        song.getTitle(), song.getAlbum(), song.getArtist());
  }

  /**
   * 예술가이름이 unknown 혹은 없는지 판별하는 함수
   * @param artistName 판별할 예술가 이름
   * @return true 이면 없는것이고 false 이면 예술가이름이 존재
   */
  public static boolean isArtistNameUnknownOrEmpty(@Nullable String artistName) {
    if (TextUtils.isEmpty(artistName)) {
      return true;
    }
    artistName = artistName.trim().toLowerCase();
    return artistName.equals("unknown") || artistName.equals("<unknown>") || artistName
        .equals("未知艺术家");
  }

  /**
   * Album 이름이 unknown 혹은 없는지 판별하는 함수
   * @param albumName 판별할 Album 이름
   * @return true 이면 없는것이고 false 이면 Album 이름이 존재
   */
  public static boolean isAlbumNameUnknownOrEmpty(@Nullable String albumName) {
    if (TextUtils.isEmpty(albumName)) {
      return true;
    }
    albumName = albumName.trim().toLowerCase();
    return albumName.equals("unknown") || albumName.equals("<unknown>") || albumName.equals("未知专辑");
  }

  /**
   * 노래이름이 unknown 혹은 없는지 판별하는 함수
   * @param songName 판별할 노래이름
   * @return true 이면 없는것이고 false 이면 노래이름이 존재
   */
  public static boolean isSongNameUnknownOrEmpty(@Nullable String songName) {
    if (TextUtils.isEmpty(songName)) {
      return true;
    }
    songName = songName.trim().toLowerCase();
    return songName.equals("unknown") || songName.equals("<unknown>") || songName.equals("未知歌曲");
  }

  public enum ImageSize {
    SMALL, MEDIUM, LARGE, EXTRALARGE, MEGA, UNKNOWN
  }

  /**
   * 주어진 예술가 id 에 관한 uri 를 얻는 함수
   * @param artistId 예술가 id
   * @return 얻은 uri
   */
  public static String getArtistArt(int artistId) {
    Timber.v("ImageUriUtil getArtistArt is called");
    try (Cursor cursor = App.getContext().getContentResolver().query(
        MediaStore.Audio.Artists.Albums.getContentUri("external", artistId),
        null,
        null, null, null)) {
      if (cursor != null && cursor.getCount() > 0) {

        List<Integer> albumIds = new ArrayList<>();
        while (cursor.moveToNext()) {
          albumIds.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ID)));
        }
        for (Integer albumId : albumIds) {
          Uri uri = ContentUris
              .withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumId);
          if (ImageUriUtil.isAlbumThumbExistInMediaCache(uri)) {
            return uri.toString();
          }
        }
      }
    } catch (Exception e) {
      Timber.v(e);
    }
    return "";
  }
}
