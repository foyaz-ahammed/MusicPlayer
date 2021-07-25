package com.kr.musicplayer.misc

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.kr.musicplayer.App
import com.kr.musicplayer.R
import com.kr.musicplayer.theme.Theme
import com.kr.musicplayer.ui.activity.SettingsActivity
import com.kr.musicplayer.ui.dialog.LoadingDialog
import com.kr.musicplayer.util.MaterialDialogHelper
import com.kr.musicplayer.util.MediaStoreUtil
import com.kr.musicplayer.util.ToastUtil
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableOnSubscribe
import io.reactivex.FlowableSubscriber
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import org.reactivestreams.Subscription
import timber.log.Timber
import java.io.File


/**
 * 노래 검색
 */

class MediaScanner(private val context: Context) {
  private var countFolder = 0 // 검색된 등록부개수
  private var countFile = 0 // 검색된 파일개수

  var isPause = false // 현재 검색중이던것이 중지된 상태인지 판별
  var isStarted = false // 노래검색이 시작되였는지 판별

  var loadingDialog : LoadingDialog? = null
  var subscription: Subscription? = null
  var connection: MediaScannerConnection? = null

  var status : Job? = null // 노래검색 중지를 위한 Job

  /**
   * 읽기지연함수
   */
  suspend fun delayLoading() {
    while (isPause) {
      yield()
      delay(300)
    }
  }

  /**
   * 노래검색
   */
  fun scanFiles(folder: File) {
    isStarted = true
    val toScanFiles = ArrayList<File>()

    loadingDialog = LoadingDialog(context, this)
    loadingDialog!!.window.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.round_gray))
    var percent: Int
    var current = 0

    connection = MediaScannerConnection(context, object : MediaScannerConnection.MediaScannerConnectionClient {
      override fun onMediaScannerConnected() {
        Flowable.create(FlowableOnSubscribe<File> { emitter ->
          getScanFiles(folder, toScanFiles)
          for (file in toScanFiles) {
            emitter.onNext(file)
          }
          emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : FlowableSubscriber<File> {
              override fun onSubscribe(s: Subscription) {
                loadingDialog!!.show()
                loadingDialog!!.setLoadingPauseCallback(context as SettingsActivity)
                subscription = s
                subscription?.request(1)
              }

              override fun onNext(file: File) {
                status = CoroutineScope(Dispatchers.Main).launch {
                  delayLoading()
                  current++
                  percent = current * 100 / toScanFiles.size
                  loadingDialog!!.setPercent(percent)
                  loadingDialog!!.setProgress(percent)
                  loadingDialog!!.setCount(current)
                  connection?.scanFile(file.absolutePath, "audio/*")
                }
                status!!.start()
              }

              override fun onError(throwable: Throwable) {
                loadingDialog!!.dismiss()
                ToastUtil.show(context, R.string.scan_failed, throwable.toString())
              }

              override fun onComplete() {
                loadingDialog!!.dismiss()
                isStarted = false
                val dialog = Theme.getBaseDialog(context)
                        .positiveText(android.R.string.yes)
                        .title(R.string.search_result_title)
                        .content(context.getString(R.string.search_result_content, toScanFiles.size))
                        .build()
                MaterialDialogHelper.adjustAlertDialog(dialog, ContextCompat.getDrawable(context, R.drawable.round_gray))
                dialog.show()
                App.getContext().contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
                isStarted = false
                status?.cancel()
                status = null
                subscription?.cancel()
                subscription = null
                connection?.disconnect()
                connection = null
              }
            })
      }

      override fun onScanCompleted(path: String?, uri: Uri?) {
        subscription?.request(1)
        Timber.v("onScanCompleted, path: $path uri: $uri")
      }
    })
    connection?.connect()

  }

  /**
   * 검색중지
   */
  fun cancelSubscription() {
    this.status?.cancel()
    this.subscription?.cancel()
    this.connection?.disconnect()
    this.loadingDialog?.dismiss()
    isStarted = false
  }

  /**
   * 검색할 파일목록 얻기
   */
  private fun getScanFiles(file: File, toScanFiles: ArrayList<File>) {
    if (file.isFile) {
      countFile++
    }
    if (file.isDirectory) {
      countFolder++
    }
    if (file.isFile && file.length() >= MediaStoreUtil.SCAN_SIZE) {
      if (isAudioFile(file))
        toScanFiles.add(file)
    } else {
      val files = file.listFiles() ?: return
      for (temp in files) {
        getScanFiles(temp, toScanFiles)
      }
    }
  }

  /**
   * Audio 파일인지 판별
   */
  private fun isAudioFile(file: File): Boolean {
    val ext = getFileExtension(file.name)
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    return !mime.isNullOrEmpty() && mime.startsWith("audio") && !mime.contains("mpegurl")
  }

  private fun getFileExtension(fileName: String): String? {
    val i = fileName.lastIndexOf('.')
    return if (i > 0) {
      fileName.substring(i + 1)
    } else
      null
  }
}
