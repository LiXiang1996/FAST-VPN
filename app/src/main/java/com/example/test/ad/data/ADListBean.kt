package com.example.test.ad.data

import android.content.Context
import android.widget.FrameLayout
import com.example.test.App
import com.example.test.ad.utils.*
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.gson.Gson
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Timer


//广告相关数据类
data class ADListBean(
    var robvn_sm: Int = 0,
    var robvn_cm: Int = 0,
    var robvn_o_open: MutableList<ADBean>,//OpenAd
    var robvn_i_2R: MutableList<ADBean>,//Interstitial
    var robvn_n_home: MutableList<ADBean>,//Native
    var robvn_n_result: MutableList<ADBean>,//Native
) {
    data class ADBean(
        var robvn_s: String = "admob",
        var robvn_l: String = "",//类型
        var robvn_p: Int = 0,//优先级
        var robvn_id: String = "",
    )
}

enum class ADType(var value: String) {
    OPEN("open"), INTER("inter"), NATIVE("native"), INTER_OPEN("interopen"), INTER_SERVER("interserver"), INTER_CONNECT(
        "interconnect"
    ),
    NATIVE_HOME("nativehome"), NATIVE_RESULT(
        "nativeresult"
    )
}


object GetADData {
    fun getFindData(
        activity: BaseActivity,
        context: Context,
        type: String,
        manager: Any,
        adListBean: MutableList<ADListBean.ADBean>,
        container: FrameLayout?,//native的承载对象
        onShowAdCompleteListener: OnShowAdCompleteListener
    ): HashMap<String, Any>? {
        val data = AppVariable.cacheDataList?.find { it["type"].toString() == type }
        if (data == null) {
            when (type) {
                ADType.INTER_SERVER.value, ADType.INTER_CONNECT.value -> {
                    if (manager is InterstitialAdManager) {
                        manager.loadAd(context, adListBean, 0, type) { it1, it2 ->
                            if (it1) {
                                manager.showInterstitial(
                                    activity, adListBean, type, onShowAdCompleteListener
                                )
                            }
                        }
                    }
                }
                ADType.NATIVE_HOME.value, ADType.NATIVE_RESULT.value -> {
                    if (manager is NativeAdManager)
                        manager.refreshAd(
                            activity, container, type, 0, adListBean
                        ) { it1 ->
                            val adView = NativeAdView1.getView(activity)
                            manager.populateNativeAdView(it1, adView)
                            container?.removeAllViews()
                            container?.addView(adView.rootView)
                        }

                }
                ADType.OPEN.value -> {
                    getOpenData(activity, manager, onShowAdCompleteListener)
                }
            }
        } else {
            when (type) {
                ADType.INTER_SERVER.value, ADType.INTER_CONNECT.value -> {
                    if (manager is InterstitialAdManager && data["value"] is InterstitialAd) manager.showInterstitialWithData(
                        activity,
                        adListBean,
                        data["value"] as InterstitialAd,
                        type,
                        onShowAdCompleteListener
                    )
                }

                ADType.NATIVE_HOME.value, ADType.NATIVE_RESULT.value -> {
                    if (manager is NativeAdManager && data["value"] is NativeAd) {
                        Timber.tag(AppConstant.TAG).e("展示缓存 type$type")
                        val adView = NativeAdView1.getView(activity)
                        manager.populateNativeAdView(data["value"] as NativeAd, adView)
                        container?.removeAllViews()
                        container?.addView(adView.rootView)
//                        container?.let {
//                            manager.refreshAd(
//                                activity, it, type, 0, adListBean
//                            )
//                        }
                    }
                }
                ADType.OPEN.value -> {
                    if (AppVariable.cacheSplashADData?.robvn_l == ADType.OPEN.value) {
                        if (manager is AppOpenAdManager) manager.showAdIfAvailable(
                            activity,
                            type,
                            AppVariable.cacheSplashADData!!,
                            onShowAdCompleteListener
                        )
                    } else if (AppVariable.cacheSplashADData?.robvn_l == ADType.INTER.value) {
                        if (manager is InterstitialAdManager) manager.showInterstitial(
                            activity,
                            mutableListOf(AppVariable.cacheSplashADData!!),
                            ADType.INTER_OPEN.value,
                            onShowAdCompleteListener
                        )
                    }
                }
            }
        }
        return data
    }

    private fun getOpenData(
        activity: BaseActivity,
        manager: Any,
        onShowAdCompleteListener: OnShowAdCompleteListener,
        position: Int = 0
    ) {
        if (position < (AppVariable.openADList?.size ?: 0)) {
            val type: String? = AppVariable.openADList?.get(position)?.robvn_l
            if (type == ADType.OPEN.value) {
                Timber.tag(AppConstant.TAG + "OpenAD").e("开屏类型为open")
                if (manager is AppOpenAdManager) AppVariable.openADList?.get(position)?.let {
                    manager.loadAd(activity, type, it) { it1, it2 ->
                        if (it1) {
                            manager.appOpenAd?.let { it3 ->
                                manager.showAdIfAvailableWithData(
                                    activity, type,
                                    AppVariable.openADList?.get(position)!!,
                                    it3, onShowAdCompleteListener
                                )
                            }
                        }
                        if (!it1 && !it2) getOpenData(
                            activity, manager, onShowAdCompleteListener, position + 1
                        )
                    }
                }
            } else if (type == ADType.INTER.value) {//这儿是判断大类别，用INTER
                Timber.tag(AppConstant.TAG + "OpenAD").e("开屏类型为inter")
                if (manager is InterstitialAdManager) AppVariable.openADList?.get(position)?.let {
                    manager.loadAd(
                        activity,
                        mutableListOf(it),
                        0,
                        ADType.INTER_OPEN.value
                    ) { it1, it2 ->
                        if (it1) {
                            manager.interstitialAd?.let { it3 ->
                                manager.showInterstitialWithData(
                                    activity, mutableListOf(AppVariable.openADList!!.get(position)),
                                    it3, type, onShowAdCompleteListener
                                )
                            }
                        } else if (!it2) {
                            getOpenData(
                                activity, manager, onShowAdCompleteListener, position + 1
                            )
                        }
                    }
                }
            }

        } else return
    }

}

object GetJsonData {
    var adListData: ADListBean? = null

    private fun getRemoteConfigData(): ADListBean? {
        return App.remoteADListData
    }

    fun getData(context: Context): ADListBean? {
        return if (getRemoteConfigData() != null) {
            getRemoteConfigData()
        } else {
            getJson(context)
        }
    }

    private fun getJson(context: Context): ADListBean? {
        var inputStreamReader: InputStreamReader? = null
        var br: BufferedReader? = null
        try {
            val assetManager = context.assets //获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
            inputStreamReader = InputStreamReader(
                assetManager.open("ad_locale_data.json"), "UTF-8"
            ) //使用IO流读取json文件内容
            br = BufferedReader(inputStreamReader) //使用字符高效流
            var line: String?
            val builder = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                builder.append(line)
            }
            val testJson = JSONObject(builder.toString()).toString() // 从builder中读取了json中的数据。
            val gson = Gson()
            adListData = gson.fromJson(testJson, ADListBean::class.java)
            return adListData
        } catch (e: Exception) {
            System.err.println(">>>>>>read json error->" + e.message)
            e.printStackTrace()
            return null
        } finally {
            try {
                br!!.close()
            } catch (e: Exception) {
            }
            try {
                inputStreamReader!!.close()
            } catch (e: Exception) {
            }
        }
    }

}
