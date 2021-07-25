package com.kr.musicplayer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

import com.kr.musicplayer.misc.manager.ServiceManager

/**
 * MusicService 를 위한 기초 Service
 */
abstract class BaseService : Service() {
  override fun onBind(intent: Intent): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    ServiceManager.AddService(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    ServiceManager.RemoveService(this)
  }
}
