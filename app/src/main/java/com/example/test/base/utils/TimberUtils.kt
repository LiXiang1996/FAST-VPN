package com.example.test.base.utils

import com.example.test.ad.data.ADListBean
import com.example.test.ad.data.ADLoading
import com.example.test.ad.data.ADType
import com.example.test.base.AppConstant
import com.example.test.base.AppVariable
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import timber.log.Timber

class TimberUtils {

    val ADTAG = AppConstant.TAG + " AD"
    fun printADLoadLog(
        type: String,
        loadStatus: Int,
        data: ADListBean.ADBean,
        error: LoadAdError? = null
    ) {
        when (loadStatus) {
            0 -> Timber.tag(ADTAG)
                .e("Type: $type  正在请求广告 优先级:${data.robvn_p} 广告ID: ${data.robvn_id}")
            1 -> Timber.tag(ADTAG)
                .e("Type: $type  请求广告成功 优先级:${data.robvn_p} 广告ID: ${data.robvn_id}")
            2 -> Timber.tag(ADTAG)
                .e("Type: $type  请求广告失败 优先级:${data.robvn_p} 广告ID: ${data.robvn_id} 错误信息：${error?.message} 错误code:${error?.code}")

        }
    }

    fun printADImpression(
        type: String
    ) {
        if (type == ADType.INTER_OPEN.value || type == ADType.OPEN.value) {
            AppVariable.isOpenIsShowing = true
            ADLoading.OPEN.isLoading = false
            ADLoading.INTER_OPEN.isLoading = false
        }
        Timber.tag(ADTAG).e("Type: $type  广告正在展示")
    }

    fun printADClick(
        type: String
    ) {
        Timber.tag(ADTAG).e("Type: $type  广告被点击")
    }

    fun printAdDismissedFullScreenContent(
        type: String
    ) {
        if (type == ADType.INTER_OPEN.value || type == ADType.OPEN.value) {
            AppVariable.isOpenIsShowing = false
            ADLoading.OPEN.isLoading = false
            ADLoading.INTER_OPEN.isLoading = false
        }
        Timber.tag(ADTAG).e("Type: $type  全屏内容消失")
    }

    fun printAdShowedFullScreenContent(
        type: String
    ) {
        Timber.tag(ADTAG).e("Type: $type  广告全屏展示")
    }

    fun printAdFailedToShowFullScreenContent(
        type: String, adError: AdError
    ) {
//        Timber.tag(ADTAG).e("Type: $type  展示失败   错误信息：${adError.message}")
    }

}