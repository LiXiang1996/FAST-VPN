package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.widget.ProgressBar
import com.example.test.R
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.bar.StatusBarUtil
import com.example.test.base.data.IPBean
import com.example.test.base.net.RetrofitInstance
import com.example.test.base.utils.SharedPreferencesUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.Locale


@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    lateinit var progress: ProgressBar
    lateinit var countDownTimer: CountDownTimer
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
//            countryCode.toLowerCase() == "ir" || Locale.getDefault().country.toLowerCase() == "irn"
            countryCode.toLowerCase() == "us" || Locale.getDefault().country.toLowerCase() == "usa"
    }


    override fun initListener() {

    }

    override fun initData() {
        super.initData()
        countDownTimer = object : CountDownTimer(3000L, 300) {
            override fun onTick(p0: Long) {
                progress.progress = ((3000 - p0) / 30).toInt() + 1
            }

            override fun onFinish() {
                if (canJump) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    this@SplashActivity.startActivity(intent)
                }
                finish()
            }
        }
        countDownTimer.start()
    }

    override fun onDestroy() {
        countDownTimer.cancel()
        super.onDestroy()
    }


}