package com.example.test

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.example.test.ui.activity.MainActivity
import com.github.shadowsocks.Core

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
    }
}
