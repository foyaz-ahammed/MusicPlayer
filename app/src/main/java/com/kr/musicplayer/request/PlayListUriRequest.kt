package com.kr.musicplayer.request

import android.net.Uri
import com.facebook.drawee.view.SimpleDraweeView
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableObserver
import com.kr.musicplayer.App
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.db.room.DatabaseRepository
import com.kr.musicplayer.request.network.RxUtil
import com.kr.musicplayer.util.ImageUriUtil.getSearchRequestWithAlbumType

/**
 * 재생목록 Uri 관련 class
 */

open class PlayListUriRequest(image: SimpleDraweeView, request: UriRequest, config: RequestConfig) : LibraryUriRequest(image, request, config) {

  override fun onError(throwable: Throwable?) {
    super.onError(throwable)
  }

  override fun load(): Disposable {
    val coverObservables = DatabaseRepository.getInstance()
        .getPlayList(request.id)
        .flatMap { playList ->
          DatabaseRepository.getInstance()
              .getPlayListSongs(App.getContext(), playList, true)
        }
        .flatMapObservable(Function<List<Song>, ObservableSource<Song>> { songs ->
          Observable.create { emitter ->
            for (song in songs) {
              emitter.onNext(song)
            }
            emitter.onComplete()
          }
        })
        .concatMapDelayError { song ->
          getCoverObservable(getSearchRequestWithAlbumType(song))
        }

    return Observable.concat(getCustomThumbObservable(request), coverObservables)
        .firstOrError()
        .toObservable()
        .compose(RxUtil.applyScheduler())
        .subscribeWith(object : DisposableObserver<String>() {
          override fun onStart() {
            ref.get()?.setImageURI(Uri.EMPTY)
          }

          override fun onNext(s: String) {
            onSuccess(s)
          }

          override fun onError(e: Throwable) {
            this@PlayListUriRequest.onError(e)
          }

          override fun onComplete() {

          }
        })
  }

}
