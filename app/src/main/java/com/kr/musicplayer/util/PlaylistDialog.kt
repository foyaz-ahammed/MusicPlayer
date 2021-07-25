package com.kr.musicplayer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.text.TextUtils
import com.kr.musicplayer.R
import com.kr.musicplayer.db.room.DatabaseRepository
import com.kr.musicplayer.helper.MusicServiceRemote
import com.kr.musicplayer.request.network.RxUtil
import com.kr.musicplayer.theme.Theme
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers

/**
 * 재생목록 대화창
 */
class PlaylistDialog {
    companion object {
        @SuppressLint("CheckResult")
        @JvmStatic fun addToPlaylist(activity: Activity) {
            val databaseRepository = DatabaseRepository.getInstance()
            val single = Single.fromCallable {
                val ids = ArrayList<Int>()
                ids.add(MusicServiceRemote.getCurrentSong().id)
                return@fromCallable ids
            }
            databaseRepository.getAllPlaylist()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ playLists ->
                        val list = playLists.map {
                            it.name
                        }.toMutableList()
                        list.removeAt(0)
                        list.add(0, activity.getString(R.string.my_favorite))
                        list.add(activity.getString(R.string.create_playlist))
                        val d = Theme.getBaseDialog(activity)
                                .title(R.string.add_to_playlist)
                                .items(list.toList())
                                .itemsCallback { dialog, view, which, text ->
                                    //기존재생목록에 직접 추가
                                    if (which != list.indexOf(list.last())) {
                                        single
                                                .flatMap {
                                                    databaseRepository.insertToPlayList(it, playLists[which].name)
                                                }
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribeOn(Schedulers.io())
                                                /*.doFinally {
                                                close()
                                            }*/
                                                .subscribe(Consumer {
                                                    if (playLists[which].name == activity.getString(R.string.db_my_favorite)) {
                                                        if (it == 0) {
                                                            ToastUtil.show(activity, activity.getString(
                                                                    R.string.already_exists_songs_in_favorite))
                                                        } else {
                                                            ToastUtil.show(activity, activity.getString(
                                                                    R.string.add_song_my_favorite_success,
                                                                    it))
                                                        }
                                                    } else {
                                                        if (it == 0) {
                                                            ToastUtil.show(activity, activity.getString(
                                                                    R.string.already_exists_songs_in_playlist,
                                                                    playLists[which].name))
                                                        } else {
                                                            ToastUtil.show(activity, activity.getString(
                                                                    R.string.add_song_playlist_success,
                                                                    it,
                                                                    playLists[which].name))
                                                        }
                                                    }
                                                })
                                    } else {
                                        val d = Theme.getBaseDialog(activity)
                                                .title(R.string.new_playlist)
                                                .positiveText(R.string.create)
                                                .negativeText(R.string.cancel)
                                                .content(R.string.input_playlist_name)
                                                .inputRange(1, 15)
                                                .input("", activity.getString(R.string.local_list) + playLists.size) { _, input ->
                                                    if (TextUtils.isEmpty(input)) {
                                                        ToastUtil.show(activity, R.string.add_error)
                                                        return@input
                                                    }
                                                    databaseRepository
                                                            .insertPlayList(input.toString())
                                                            .flatMap {
                                                                single
                                                            }
                                                            .flatMap {
                                                                databaseRepository.insertToPlayList(it, input.toString())
                                                            }
                                                            .compose(RxUtil.applySingleScheduler())
                                                            /*.doFinally {
                                                                close()
                                                            }*/
                                                            .subscribe({
                                                                ToastUtil.show(activity, R.string.add_playlist_success)
                                                                ToastUtil.show(
                                                                        activity,
                                                                        activity.getString(
                                                                                R.string.add_song_playlist_success,
                                                                                it,
                                                                                input.toString()
                                                                        )
                                                                )
                                                            }, {
                                                                ToastUtil.show(activity, activity.getString(R.string.add_error))
                                                            })
                                                }.build()
                                        MaterialDialogHelper.adjustAlertDialog(d, activity.getDrawable(R.drawable.round_gray_top))
                                        d.show()
                                    }
                                }
                                .build()
                        MaterialDialogHelper.adjustAlertDialog(d, activity.getDrawable(R.drawable.round_gray_top))
                        d.show()
                    }, {
                        ToastUtil.show(activity, activity.getString(R.string.add_error))
                    })
        }
    }
}