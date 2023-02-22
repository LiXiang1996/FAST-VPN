package com.example.test.base.data

import com.example.test.R


object CountryUtils {
    fun getCountrySource(country: String): Int {
        val a = country.trim().replace(" ", "").lowercase()
        when (a) {
            "australia" -> return R.mipmap.australia
            "belgium" -> return R.mipmap.belgium
            "brazil" -> return R.mipmap.brazil
            "canada" -> return R.mipmap.canada
            "france" -> return R.mipmap.france
            "germany" -> return R.mipmap.germany
            "india" -> return R.mipmap.india
            "ireland" -> return R.mipmap.ireland
            "italy" -> return R.mipmap.italy
            "japan" -> return R.mipmap.japan
            "southkorea" -> return R.mipmap.koreasouth
            "netherlands" -> return R.mipmap.netherlands
            "newzealand" -> return R.mipmap.newzealand
            "norway" -> return R.mipmap.norway
            "russianfederation" -> return R.mipmap.russianfederation
            "singapore" -> return R.mipmap.singapore
            "sweden" -> return R.mipmap.sweden
            "switzerland" -> return R.mipmap.switzerland
            "unitedarabemirates" -> return R.mipmap.unitedarabemirates
            "unitedkingdom" -> return R.mipmap.unitedkingdom
            "unitedstates" -> return R.mipmap.unitedstates
            else -> return R.mipmap.server_default
        }
    }

}
