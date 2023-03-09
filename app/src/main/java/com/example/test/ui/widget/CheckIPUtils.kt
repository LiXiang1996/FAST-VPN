package com.example.test.ui.widget

import java.util.Locale

object CheckIPUtils {
    fun checkIpIsOK(string: String?): Boolean {
        return if (string == "cn" && Locale.getDefault().country.lowercase() == "cn") false
        else if (string == "hk" && Locale.getDefault().country.lowercase() == "hk") false
        else if (string == "mo" && Locale.getDefault().country.lowercase() == "mo") false
        else !(string == "ir" && Locale.getDefault().country.lowercase() == "ir")
    }
}