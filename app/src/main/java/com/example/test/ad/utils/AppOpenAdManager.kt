package com.example.test.ad.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.ADType
import com.example.test.ad.data.CheckADStatus
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 *    ERROR_CODE_INTERNAL_ERROR = 0;Internal error, 从广告服务器收到无效响应
 *    ERROR_CODE_INVALID_REQUEST = 1; Invalid ad request, possibly an incorrect ad unit ID was given
 *    ERROR_CODE_NETWORK_ERROR = 2;网络错误
 *    ERROR_CODE_NO_FILL = 3;The ad request was successful, but no ad was returned due to lack of ad inventory
 *    ERROR_CODE_APP_ID_MISSING = 8;
 *    ERROR_CODE_REQUEST_ID_MISMATCH = 10;
 *    ERROR_CODE_INVALID_AD_STRING = 11;
 *    ERROR_CODE_MEDIATION_NO_FILL = 9;
 */
class AppOpenAdManager {

    var appOpenAd: AppOpenAd? = null


    private var isLoadingAd = false
    var isShowingAd = false
    private var TAG = AppConstant.TAG + "OpenAD"

    private var loadTime: Long = 0

    /**
     * params result(bool,bool) 第一个代表ad load 是否成功，第二个代表广告是open还是inter open为true
     */

    fun loadAd(
        context: Context,
        type: String,
        openData: ADListBean.ADBean,
        result: (Boolean, Boolean) -> Unit
    ) {
        Timber.tag(TAG).e("AppOpen 是否正在loading广告 $isLoadingAd  ")
        if (isLoadingAd || isAdAvailable()) {
            Timber.tag(TAG)
                .e("AppOpen 是否有正在loading的广告 $isLoadingAd appOpenAd是否为空${appOpenAd == null}  isAdAvailable ${isAdAvailable()}")
            return
        }
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(context,
            openData.robvn_id,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val a = format.format(Date())
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Timber.tag(TAG).e(" on AdLoaded. 缓存 type:$type,LoadTime $loadTime date${a}")
                    result.invoke(true, true)
                    AppVariable.cacheSplashADData = openData

                    val cacheData =
                        AppVariable.cacheDataList?.find { it["type"].toString() == type }
                    if (cacheData != null) {
                        AppVariable.cacheDataList?.remove(cacheData)
                    }
                    val data = HashMap<String, Any>().apply {
                        put("type", type)
                        put("value", appOpenAd!!)
                        put(AppConstant.LOAD_TIME,Date().time)
                    }
                    AppVariable.cacheDataList?.add(data)
                }

                @SuppressLint("BinaryOperationInTimber")
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    result.invoke(false, false)
                    Timber.tag(TAG).e(
                            "onAdFailedToLoad: %s",
                            loadAdError.message + "code " + loadAdError.code + "   id${openData.robvn_id}"
                        )
                }
            })
    }


    /** 检查广告是否在 n 小时前加载. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        Timber.tag(TAG).e("datadiff $dateDifference")
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** 检查广告是否存在并且可以展示. */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(1)
    }


    fun showAdIfAvailableWithData(
        activity: Activity,
        type: String,
        openADID: ADListBean.ADBean,
        appOpenAdCache: AppOpenAd,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        Timber.tag(TAG).e("有缓存")
        appOpenAd = appOpenAdCache
        if (isShowingAd) {
            Timber.tag(TAG).e("The app open ad is already showing.")
            return
        }
        if (!isAdAvailable()) {
            Timber.tag(TAG).e("ad对象为空或者广告一小时前展示过")
            loadAd(activity, type, openADID) { _, _ -> }
            return
        }
        Timber.tag(TAG).e("Will show ad.")
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                onShowAdCompleteListener.onShowAdComplete()
                CheckADStatus().setShowAndClickCount(activity,
                    isShow = false,
                    isClick = true
                )
                super.onAdClicked()
            }

            override fun onAdImpression() {
                Timber.tag(TAG).e(" AD onAdImpression.移除缓存")
                val a = AppVariable.cacheDataList?.find { it["type"].toString() == type }
                a?.remove(type)
                CheckADStatus().setShowAndClickCount(activity,
                    isShow = true,
                    isClick = false
                )
                AppVariable.cacheSplashADData = null
                super.onAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                onShowAdCompleteListener.onShowAdComplete()
                Timber.tag(TAG).e("onAdDismissedFullScreenContent.")
            }

            @SuppressLint("BinaryOperationInTimber")
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Timber.tag(TAG).e(
                    "onAdFailedToShowFullScreenContent: %s", adError.message + "code" + adError.code
                )
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity, type, openADID) { _, _ -> }
            }

            override fun onAdShowedFullScreenContent() {
                Timber.tag(TAG).e("onAdShowedFullScreenContent.")
            }
        }
        isShowingAd = true
        appOpenAd?.show(activity)
    }

}

/**
 * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
 * dismissed or fails to show).
 */
interface OnShowAdCompleteListener {
    fun onShowAdComplete()
}




//    fun showAdIfAvailable(
//        activity: Activity,
//        type: String,
//        openADID: ADListBean.ADBean,
//        onShowAdCompleteListener: OnShowAdCompleteListener
//    ) {
//        if (isShowingAd) {
//            Timber.tag(TAG).e("The app open ad is already showing.")
//            return
//        }
//        if (!isAdAvailable()) {
//            Timber.tag(TAG).e("ad对象为空或者广告一小时前展示过")
//            loadAd(activity, type, openADID) { _, _ -> }
//            return
//        }
//        if (appOpenAd == null) {
//            Timber.tag(TAG).e("ad对象为空或者广告一小时前展示过")
//            return
//        }
//        Timber.tag(TAG).e("Will show ad.")
//        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
//            override fun onAdClicked() {
//                onShowAdCompleteListener.onShowAdComplete()
//                CheckADStatus().setShowAndClickCount(activity,
//                    isShow = false,
//                    isClick = true
//                )
//                super.onAdClicked()
//            }
//
//            override fun onAdImpression() {
//                Timber.tag(TAG).e(" AD onAdImpression.移除缓存")
//                CheckADStatus().setShowAndClickCount(activity,
//                    isShow = true,
//                    isClick = false
//                )
//                AppVariable.cacheSplashADData = null
//                val a = AppVariable.cacheDataList?.find { it["type"].toString() == type }
//                a?.remove(type)
//                super.onAdImpression()
//            }
//
//            override fun onAdDismissedFullScreenContent() {
//                appOpenAd = null
//                isShowingAd = false
//                onShowAdCompleteListener.onShowAdComplete()
//                Timber.tag(TAG).e("onAdDismissedFullScreenContent.")
//            }
//
//            @SuppressLint("BinaryOperationInTimber")
//            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
//                appOpenAd = null
//                isShowingAd = false
//                Timber.tag(TAG).e(
//                    "onAdFailedToShowFullScreenContent: %s", adError.message + "code" + adError.code
//                )
//                onShowAdCompleteListener.onShowAdComplete()
//                loadAd(activity, type, openADID) { _, _ -> }
//            }
//
//            override fun onAdShowedFullScreenContent() {
//                Timber.tag(TAG).e("onAdShowedFullScreenContent.")
//            }
//        }
//        isShowingAd = true
//        appOpenAd?.show(activity)
//    }

