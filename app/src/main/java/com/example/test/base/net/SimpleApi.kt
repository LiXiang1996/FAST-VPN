package com.example.test.base.net

import com.example.test.base.data.IPBean
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import java.util.jar.Attributes.Name

interface SimpleApi {
    @GET("geoip/")
    fun getIPAddress(): Call<IPBean>
}