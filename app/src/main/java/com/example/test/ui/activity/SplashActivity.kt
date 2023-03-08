package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
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
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
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
    private lateinit var interstitialAaManager1: InterstitialAdManager
    private lateinit var interstitialAaManagerOpen: InterstitialAdManager
    private lateinit var nativeAdManagerHome: NativeAdManager
    private lateinit var nativeAdManagerResult: NativeAdManager
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
        interstitialAaManager1 = InterstitialAdManager()
        interstitialAaManagerOpen = InterstitialAdManager()
        appOpenAdManager = AppOpenAdManager()
        nativeAdManagerResult = NativeAdManager()
        nativeAdManagerHome = NativeAdManager()

    }


    override fun initListener() {

    }

    override fun initData() {
        if (CheckADStatus().canShowAD(this)) {
            //progress
            countDownTimer = object : CountDownTimer(3000L, 300) {
                override fun onTick(p0: Long) {
//                    progress.progress = ((3000 - p0) / 30).toInt() + 1
                }

                override fun onFinish() {
                }
            }
            countDownTimer.start()

            //ad Timer
            countDownADTimer = object : CountDownTimer(10000L, 1000) {
                override fun onTick(p0: Long) {
                    progress.progress = ((10000 - p0) / 100).toInt() + 1
                }

                override fun onFinish() {
                    if (AppVariable.isBackGroundToMain)
                        nextTo()
                    else finish()
                }
            }

            MainScope().launch {
                countDownADTimer?.start()
                Timber.tag(AppConstant.TAG).e("是否从后台切回前台: ${AppVariable.isBackGroundToSplash}")
                delay(1000)
                if (!AppVariable.isBackGroundToSplash) loadADData()//冷启动请求广告数据
                else {
                    checkCache()
                }
                if (!ADLoading.INTER_OPEN.isLoading && !ADLoading.OPEN.isLoading) loadOpenAD()
                else Timber.tag(AppConstant.TAG + "splash")
                    .e("${ADLoading.INTER_OPEN.isLoading}-----${ADLoading.OPEN.isLoading}")

            }
        } else {
            countDownTimer = object : CountDownTimer(3000L, 300) {
                override fun onTick(p0: Long) {
                    progress.progress = ((3000 - p0) / 30).toInt() + 1
                }

                override fun onFinish() {
                    Timber.tag(AppConstant.TAG + "timer").e("onFinish")
                    if (!AppVariable.isBackGroundToSplash) {
                        nextTo()
                    } else finish()
                }
            }
            countDownTimer.start()
        }
        super.initData()
    }

    private fun loadOpenAD() {
        Timber.tag(AppConstant.TAG + "splash").e("1")
        val dataOpenOpen =
            AppVariable.cacheDataList?.find { it["type"].toString() == ADType.OPEN.value }
        val dataOpenInter =
            AppVariable.cacheDataList?.find { it["type"].toString() == ADType.INTER_OPEN.value }
        //检测本地开屏缓存
        if (dataOpenOpen != null || dataOpenInter != null) {
            Timber.tag(AppConstant.TAG + "splash").e("3")
            if (dataOpenOpen != null) {
                showDataAD(dataOpenOpen["type"].toString(),
                    object : OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            countDownADTimer?.cancel()
                            if (!AppVariable.isBackGroundToSplash) {
                                val intent =
                                    Intent(this@SplashActivity, MainActivity::class.java)
                                this@SplashActivity.startActivity(intent)
                            }
                            finish()
                        }

                    })
            } else if (dataOpenInter != null) {
                showDataAD(dataOpenInter["type"].toString(),
                    object : OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            countDownADTimer?.cancel()
                            if (!AppVariable.isBackGroundToSplash) {
                                val intent =
                                    Intent(this@SplashActivity, MainActivity::class.java)
                                this@SplashActivity.startActivity(intent)
                            }
                            finish()
                        }

                    })
            } else {
                Timber.tag(AppConstant.TAG).e("splash 缓存获取失败")
            }
        } else if (AppVariable.cacheSplashADData != null && AppVariable.cacheSplashADData?.robvn_l != null) {
            Timber.tag(AppConstant.TAG + "splash").e("2")
            showDataAD(AppVariable.cacheSplashADData?.robvn_l.toString(),
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        countDownADTimer?.cancel()
                        if (!AppVariable.isBackGroundToSplash) {
                            val intent =
                                Intent(this@SplashActivity, MainActivity::class.java)
                            this@SplashActivity.startActivity(intent)
                        }
                        finish()
                    }

                })
        } else {
            Timber.tag(AppConstant.TAG + "splash").e("4 无缓存")
            showAD()
        }
    }

    private fun checkCache() {
        if (AppVariable.cacheDataList?.find { it["type"].toString() == ADType.INTER_CONNECT.value } == null && !ADLoading.INTER.isLoading) {
            AppVariable.interADList?.let {
                interstitialAaManager1.loadAd(
                    applicationContext,
                    it,
                    0,
                    type = ADType.INTER_CONNECT.value
                ) { _, _ -> }
            }
        }
        if (AppVariable.cacheDataList?.find { it["type"].toString() == ADType.NATIVE_HOME.value } == null && !ADLoading.NATIVE_HOME.isLoading) {
            //native首页
            AppVariable.nativeHomeADList?.let {
                nativeAdManagerHome.refreshAd(this, null, ADType.NATIVE_HOME.value, 0, it) {
                }
            }
        }
        if (AppVariable.cacheDataList?.find { it["type"].toString() == ADType.NATIVE_RESULT.value } == null && !ADLoading.NATIVE_RESULT.isLoading) {
            //native结果页
            AppVariable.nativeResultADList?.let {
                nativeAdManagerResult.refreshAd(this, null, ADType.NATIVE_RESULT.value, 0, it) {
                }
            }
        }
    }

    private fun showDataAD(type: String, onShowAdCompleteListener: OnShowAdCompleteListener) {
        val manager = if (type == ADType.OPEN.value) appOpenAdManager else interstitialAaManagerOpen
        Timber.tag(AppConstant.TAG + "splash").e(" 有缓存1")
        val openType =
            if (type == ADType.INTER.value || type == ADType.INTER_OPEN.value) ADType.INTER_OPEN.value else ADType.OPEN.value
        val data = AppVariable.cacheDataList?.find { it["type"].toString() == openType }
        if (data != null) {
            Timber.tag(AppConstant.TAG + "splash").e(" 有缓存2")
            if (data[AppConstant.LOAD_TIME] is Long) {
                if (CheckADStatus().wasLoadTimeLessThanNHoursAgo(
                        1,
                        (data[AppConstant.LOAD_TIME] as Long)
                    )
                ) {
                    if (AppVariable.cacheSplashADData?.robvn_l == ADType.OPEN.value && data["value"] is AppOpenAd) {
                        if (manager is AppOpenAdManager) manager.showAdIfAvailableWithData(
                            this,
                            type,
                            AppVariable.cacheSplashADData!!,
                            data["value"] as AppOpenAd,
                            onShowAdCompleteListener
                        ) { it1, it2 ->
                            GetADData.getOpenData(
                                this, manager, null, onShowAdCompleteListener, 0
                            )
                        }
                    } else if (AppVariable.cacheSplashADData?.robvn_l == ADType.INTER.value && data["value"] is InterstitialAd) {
                        if (manager is InterstitialAdManager) manager.showInterstitialWithData(
                            this,
                            mutableListOf(AppVariable.cacheSplashADData!!),
                            data["value"] as InterstitialAd,
                            ADType.INTER_OPEN.value,
                            onShowAdCompleteListener
                        )
                    }
                }
            }
        }
//        else if(){
//
//        }
        else {
            showAD()
        }
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
        //连接页面  服务器列表页面跳转首页 todo 3.2日  测试提出现在的需求 共用广告 只缓存一个
        AppVariable.interADList?.let {
            interstitialAaManager1.loadAd(
                applicationContext,
                it,
                0,
                type = ADType.INTER_CONNECT.value
            ) { _, _ -> }
        }


        //native首页
        AppVariable.nativeHomeADList?.let {
            nativeAdManagerHome.refreshAd(this, null, ADType.NATIVE_HOME.value, 0, it) {
            }
        }

        //native结果页
        AppVariable.nativeResultADList?.let {
            nativeAdManagerResult.refreshAd(this, null, ADType.NATIVE_RESULT.value, 0, it) {
            }
        }

    }


    override fun onDestroy() {
        countDownTimer?.cancel()
        countDownADTimer?.cancel()
        super.onDestroy()
    }

    private fun showAD() {
        AppVariable.openADList?.let {
            GetADData.getOpenData(
                this,
                appOpenAdManager,
                interstitialAaManagerOpen,
                object : OnShowAdCompleteListener {
                    override fun onShowAdComplete() {
                        countDownADTimer?.cancel()
                        if (!AppVariable.isBackGroundToSplash) {
                            val intent =
                                Intent(this@SplashActivity, MainActivity::class.java)
                            this@SplashActivity.startActivity(intent)
                        }
                        finish()
                    }
                },
                0
            )

        }

    }

    fun nextTo() {
        if (AppVariable.isBackGroundToSplash) {
            AppVariable.isBackGroundToSplash = false
        }
        Timber.tag(AppConstant.TAG + "splash").e("next to")
        if (canJump) {
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            this@SplashActivity.startActivity(intent)
            finish()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}