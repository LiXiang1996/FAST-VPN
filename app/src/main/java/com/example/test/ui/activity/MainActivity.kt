package com.example.test.ui.activity

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.test.R
import com.example.test.base.AppConstant
import com.example.test.ui.fragment.HomeFragment
import com.example.test.ui.fragment.SettingFragment
import com.google.android.material.tabs.TabLayout
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var settingFragment: SettingFragment
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    lateinit var frameLayout: FrameLayout
    private lateinit var fragments: Array<Fragment>
    var isShowDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_main)
        initView()
        initListener()
    }

    private fun initView() {
        isShowDialog = intent.getBooleanExtra(AppConstant.SHOW_DIALOG, false)
        if (isShowDialog) showTipDialog(this)
        else showGuide()
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

    }

    private fun showGuide() {
        TODO("Not yet implemented")
    }

    private fun initListener() {
        frameLayout.setOnClickListener {  }
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
        val dialog = Dialog(context)
        val localView = LayoutInflater.from(context)
            .inflate(R.layout.common_global_volume_dialog, null) //设置自定义的弹窗UI
        dialog.setContentView(localView)
        val window = dialog.window
        dialog.setCancelable(false)
        window?.setLayout(-2, -2) //-2 其实就是WRAP_CONTENT
        val localLayoutParams = window?.attributes
        localLayoutParams?.gravity = Gravity.BOTTOM
        window?.attributes = localLayoutParams
        val confirmTv = dialog.findViewById<AppCompatTextView>(R.id.volume_text_confirm)
        confirmTv.setOnClickListener {
            exitProcess(0)
            finish()
        }
        dialog.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

}

