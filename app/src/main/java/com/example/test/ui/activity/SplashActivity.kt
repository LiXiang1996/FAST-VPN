package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.widget.ProgressBar
import com.example.test.App
import com.example.test.R
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.GetJsonData
import com.example.test.ad.utils.AppOpenAdManager
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
    lateinit var countDownADTimer: CountDownTimer
    override var layoutId: Int = R.layout.activity_splash

    override fun initView() {
        StatusBarUtil.setTranslucentStatus(this)
        progress = findViewById(R.id.splash_progress)
        progress.max = 100
        val countryCode =
            SharedPreferencesUtils.getParam(this, AppConstant.COUNTRY_CODE, "").toString()
        if (countryCode.isBlank()) {
            SharedPreferencesUtils.setParam(
                this,
                AppConstant.COUNTRY_CODE,
                Locale.getDefault().country
            )
        }
        AppVariable.isShowBanedIpDialog =
            countryCode.lowercase() == "ir" || Locale.getDefault().country.lowercase() == "irn"
    }


    override fun initListener() {

    }

    override fun initData() {
        super.initData()

        //获取ad数据
        val ad: ADListBean? = GetJsonData.getData(this)
        if (ad != null) {
            Timber.tag(AppConstant.TAG)
                .e("click" + "${ad.robvn_cm} show ${ad.robvn_sm} robvn_n_home size ${ad.robvn_n_home.size}")
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
            countDownADTimer.start()
            Timber.tag(AppConstant.TAG + "Splash").e("isBackGround: ${AppVariable.isBackGround}")
            if (AppVariable.isBackGroundToSplash) {
                delay(3000)
            } else delay(1000)
            showAD()
        }
    }

    override fun onStop() {
        countDownADTimer.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        countDownTimer.cancel()
        countDownADTimer.cancel()
        super.onDestroy()
    }

    private fun showAD() {
        val application = application as? App
        if (application == null) {
            nextTo()
            return
        }
        AppVariable.openADList?.let {
            if (AppOpenAdManager.appOpenAd == null) application.loadAD(this@SplashActivity, it) {
                countDownADTimer.cancel()
                application.showAdIfAvailable(
                    this@SplashActivity, it,
                    object : OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            val intent = Intent(this@SplashActivity, MainActivity::class.java)
                            this@SplashActivity.startActivity(intent)
                            finish()
                        }
                    })
            }
            else {
                countDownADTimer.cancel()
                application.showAdIfAvailable(
                    this@SplashActivity, it,
                    object : OnShowAdCompleteListener {
                        override fun onShowAdComplete() {
                            val intent = Intent(this@SplashActivity, MainActivity::class.java)
                            this@SplashActivity.startActivity(intent)
                            finish()
                        }
                    })
            }
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