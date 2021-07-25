package com.kr.musicplayer.request;

import static com.kr.musicplayer.request.LibraryUriRequest.ERROR_BLACKLIST;
import static com.kr.musicplayer.request.LibraryUriRequest.ERROR_NO_RESULT;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import timber.log.Timber;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import com.kr.musicplayer.App;
import com.kr.musicplayer.R;
import com.kr.musicplayer.bean.mp3.Song;
import com.kr.musicplayer.util.DensityUtil;
import com.kr.musicplayer.util.ImageUriUtil;
import com.kr.musicplayer.util.MediaStoreUtil;
import com.kr.musicplayer.util.SPUtil;

/**
 * 화상읽기 클라스
 */

public abstract class ImageUriRequest<T> {

  private static final ConcurrentHashMap<Integer, String> MEMORY_CACHE = new ConcurrentHashMap<>();

  private static final List<String> BLACKLIST = Arrays
      .asList();

  private static final String PREFIX_FILE = "file://";
  private static final String PREFIX_EMBEDDED = "embedded://";

  public static final int BIG_IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 200);
  public static final int SMALL_IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 45);
  public static final int URL_PLAYLIST = 1000;
  public static final int URL_ALBUM = 10;
  public static final int URL_ARTIST = 100;

  //MediaStore 무시
  public static boolean IGNORE_MEDIA_STORE = SPUtil
      .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE, false);

  protected RequestConfig mConfig = DEFAULT_CONFIG;

  private static final RequestConfig DEFAULT_CONFIG = new RequestConfig.Builder()
      .forceDownload(true).build();

  public ImageUriRequest(RequestConfig config) {
    mConfig = config;
  }

  public ImageUriRequest() {
  }

  public static void clearUriCache() {
    MEMORY_CACHE.clear();
  }

  public abstract void onError(Throwable throwable);

  public abstract void onSuccess(@Nullable T result);

  protected void onStart() {

  }

  public abstract Disposable load();

  protected Observable<String> getCoverObservable(UriRequest request) {
    return Observable
        .concat(
            getMemoryCacheObservable(request),
            getCustomThumbObservable(request),
            getContentThumbObservable(request)
        )
        .doOnNext(result -> {
          if (TextUtils.isEmpty(result)) {
            throw new Exception(ERROR_NO_RESULT);
          }
          if (ImageUriRequest.BLACKLIST.contains(result)) {
            throw new Exception(ERROR_BLACKLIST);
          }
        })
        .doOnError(throwable -> {
          if (throwable == null) {
            // 오유류형이 없으므로 무시
          } else if (throwable instanceof UnknownHostException) {
            // 망이 없으므로 무시
          } else if (throwable instanceof NoSuchElementException) {
            // 결과 없음
            MEMORY_CACHE.put(request.hashCode(), "");
          } else if (ERROR_NO_RESULT.equals(throwable.getMessage()) ||
              ERROR_BLACKLIST.equals(throwable.getMessage())) {
            // Blacklist 혹은 결과 없음
            MEMORY_CACHE.put(request.hashCode(), "");
          } else {
            // 처리되지 않음
          }
        })
        .doOnNext(result -> {
          MEMORY_CACHE.put(request.hashCode(), result);
        })
        .doOnSubscribe(disposable -> onStart())
        .firstOrError()
        .toObservable();
  }

  private Observable<String> getMemoryCacheObservable(UriRequest request) {
    return Observable.create(emitter -> {
      final String cache = MEMORY_CACHE.get(request.hashCode());
      if (cache != null) {
        emitter.onNext(cache);
      }
      emitter.onComplete();
    });
  }


  Observable<String> getCustomThumbObservable(UriRequest request) {
    return Observable.create(emitter -> {
      //맞춤표지 설정여부 확인
      if (request.getSearchType() != URL_ALBUM) {
        File customImage = ImageUriUtil
            .getCustomThumbIfExist(request.getID(), request.getSearchType());
        if (customImage != null && customImage.exists()) {
          emitter.onNext(PREFIX_FILE + customImage.getAbsolutePath());
        }
      }
      emitter.onComplete();
    });
  }

  /**
   * Local DB query
   */
  private Observable<String> getContentThumbObservable(UriRequest request) {
    return Observable.create(observer -> {
      String imageUrl = "";
      if (request.getSearchType() == URL_ALBUM) {//Album 표지
        //MediaStore cache 무시
        if (IGNORE_MEDIA_STORE) {
          final String selection = TextUtils.isEmpty(request.getTitle()) ?
              MediaStore.Audio.Media.ALBUM_ID + "=" + request.getID() :
              MediaStore.Audio.Media.ALBUM_ID + "=" + request.getID() + " and " +
                  MediaStore.Audio.Media.TITLE + "=?";
          final String[] selectionValues = TextUtils.isEmpty(request.getTitle()) ?
              null :
              new String[]{request.getTitle()};

          List<Song> songs = MediaStoreUtil.getSongs(selection, selectionValues);
          if (songs.size() > 0) {
            imageUrl = PREFIX_EMBEDDED + songs.get(0).getUrl();
          }
        } else {
          Uri uri;
          if (request.getSongId() > 0) {
            uri = Uri
                .parse("content://media/external/audio/media/" + request.getSongId() + "/albumart");
          } else {
            uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"),
                request.getID());
          }
          if (ImageUriUtil.isAlbumThumbExistInMediaCache(uri)) {
            imageUrl = uri.toString();
          }
        }

      } else {//Artist cover
        imageUrl = ImageUriUtil.getArtistArt(request.getID());
      }
      if (!TextUtils.isEmpty(imageUrl)) {
        observer.onNext(imageUrl);
      }
      observer.onComplete();
    });
  }

  protected Observable<Bitmap> getThumbBitmapObservable(UriRequest request) {
    return getCoverObservable(request)
        .flatMap((Function<String, ObservableSource<Bitmap>>) url -> Observable.create(e -> {
          Uri imageUri = !TextUtils.isEmpty(url) ? Uri.parse(url) : Uri.EMPTY;
          ImageRequest imageRequest =
              ImageRequestBuilder.newBuilderWithSource(imageUri)
                  .setResizeOptions(new ResizeOptions(mConfig.getWidth(), mConfig.getHeight()))
                  .build();
          DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline()
              .fetchDecodedImage(imageRequest, App.getContext());
          dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(Bitmap bitmap) {
              if (bitmap == null) {
                bitmap = BitmapFactory
                    .decodeResource(App.getContext().getResources(), R.drawable.ic_disc);
              }
              e.onNext(bitmap);
              e.onComplete();
            }

            @Override
            protected void onFailureImpl(
                DataSource<CloseableReference<CloseableImage>> dataSource) {
              e.onError(dataSource.getFailureCause());
            }
          }, CallerThreadExecutor.getInstance());
        }));
  }
}
