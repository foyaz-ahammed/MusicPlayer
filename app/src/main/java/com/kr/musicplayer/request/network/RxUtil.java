package com.kr.musicplayer.request.network;

import io.reactivex.ObservableTransformer;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class RxUtil {

  private RxUtil() {
  }

  public static <T> ObservableTransformer<T, T> applyScheduler() {
    return upstream -> upstream.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static <T> ObservableTransformer<T, T> applySchedulerToIO() {
    return upstream -> upstream.subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io());
  }

  public static <T> SingleTransformer<T, T> applySingleScheduler() {
    return upstream -> upstream.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }
}
