package com.kr.musicplayer.lyric

import android.os.Environment
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kr.musicplayer.App
import com.kr.musicplayer.lyric.bean.LrcRow
import com.kr.musicplayer.misc.cache.DiskCache
import java.io.BufferedReader
import java.io.File
import kotlin.collections.ArrayList

/**
 * 가사 구현 클라스 구문분석
 */

class DefaultLrcParser : ILrcParser {
  override fun saveLrcRows(lrcRows: List<LrcRow>?, cacheKey: String, searchKey: String) {
    if (lrcRows == null || lrcRows.isEmpty())
      return

    //Cache
    DiskCache.getLrcDiskCache()?.apply {
      edit(cacheKey)?.apply {
        newOutputStream(0)?.use { outStream ->
          outStream.write(Gson().toJson(lrcRows, object : TypeToken<List<LrcRow>>() {}.type).toByteArray())
        }
      }?.commit()
    }?.flush()

    //원본가사파일저장
    if (TextUtils.isEmpty(searchKey) || Environment.MEDIA_MOUNTED != Environment.getExternalStorageState())
      return

    File(Environment.getExternalStorageDirectory(), "Android/data/"
        + App.getContext().packageName + "/lyric")
        .run {
          if (exists() || mkdirs())
            File(this, searchKey.replace("/".toRegex(), "") + ".lrc")
          else null
        }?.run {
          //파일이 없거나 성공적으로 다시 생성
          if (!exists() || (delete() && createNewFile()))
            this
          else null
        }?.run {
          lrcRows.forEach { lrcRow ->
            val strBuilder = StringBuilder(128)
            strBuilder.append("[")
            strBuilder.append(lrcRow.timeStr)
            strBuilder.append("]")
            strBuilder.append(lrcRow.content).append("\n")
            if (lrcRow.hasTranslate()) {
              strBuilder.append("[")
              strBuilder.append(lrcRow.timeStr)
              strBuilder.append("]")
              strBuilder.append(lrcRow.translate).append("\n")
            }
            appendText(strBuilder.toString())
          }
        }
  }

  /**
   * 가사 LrcRow 목록 얻기
   */
  override fun getLrcRows(bufferedReader: BufferedReader?, needCache: Boolean, cacheKey: String, searchKey: String): List<LrcRow>? {

    //가사해석
    val lrcRows = ArrayList<LrcRow>()
    val allLine = ArrayList<String>()
    var offset = 0
    if (bufferedReader == null)
      return lrcRows
    bufferedReader.useLines { allLines ->
      allLines.forEach { eachLine ->
        allLine.add(eachLine)
        //Offset tag 읽기
        if (eachLine.startsWith("[offset:") && eachLine.endsWith("]")) {
          val offsetInString = eachLine.substring(eachLine.lastIndexOf(":") + 1, eachLine.length - 1)
          if (offsetInString.isNotEmpty() && TextUtils.isDigitsOnly(offsetInString)) {
            offset = Integer.valueOf(offsetInString)
          }
        }
      }
    }

    if (allLine.size == 0)
      return lrcRows

    for (temp in allLine) {
      //가사의 모든 줄을 구문분석
      val rows = LrcRow.createRows(temp, offset)
      if (rows != null && rows.size > 0)
        lrcRows.addAll(rows)
    }

    lrcRows.sort()
    //통합
    val combineLrcRows = ArrayList<LrcRow>()
    var index = 0
    while (index < lrcRows.size) {
      // 가사의 다음 문장의 시간이 현재 가사와 일치하는지 확인합니다. 일치하면 다음 문장이 현재 가사의 번역으로 간주된다.
      val currentRow = lrcRows[index]
      val nextRow = lrcRows.getOrNull(index + 1)
      if (currentRow.time == nextRow?.time && !currentRow.content.isNullOrBlank()) { // 带翻译的歌词
        val tmp = LrcRow()
        tmp.content = currentRow.content
        tmp.time = currentRow.time
        tmp.timeStr = currentRow.timeStr
        tmp.translate = nextRow.content
        combineLrcRows.add(tmp)
        index++
      } else { // 普通歌词
        combineLrcRows.add(currentRow)
      }
      index++
    }

    lrcRows.clear()
    lrcRows.addAll(combineLrcRows)


    if (lrcRows.size == 0)
      return lrcRows
    lrcRows.sort()
    //줄당 시간
    for (i in 0 until lrcRows.size - 1) {
      lrcRows[i].setTotalTime(lrcRows[i + 1].time - lrcRows[i].time)
    }
    lrcRows[lrcRows.size - 1].setTotalTime(5000)
    //Cache
    if (needCache) {
      saveLrcRows(lrcRows, cacheKey, searchKey)
    }

    return lrcRows
  }

  companion object {
    const val TAG = "DefaultLrcParser"
    const val THRESHOLD_PROPORTION = 0.3
  }
}
