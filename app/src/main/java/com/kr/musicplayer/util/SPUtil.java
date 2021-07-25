package com.kr.musicplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kr.musicplayer.bean.misc.LyricPriority;

/**
 * SharedPrefs 관련 클라스
 */
public class SPUtil {

  private static SPUtil mInstance;

  public SPUtil() {
    if (mInstance == null) {
      mInstance = this;
    }
  }

  /**
   * SharedPreferences 에 주어진 key 에 관한 StringSet 넣는 함수
   * @param context The application context
   * @param name 보관될 preference 파일
   * @param key 수정할 기본설정의 이름
   * @param set 새로 추가할 값들
   */
  public static void putStringSet(Context context, String name, String key, Set<String> set) {
    if (set == null) {
      return;
    }
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.remove(key);
    editor.putStringSet(key, set).apply();
  }

  /**
   * SharedPreference 에서 String set 를 얻는 함수
   * @param context The application context
   * @param name preference 파일
   * @param key 수정할 기본설정의 이름
   * @return String set 값
   */
  public static Set<String> getStringSet(Context context, String name, String key) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .getStringSet(key, new HashSet<>());
  }

  /**
   * SharedPreference 에 Int 형 값을 넣는 함수
   * @param context The application context
   * @param name preference 파일
   * @param key 수정할 기본설정의 이름
   * @param value 넣을 값
   */
  public static void putValue(Context context, String name, String key, int value) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.putInt(key, value).apply();
  }

  /**
   * SharedPreference 에 String 형 값을 넣는 함수
   * @param context The application context
   * @param name preference 파일
   * @param key 수정할 기본설정의 값
   * @param value 넣을 값
   */
  public static void putValue(Context context, String name, String key, String value) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();
    editor.putString(key, value).apply();
  }

  /**
   * SharedPreference 에 boolean 형 값을 넣는 함수
   * @param context The application context
   * @param name preference 파일
   * @param key 수정할 기본설정의 이름
   * @param value 넣을 값
   */
  public static void putValue(Context context, String name, String key, boolean value) {
    SharedPreferences.Editor editor = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        .edit();

    editor.putBoolean(key, value).apply();
  }

  /**
   * SharedPreference 에서 boolean 형 값을 넣는 함수
   * @param context The application context
   * @param name preference 파일
   * @param key 얻을 기본설정의 이름
   * @param dft 기본값
   * @return
   */
  public static boolean getValue(Context context, String name, Object key, boolean dft) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE).getBoolean(key.toString(), dft);
  }

  /**
   * SharedPreference 에서 int 형 값을 넣는 함수
   * @param context The application context
   * @param name preference 파일
   * @param key 얻을 기본설정의 이름
   * @param dft 기본값
   * @return
   */
  public static int getValue(Context context, String name, Object key, int dft) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE).getInt(key.toString(), dft);
  }

  /**
   * SharedPreference 에서 String 형 값을 넣는 함수
   * @param context The application context
   * @param name preference 파일
   * @param key 얻을 기본설정의 이름
   * @param dft 기본값
   * @return
   */
  public static String getValue(Context context, String name, Object key, String dft) {
    return context.getSharedPreferences(name, Context.MODE_PRIVATE).getString(key.toString(), dft);
  }

  public interface LYRIC_KEY {

    String NAME = "Lyric";
    //가사검색우선순위
    String PRIORITY_LYRIC = "priority_lyric";
    String DEFAULT_PRIORITY = new Gson().toJson(Arrays
            .asList(LyricPriority.LOCAL,
                LyricPriority.EMBEDED),
        new TypeToken<List<LyricPriority>>() {
        }.getType());

    int LYRIC_DEFAULT = LyricPriority.DEF.getPriority();
    int LYRIC_IGNORE = LyricPriority.IGNORE.getPriority();
    int LYRIC_LOCAL = LyricPriority.LOCAL.getPriority();
    int LYRIC_EMBEDDED = LyricPriority.EMBEDED.getPriority();
    int LYRIC_MANUAL = LyricPriority.MANUAL.getPriority();

  }

  public interface SETTING_KEY {

    String NAME = "Setting";
    //처음으로 자료 읽기
    String FIRST_LOAD = "first_load";
    //Desktop 가사 잠금
    String DESKTOP_LYRIC_LOCK = "desktop_lyric_lock";
    //Desktop 가사 서체 크기
    String DESKTOP_LYRIC_TEXT_SIZE = "desktop_lyric_text_size";
    //Desktop 가사 y 좌표
    String DESKTOP_LYRIC_Y = "desktop_lyric_y";
    //화면이 항상 켜져 있는지 확인여부
    String SCREEN_ALWAYS_ON = "key_screen_always_on";
    //알림창의 classic 형태 사용 여부
    String NOTIFY_STYLE_CLASSIC = "notify_classic";
    //음악 library 구성
    String LIBRARY_CATEGORY = "library_category";
    //잠금화면설정
    String LOCKSCREEN = "lockScreen";
    //흔들기
    String SHAKE = "shake";
    String WAKE = "wake";
    String HEADSET = "headset";
    //Desktop 가사 현시 여부
    String DESKTOP_LYRIC_SHOW = "desktop_lyric_show";
    //검색크기
    String SCAN_SIZE = "scan_size";
    //노래정렬순서
    String SONG_SORT_ORDER = "song_sort_order";
    //예술가정렬순서
    String ARTIST_SORT_ORDER = "artist_sort_order";
    //등록부노래정렬순서
    String CHILD_FOLDER_SONG_SORT_ORDER = "child_folder_song_sort_order";
    //예술가노래정렬순서
    String CHILD_ARTIST_SONG_SORT_ORDER = "child_artist_sort_order";
    //Album 노래정렬순서
    String CHILD_ALBUM_SONG_SORT_ORDER = "child_album_song_sort_order";
    //재생목록의 노래정렬순서
    String CHILD_PLAYLIST_SONG_SORT_ORDER = "child_playlist_song_sort_order";
    //노래제거
    String BLACKLIST_SONG = "black_list_song";
    //종료시 재생시간
    String LAST_PLAY_PROGRESS = "last_play_progress";
    //종료시 재생되는 노래
    String LAST_SONG_ID = "last_song_id";
    //재생방식
    String PLAY_MODEL = "play_model";
    //알림창의 배경이 체계배경색인지 확인여부
    String NOTIFY_SYSTEM_COLOR = "notify_system_color";
    //중단점 재생
    String PLAY_AT_BREAKPOINT = "play_at_breakpoint";
    //Media cache 무시여부
    String IGNORE_MEDIA_STORE = "ignore_media_store";
    //표준으로 Timer 를 시작할 여부
    String TIMER_DEFAULT = "timer_default";
    //Timer 기간
    String TIMER_DURATION = "timer_duration";
    //재생 interface 하단에 표시
    String BOTTOM_OF_NOW_PLAYING_SCREEN = "bottom_of_now_playing_screen";
    //속도
    String SPEED = "speed";
    //원천 삭제
    String DELETE_SOURCE = "delete_source";
    //목록노래이름을 등록부이름으로 바꿀지 여부
    String SHOW_DISPLAYNAME = "show_displayname";
    //예술가목록의 표시방식
    String MODE_FOR_ARTIST = "mode_for_artist";
    //재생목록의 표시방식
    String MODE_FOR_PLAYLIST = "mode_for_playlist";
    //언어
    String LANGUAGE = "language";
    //eq
    String ENABLE_EQ = "enable_eq";
    //bass boost strength
    String BASS_BOOST_STRENGTH = "bass_boost_strength";
    //Audio 초점
    String AUDIO_FOCUS = "audio_focus";
    //자동재생
    String AUTO_PLAY = "auto_play_headset_plug_in";
    //수동으로 등록부 검색
    String MANUAL_SCAN_FOLDER = "manual_scan_folder";

    String BACKGROUND_INTERNAL = "background_internal";
    String BACKGROUND_INTERNAL_ID = "background_internal_id";
    String BACKGROUND_EXTERNAL = "background_external";
    String PLAY_QUEUE = "play_queue";
    String PLAY_QUEUE_TYPE = "play_queue_type";

    String PLAY_FROM_OTHER_APP = "play_from_other_app";
    String PREVIOUS_LANGUAGE = "previous_language";
  }

  public interface LYRIC_OFFSET_KEY {

    String NAME = "LyricOffset";
  }
}
