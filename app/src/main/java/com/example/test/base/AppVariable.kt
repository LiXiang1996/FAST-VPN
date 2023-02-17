package com.example.test.base

import com.github.shadowsocks.bg.BaseService

object AppVariable {
    var host:String=""
    var state = BaseService.State.Idle
    var country:String=""
    var connectTotalTime= "00:00:00"
    var isFast:Boolean = true
}