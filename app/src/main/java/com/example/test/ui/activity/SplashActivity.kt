package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.widget.ProgressBar
import com.example.test.App
import com.example.test.R
import com.example.test.ad.data.*
import com.example.test.ad.utils.AppOpenAdManager
import com.example.test.ad.utils.InterstitialAdManager
import com.example.test.ad.utils.NativeAdManager
import com.example.test.ad.utils.OnShowAdCompleteListener
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.bar.StatusBarUtil
import com.example.test.base.utils.SharedPreferencesUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*


@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    lateinit var progress: ProgressBar
    lateinit var countDownTimer: CountDownTimer
    var countDownADTimer: CountDownTimer? = null
    override var layoutId: Int = R.layout.activity_splash
    private lateinit var interstitialAaManager: InterstitialAdManager
    private lateinit var nativeAdManager: NativeAdManager
    private lateinit var appOpenAdManager: AppOpenAdManager


    override fun initView() {
        StatusBarUtil.setTranslucentStatus(this)
        progress = findViewById(R.id.splash_progress)
        progress.max = 100
        val countryCode =
            SharedPreferencesUtils.getParam(this, AppConstant.COUNTRY_CODE, "").toString()
        if (countryCode.isBlank()) {
            SharedPreferencesUtils.setParam(
                this, AppConstant.COUNTRY_CODE, Locale.getDefault().country
            )
        }
        AppVariable.isShowBanedIpDialog =
            countryCode.lowercase() == "ir" || Locale.getDefault().country.lowercase() == "irn"
        interstitialAaManager = InterstitialAdManager()
        appOpenAdManager = AppOpenAdManager()
        nativeAdManager = NativeAdManager()

    }


    override fun initListener() {

    }

    override fun initData() {
        if (CheckADStatus().canShowAD(this)) {
            loadADData()
            //progress
            countDownTimer = object : CountDownTimer(3000L, 300) {
                override fun onTick(p0: Long) {
                    progress.progress = ((3000 - p0) / 30).toInt() + 1
                }

                override fun onFinish() {
                }
            }
            countDownTimer.start()

            //ad Timer
            countDownADTimer = object : CountDownTimer(10000L, 1000) {
                override fun onTick(p0: Long) {
                }

                override fun onFinish() {
                    nextTo()
                }
            }

            MainScope().launch {
                countDownADTimer?.start()
                Timber.tag(AppConstant.TAG)
                    .e("是否从后台切回前台: ${AppVariable.isBackGround}")
                if (AppVariable.isBackGroundToSplash) {
                    delay(3000)
                } else delay(1000)
                showAD()
            }
        } else {
            countDownTimer = object : CountDownTimer(3000L, 300) {
                override fun onTick(p0: Long) {
                    progress.progress = ((3000 - p0) / 30).toInt() + 1
                }

                override fun onFinish() {
                    nextTo()
                }
            }
            countDownTimer.start()
        }
        super.initData()
    }

    private fun loadADData() {
        //1、获取所有ad数据
        val ad: ADListBean? = GetJsonData.getData(this)
        if (ad != null) {
//            Timber.tag(AppConstant.TAG)
//                .e("click" + "${ad.robvn_cm} show ${ad.robvn_sm} robvn_o_open size ${ad.robvn_o_open.size}")
            AppVariable.openADList =
                ad.robvn_o_open.sortedByDescending { it.robvn_p } as MutableList<ADListBean.ADBean>
            AppVariable.interADList =
                ad.robvn_i_2R.sortedByDescending { it.robvn_p } as MutableList<ADListBean.ADBean>
            AppVariable.nativeHomeADList =
                ad.robvn_n_home.sortedByDescending { it.robvn_p } as MutableList<ADListBean.ADBean>
            AppVariable.nativeResultADList =
                ad.robvn_n_result.sortedByDescending { it.robvn_p } as MutableList<ADListBean.ADBean>
        } else {
            Timber.tag(AppConstant.TAG).e("AD DATA NULL")
        }

        //2、按需求开始load所谓位置的广告
        //开屏页

        //服务器列表页面跳转首页
        AppVariable.interADList?.let {
            interstitialAaManager.loadAd(
                applicationContext,
                it,
                0,
                type = ADType.INTER_SERVER.value
            ) { _, _ -> }
        }
        //连接页面
        AppVariable.interADList?.let {
            interstitialAaManager.loadAd(
                applicationContext,
                it,
                0,
                type = ADType.INTER_CONNECT.value
            ) { _, _ -> }
        }
        //native结果页
        AppVariable.nativeHomeADList?.let {
            nativeAdManager.refreshAd(this, null, ADType.NATIVE_RESULT.value, 0, it) {

            }
        }
        //native首页
        AppVariable.nativeResultADList?.let {
            nativeAdManager.refreshAd(this, null, ADType.NATIVE_HOME.value, 0, it) {

            }
        }
    }

    override fun onStop() {
        countDownADTimer?.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        countDownADTimer?.cancel()
        super.onDestroy()
    }

    private fun showAD() {
        AppVariable.openADList?.let {
            GetADData.getFindData(this, this, ADType.OPEN.value, appOpenAdManager,
                it, null, object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        countDownADTimer?.cancel()
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        this@SplashActivity.startActivity(intent)
                        finish()
                    }
                })
        }
    }

    fun nextTo() {
        if (AppVariable.isBackGroundToSplash) {
            AppVariable.isBackGroundToSplash = false
        }
        if (canJump) {
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            this@SplashActivity.startActivity(intent)
            finish()
        }
    }


}