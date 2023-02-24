package com.example.test.ad.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.test.ad.data.ADListBean
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import timber.log.Timber
import java.text.FieldPosition
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

    companion object {
        var appOpenAd: AppOpenAd? = null

    }

    private var isLoadingAd = false
    var isShowingAd = false
    private var TAG = AppConstant.TAG + "OpenAD"

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    /**
     * Load an ad. 生成广告对象
     */
    fun loadAd(
        context: Context,
        openADID: List<ADListBean.ADBean>,
        position: Int = 0,
        result: () -> Unit
    ) {
        Timber.tag(TAG).e("AppOpen 是否正在loading广告 $isLoadingAd  ")
//        if (isLoadingAd || isAdAvailable()) {
//            Timber.tag(TAG).e("AppOpen 是否有正在loading的广告 $isLoadingAd appOpenAd是否为空${appOpenAd==null}  isAdAvailable ${isAdAvailable()}")
//            return
//        }
        if (isLoadingAd || appOpenAd != null) {
            Timber.tag(TAG)
                .e("AppOpen 是否有正在loading的广告 $isLoadingAd appOpenAd是否为空${appOpenAd == null}  isAdAvailable ${isAdAvailable()}")
            return
        }
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            openADID[position].robvn_id,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Timber.tag(TAG).e("onAdLoaded. $appOpenAd" + "id${openADID[position].robvn_id}")
                    result.invoke()
                }

                @SuppressLint("BinaryOperationInTimber")
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    if (position < openADID.size) loadAd(context, openADID, position + 1, result)
                    Timber.tag(TAG)
                        .e(
                            "onAdFailedToLoad: %s",
                            loadAdError.message + "code " + loadAdError.code + "   id${openADID[position].robvn_id}"
                        )
                }
            }
        )
    }

    /** 检查广告是否在 n 小时前加载. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** 检查广告是否存在并且可以展示. */
    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(1)
    }


    fun showAdIfAvailable(
        activity: Activity,
        openADID: List<ADListBean.ADBean>,
        onShowAdCompleteListener: OnShowAdCompleteListener
    ) {
        if (isShowingAd) {
            Timber.tag(TAG).e("The app open ad is already showing.")
            return
        }
//        if (!isAdAvailable()) {
//            Timber.tag(TAG).e("ad对象为空或者广告一小时前展示过")
//            loadAd(activity, openADID)
//            return
//        }
        if (appOpenAd == null) {
            Timber.tag(TAG).e("ad对象为空或者广告一小时前展示过")
            // TODO:
//            onShowAdCompleteListener.onShowAdComplete()
            loadAd(activity, openADID) {}
            return
        }
        Timber.tag(TAG).e("Will show ad.")
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                onShowAdCompleteListener.onShowAdComplete()
                super.onAdClicked()
            }

            override fun onAdImpression() {
                Timber.tag(TAG).e(" AD onAdImpression.")
                super.onAdImpression()
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                onShowAdCompleteListener.onShowAdComplete()
                Timber.tag(TAG).e("onAdDismissedFullScreenContent.")
                loadAd(activity, openADID) {}
            }

            @SuppressLint("BinaryOperationInTimber")
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                Timber.tag(TAG).e(
                    "onAdFailedToShowFullScreenContent: %s",
                    adError.message + "code" + adError.code
                )
                onShowAdCompleteListener.onShowAdComplete()
                loadAd(activity, openADID) {}
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