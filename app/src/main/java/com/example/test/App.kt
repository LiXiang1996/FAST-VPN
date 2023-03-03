package com.example.test

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.test.ad.data.ADListBean
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.data.RemoteProfile
import com.example.test.base.data.ToProfile
import com.example.test.ui.activity.MainActivity
import com.example.test.ui.activity.ServersListProfile
import com.example.test.ui.activity.SplashActivity
import com.github.shadowsocks.Core
import com.github.shadowsocks.database.Profile
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.util.*


class App : Application(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private var activityCount = 0
    private var currentActivity: Activity? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
            private set
        var remoteADListData: ADListBean? = null
    }


    override fun onCreate() {
        super.onCreate()
        fixWebViewDataDirectoryBug()
        Firebase.initialize(this)
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                listOf(
                    "001233B6A65D1EF088BA61537BB77C43",
                    "1632B27F26C7337301F620C5BE220833"
                )
            ).build()
        )
        MobileAds.initialize(this) {}
        registerActivityLifecycleCallbacks(this)
        context = applicationContext
        Core.init(this, MainActivity::class)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//        getRemoteConfig()
        var str = " {\"Dallas\",\"aaa\",\"bbb\",\"cccc\"}"
        getServerSmartDataList2(str)
    }


    private fun fixWebViewDataDirectoryBug() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            val packageName = this.packageName
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
    }


    /** ActivityLifecycleCallback methods. */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (AppVariable.isBackGround) {
            AppVariable.isBackGround = false
            Timber.tag(AppConstant.TAG).e("${(System.currentTimeMillis() - AppVariable.exitAppTime) / 1000}")
            if ((System.currentTimeMillis() - AppVariable.exitAppTime) / 1000 > 3) {
                if (activity !is SplashActivity) {//不在启屏页做重复跳转
                    AppVariable.isBackGroundToSplash = true
                    val intent = Intent(activity, SplashActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        }
        activityCount++
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {
        currentActivity = activity
        activityCount--
        if (activityCount == 0) {
            AppVariable.isBackGround = true
            AppVariable.exitAppTime = System.currentTimeMillis()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}


    private fun getServerDataList(list: String) {
        try {
            val gson = Gson()
            val resultBean: MutableList<RemoteProfile> =
                gson.fromJson(list, object : TypeToken<List<RemoteProfile?>?>() {}.type)
            if ((resultBean.size) > 0) {
//                Timber.tag(AppConstant.TAG).e("remoteConfig $list  size:${resultBean.size}")
                val profileList = mutableListOf<Profile>()
                resultBean.forEach { profileList.add(ToProfile.remoteProfileToProfile(it)) }
//                Timber.tag(AppConstant.TAG).e("profileList $profileList  size:${profileList.size}")
                if ((profileList.size) > 0) {
                    ServersListProfile.defaultList.clear()
                    profileList.forEach {
                        ServersListProfile.defaultList.add(it)
                    }
                    Timber.tag(AppConstant.TAG)
                        .e("servers${ServersListProfile.getServersList().size}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(AppConstant.TAG).e(e)
        }
    }

    private fun getServerSmartDataList(list: String) {
        try {
            val gson = Gson()
            val resultBean: MutableList<RemoteProfile> =
                gson.fromJson(list, object : TypeToken<List<RemoteProfile?>?>() {}.type)
            if ((resultBean.size) > 0) {
                val profileList = mutableListOf<Profile>()
                resultBean.forEach { profileList.add(ToProfile.remoteProfileToProfile(it)) }
                Timber.tag(AppConstant.TAG)
                    .e("smartProfileList $profileList  size:${profileList.size}")
                if ((profileList.size) > 0) {
                    profileList.forEach { it1 ->
                        val findData = ServersListProfile.getServersList().find { it2 ->
                            it2.city == it1.city
                        }
                        if (findData != null) {
                            it1.name = findData.name//国家名
                            it1.host = findData.host//ip
                            it1.remotePort = findData.remotePort//端口
                            it1.password = findData.password//密码
                            it1.method = findData.method
                        }
                    }
                    ServersListProfile.setSmartListProfile(profileList)
                    Timber.tag(AppConstant.TAG)
                        .e("smart servers${profileList.size}")
                }
            }
        } catch (e: Exception) {
            Timber.tag(AppConstant.TAG).e(e)
        }
    }

    private fun getServerSmartDataList2(str: String) {
        val arr: List<String> = str.split(",")
        val list = mutableListOf<String>()
        arr.forEach {
            val s = it
                .replace("\"", "")
                .replace("{", "")
                .replace("}", "")
                .replace(" ", "").trim()
            list.add(s)
            println("lixiang-----$s  ${list.size}")
        }


    }

    private fun getADList(list: String) {
        try {
            val gson = Gson()
            val resultBean: ADListBean = gson.fromJson(list, ADListBean::class.java)
            remoteADListData = resultBean
        } catch (e: Exception) {
            Timber.tag(AppConstant.TAG).e(e)
        }
    }


    private fun getRemoteConfig() {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        var listServer: String? = ""
        var listAD: String? = ""
        var listServerSmart: String? = ""
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listServer = remoteConfig.getString("robvn_ser")
                listServerSmart = remoteConfig.getString("robvn_smart")
                listAD = remoteConfig.getString("robvn_ad")
                listServer?.let { getServerDataList(it) }
                listServerSmart?.let { getServerSmartDataList2(it) }
                listAD?.let { getADList(it) }
            }
        }
        if (listServer?.isEmpty() == true && listServer?.isBlank() == true) {
            listServer = remoteConfig.getString("robvn_ser")
            if (listServer?.isEmpty() == true || listServer?.isBlank() == true) ServersListProfile.getServersList()
            else {
                listServer?.let { getServerDataList(it) }
            }
        }
        if (listServerSmart?.isEmpty() == true && listServerSmart?.isBlank() == true) {
            listServerSmart = remoteConfig.getString("robvn_smart")
            if (listServerSmart?.isNotEmpty() == true || listServerSmart?.isNotBlank() == true)
                listServerSmart?.let {
                    getServerSmartDataList2(it)
                }
        }

        if (listAD?.isEmpty() == true && listAD?.isBlank() == true) {
            listAD = remoteConfig.getString("robvn_ad")
            if (listServer?.isNotEmpty() == true && listServer?.isNotBlank() == true && listAD != null) {
                getADList(listAD!!)
            }
        }
    }

}
