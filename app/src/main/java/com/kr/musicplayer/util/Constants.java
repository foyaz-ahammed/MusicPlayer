package com.kr.musicplayer.util;

/**
 * 상수값
 */
public class Constants {

  //류형
  public final static int SONG = 0;
  public final static int ALBUM = 1;
  public final static int ARTIST = 2;
  public final static int FOLDER = 3;
  public final static int PLAYLIST = 4;
  public final static int PLAYLISTSONG = 5;

  public final static String ACTION_EXIT = "com.kr.music.EXIT";

  //재생방식
  public final static int MODE_LOOP = 1;
  public final static int MODE_SHUFFLE = 2;
  public final static int MODE_REPEAT = 3;


  //0: 프로그람잠금화면 1: 체계잠금화면 2: 닫기
  public final static int APLAYER_LOCKSCREEN = 0;
  public final static int SYSTEM_LOCKSCREEN = 1;
  public final static int CLOSE_LOCKSCREEN = 2;

}
