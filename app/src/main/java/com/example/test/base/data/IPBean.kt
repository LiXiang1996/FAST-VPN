package com.example.test.base.data

import com.google.errorprone.annotations.Keep

@Keep
data class IPBean(
    var ip:String,
    var continent_code:String,
    var country:String,
    var country_code:String,
    var country_code3:String,
    var region:String,
    var latitude:Double,
    var longitude:Double,
)