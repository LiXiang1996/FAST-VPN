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
import com.example.test.ui.activity.SeverConnectStateActivity
import com.example.test.ui.activity.SplashActivity
import com.github.shadowsocks.Core
import com.github.shadowsocks.database.Profile
import com.google.android.gms.ads.AdActivity
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
    private var activityList = mutableListOf<Activity>()

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
        getServerSmartDataList(str)
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
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Timber.tag(AppConstant.TAG).e("create ${activity.localClassName}")
        activityList.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        Timber.tag(AppConstant.TAG).e("start ${activity.localClassName}")
        if (AppVariable.isBackGround) {
            AppVariable.isBackGround = false
            //home退出App
            if ((System.currentTimeMillis() - AppVariable.exitAppTime) / 1000 > 3) {
                jump(activity)
            } else if (AppVariable.isHomeBack) {//back 退出
                jump(activity)
                AppVariable.isHomeBack = false
            }
        }
        activityCount++
    }

    private fun jump(activity: Activity) {
        Timber.tag(AppConstant.TAG).e("从后台切回前台")
        activityList.forEach {
            if (it is AdActivity) {
                Timber.tag(AppConstant.TAG).e("finish and remove ${it.localClassName}")
                it.finish()
                activityList.remove(it)
            }
        }
        if (activity !is SplashActivity) {//不在启屏页做重复跳转
            AppVariable.isBackGroundToSplash = true
            if (activity is MainActivity) AppVariable.isBackGroundToMain = true
            if (activity is SeverConnectStateActivity) AppVariable.isBackGroundToResult =
                true
            val intent = Intent(activity, SplashActivity::class.java)
            Timber.tag(AppConstant.TAG).e("A ${activity.localClassName}   to SplashActivity")
            activity.startActivity(intent)
        } else {
            AppVariable.isBackGroundToSplash = true
            val intent = Intent(activity, SplashActivity::class.java)
            Timber.tag(AppConstant.TAG).e("intent ${activity.localClassName}")
            activity.startActivity(intent)
        }
    }

    @SuppressLint("BinaryOperationInTimber")
    override fun onActivityResumed(activity: Activity) {
        Timber.tag(AppConstant.TAG).e("resume ${activity.localClassName}")
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {
        Timber.tag(AppConstant.TAG).e("stop ${activity.localClassName}")
        currentActivity = activity
        activityCount--
        if (activityCount == 0) {
            AppVariable.isBackGround = true
            AppVariable.exitAppTime = System.currentTimeMillis()
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        Timber.tag(AppConstant.TAG).e("destroy ${activity.localClassName}")
        activityList.remove(activity)
    }


    private fun getServerDataList(list: String) {
        try {
            val gson = Gson()
            val resultBean: MutableList<RemoteProfile> =
                gson.fromJson(list, object : TypeToken<List<RemoteProfile?>?>() {}.type)
            if ((resultBean.size) > 0) {
                val profileList = mutableListOf<Profile>()
                resultBean.forEach { profileList.add(ToProfile.remoteProfileToProfile(it)) }
                Timber.tag(AppConstant.TAG).e("profileList $profileList  size:${profileList.size}")
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

//    private fun getServerSmartDataList(list: String) {
//        try {
//            val gson = Gson()
//            val resultBean: MutableList<RemoteProfile> =
//                gson.fromJson(list, object : TypeToken<List<RemoteProfile?>?>() {}.type)
//            if ((resultBean.size) > 0) {
//                val profileList = mutableListOf<Profile>()
//                resultBean.forEach { profileList.add(ToProfile.remoteProfileToProfile(it)) }
//                Timber.tag(AppConstant.TAG)
//                    .e("smartProfileList $profileList  size:${profileList.size}")
//                if ((profileList.size) > 0) {
//                    profileList.forEach { it1 ->
//                        val findData = ServersListProfile.getServersList().find { it2 ->
//                            it2.city == it1.city
//                        }
//                        if (findData != null) {
//                            it1.name = findData.name//国家名
//                            it1.host = findData.host//ip
//                            it1.remotePort = findData.remotePort//端口
//                            it1.password = findData.password//密码
//                            it1.method = findData.method
//                        }
//                    }
//                    ServersListProfile.setSmartListProfile(profileList)
//                    Timber.tag(AppConstant.TAG)
//                        .e("smart servers${profileList.size}")
//                }
//            }
//        } catch (e: Exception) {
//            Timber.tag(AppConstant.TAG).e(e)
//        }
//    }

    private fun getServerSmartDataList(str: String) {
        val arr: List<String> = str.split(",")
        val list = mutableListOf<String>()
        arr.forEach {
            val s = it
                .replace("\"", "")
                .replace("{", "")
                .replace("}", "")
                .replace(" ", "").trim()
            list.add(s)
        }
        val smartList: MutableList<Profile> = mutableListOf()
        list.forEach { it1 ->
            ServersListProfile.getServersList().forEach { it2 ->
                if (it1 == it2.city) smartList.add(it2)
            }
        }
        if (smartList.size > 0) ServersListProfile.setSmartListProfile(smartList)
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
                listServerSmart?.let { getServerSmartDataList(it) }
                listAD?.let { getADList(it) }
            }
        }
        if (listServer?.isEmpty() == true && listServer?.isBlank() == true) {
            listServer = remoteConfig.getString("robvn_ser")
            if (listServer?.isEmpty() == true || listServer?.isBlank() == true)//为空不对本地数据做操作
            else {
                listServer?.let { getServerDataList(it) }
            }
        }
        if (listServerSmart?.isEmpty() == true && listServerSmart?.isBlank() == true) {
            listServerSmart = remoteConfig.getString("robvn_smart")
            if (listServerSmart?.isNotEmpty() == true || listServerSmart?.isNotBlank() == true)
                listServerSmart?.let {
                    getServerSmartDataList(it)
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
