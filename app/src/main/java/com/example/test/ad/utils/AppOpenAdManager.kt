package com.example.test.ad.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.ADLoading
import com.example.test.ad.data.ADType
import com.example.test.ad.data.CheckADStatus
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.example.test.base.BaseActivity
import com.example.test.base.utils.TimberUtils
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
    private var TAG = AppConstant.TAG + "Open"

    private var loadTime: Long = 0

    /**
     * params result(bool,bool) 第一个代表ad load 是否成功，第二个代表广告是open还是inter open为true
     */

    fun loadAd(
        context: Context,
        type: String,
        openData: ADListBean.ADBean,
        position: Int = 0,
        result: (Boolean, Boolean) -> Unit
    ) {
        if (isLoadingAd) {
            return
        }
        if (ADLoading.OPEN.isLoading) {
            Timber.tag(AppConstant.TAG).e("正在请求开屏")
            return
        }
        TimberUtils().printADLoadLog(type, AppConstant.LOADING, openData)
        ADLoading.OPEN.isLoading = true
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(context,
            openData.robvn_id,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    ADLoading.INTER_OPEN.isLoading = false
                    ADLoading.OPEN.isLoading = false
                    loadTime = Date().time
                    TimberUtils().printADLoadLog(type, AppConstant.LOAD_SUC, openData)
                    result.invoke(true, true)
                    AppVariable.cacheSplashADData = openData
//                    val cacheData =
//                        AppVariable.cacheDataList?.find { it["type"].toString() == type }
//                    if (cacheData != null) {
//                        AppVariable.cacheDataList?.remove(cacheData)
//                    }
                    val data = HashMap<String, Any>().apply {
                        put("type", type)
                        put("value", appOpenAd!!)
                        put(AppConstant.LOAD_TIME, Date().time)
                    }
                    AppVariable.cacheDataList?.add(data)
                }

                @SuppressLint("BinaryOperationInTimber")
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    ADLoading.OPEN.isLoading = false
                    ADLoading.INTER_OPEN.isLoading = false
                    TimberUtils().printADLoadLog(type, AppConstant.LOAD_FAIL, openData, loadAdError)
                    if (position + 1 == AppVariable.openADList?.size) {
                        return
                    }
                    if (context is BaseActivity) {
                        if (!context.canJump) return
                    }
                    result.invoke(false, false)
                }
            })
    }


    fun showAdIfAvailableWithData(
        activity: Activity,
        type: String,
        openADID: ADListBean.ADBean,
        appOpenAdCache: AppOpenAd,
        onShowAdCompleteListener: OnShowAdCompleteListener, result: (Boolean, Boolean) -> Unit
    ) {
        appOpenAd = appOpenAdCache
        if (isShowingAd) {
            Timber.tag(TAG).e("The app open ad is already showing.")
            return
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                onShowAdCompleteListener.onShowAdComplete()
                CheckADStatus().setShowAndClickCount(
                    activity, isShow = false, isClick = true
                )
                super.onAdClicked()
            }

            override fun onAdImpression() {
                TimberUtils().printADImpression(type)
                AppVariable.cacheDataList?.forEach {
                    if (it["type"].toString() == type) AppVariable.cacheDataList?.remove(it)
                }
                CheckADStatus().setShowAndClickCount(
                    activity, isShow = true, isClick = false
                )
                AppVariable.cacheSplashADData = null
                super.onAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                AppVariable.cacheSplashADData = null
                appOpenAd = null
                isShowingAd = false
                onShowAdCompleteListener.onShowAdComplete()
                TimberUtils().printAdDismissedFullScreenContent(type)
            }

            @SuppressLint("BinaryOperationInTimber")
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                AppVariable.cacheSplashADData = null
                isShowingAd = false
                TimberUtils().printAdFailedToShowFullScreenContent(type, adError)
                result.invoke(false, false)
                onShowAdCompleteListener.onShowAdComplete()
            }

            override fun onAdShowedFullScreenContent() {
                AppVariable.cacheSplashADData = null
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


