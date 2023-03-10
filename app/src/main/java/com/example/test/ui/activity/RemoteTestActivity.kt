package com.example.test.ui.activity

import android.annotation.SuppressLint
import androidx.appcompat.widget.AppCompatTextView
import com.example.test.App
import com.example.test.R
import com.example.test.ad.data.ADListBean
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity

class RemoteTestActivity : BaseActivity() {
    override var layoutId: Int = R.layout.test_layout
    var text1: AppCompatTextView? = null
    var text2: AppCompatTextView? = null
    var text3: AppCompatTextView? = null
    var text4: AppCompatTextView? = null
    var text5: AppCompatTextView? = null
    var text6: AppCompatTextView? = null

    @SuppressLint("SetTextI18n")
    override fun initView() {
        text1 = findViewById(R.id.test_1)
        text2 = findViewById(R.id.test_2)
        text3 = findViewById(R.id.test_3)
        text4 = findViewById(R.id.test_4)
        text5 = findViewById(R.id.test_5)
        text6 = findViewById(R.id.test_6)

        text1?.text = "广告是否为空${App.remoteADListData == null} "
        val a = AppVariable.openADList?.get(0)?.robvn_id
        val ab = AppVariable.interADList?.get(0)?.robvn_id
        val abc = AppVariable.nativeHomeADList?.get(0)?.robvn_id
        val abcd = AppVariable.nativeResultADList?.get(0)?.robvn_id

        text2?.text = "缓存广告 open id $a   chapingid $ab "
        text3?.text = "severlist${TestVa.serverList}"
        text4?.text = "seversmartliset${TestVa.serversmartList}"
        text6?.text = "adlist${TestVa.adlist}"
        text5?.text = "remote${TestVa.remote.toString()}    robvn_i_2R size${TestVa.remote?.robvn_i_2R?.size}"
        super.initView()
    }
}

object TestVa{
    var serverList:String?=null
    var serversmartList:String?=null
    var adlist:String?=null
    var remote:ADListBean?=null

}