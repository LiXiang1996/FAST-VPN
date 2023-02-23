package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
import com.example.test.App
import com.example.test.R
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.GetJsonData
import com.example.test.ad.utils.OnShowAdCompleteListener
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.bar.StatusBarUtil
import com.example.test.base.data.IPBean
import com.example.test.base.net.RetrofitInstance
import com.example.test.base.utils.SharedPreferencesUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.Locale


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
            countryCode.toLowerCase() == "ir" || Locale.getDefault().country.toLowerCase() == "irn"
//            countryCode.toLowerCase() == "us" || Locale.getDefault().country.toLowerCase() == "usa"
    }


    override fun initListener() {

    }

    override fun initData() {
        super.initData()

        val ad:ADListBean? = GetJsonData.getJson(this)
        if (ad!=null){
            Timber.tag(AppConstant.TAG).e("${ad.click} show  size ${ad.robvn_n_home.size}")
        }else{
            Timber.tag(AppConstant.TAG).e("AD DATA NULL")
        }


        // TODO: 此处loading最少展示一秒，一秒后开始去加载广告数据，如果有缓存就加载，没有就去拉 ，拿到数据去展示广告（广告请求两次），失败就走，成功展示，总计<10秒
        countDownTimer = object : CountDownTimer(3000L, 300) {
            override fun onTick(p0: Long) {
                progress.progress = ((3000 - p0) / 30).toInt() + 1
            }

            override fun onFinish() {
            }
        }
        countDownTimer.start()

        countDownADTimer = object : CountDownTimer(10000L, 1000) {
            override fun onTick(p0: Long) {
                Timber.tag(AppConstant.TAG).e(p0.toString())
            }
            override fun onFinish() {
                if (canJump) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    this@SplashActivity.startActivity(intent)
                }
                finish()
            }
        }
        lifecycleScope.launch {
            countDownADTimer.start()
            delay(1000)
            showAD()
        }
    }

    override fun onDestroy() {
        countDownTimer.cancel()
        countDownADTimer.cancel()
        super.onDestroy()
    }

    fun showAD(){
        val application = application as? App
        if (application == null) {
            if (canJump) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                this@SplashActivity.startActivity(intent)
            }
            finish()
            return
        }
        application.showAdIfAvailable(
            this@SplashActivity,
            object : OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                    if (canJump) {
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        this@SplashActivity.startActivity(intent)
                        finish()
                    }
                }
            })
    }


}