package com.kr.musicplayer.request;

import java.io.Serializable;

/**
 * Uri 관련 object
 */
public class UriRequest implements Serializable {

  private static final long serialVersionUID = 3225464659169043757L;
  public static final int TYPE_NETEASE_SONG = 1;
  public static final int TYPE_NETEASE_ALBUM = 10;
  public static final int TYPE_NETEASE_ARTIST = 100;

  public static final UriRequest DEFAULT_REQUEST = new UriRequest();

  private int mId;
  private int mSearchType; // Cover 검색형태 URL_ALBUM, URL_ARTIST, URL_PLAYLIST 중 하나
  private int mNeteaseType; // TYPE_NETEASE_SONG, TYPE_NETEASE_ARTIST 중 하나
  private String mTitle = ""; // 노래제목
  private String mAlbumName = ""; // Album 이름
  private String mArtistName = ""; // 예술가이름
  private int mSongId;

  public UriRequest() {
  }

  public UriRequest(int id, int searchType, int neteaseType) {
    this.mSearchType = searchType;
    this.mId = id;
    this.mNeteaseType = neteaseType;
  }

  public UriRequest(int id, int searchType, int neteaseType, String albumName, String artistName) {
    this.mId = id;
    this.mSearchType = searchType;
    this.mNeteaseType = neteaseType;
    this.mAlbumName = albumName;
    this.mArtistName = artistName;
  }

  public UriRequest(int id, int searchType, int neteaseType, String artistName) {
    this.mId = id;
    this.mSearchType = searchType;
    this.mNeteaseType = neteaseType;
    this.mArtistName = artistName;
  }

  public UriRequest(int id, int songId,int searchType, int neteaseType, String title, String albumName,
      String artistName) {
    this.mId = id;
    this.mSongId = songId;
    this.mSearchType = searchType;
    this.mNeteaseType = neteaseType;
    this.mTitle = title;
    this.mAlbumName = albumName;
    this.mArtistName = artistName;
  }

  public int getID() {
    return mId;
  }

  public void setId(int id) {
    mId = id;
  }

  public int getSearchType() {
    return mSearchType;
  }

  public String getTitle() {
    return mTitle;
  }

  public void setTitle(String title) {
    mTitle = title;
  }

  public int getSongId() {
    return mSongId;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UriRequest)) {
      return false;
    }

    UriRequest that = (UriRequest) o;

    if (mId != that.mId) {
      return false;
    }
    if (mSearchType != that.mSearchType) {
      return false;
    }
    if (mNeteaseType != that.mNeteaseType) {
      return false;
    }
    if (mTitle != null ? !mTitle.equals(that.mTitle) : that.mTitle != null) {
      return false;
    }
    if (mAlbumName != null ? !mAlbumName.equals(that.mAlbumName) : that.mAlbumName != null) {
      return false;
    }
    return mArtistName != null ? mArtistName.equals(that.mArtistName) : that.mArtistName == null;
  }

  @Override
  public int hashCode() {
    int result = mId;
    result = 31 * result + mSearchType;
    result = 31 * result + mNeteaseType;
    result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
    result = 31 * result + (mAlbumName != null ? mAlbumName.hashCode() : 0);
    result = 31 * result + (mArtistName != null ? mArtistName.hashCode() : 0);
    return result;
  }
}
