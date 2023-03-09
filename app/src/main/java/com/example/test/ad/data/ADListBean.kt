package com.example.test.ad.data

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import com.example.test.App
import com.example.test.ad.utils.*
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.utils.SharedPreferencesUtils
import com.example.test.base.utils.TimberUtils
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.gson.Gson
import org.json.JSONObject
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


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
        onShowAdCompleteListener: OnShowAdCompleteListener,
        nativeDestroyBlock:()->Unit ={},
        nativeAdBlock:(NativeAd)->Unit ={},
    ) {
        if (!CheckADStatus().canShowAD(activity)) return
        val data = AppVariable.cacheDataList?.find { it[AppConstant.AD_TYPE].toString() == type }
        if (data == null) {
            Timber.tag(AppConstant.TAG).e("no cache")
            when (type) {
                ADType.INTER_SERVER.value, ADType.INTER_CONNECT.value -> {
                    if (manager is InterstitialAdManager) {
                        manager.loadAd(context, adListBean, 0, type) { it1, it2 ->
                            if (it1 && activity.canJump && !activity.isFinishing && !activity.isDestroyed) {
                                manager.showInterstitial(
                                    activity,
                                    adListBean,
                                    type,
                                    onShowAdCompleteListener
                                )
                            }
//                            if (!it1 && !it2) {//不能回调
//                                onShowAdCompleteListener.onShowAdComplete()
//                            }
                        }
                    }
                }
                ADType.NATIVE_HOME.value, ADType.NATIVE_RESULT.value -> {
                    if (manager is NativeAdManager)
                        manager.refreshAd(
                            activity, container, type, 0, adListBean
                        ) { it1 ->
                            nativeDestroyBlock()
                            nativeAdBlock(it1)
                            val adView = NativeAdView1.getView(activity)
                            manager.populateNativeAdView(activity,it1, adView,type)
                            adView.bringToFront()
                            container?.removeAllViews()
                            container?.addView(adView)
                        }

                }

            }
        } else {
            when (type) {
                ADType.INTER_SERVER.value, ADType.INTER_CONNECT.value -> {
                    if (manager is InterstitialAdManager && data["value"] is InterstitialAd && data[AppConstant.LOAD_TIME] is Long) {
                        if (CheckADStatus().wasLoadTimeLessThanNHoursAgo(
                                1,
                                (data[AppConstant.LOAD_TIME] as Long)
                            )
                        ) {
                            manager.showInterstitialWithData(
                                activity,
                                adListBean,
                                data["value"] as InterstitialAd,
                                type,
                                onShowAdCompleteListener
                            )
                        } else {//过期移除
                            val data =
                                AppVariable.cacheDataList?.find { it[AppConstant.AD_TYPE].toString() == type }
                            AppVariable.cacheDataList?.remove(data)
                            manager.loadAd(context, adListBean, 0, type) { it1, it2 ->
                                if (it1 && activity.canJump) {
                                    manager.showInterstitial(
                                        activity, adListBean, type, onShowAdCompleteListener
                                    )
                                }
//                                if (!it1 && !it2) //这儿不能回调
//                                    onShowAdCompleteListener.onShowAdComplete()
                            }
                        }
                    }
                }

                ADType.NATIVE_HOME.value, ADType.NATIVE_RESULT.value -> {
                    if (manager is NativeAdManager && data["value"] is NativeAd && data[AppConstant.LOAD_TIME] is Long) {
                        Timber.tag(AppConstant.TAG).e("展示缓存 type$type")
                        //判断过期
                        if (CheckADStatus().wasLoadTimeLessThanNHoursAgo(
                                1,
                                (data[AppConstant.LOAD_TIME] as Long)
                            )
                        ) {
                            nativeDestroyBlock()
                            nativeAdBlock(data["value"] as NativeAd)
                            val adView = NativeAdView1.getView(activity)
                            manager.populateNativeAdView(activity,data["value"] as NativeAd, adView,type)
                            adView.bringToFront()
                            container?.removeAllViews()
                            container?.addView(adView)


                        } else {
                            //过期 删除缓存
                            val data =
                                AppVariable.cacheDataList?.find { it[AppConstant.AD_TYPE].toString() == type }
                            AppVariable.cacheDataList?.remove(data)
                            manager.refreshAd(
                                activity, container, type, 0, adListBean
                            ) { it1 ->
                                nativeDestroyBlock()
                                nativeAdBlock(it1)
                                val adView = NativeAdView1.getView(activity)
                                manager.populateNativeAdView(activity,it1, adView,type)
                                adView.bringToFront()
                                container?.removeAllViews()
                                container?.addView(adView)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getOpenData(
        activity: BaseActivity,
        managerOpen: AppOpenAdManager?,
        managerInter: InterstitialAdManager?,
        onShowAdCompleteListener: OnShowAdCompleteListener,
        position: Int = 0
    ) {
        if (position < (AppVariable.openADList?.size ?: 0)) {
            val type: String? = AppVariable.openADList?.get(position)?.robvn_l
            Timber.tag(AppConstant.TAG).e("${AppVariable.openADList?.get(position)?.robvn_id}")
            AppVariable.openADList?.get(position)
            if (type == ADType.OPEN.value) {
                Timber.tag(AppConstant.TAG + "Open").e("开屏类型为open $position")
                if (managerOpen is AppOpenAdManager) AppVariable.openADList?.get(position)?.let {
                    managerOpen.loadAd(activity, type, it, position) { it1, it2 ->
                        if (it1) {
                            managerOpen.appOpenAd?.let { it3 ->
                                managerOpen.showAdIfAvailableWithData(
                                    activity, type,
                                    it3, onShowAdCompleteListener
                                ) { it1, it2 ->
                                    if (!it1 && !it2) {
                                        getOpenData(
                                            activity,
                                            managerOpen,
                                            managerInter,
                                            onShowAdCompleteListener,
                                            position + 1
                                        )
                                    }
                                }
                            }
                        } else if (!it2) getOpenData(
                            activity,
                            managerOpen,
                            managerInter,
                            onShowAdCompleteListener,
                            position + 1
                        )
                    }
                }
            } else if (type == ADType.INTER.value) {//这儿是判断广告类别，用INTER
                Timber.tag(AppConstant.TAG + "Open").e("开屏类型为inter $position")
                AppVariable.openADList?.get(position)?.let {
                    managerInter?.loadAd(
                        activity,
                        mutableListOf(it),
                        0,
                        ADType.INTER_OPEN.value
                    ) { it1, it2 ->
                        if (it1) {
                            managerInter.interstitialAd?.let { it3 ->
                                managerInter.showInterstitialWithData(
                                    activity, mutableListOf(AppVariable.openADList!!.get(position)),
                                    it3, ADType.INTER_OPEN.value, onShowAdCompleteListener
                                )
                            }
                        } else if (!it2) {
                            getOpenData(
                                activity,
                                managerOpen,
                                managerInter,
                                onShowAdCompleteListener,
                                position + 1
                            )
                        }
                    }
                }
            }

        } else {
            ADLoading.OPEN.isLoading = false
            ADLoading.INTER_OPEN.isLoading = false
            return
        }
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

class CheckADStatus {
    fun setShowAndClickCount(activity: Activity, isShow: Boolean, isClick: Boolean) {
        val dayShow: Int = SharedPreferencesUtils.getParam(activity, AppVariable.dateShow, 0) as Int
        val dayClick: Int =
            SharedPreferencesUtils.getParam(activity, AppVariable.dateClick, 0) as Int
        if (isShow && dayShow < (GetJsonData.getData(activity)?.robvn_sm ?: 40)) {
            SharedPreferencesUtils.setParam(activity, AppVariable.dateShow, dayShow + 1)
//            Timber.tag(AppConstant.TAG).e("展示次数${dayShow + 1}")
        }
        if (isClick && dayClick < (GetJsonData.getData(activity)?.robvn_cm ?: 10)) {
            SharedPreferencesUtils.setParam(activity, AppVariable.dateClick, dayClick + 1)
//            Timber.tag(AppConstant.TAG).e("点击次数${dayClick + 1}")
        }
    }

    private fun getShowCountIsOk(activity: Activity): Boolean {
        return (SharedPreferencesUtils.getParam(
            activity,
            AppVariable.dateShow,
            0
        ) as Int) < (GetJsonData.getData(activity)?.robvn_sm ?: 40)
    }

    private fun getClickCountIsOk(activity: Activity): Boolean {
        return (SharedPreferencesUtils.getParam(
            activity,
            AppVariable.dateClick,
            0
        ) as Int) < (GetJsonData.getData(activity)?.robvn_cm ?: 10)
    }

    fun canShowAD(activity: Activity): Boolean {
        if (!CheckADStatus().getShowCountIsOk(activity) || !CheckADStatus().getClickCountIsOk(
                activity
            )
        ) {
            Timber.tag(AppConstant.TAG + " AD").e("广告加载已达上限")
        }
        return CheckADStatus().getShowCountIsOk(activity) && CheckADStatus().getClickCountIsOk(
            activity
        )
    }

    fun wasLoadTimeLessThanNHoursAgo(numHours: Long, loadTime: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }
}
