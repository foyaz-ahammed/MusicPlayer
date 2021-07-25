package com.kr.musicplayer.ui.activity


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kr.musicplayer.App
import com.kr.musicplayer.R
import com.kr.musicplayer.bean.misc.Category
import com.kr.musicplayer.bean.mp3.Song
import com.kr.musicplayer.helper.CloseEvent
import com.kr.musicplayer.misc.handler.MsgHandler
import com.kr.musicplayer.misc.handler.OnHandleMessage
import com.kr.musicplayer.request.ImageUriRequest
import com.kr.musicplayer.service.MusicService
import com.kr.musicplayer.ui.adapter.MainPagerAdapter
import com.kr.musicplayer.ui.fragment.*
import com.kr.musicplayer.ui.misc.DoubleClickListener
import com.kr.musicplayer.ui.misc.MultipleChoice
import com.kr.musicplayer.util.DensityUtil
import com.kr.musicplayer.util.MaterialDialogHelper
import com.kr.musicplayer.util.MusicUtil
import com.kr.musicplayer.util.SPUtil
import com.kr.musicplayer.util.Util.unregisterLocalReceiver
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * 노래보기화면
 */
open class AllSongsActivity : MenuActivity(), CloseEvent {
    @BindView(R.id.tabs)
    lateinit var mTabLayout: TabLayout

    @BindView(R.id.ViewPager)
    lateinit var mViewPager: androidx.viewpager.widget.ViewPager

    @BindView(R.id.search)
    lateinit var mSearch: EditText

    @BindView(R.id.searchContainer)
    lateinit var mSearchContainer: LinearLayout

    @BindView(R.id.toggle_select)
    lateinit var toggleSelect: ImageView

    private val mPagerAdapter by lazy {
        MainPagerAdapter(supportFragmentManager)
    }

    private val mRefreshHandler by lazy {
        MsgHandler(this)
    }
    private val mReceiver by lazy {
        MainReceiver(this)
    }

    //현재 선택된 fragment
    private var mCurrentFragment: LibraryFragment<*, *>? = null

    private var mMenuLayoutId = R.menu.menu_main

    private var mSelectedTab = -1 // TabLayout 에서 현재 선택된 Tab
    private var isFavourite = false

    override fun onDestroy() {
        super.onDestroy()
        unregisterLocalReceiver(mReceiver)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_songs)
        ButterKnife.bind(this)

        // 등록부보기를 위한 tab 위치 얻기
        mSelectedTab = intent.getIntExtra("position", -1)
        // Get value for separate tabs by 2.(Songs/Folder, Favorite/Artist)
        isFavourite = intent.getBooleanExtra("favourite", false)

        //초기화
        setUpPager()
        setUpTab()
        
        // 검색창을 누르면 검색화면 현시
        mSearch.setOnTouchListener { v: View?, event: MotionEvent? ->
            if (event!!.action == MotionEvent.ACTION_DOWN) {
                startActivity(Intent(applicationContext, SearchActivity::class.java))
            }
            true
        }

