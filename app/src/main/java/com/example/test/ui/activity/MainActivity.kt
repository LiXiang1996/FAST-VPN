package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.example.test.R
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.utils.ScreenSizeUtils
import com.example.test.ui.fragment.HomeFragment
import com.example.test.ui.fragment.SettingFragment
import com.example.test.ui.widget.NoScrollViewPager
import com.example.test.ui.widget.guideview.Guide
import com.example.test.ui.widget.guideview.GuideBuilder
import com.github.shadowsocks.bg.BaseService
import com.google.android.material.tabs.TabLayout
import kotlin.system.exitProcess


class MainActivity : BaseActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var settingFragment: SettingFragment
    lateinit var viewPager: NoScrollViewPager
    lateinit var tabLayout: TabLayout
    lateinit var frameLayout: FrameLayout
    private lateinit var fragments: Array<Fragment>
    private var builder: GuideBuilder? = null
    var guide: Guide? = null
    override var layoutId: Int = R.layout.layout_activity_main



    override fun initView() {
        homeFragment = HomeFragment()
        settingFragment = SettingFragment()
        fragments = arrayOf(homeFragment, settingFragment)
        viewPager = findViewById(R.id.fragment_container_viewpager)
        tabLayout = findViewById(R.id.nav_tab_layout)
        frameLayout = findViewById(R.id.frame_layout)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.setSelectedTabIndicator(null)
        viewPager.adapter = object :
            FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getCount(): Int {
                return fragments.size
            }

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getPageTitle(position: Int): CharSequence {
                return ""
            }
        }

        tabLayout.getTabAt(0)?.icon = ContextCompat.getDrawable(this, R.mipmap.tab_home_select)
        tabLayout.getTabAt(1)?.icon = ContextCompat.getDrawable(this, R.mipmap.tab_setting_unselect)
        tabLayout.tabIconTint = null

        if (AppVariable.isShowBanedIpDialog) showTipDialog(this)
        else showGuide()
    }

    private fun showGuide() {
        if (AppVariable.state != BaseService.State.Connected) {
            homeFragment.isShowGuideDialog = true
            val tabStrip = tabLayout.getChildAt(0) as LinearLayout
            for (i in 0 until tabStrip.childCount) {
                tabStrip.getChildAt(i).setOnTouchListener { v, event -> true }
            }
        }else {
            homeFragment.isShowGuideDialog = false
            viewPager.setCanScroll(true)
        }
    }

    override fun initListener() {
        frameLayout.setOnClickListener { }
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewPager.currentItem = tab?.position ?: 0
                if (tab?.position == 0) {
                    tab.setIcon(R.mipmap.tab_home_select)
                } else {
                    tab?.setIcon(R.mipmap.tab_setting_select)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    tab.setIcon(R.mipmap.tab_home_unselect)
                } else {
                    tab?.setIcon(R.mipmap.tab_setting_unselect)
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

    }


    private fun showTipDialog(context: Context) {
        val dialog = Dialog(context, R.style.NormalDialogStyle)
        val localView = LayoutInflater.from(context)
            .inflate(R.layout.common_global_volume_dialog, null)
        dialog.setContentView(localView)
        val window = dialog.window
        dialog.setCancelable(false)
        window?.setLayout(-2, -2) //-2 其实就是WRAP_CONTENT
        val localLayoutParams = window?.attributes
        localLayoutParams?.gravity = Gravity.CENTER
        localLayoutParams?.width = (ScreenSizeUtils.getInstance(context).screenWidth * 0.75).toInt()
        localLayoutParams?.height =
            (ScreenSizeUtils.getInstance(context).screenHeight * 0.3).toInt()
        window?.attributes = localLayoutParams
        val confirmTv = dialog.findViewById<AppCompatTextView>(R.id.volume_text_confirm)
        confirmTv.setOnClickListener {
            exitProcess(0)
            finish()
        }
        dialog.show()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (homeFragment.isShowGuideDialog && guide != null) {
                guide?.dismiss()
                guide = null
                homeFragment.isShowGuideDialog = false
                viewPager.setCanScroll(true)
                frameLayout.visibility = View.GONE
                val tabStrip = tabLayout.getChildAt(0) as LinearLayout
                for (i in 0 until tabStrip.childCount) {
                    tabStrip.getChildAt(i).setOnTouchListener { v, _ -> false }
                }
                return true
            }
            moveTaskToBack(true)
            AppVariable.isHomeBack = true
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    fun showGuideView() {
        builder = GuideBuilder()
        homeFragment.connectClickGuideLottie.post {
            homeFragment.connectClickGuideLottie.visibility = View.VISIBLE
            builder?.setTargetView(homeFragment.connectClickBtn)
                ?.setAlpha(150)
                ?.setHighTargetCorner(20)
                ?.setHighTargetPadding(10)
                ?.setOutsideTouchable(true)
                ?.setOnSlideListener {
                }
            builder?.setOnVisibilityChangedListener(object :
                GuideBuilder.OnVisibilityChangedListener {
                override fun onShown() {
                }

                override fun onDismiss() {
                    homeFragment.connectClickGuideLottie.visibility = View.GONE
                    homeFragment.connectClickGuideLottie.cancelAnimation()
                }
            })
            guide = builder?.createGuide()
            guide?.show(this)
        }
    }

}



