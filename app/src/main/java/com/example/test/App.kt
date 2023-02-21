package com.example.test

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.example.test.base.AppConstant
import com.example.test.base.data.RemoteProfile
import com.example.test.base.data.ToProfile
import com.example.test.ui.activity.MainActivity
import com.example.test.ui.activity.ServersListProfile
import com.github.shadowsocks.Core
import com.github.shadowsocks.database.Profile
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber


class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var context: Context? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        Core.init(this, MainActivity::class)
        Firebase.initialize(this)
//        getRemoteConfig()
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
            else { list?.let { getDataList(it) } }
        }
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

}
