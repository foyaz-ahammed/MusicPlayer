package com.kr.musicplayer.helper

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.SPUtil.SETTING_KEY
import java.util.*

/**
 * 언어관련 Helper
 */
object LanguageHelper {
  const val AUTO = 0
  const val CHINESE = 1
  const val ENGLISH = 2
  var current = -1

  private var sLocal: Locale = Locale.getDefault()

  private val TAG = "LanguageHelper"

  /**
   * 선택한 언어설정 가져오기
   */
  private fun selectLanguageLocale(context: Context): Locale? {
    if (current == -1) {
      current = SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, AUTO)
    }
    return when (current) {
      AUTO -> sLocal
      CHINESE -> Locale.CHINA
      ENGLISH -> Locale.ENGLISH
      else -> sLocal
    }
  }

  /**
   * Locale 설정
   */
  @JvmStatic
  fun setLocal(context: Context): Context {
    return updateResources(context, selectLanguageLocale(context))
  }

  private fun updateResources(context: Context, locale: Locale?): Context {
    Locale.setDefault(locale)

    val res = context.resources
    val config = Configuration(res.configuration)
    return if (Build.VERSION.SDK_INT >= 17) {
      config.setLocale(locale)
      context.createConfigurationContext(config)
    } else {
      config.locale = locale
      res.updateConfiguration(config, res.displayMetrics)
      context
    }
  }

  /**
   * 언어류형설정
   */
  @JvmStatic
  fun setApplicationLanguage(context: Context) {
    val resources = context.applicationContext.resources
    val dm = resources.displayMetrics
    val config = resources.configuration
    val locale = selectLanguageLocale(context)
    config.locale = locale
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val localeList = LocaleList(locale ?: return)
      LocaleList.setDefault(localeList)
      config.locales = localeList
      context.applicationContext.createConfigurationContext(config)
      Locale.setDefault(locale)
    }
    resources.updateConfiguration(config, dm)
  }

  /**
   * 현재 언어 보관
   */
  @JvmStatic
  fun saveSystemCurrentLanguage() {
    val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      LocaleList.getDefault().get(0)
    } else {
      Locale.getDefault()
    }
    sLocal = locale
  }

  @JvmStatic
  fun onConfigurationChanged(context: Context) {
    saveSystemCurrentLanguage()
    setLocal(context)
    setApplicationLanguage(context)
  }
}
