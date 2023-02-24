package com.example.test.base

import com.example.test.ad.data.ADListBean
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd

object AppVariable {
    var host: String = "" //执行操作的host
    var state = BaseService.State.Idle
    var country: String = ""
    var connectTotalTime = "00:00:00"
    var isFast: Boolean = true
    var temporaryProfile: Profile? = null
    var isShowBanedIpDialog = false
    var isBackGround = false
    var isBackGroundToSplash = false
    var openADList: List<ADListBean.ADBean>? = null
    var interADList: List<ADListBean.ADBean>? = null
    var nativeADList: List<ADListBean.ADBean>? = null
    var cacheAppOpenAd: AppOpenAd? = null
    var cacheNativeAd: NativeAd? = null
    var cacheInterstitialAd: InterstitialAd? = null
}