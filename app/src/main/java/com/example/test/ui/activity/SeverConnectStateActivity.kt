package com.example.test.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.bumptech.glide.Glide
import com.example.test.R
import com.example.test.ad.data.ADType
import com.example.test.ad.data.GetADData
import com.example.test.ad.utils.NativeAdManager
import com.example.test.ad.utils.OnShowAdCompleteListener
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.bar.StatusBarUtil
import com.example.test.base.data.CountryUtils
import com.example.test.base.utils.NetworkPing
import com.example.test.base.utils.SharedPreferencesUtils
import com.example.test.ui.widget.TitleView
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.utils.StartService
import kotlinx.coroutines.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


class SeverConnectStateActivity : BaseActivity() {
    private lateinit var container: LinearLayout
    private lateinit var countryImg: AppCompatImageView
    private lateinit var robotImg: AppCompatImageView
    private lateinit var countryName: AppCompatTextView
    lateinit var connectText: AppCompatTextView
    private lateinit var connectTime: Chronometer
    lateinit var titleView: TitleView
    override var layoutId: Int = R.layout.activity_server_connect_state_layout
    private lateinit var nativeAdManager: NativeAdManager
    private lateinit var nativeAdContainer: FrameLayout
    override fun initView() {
        super.initView()
        container = findViewById(R.id.server_connect_container)
        titleView = findViewById(R.id.sever_state_title_view)
        countryImg = findViewById(R.id.server_connect_country_img)
        robotImg = findViewById(R.id.server_connect_robot_img)
        countryName = findViewById(R.id.server_connect_country_text)
        connectText = findViewById(R.id.server_connect_state_text)
        connectTime = findViewById(R.id.server_connect_time_text)
        nativeAdContainer = findViewById(R.id.server_connect_state_native_ad_frame)
        connectTime.text = "00:00:00"
        StatusBarUtil.setTranslucentStatus(this)
        val country = AppVariable.country
        countryName.text =
            if (AppVariable.isFast || country.isBlank()) getString(R.string.super_fast_server) else country
        if (AppVariable.isFast) Glide.with(this).load(R.mipmap.server_default)
            .circleCrop().into(countryImg)
        else
            Glide.with(this).load(CountryUtils.getCountrySource(country)).circleCrop()
                .into(countryImg)
        nativeAdManager = NativeAdManager()
    }

    override fun onResume() {
        showNativeAD()
        super.onResume()
    }

    override fun initListener() {
        titleView.leftImg.setOnClickListener {
            finish()
        }
        connectTime.onChronometerTickListener = Chronometer.OnChronometerTickListener { cArg ->
            val time = System.currentTimeMillis() - cArg.base
            val d = Date(time)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            connectTime.text = sdf.format(d)
        }
        setConnectTime(AppVariable.state)
    }


    private fun setConnectTime(state: BaseService.State) {
        when (state) {
            BaseService.State.Connected -> {
                connectTime.setTextColor(Color.parseColor("#FFFFFF"))
                connectTime.base = System.currentTimeMillis()
                connectTime.start()
                connectText.text = getString(R.string.connection_succeed)
                container.setBackgroundResource(R.drawable.server_connect_bg)
                robotImg.setImageDrawable(getDrawable(R.mipmap.server_connect_robot_img))
            }
            BaseService.State.Stopped -> {
                connectTime.setTextColor(Color.parseColor("#80FFFFFF"))
                connectTime.text = AppVariable.connectTotalTime
                connectText.text = getString(R.string.disconnected)
                container.setBackgroundResource(R.drawable.server_disconnect_bg)
                ServersListProfile.defaultList.forEach { it.isChecked = false }
                ServersListProfile.getSmartServersList().forEach { it.isChecked = false }
                robotImg.setImageDrawable(getDrawable(R.mipmap.server_disconnect_robot_img))
            }
            else -> {}
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun showNativeAD() {
        AppVariable.nativeResultADList?.let {
            GetADData.getFindData(this, this, ADType.NATIVE_RESULT.value, nativeAdManager,
                it, nativeAdContainer, object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                    }
                })
        }
    }
}