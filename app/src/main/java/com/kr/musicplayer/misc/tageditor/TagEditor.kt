package com.kr.musicplayer.misc.tageditor

import androidx.annotation.WorkerThread
import io.reactivex.Single
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import java.io.File
import kotlin.concurrent.thread

/**
 * Tag Editor
 */
class TagEditor(path: String) {
    private var audioFile: AudioFile? = null // 노래파일
    private var audioHeader: AudioHeader? = audioFile?.audioHeader
    var initSuccess: Boolean = false

    private val lock = Any()

    init {
        thread {
            synchronized(lock) {
                audioFile = try {
                    AudioFileIO.read(File(path))
                } catch (e: Exception) {
                    null
                }
                audioHeader = audioFile?.audioHeader
                initSuccess = audioFile != null && audioHeader != null
            }
        }
    }

    fun getFieldValueSingle(field: FieldKey): Single<String> {
        return Single.fromCallable {
            getFiledValue(field)
        }
    }

    @WorkerThread
    private fun getFiledValue(field: FieldKey): String? {
        synchronized(lock) {
            if (!initSuccess)
                return ""
            return try {
                audioFile?.tagOrCreateAndSetDefault?.getFirst(field)
            } catch (e: Exception) {
                ""
            }
        }
    }
}
