package com.kr.musicplayer.lyric;

import java.util.List;
import com.kr.musicplayer.lyric.bean.LrcRow;


public interface ILrcView {

  /**
   * 색상, 서체크기 및 기타설정 초기화
   */
  void init();

  /***
   * 자료설정
   * @param lrcRows
   */
  void setLrcRows(List<LrcRow> lrcRows);

  /**
   * 지정된 시간
   *
   * @param progress 진행
   * @param fromSeekBarByUser 사용자가 seekbar 를 다치기하여 trigger 되는지 여부
   */
  void seekTo(int progress, boolean fromSeekBar, boolean fromSeekBarByUser);

  /***
   * 가사 본문의 확대/축소 비률 설정
   * @param scalingFactor
   */
  void setLrcScalingFactor(float scalingFactor);

  /**
   * 초기화
   */
  void reset();
}
