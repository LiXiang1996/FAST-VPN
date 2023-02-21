package com.example.test.base

import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.database.Profile

object AppVariable {
    var host: String = "" //执行操作的host
    var state = BaseService.State.Idle
    var country: String = ""
    var connectTotalTime = "00:00:00"
    var isFast: Boolean = true
    var temporaryProfile: Profile? = null
    var isShowBanedIpDialog = false
}