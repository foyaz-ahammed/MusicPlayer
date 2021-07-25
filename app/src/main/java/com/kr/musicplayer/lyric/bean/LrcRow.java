package com.kr.musicplayer.lyric.bean;

import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import com.kr.musicplayer.App;
import com.kr.musicplayer.R;

/**
 * 가사의 매 줄의 entity 클라스
 * Comparable Interface 구현하여 List<LrcRow> 정렬에 편리
 */
public class LrcRow implements Comparable<LrcRow> {

  /**
   * 시간문자렬
   ***/
  private String mTimeStr;
  /**
   * 시간
   **/
  private int mTime;
  /**
   * 가사내용
   **/
  private String mContent;

  private String mTranslate;
  /**
   * 가사가 표시되는 총 시간
   **/
  private long mTotalTime;
  /**
   * 가사줄의 높이
   */
  private int mContentHeight;
  /**
   * 높이
   */
  private int mTranslateHeight;
  /**
   * 가사의 총 높이
   */
  private int mTotalHeight;

  public void setTotalHeight(int height) {
    this.mTotalHeight = height;
  }

  public int getTotalHeight() {
    return mTotalHeight;
  }

  public void setContentHeight(int height) {
    this.mContentHeight = height;
  }

  public int getContentHeight() {
    return mContentHeight;
  }

  public void setTranslateHeight(int height) {
    this.mTranslateHeight = height;
  }

  public int getTranslateHeight() {
    return mTranslateHeight;
  }

  public long getTotalTime() {
    return mTotalTime;
  }

  public void setTotalTime(int totalTime) {
    this.mTotalTime = totalTime;
  }

  public String getTimeStr() {
    return mTimeStr;
  }

  public void setTimeStr(String timeStr) {
    this.mTimeStr = timeStr;
  }

  public int getTime() {
    return mTime;
  }

  public void setTime(int time) {
    this.mTime = time;
  }

  public String getContent() {
    return mContent;
  }

  public void setContent(String content) {
    this.mContent = content;
  }

  public String getTranslate() {
    return mTranslate;
  }

  public void setTranslate(String translate) {
    mTranslate = translate;
  }

  public boolean hasTranslate() {
    return !TextUtils.isEmpty(mTranslate);
  }

  public LrcRow() {
  }

  public LrcRow(LrcRow lrcRow) {
    mTimeStr = lrcRow.getTimeStr();
    mTime = lrcRow.getTime();
    mTotalTime = lrcRow.getTotalTime();
    mContent = lrcRow.getContent();
    mTranslate = lrcRow.getTranslate();
  }

  public LrcRow(String timeStr, int time, String content) {
    super();
    mTimeStr = timeStr;
    mTime = time;
    if (TextUtils.isEmpty(content)) {
      mContent = "";
      mTranslate = "";
      return;
    }
    String[] mulitiContent = content.split("\t");
    mContent = mulitiContent[0];
    if (mulitiContent.length > 1) {
      mTranslate = mulitiContent[1];
    }
  }

  /**
   * 한줄에 여러 LrcRow 가 포함될수 있으므로 가사 파일의 한 줄을 List<LrcRow>로 구문분석한다
   */
  public static List<LrcRow> createRows(String lrcLine, int offset) {
    if (!lrcLine.startsWith("[") || (lrcLine.indexOf("]") != 9 && lrcLine.indexOf(']') != 10)) {
      return null;
    }
    //마지막 하나
    int lastIndexOfRightBracket = lrcLine.lastIndexOf("]");
    //가사내용
    String content = lrcLine.substring(lastIndexOfRightBracket + 1, lrcLine.length());

    String times = lrcLine.substring(0, lastIndexOfRightBracket + 1).replace("[", "-")
        .replace("]", "-");
    String[] timesArray = times.split("-");
    List<LrcRow> lrcRows = new ArrayList<>();
    for (String tem : timesArray) {
      //빈줄 남겨두기
      if (TextUtils.isEmpty(tem.trim()) /**|| TextUtils.isEmpty(content)*/) {
        continue;
      }
      try {
        LrcRow lrcRow = new LrcRow(tem, formatTime(tem) - offset, content);
        lrcRows.add(lrcRow);
      } catch (Exception e) {
      }
    }
    return lrcRows;
  }

  /****
   * 가사 시간을 미리초로 변환
   * @param timeStr 변환할 시간
   * @return 변환된 미리초
   */
  private static int formatTime(String timeStr) {
    timeStr = timeStr.replace('.', ':');
    String[] times = timeStr.split(":");

    return Integer.parseInt(times[0]) * 60 * 1000
        + Integer.parseInt(times[1]) * 1000
        + Integer.parseInt(times[2]);
  }

  @Override
  public int compareTo(@NonNull LrcRow anotherLrcRow) {
    return this.mTime - anotherLrcRow.mTime;
  }

  @Override
  public String toString() {
    return "[" + mTimeStr + "] " + mContent;
  }

  public static int getOffset() {
    return 0;
  }

  public static LrcRow LYRIC_EMPTY_ROW = new LrcRow("", 0, "");
  public static LrcRow LYRIC_NO_ROW = new LrcRow("", 0,
      App.getContext().getString(R.string.no_lrc));
  public static LrcRow LYRIC_SEARCHING_ROW = new LrcRow("", 0,
      App.getContext().getString(R.string.searching));
}
