package com.kr.musicplayer.db.room

import androidx.room.Database
import androidx.room.InvalidationTracker
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import android.content.Intent
import com.kr.musicplayer.db.room.AppDatabase.Companion.VERSION
import com.kr.musicplayer.db.room.dao.*
import com.kr.musicplayer.db.room.model.History
import com.kr.musicplayer.db.room.model.PlayList
import com.kr.musicplayer.db.room.model.PlayQueue
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_PLAYLIST
import com.kr.musicplayer.util.Util.sendLocalBroadcast
import timber.log.Timber

@Database(entities = [
  PlayList::class,
  PlayQueue::class,
  History::class
], version = VERSION, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

  abstract fun playListDao(): PlayListDao

  abstract fun playQueueDao(): PlayQueueDao

  abstract fun playQueueDaoVM(): PlayQueueVM

  abstract fun historyDao(): HistoryDao

  abstract fun playListDaoVM(): PlayListDaoVM

  companion object {
    const val VERSION = 1

    @Volatile
    private var INSTANCE: AppDatabase? = null

    @JvmStatic
    fun getInstance(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
        }

    /**
     * DB 창조
     */
    private fun buildDatabase(context: Context): AppDatabase {
      val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "krplayer.db")
          .build()
      database.invalidationTracker.addObserver(object : InvalidationTracker.Observer(PlayList.TABLE_NAME,PlayQueue.TABLE_NAME) {
        override fun onInvalidated(tables: MutableSet<String>) {
          Timber.v("onInvalidated is called: $tables")
          if(tables.contains(PlayList.TABLE_NAME)){
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST,PlayList.TABLE_NAME))
          } else if(tables.contains(PlayQueue.TABLE_NAME)){
            sendLocalBroadcast(Intent(MusicService.PLAYLIST_CHANGE)
                .putExtra(EXTRA_PLAYLIST,PlayQueue.TABLE_NAME))
          }
        }
      })
      return database
    }

  }
}
