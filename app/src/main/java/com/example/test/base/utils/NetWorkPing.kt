package com.example.test.base.utils

import androidx.annotation.Keep
import com.example.test.ui.activity.ServersListProfile
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

object NetworkPing {

    private val job = Job()

    private val scope = CoroutineScope(job)

    private val TAG = "PING-SJSX:"

    /**
     * @param ip 默认ping百度的地址
     */
    private fun ping(ip: String): PingBean {
        val command = "ping -c 1 -W 1 $ip"
        val proc = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        if (proc.waitFor() == 0) {
            val result = StringBuilder()
            while (true) {
                val line = reader.readLine() ?: break
                result.append(line).append("\n")
            }
//            Timber.tag(AppConstant.TAG).e("ping OK")
            return PingBean(ip, getPing(result.toString()))
        }else{
//            Timber.tag(AppConstant.TAG).e("ping Failed")
        }
        return PingBean(ip, 10000.0)
    }

    fun toFastToggle(listener: (String) -> Unit) {
//        Timber.tag(AppConstant.TAG).e("测试ping")
        GlobalScope.launch {
            val list = mutableListOf<PingBean>()
            coroutineScope {
                val servers = ArrayList(ServersListProfile.getServersList()).apply { removeAt(0) }
                servers.forEach { launch { list.add(ping((it.host))) } }
            }
            list.sortBy { it.avg }
            listener(list.take(3).random().host?:"")
        }
    }

    private fun getPing(str: String): Double {
        runCatching {
            val tempInfo: String = str.substring(str.indexOf("min/avg/max/mdev") + 19)
            val temps = tempInfo.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//            Timber.tag(AppConstant.TAG).e("延迟avg:%s", temps[1])
            return temps[1].toDoubleOrNull() ?: 10000.0
        }
        return 10000.0
    }
}
@Keep
data class PingBean(var host: String?, var avg: Double)

