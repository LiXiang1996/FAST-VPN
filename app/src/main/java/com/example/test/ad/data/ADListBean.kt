package com.example.test.ad.data

import android.content.Context
import com.google.gson.Gson
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader


//广告相关数据类
data class ADListBean(
    var show: Int = 0,
    var click: Int = 0,
    var robvn_o_open: List<ADBean>,//OpenAd
    var robvn_i_2R: List<ADBean>,//Interstitial
    var robvn_n_home: List<ADBean>,//Native
    var robvn_n_result: List<ADBean>,//Native
) {
    data class ADBean(
        var sour: String = "admob",
        var ADType: String = "",
        var ADLevel: Int = 0,
        var ADid: String = "",
    )
}

object GetJsonData {
    var adListData :ADListBean?=null
    fun getJson(context: Context) :ADListBean?{
        var inputStreamReader: InputStreamReader? = null
        var br: BufferedReader? = null
        try {
            val assetManager = context.assets //获得assets资源管理器（assets中的文件无法直接访问，可以使用AssetManager访问）
            inputStreamReader = InputStreamReader(assetManager.open("ad_locale_data.json"), "UTF-8") //使用IO流读取json文件内容
            br = BufferedReader(inputStreamReader) //使用字符高效流
            var line: String?
            val builder = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                builder.append(line)
            }
            val testJson = JSONObject(builder.toString()).toString() // 从builder中读取了json中的数据。
            val gson = Gson()
            adListData = gson.fromJson(testJson,ADListBean::class.java)
            return adListData
        } catch (e: Exception) {
            System.err.println(">>>>>>read json error->" + e.message)
            e.printStackTrace()
            return null
        } finally {
            try { br!!.close() } catch (e: Exception) { }
            try { inputStreamReader!!.close()} catch (e: Exception) { }
        }
    }

}
