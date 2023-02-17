package com.example.test.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity:AppCompatActivity(),InitInterface {

    abstract var layoutId:Int
    var canJump:Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        initView()
        initListener()
        initData()
    }
    override fun onResume() {
        super.onResume()
        canJump = true
    }

    override fun onPause() {
        super.onPause()
        canJump= false
    }

    override fun onStop() {
        super.onStop()
        canJump = false
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
