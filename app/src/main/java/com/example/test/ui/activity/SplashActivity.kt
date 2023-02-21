package com.example.test.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.CountDownTimer
import android.widget.ProgressBar
import com.example.test.R
import com.example.test.base.AppConstant
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
    private var isShowDialog: Boolean = false

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
        isShowDialog =
//            countryCode.toLowerCase() == "ir" || Locale.getDefault().country.toLowerCase() == "irn"
            countryCode.toLowerCase() == "usa" || Locale.getDefault().country.toLowerCase() == "us"
    }


    override fun initListener() {

    }

    override fun initData() {
        super.initData()
        try {
            val call: Call<IPBean> = RetrofitInstance.api.getIPAddress()
            call.enqueue(object : Callback<IPBean> {
                override fun onResponse(call: Call<IPBean>, response: Response<IPBean>) {
                    if (response.isSuccessful) {
                        val data: IPBean? = response.body()
                        Timber.tag(AppConstant.TAG).e("-okhttp- ${data?.country_code}")
                        isShowDialog =
                            data?.country_code?.lowercase() == "us" || data?.country_code?.lowercase() == "usa"
//                            data?.country_code?.lowercase() == "ir" || data?.country_code?.lowercase() == "irn"
                        if (data?.country_code?.isNotBlank() == true)
                            SharedPreferencesUtils.setParam(
                                this@SplashActivity,
                                AppConstant.COUNTRY_CODE,
                                data.country_code.lowercase()
                            )
                    }
                }

                override fun onFailure(call: Call<IPBean>, t: Throwable) {
                    Timber.tag(AppConstant.TAG).e("-okhttp- ${t.message}")
                }

            })
        } catch (e: Exception) {
            Timber.tag(AppConstant.TAG).e("--okhttp--$e")
        }

        countDownTimer = object : CountDownTimer(3000L, 300) {
            override fun onTick(p0: Long) {
                progress.progress = ((3000 - p0) / 30).toInt() + 1
            }

            override fun onFinish() {
                if (canJump) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.putExtra(AppConstant.SHOW_DIALOG, isShowDialog)
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