package com.kr.musicplayer.misc

import android.content.Context
import android.content.res.Configuration
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import com.kr.musicplayer.bean.mp3.Album
import com.kr.musicplayer.bean.mp3.Artist
import com.kr.musicplayer.bean.mp3.Folder
import com.kr.musicplayer.util.MediaStoreUtil
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun Album.getSongIds(): List<Int> {
  return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ALBUM_ID + "=?", arrayOf((albumID.toString())))
}

fun Artist.getSongIds(): List<Int> {
  return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ARTIST_ID + "=?", arrayOf(artistID.toString()))
}

fun Folder.getSongIds(): List<Int> {
  return MediaStoreUtil.getSongIdsByParentId(parentId)
}

fun Context.isPortraitOrientation(): Boolean {
  val configuration = this.resources.configuration //설정구성정보 가져오기
  val orientation = configuration.orientation //화면방향 가져오기
  return orientation == Configuration.ORIENTATION_PORTRAIT
}

fun CoroutineScope.tryLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend () -> Unit,
    catch: ((e: Exception) -> Unit)? = { Timber.w(it) }) {
  launch(context, start) {
    try {
      block()
    } catch (e: Exception) {
      catch?.invoke(e)
    }
  }
}

fun CoroutineScope.tryLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend () -> Unit) {
  tryLaunch(context = context,
      start = start,
      block = block,
      catch = {
        Timber.w(it)
      })
}

fun Any?.checkMainThread() {
  if (Looper.myLooper() != Looper.getMainLooper()) {
    throw RuntimeException("$this should be used only from the application's main thread")
  }
}

fun Any?.checkWorkerThread() {
  if (Looper.myLooper() == Looper.getMainLooper()) {
    throw RuntimeException("$this should be used only from the worker thread")
  }
}


fun File.zipInputStream() = ZipInputStream(this.inputStream())

fun File.zipOutputStream() = ZipOutputStream(this.outputStream())

fun ZipOutputStream.zipFrom(vararg paths: String) {
  use {
    val files = paths.map { File(it) }
    files.forEach {
      if (it.isFile) {
        zip(arrayOf(it), null)
      } else if (it.isDirectory) {
        zip(it.listFiles(), it.name)
      }
    }
  }

}

private fun ZipOutputStream.zip(files: Array<File>, path: String?) {
  //경로를 구성하는데 리용되는 prefix
  val prefix = if (path == null) "" else "$path/"

  if (files.isEmpty()) createEmptyFolder(prefix)

  files.forEach {
    if (it.isFile) {
      val entry = ZipEntry("$prefix${it.name}")
      val ins = it.inputStream().buffered()
      putNextEntry(entry)
      ins.writeTo(this, DEFAULT_BUFFER_SIZE, closeOutput = false)
      closeEntry()
    } else {
      zip(it.listFiles(), "$prefix${it.name}")
    }
  }
}

/**
 * inputStream 내용은 outputStream 에 기록
 */
fun InputStream.writeTo(outputStream: OutputStream, bufferSize: Int = 1024 * 2,
                        closeInput: Boolean = true, closeOutput: Boolean = true) {

  val buffer = ByteArray(bufferSize)
  val br = this.buffered()
  val bw = outputStream.buffered()
  var length = 0

  while ({ length = br.read(buffer);length != -1 }()) {
    bw.write(buffer, 0, length)
  }

  bw.flush()

  if (closeInput) {
    close()
  }

  if (closeOutput) {
    outputStream.close()
  }
}

/**
 * 압축파일등록부생성
 */
private fun ZipOutputStream.createEmptyFolder(location: String) {
  putNextEntry(ZipEntry(location))
  closeEntry()
}


