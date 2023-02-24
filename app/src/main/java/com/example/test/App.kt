package com.example.test

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.test.ad.data.ADListBean
import com.example.test.ad.utils.AppOpenAdManager
import com.example.test.ad.utils.OnShowAdCompleteListener
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

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
            private set
    }

    private lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        fixWebViewDataDirectoryBug()
        Firebase.initialize(this)
        MobileAds.initialize(this) {}
        registerActivityLifecycleCallbacks(this)
        context = applicationContext
        Core.init(this, MainActivity::class)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
        RequestConfiguration.Builder().setTestDeviceIds(listOf("1632B27F26C7337301F620C5BE220833"))
//        getRemoteConfig()
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
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
        if (AppVariable.isBackGround) {
            AppVariable.isBackGround = false
            if (activity !is SplashActivity) {//不在启屏页做重复跳转
                AppVariable.isBackGroundToSplash = true
                val intent = Intent(activity, SplashActivity::class.java)
                activity.startActivity(intent)
            }
        }
        activityCount++

    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0 ) {
            AppVariable.isBackGround = true
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Shows an app open ad.
     */
    fun showAdIfAvailable(activity: Activity, openADId:List<ADListBean.ADBean>,onShowAdCompleteListener: OnShowAdCompleteListener) {
        appOpenAdManager.showAdIfAvailable(activity, openADId,onShowAdCompleteListener)
    }

    private fun getDataList(list: String) {
        try {
            val gson = Gson()
            val resultBean: MutableList<RemoteProfile> =
                gson.fromJson(list, object : TypeToken<List<RemoteProfile?>?>() {}.type)
            if ((resultBean.size) > 0) {
                Timber.tag(AppConstant.TAG).e("remoteConfig $list  size:${resultBean.size}")
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


    private fun getRemoteConfig() {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        var list: String? = ""
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    list = remoteConfig.getString("axxxxxx")
                    list?.let { getDataList(it) }
                }
            }
        if (list?.isEmpty() == true && list?.isBlank() == true) {
            list = remoteConfig.getString("axxxxxx")
            if (list?.isEmpty() == true || list?.isBlank() == true) ServersListProfile.getServersList()
            else {
                list?.let { getDataList(it) }
            }
        }
    }

}
