package com.kr.musicplayer.helper

import android.os.CountDownTimer
import com.kr.musicplayer.App
import com.kr.musicplayer.R
import com.kr.musicplayer.util.ToastUtil

/**
 * Sleep Timer
 */
class SleepTimer(millisInFuture: Long, countDownInterval: Long) : CountDownTimer(millisInFuture, countDownInterval) {

  override fun onFinish() {
    callbacks.forEach {
      it.onFinish()
    }
    android.os.Process.killProcess(android.os.Process.myPid())
  }

  override fun onTick(millisUntilFinished: Long) {
    millisUntilFinish = millisUntilFinished
  }

  interface Callback {
    fun onFinish()
  }

  companion object {
    /**  마감시간까지 남은 시간 */
    @JvmStatic
    private var millisUntilFinish: Long = 0

    @JvmStatic
    fun getMillisUntilFinish(): Long {
      return millisUntilFinish
    }

    @JvmStatic
    private var instance: SleepTimer? = null

    private val callbacks: MutableList<Callback> by lazy { ArrayList<Callback>() }

    @JvmStatic
    fun isTicking(): Boolean {
      return millisUntilFinish > 0
    }

    /**
     * Timer 시작 및 중지
     * @param start
     * @param duration
     */
    @JvmStatic
    fun toggleTimer(duration: Long) {
      val context = App.getContext()
      val start = instance == null
      if (start) {
        if (duration <= 0) {
          ToastUtil.show(context, R.string.plz_set_correct_time)
          return
        }
        instance = SleepTimer(duration, 1000)
        instance?.start()
      } else {
        if (instance != null) {
          instance?.cancel()
          instance = null
        }
        millisUntilFinish = 0
      }
      ToastUtil.show(context, if (!start) context.getString(R.string.cancel_timer) else context.getString(R.string.will_stop_at_x, Math.ceil((duration / 1000 / 60).toDouble()).toInt()))
    }

    @JvmStatic
    fun cancelTimer() {
      if (instance != null) {
        instance?.cancel()
        millisUntilFinish = 0
        instance = null
      }
    }

    fun addCallback(callback: Callback) {
      callbacks.add(callback)
    }

  }
}