package com.kr.musicplayer.lyric;

import java.io.BufferedReader;
import java.util.List;
import com.kr.musicplayer.lyric.bean.LrcRow;

public interface ILrcParser {

  void saveLrcRows(List<LrcRow> lrcRows, String cacheKey, String searchKey);

  List<LrcRow> getLrcRows(BufferedReader bufferedReader, boolean needCache, String cacheKey,
      String searchKey);
}