        //다중선택 상태 지우기
        MultipleChoice.isActiveSomeWhere = false
    }

    override fun onResume() {
        super.onResume()
        // Set background which is selected by user
        MaterialDialogHelper.setBackground(this, R.id.container_background)
        val fragment = (mPagerAdapter.getFragment(0) as LibraryFragment<*, *>)
        if(MaterialDialogHelper.favoriteCount > 0 && fragment is FavouriteFragment) {
            MaterialDialogHelper.favoriteCount = 0
        }
    }

    /**
     * 검색편집칸옆의 화상전환
     */
    override fun closeListener() {
        if (MultipleChoice.isActiveSomeWhere) {
            toggleSelect.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.btn_cancel_normal))
        } else {
            toggleSelect.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check))
        }
    }

    /**
     * 새 재생목록
     */
    @OnClick(R.id.toggle_select)
    fun onClick(v: View) {
        when (v.id) {
            R.id.toggle_select -> {
                val fragment = (mPagerAdapter.getFragment(mTabLayout.selectedTabPosition) as LibraryFragment<Song, *>)
                val multipleChoice = fragment.multipleChoice
                val data = (mPagerAdapter.getFragment(mTabLayout.selectedTabPosition) as LibraryFragment<*, *>).datas as List<Song>
                if (MultipleChoice.isActiveSomeWhere) {
                    multipleChoice.close()
                    toggleSelect.setImageDrawable(getDrawable(R.drawable.ic_check))
                } else {
                    if (data.isEmpty()) return
                    multipleChoice.changeSelectStatusTitleToNone()
                    multipleChoice.selectAll(data)
                    toggleSelect.setImageDrawable(getDrawable(R.drawable.btn_cancel_normal))
                }
            }
            else -> {
            }
        }
    }

    /**
     * viewPager 초기화
     */
    private fun setUpPager() {
        val categoryJson = SPUtil
                .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, "")
        val categories = if (TextUtils.isEmpty(categoryJson))
            ArrayList()
        else
            Gson().fromJson<ArrayList<Category>>(categoryJson, object : TypeToken<List<Category>>() {}.type)
        if (categories.isEmpty()) {
            val defaultCategories = Category.getDefaultLibrary(this)
            if (!isFavourite) {
                categories.addAll(listOf(defaultCategories[0], defaultCategories[1]))
            } else {
                categories.addAll(listOf(defaultCategories[2], defaultCategories[3]))
            }
        }

        mPagerAdapter.list = categories
        mMenuLayoutId = parseMenuId(mPagerAdapter.list[0].tag)
        //tab가 한개만 존재
        if (categories.size == 1) {
            mTabLayout.visibility = View.GONE
        } else {
            mTabLayout.visibility = View.VISIBLE
        }

        mViewPager.adapter = mPagerAdapter
        mViewPager.offscreenPageLimit = mPagerAdapter.count - 1
        mViewPager.currentItem = 0
        mViewPager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                val fragment = (mPagerAdapter.getFragment(mTabLayout.selectedTabPosition) as LibraryFragment<Song, *>)
                val multipleChoice = fragment.multipleChoice
                if (MultipleChoice.isActiveSomeWhere) multipleChoice.close()
                closeListener()
            }

            override fun onPageSelected(position: Int) {
                mMenuLayoutId = parseMenuId(mPagerAdapter.list[position].tag)
                mCurrentFragment = mPagerAdapter.getFragment(position) as LibraryFragment<*, *>

                invalidateOptionsMenu()
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        mCurrentFragment = mPagerAdapter.getFragment(0) as LibraryFragment<*, *>
    }

    /**
     * Menu Id 설정
     * @param tag Menu Id에 해당하는 tag
     */
    fun parseMenuId(tag: Int): Int {
        return when (tag) {
            Category.TAG_SONG -> R.menu.menu_main
            Category.TAG_ALBUM -> R.menu.menu_album
            Category.TAG_ARTIST -> R.menu.menu_artist
            Category.TAG_PLAYLIST -> R.menu.menu_playlist
            Category.TAG_FOLDER -> R.menu.menu_folder
            else -> R.menu.menu_main_simple
        }
    }

    /**
     * Menu Layout Id 를 얻는 함수
     * @return Menu Layout Id
     */
    override fun getMenuLayoutId(): Int {
        return mMenuLayoutId
    }

    /**
     * 해당 Fragment 에 대한 정렬순서 보관
     * @param sortOrder 정렬순서
     */
    override fun saveSortOrder(sortOrder: String?) {
        when (mCurrentFragment) {
            is SongFragment -> SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
                    sortOrder)
            is ArtistFragment -> SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
                    sortOrder)
        }
        mCurrentFragment?.onMediaStoreChanged()
    }

    //custom tab 초기화
    private fun setUpTab() {
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.tab_song))
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.tab_folder))
        //viewpager는 tablayout과 련결
        mTabLayout.setupWithViewPager(mViewPager)
        if (mSelectedTab > 0) {
            mViewPager.currentItem = 1
        }

        setTabClickListener()
        mTabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // Cancel long click when change tab
                (mPagerAdapter.getFragment(1 - mTabLayout.selectedTabPosition) as LibraryFragment<*, *>)
                        .multipleChoice.popupTop?.contentView?.findViewById<View>(R.id.multi_close)?.performClick()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    /**
     * Tab Click Listener 설정
     */
    private fun setTabClickListener() {
        for (i in 0 until mTabLayout.tabCount) {
            val tab = mTabLayout.getTabAt(i) ?: return
            val c = tab.javaClass
            try {
                val field = c.getDeclaredField("view")
                field.isAccessible = true
                val view = field.get(tab) as View
                view.setOnClickListener(object : DoubleClickListener() {
                    override fun onDoubleClick(v: View) {
                        // Only the first label may be "song"
                        if (mCurrentFragment is SongFragment) {
                            // Scroll to current song
                            val fragments = supportFragmentManager.fragments
                            for (fragment in fragments) {
                                if (fragment is SongFragment) {
                                    fragment.scrollToCurrent()
                                }
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Timber.w(e)
            }
        }
    }

    /**
     * MediaStore 가 변경되였을 때 호출되는 Callback
     */
    override fun onMediaStoreChanged() {
        super.onMediaStoreChanged()
        onMetaChanged()
    }

    @SuppressLint("CheckResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SETTING -> {
                if (data == null) {
                    return
                }
                if (data.getBooleanExtra(EXTRA_RECREATE, false)) { //설정후 Activity 다시 시작
                    mRefreshHandler.sendEmptyMessage(MSG_RECREATE_ACTIVITY)
                } else if (data.getBooleanExtra(EXTRA_REFRESH_ADAPTER, false)) { //Adapter 새로 고침
                    ImageUriRequest.clearUriCache()
                    mRefreshHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER)
                } else if (data.getBooleanExtra(EXTRA_REFRESH_LIBRARY, false)) { //Library 새로 고침
                    val categories = data.getSerializableExtra(EXTRA_CATEGORY) as List<Category>?
                    if (categories != null && categories.isNotEmpty()) {
                        mPagerAdapter.list = categories
                        mPagerAdapter.notifyDataSetChanged()
                        mViewPager.offscreenPageLimit = categories.size - 1
                        mMenuLayoutId = parseMenuId(mPagerAdapter.list[mViewPager.currentItem].tag)
                        mCurrentFragment = mPagerAdapter.getFragment(mViewPager.currentItem) as LibraryFragment<*, *>
                        invalidateOptionsMenu()
                        //Library가 하나만 있는 경우 tab 표시줄 숨기기
                        if (categories.size == 1) {
                            mTabLayout.visibility = View.GONE
                        } else {
                            mTabLayout.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    /**
     * 뒤로 가기 단추를 눌렀을때 선택상태이면 선택화상 교체
     */
    override fun onBackPressed() {
        var closed = false
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is LibraryFragment<*, *>) {
                val choice = fragment.choice
                if (choice.isActive) {
                    closed = true
                    choice.close()
                    toggleSelect.setImageDrawable(getDrawable(R.drawable.ic_check))
                    break
                }
            }
        }
        if (!closed) {
            super.onBackPressed()
        }
    }

    /**
     * Service 에 접속하였을때 호출되는 Callback
     * @param service 접속된 Service
     */
    override fun onServiceConnected(service: MusicService) {
        super.onServiceConnected(service)
        mRefreshHandler.postDelayed({ this.parseIntent() }, 500)
    }

    @OnHandleMessage
    fun handleInternal(msg: Message) {
        when {
            msg.what == MSG_RECREATE_ACTIVITY -> recreate()
            msg.what == MSG_RESET_MULTI -> for (temp in supportFragmentManager.fragments) {
                if (temp is LibraryFragment<*, *>) {
                    temp.adapter?.notifyDataSetChanged()
                }
            }
            msg.what == MSG_UPDATE_ADAPTER -> //Adapter 새로 고침
                for (temp in supportFragmentManager.fragments) {
                    if (temp is LibraryFragment<*, *>) {
                        temp.adapter?.notifyDataSetChanged()
                    }
                }
        }
    }

    /**
     * Parsing external open Intent
     */
    private fun parseIntent() {
        if (intent == null) {
            return
        }
        val intent = intent
        val uri = intent.data
        if (uri != null && uri.toString().isNotEmpty()) {
            MusicUtil.playFromUri(uri)
            setIntent(Intent())
        }
    }

    class MainReceiver internal constructor(mainActivity: AllSongsActivity) : BroadcastReceiver() {
        private val mRef: WeakReference<AllSongsActivity> = WeakReference(mainActivity)

        override fun onReceive(context: Context, intent: Intent?) {
            if (intent == null) {
                return
            }
            val action = intent.action
            if (action.isNullOrEmpty()) {
                return
            }
        }
    }

    companion object {
        const val EXTRA_RECREATE = "needRecreate"
        const val EXTRA_REFRESH_ADAPTER = "needRefreshAdapter"
        const val EXTRA_REFRESH_LIBRARY = "needRefreshLibrary"
        const val EXTRA_CATEGORY = "Category"

        //Set interface
        private const val REQUEST_SETTING = 1

        //설치권한
        private const val REQUEST_INSTALL_PACKAGES = 2

        private val IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 108f)

        /**
         * Check for updates
         */
        private var mAlreadyCheck: Boolean = false
    }
}

