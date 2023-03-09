package com.example.test.ui.widget

import com.example.test.base.AppConstant
import timber.log.Timber
import java.util.Locale

object CheckIPUtils {
    fun checkIpIsOK(string: String?): Boolean {
        Timber.tag(AppConstant.TAG +"IPPPPP").e("$string   -----${Locale.getDefault().country.lowercase()}")
        return if (string.isNullOrEmpty()){
            when(Locale.getDefault().country.lowercase()){
                "cn","hk","ir","mo"-> true
                else -> false
            }
        }else{
            when(string){
                "cn","hk","ir","mo"-> true
                else -> false
            }
        }
    }
}