package com.example.test

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.example.test.ui.activity.MainActivity
import com.github.shadowsocks.Core
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig

@SuppressLint("StaticFieldLeak")
val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

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
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    remoteConfig.getString("axxxxxx")
                }
            }
    }
}
