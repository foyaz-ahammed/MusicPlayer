package com.kr.musicplayer.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.kr.musicplayer.R
import com.kr.musicplayer.helper.ShakeDetector
import com.kr.musicplayer.misc.MediaScanner
import com.kr.musicplayer.misc.floatpermission.FloatWindowManager
import com.kr.musicplayer.service.Command
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.ui.dialog.FolderChooserDialog
import com.kr.musicplayer.ui.dialog.LoadingDialog
import com.kr.musicplayer.util.*
import java.io.File

/**
 * 설정화면
 */
class SettingsActivity : ToolbarActivity(), FolderChooserDialog.FolderCallback, LoadingDialog.LoadingPauseCallback {
    @BindView(R.id.setting_shake_switch)
    lateinit var mShakeSwitch: SwitchCompat

    @BindView(R.id.setting_wakeup_switch)
    lateinit var mWakeupSwitch: SwitchCompat

    @BindView(R.id.setting_headset_switch)
    lateinit var mHeadsetSwitch: SwitchCompat

    @BindView(R.id.setting_lyric_switch)
    lateinit var mLyricSwitch: SwitchCompat

    var mediaScanner : MediaScanner? = null

    val REQUEST_OVERLAY_PERMISISON = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        ButterKnife.bind(this)
        setUpToolbar(getString(R.string.setting))

        val keyWord = arrayOf(SPUtil.SETTING_KEY.SHAKE, SPUtil.SETTING_KEY.WAKE, SPUtil.SETTING_KEY.HEADSET, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW)

        ButterKnife.apply(arrayOf(mShakeSwitch, mWakeupSwitch, mHeadsetSwitch, mLyricSwitch)) { view, index ->
            view.isChecked = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, keyWord[index], false)
            view.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
                override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                    SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, keyWord[index], isChecked)
                    when (buttonView.id) {
                        //흔들기
                        R.id.setting_shake_switch -> if (isChecked) {
                            ShakeDetector.getInstance().beginListen()
                        } else {
                            ShakeDetector.getInstance().stopListen()
                        }
                        R.id.setting_lyric_switch -> {
                            if (isChecked && !FloatWindowManager.getInstance().checkPermission(mContext)) {
                                mLyricSwitch.setOnCheckedChangeListener(null);
                                mLyricSwitch.isChecked = false;
                                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, keyWord[index], false)
                                mLyricSwitch.setOnCheckedChangeListener(this);
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                intent.data = Uri.parse("package:$packageName")
                                if (Util.isIntentAvailable(mContext, intent))
                                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISISON)
                                ToastUtil.show(mContext, R.string.plz_give_float_permission)
                                return
                            }
                            val intent = MusicUtil.makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC)
                            intent.putExtra(MusicService.EXTRA_DESKTOP_LYRIC, mLyricSwitch.isChecked)
                            Util.sendLocalBroadcast(intent)
                        }
                        R.id.setting_wakeup_switch -> {
                            // FLAG_KEEP_SCREEN_ON 설정
                            if (isChecked || SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, keyWord[index], false)) {
                                this@SettingsActivity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            } else {
                                this@SettingsActivity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        }
                    }

                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_OVERLAY_PERMISISON) {
            if (!FloatWindowManager.getInstance().checkPermission(mContext)) {
                mLyricSwitch.isChecked = false;
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false)
            } else {
                mLyricSwitch.isChecked = true;
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //사용자가 선택한 화상으로 배경 설정
        MaterialDialogHelper.setBackground(this, R.id.container_background)
    }

    override fun onDestroy() {
        mediaScanner?.connection?.disconnect()
        mediaScanner?.connection = null
        mediaScanner?.loadingDialog?.dismiss()
        mediaScanner?.status?.cancel()
        mediaScanner?.status = null
        mediaScanner = null

        super.onDestroy()
    }

    override fun onFolderSelection(dialog: FolderChooserDialog, folder: File) {
        if (folder.exists() && folder.isDirectory && folder.list() != null) {
            SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MANUAL_SCAN_FOLDER, folder.absolutePath)
        }

        mediaScanner = MediaScanner(this)
        mediaScanner?.scanFiles(folder)
    }

    @OnClick(
            R.id.setting_shake_container,
            R.id.setting_lyric_container,
            R.id.setting_wakeup_container,
            R.id.setting_headset_container,
            R.id.setting_scan_container
    )
    fun onClick(v: View) {
        when (v.id) {
            R.id.setting_shake_container -> mShakeSwitch.isChecked = !mShakeSwitch.isChecked
            R.id.setting_lyric_container -> mLyricSwitch.isChecked = !mLyricSwitch.isChecked
            R.id.setting_wakeup_container -> mWakeupSwitch.isChecked = !mWakeupSwitch.isChecked
            R.id.setting_headset_container -> mHeadsetSwitch.isChecked = !mHeadsetSwitch.isChecked
            R.id.setting_scan_container -> {
                val initialFile = File(
                        SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.MANUAL_SCAN_FOLDER, ""))
                val builder = FolderChooserDialog.Builder(this)
                        .chooseButton(R.string.choose_folder)
                        .tag("Scan")
                        .allowNewFolder(false, R.string.new_folder)
                if (initialFile.exists() && initialFile.isDirectory && initialFile.list() != null) {
                    builder.initialPath(initialFile.absolutePath)
                }
                builder.show()
            }
        }
    }

    /**
     * Media Scan 중지
     */
    override fun onLoadingPause() {
        stopLoading()
    }

    private fun stopLoading() {
        val v = layoutInflater.inflate(R.layout.dialog_scan_stop, null)
        val p = v.findViewById<TextView>(R.id.confirm)
        val n = v.findViewById<TextView>(R.id.cancel)
        val title = v.findViewById<TextView>(R.id.title)
        title.setText(R.string.scan_stop_title)
        val dialog = AlertDialog.Builder(this, R.style.CustomDialog)
                .setView(v)
                .create()
        dialog.setOnCancelListener { it ->
            mediaScanner?.isPause = false
            it.dismiss()
        }
        n.setOnClickListener { v1: View? ->
            mediaScanner?.isPause = false
            dialog.dismiss()
        }
        p.setOnClickListener { v1: View? ->
            mediaScanner?.cancelSubscription()
            dialog.dismiss()
        }
        dialog.show()

        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(getDrawable(R.drawable.round_gray_top))
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        layoutParams.height = -2
        dialog.window?.attributes = layoutParams
    }
}