package com.example.test.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.example.test.R
import com.example.test.base.data.IPBean
import com.example.test.base.net.RetrofitInstance
import com.example.test.base.network.NetStateChangeObserver
import com.example.test.base.network.NetStateChangeReceiver
import com.example.test.base.network.NetworkType
import com.example.test.base.utils.ScreenSizeUtils
import com.example.test.base.utils.SharedPreferencesUtils
import com.example.test.ui.activity.SplashActivity
import com.example.test.ui.widget.CheckIPUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import kotlin.system.exitProcess

abstract class BaseActivity : AppCompatActivity(), InitInterface, NetStateChangeObserver {

    abstract var layoutId: Int
    var canJump: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        initView()
        initListener()
        initData()
        NetStateChangeReceiver.registerObserver(this)
        NetStateChangeReceiver.registerReceiver(this)
    }

    override fun onStart() {
        canJump = true
        super.onStart()
    }
    override fun onResume() {
        canJump = true
        super.onResume()
    }

    override fun onDestroy() {
        NetStateChangeReceiver.unRegisterObserver(this)
        NetStateChangeReceiver.unRegisterReceiver(this)
        super.onDestroy()
    }

    override fun onNetConnected(networkType: NetworkType?, context: Context) {
//        Timber.tag(AppConstant.TAG).e(networkType?.name)
        try {
            val call: Call<IPBean> = RetrofitInstance.api.getIPAddress()
            call.enqueue(object : Callback<IPBean> {
                override fun onResponse(call: Call<IPBean>, response: Response<IPBean>) {
                    if (response.isSuccessful) {
                        val data: IPBean? = response.body()
                        AppVariable.isShowBanedIpDialog = CheckIPUtils.checkIpIsOK(data?.country_code?.lowercase())
                        if (AppVariable.isShowBanedIpDialog && this@BaseActivity !is SplashActivity)
                            showTipDialog(context)
                        if (data?.country_code?.isNotBlank() == true)
                            SharedPreferencesUtils.setParam(
                                this@BaseActivity,
                                AppConstant.COUNTRY_CODE,
                                data.country_code.lowercase()
                            )
                    }
                }

                override fun onFailure(call: Call<IPBean>, t: Throwable) {
//                    Timber.tag(AppConstant.TAG).e("-okhttp- ${t.message}")
                }

            })
        } catch (e: Exception) {
//            Timber.tag(AppConstant.TAG).e("--okhttp--$e")
        }
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


    override fun onNetDisconnected() {
    }

    override fun onPause() {
        canJump = false
        super.onPause()
    }

    override fun onStop() {
        canJump = false
        super.onStop()
    }

    override fun initView() {
    }

    override fun initListener() {
    }

    override fun initData() {
    }

}

interface InitInterface {
    fun initView()
    fun initListener()
    fun initData()


}
