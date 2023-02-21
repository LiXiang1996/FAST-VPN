package com.example.test

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.example.test.base.data.ListProfile
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
        getRemoteConfig()
    }

    private fun getRemoteConfig() {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                   val list =  remoteConfig.getString("axxxxxx")
                    val gson = Gson()
                    val resultBean: ListProfile = gson.fromJson(list, ListProfile::class.java)
                    val profileList = mutableListOf<Profile>()
                    resultBean.profileList?.forEach {
                        profileList.add(ToProfile.remoteProfileToProfile(it))
                    }
                    if ((resultBean.profileList?.size ?: 0) > 0){
                        resultBean.profileList?.let { ServersListProfile.getServersList().addAll(profileList) }
                    }
                }
            }
    }
}
