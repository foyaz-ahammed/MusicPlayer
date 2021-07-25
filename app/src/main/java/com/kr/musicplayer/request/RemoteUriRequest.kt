package com.kr.musicplayer.request

import android.graphics.Bitmap
import io.reactivex.disposables.Disposable
import com.kr.musicplayer.request.network.RxUtil
import timber.log.Timber

/**
 * Album cover bitmap 얻기
 */

abstract class RemoteUriRequest(private val request: UriRequest, config: RequestConfig) : ImageUriRequest<Bitmap>(config) {

  override fun load(): Disposable {
    return getThumbBitmapObservable(request)
        .compose(RxUtil.applySchedulerToIO())
        .subscribe({ bitmap -> onSuccess(bitmap) }, { throwable -> onError(throwable) })
  }
}
