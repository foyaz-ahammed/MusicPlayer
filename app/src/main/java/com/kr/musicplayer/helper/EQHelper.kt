package com.kr.musicplayer.helper

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.kr.musicplayer.App
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.SPUtil.SETTING_KEY.*
import com.kr.musicplayer.util.Util.isIntentAvailable
import timber.log.Timber

/**
 * Equalizer Helper
 */
object EQHelper {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val bandLevels = ArrayList<Short>() //현재 equalizer engine 에서 지원하는 조종빈도로 tag 개수

    var bandNumber: Short = 0 // 주파수 대역수
    var maxLevel: Short = 0 // 최대범위
    var minLevel: Short = 0 // 최소범위

    var enable = false // 음향효과설정 활설화 여부

    var builtEqualizerInit = false

    val isBassBoostEnabled: Boolean // BassBoost 능동상태여부
        get() = enable && bassBoost?.strengthSupported == true

    var bassBoostStrength: Int
        get() = SPUtil.getValue(App.getContext(), NAME, BASS_BOOST_STRENGTH, 0)
        set(strength) {
            SPUtil.putValue(App.getContext(), NAME, BASS_BOOST_STRENGTH, strength)
            if (isBassBoostEnabled) {
                tryRun({
                  bassBoost?.setStrength(strength.toShort())
                }, {
                  releaseBassBoost()
                })
            }
        }

    /**
     * 초기화
     */
    fun init(context: Context, sessionId: Int, force: Boolean = false): Boolean {

        if (sessionId == AudioEffect.ERROR_BAD_VALUE) {
            return false
        }

        //초기화전에 체계 equalizer 존재여부 확인
        if (isSystemEqualizerAvailable(context)) {
            return false
        }

        //음향효과설정 활설화 여부
        enable = SPUtil.getValue(App.getContext(), NAME, ENABLE_EQ, false)
        // 초기화 할 필요 없음
        if (!enable && !force) {
            builtEqualizerInit = false
            return builtEqualizerInit
        }

        tryRun({
          equalizer = Equalizer(0, sessionId)
          equalizer?.also { equalizer ->
            equalizer.enabled = enable

            //현재 equalizer engine 에서 지원하는 조종빈도로 tag 개수 가져오기
            bandNumber = equalizer.numberOfBands

            //최소범위
            minLevel = equalizer.bandLevelRange[0]
            //최대범위
            maxLevel = equalizer.bandLevelRange[1]

            bandNumber = equalizer.numberOfBands

            //이전에 저장된 각 주파수의 db 값을 가져온다
            for (i in 0 until bandNumber) {
              val bangLevel = SPUtil.getValue(App.getContext(), NAME, "band$i", 0)
              bandLevels.add(bangLevel.toShort())
            }

            builtEqualizerInit = true
          }
        }, {
          equalizer = null
          builtEqualizerInit = false
          Timber.v("init failed")
        })

        return builtEqualizerInit
    }

    fun open(context: Context, audioSessionId: Int) {
        Timber.v("open, audioSessionId: $audioSessionId")

        if (audioSessionId == AudioEffect.ERROR_BAD_VALUE) {
            return
        }

        if (isSystemEqualizerAvailable(context)) {
            openSystemAudioEffectSession(context, audioSessionId)
        } else {
            Timber.v("open built-in")
            //음향효과설정 활성화 여부
            enable = SPUtil.getValue(App.getContext(), NAME, ENABLE_EQ, false)
            if (!enable) {
                return
            }
            tryRun({
              //EQ
              equalizer?.release()

              equalizer = Equalizer(0, audioSessionId)
              equalizer?.also { equalizer ->
                equalizer.enabled = enable

                //이전에 저장된 각 주파수의 db 값을 가져옵니다.
                for (i in 0 until bandNumber) {
                  if (enable) {
                    equalizer.setBandLevel(i.toShort(), bandLevels[i])
                  }
                }
              }
            }, {
              releaseEqualizer()
            })

            tryRun({
              bassBoost?.release()
              bassBoost = BassBoost(0, audioSessionId)
              bassBoost?.also { bassBoost ->
                bassBoost.enabled = enable && bassBoost.strengthSupported
                if (bassBoost.enabled) {
                  bassBoost.setStrength(bassBoostStrength.toShort())
                }
              }
            }, {
              releaseBassBoost()
            })

            Timber.v("min: $minLevel max: $maxLevel bandNumber: $bandNumber")
        }
    }

    fun close(context: Context, audioSessionId: Int) {
        Timber.v("close")

        tryRun({
          releaseEqualizer()
        }, {

        })

        tryRun({
          releaseBassBoost()
        }, {

        })

        closeSystemAudioEffectSession(context, audioSessionId)
    }

    private fun releaseBassBoost() {
        bassBoost?.release()
        bassBoost = null
    }

    private fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    private fun openSystemAudioEffectSession(context: Context, audioSessionId: Int) {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(audioEffectsIntent)
    }

    private fun closeSystemAudioEffectSession(context: Context, audioSessionId: Int) {
        val audioEffectsIntent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        context.sendBroadcast(audioEffectsIntent)
    }

    private fun isSystemEqualizerAvailable(context: Context): Boolean {
        return isIntentAvailable(context, Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL))
    }

    private fun tryRun(block: () -> Unit, error: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Timber.w(e)
            error()
        } finally {
        }
    }


    const val REQUEST_EQ = 0


}