package com.kr.musicplayer.helper

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.kr.musicplayer.App
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.util.Util.sendCMDLocalBroadcast
import kotlin.math.sqrt

/**
 * 흔들기 감지
 */

class ShakeDetector private constructor() : SensorEventListener, Runnable {
  private var sensorManager: SensorManager? = null
  private var sensor: Sensor? = null

  private var lastDetectTime: Long = 0 // 마지막으로 흔들기를 감지한 시간
  private var lastPostTime: Long = 0 // 마지막으로 Handler 에 대한 post 를 진행한 시간
  private var lastX: Float = 0.toFloat() // 마지막으로 흔들기하였을때의 x좌표
  private var lastY: Float = 0.toFloat() // 마지막으로 흔들기하였을때의 y좌표
  private var lastZ: Float = 0.toFloat() // 마지막으로 흔들기하였을때의 z좌표
  private var begin = false // 흔들기 감지 시작 여부
  private val handler = Handler(Looper.getMainLooper())

  /**
   * 흔들기 감지등록
   */
  fun beginListen() {
    if (begin)
      return
    begin = true
    sensorManager = App.getContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
    sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
  }

  /**
   * 흔들기 감지해제
   */
  fun stopListen() {
    begin = false
    sensorManager?.unregisterListener(this, sensor)
    sensorManager = null
    sensor = null
    handler.removeCallbacks(this)
  }

  override fun onSensorChanged(event: SensorEvent) {
    if (!begin)
      return
    val currentTime = System.currentTimeMillis()
    val detectInterval = currentTime - lastDetectTime
    if (detectInterval < DETECTION_THRESHOLD) {
      return
    }
    lastDetectTime = currentTime
    // Sensor 차이 계산
    val values = event.values
    val x = values[0]
    val y = values[1]
    val z = values[2]
    val deltaX = x - lastX
    val deltaY = y - lastY
    val deltaZ = z - lastZ
    val speed = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()) / detectInterval * 1000
    if (speed > SPEED_THRESHOLD) {
      val postInterval = currentTime - lastPostTime
      if (postInterval > POST_THRESHOLD) {
        handler.removeCallbacks(this)
        handler.post(this)
        lastPostTime = currentTime
      }
    }
    lastX = x
    lastY = y
    lastZ = z

  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

  }

  override fun run() {
    sendCMDLocalBroadcast(Command.NEXT)
  }

  companion object {
    //가속림계값
    private const val SPEED_THRESHOLD = 300
    //감지 간격
    private const val DETECTION_THRESHOLD = 100
    //명령간격
    private const val POST_THRESHOLD = 1000L

    @JvmStatic
    @Synchronized
    fun getInstance() = Holder.INSTANCE

  }

  private object Holder {
    val INSTANCE = ShakeDetector()
  }
}
