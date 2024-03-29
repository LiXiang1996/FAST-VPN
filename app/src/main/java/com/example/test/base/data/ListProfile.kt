package com.example.test.base.data

import androidx.annotation.Keep
import com.github.shadowsocks.database.Profile
@Keep
data class RemoteProfile(
    var robvn_pwd: String,
    var robvn_account: String,
    var robvn_port: Int,
    var robvn_country: String,
    var robvn_city: String,
    var robvn_ip: String
)

object ToProfile {
    fun remoteProfileToProfile(remoteProfile: RemoteProfile): Profile {
        val profile: Profile = Profile().apply {
            this.host = remoteProfile.robvn_ip
            this.name = remoteProfile.robvn_country
            this.city = remoteProfile.robvn_city
            this.password = remoteProfile.robvn_pwd
            this.remotePort = remoteProfile.robvn_port
            this.method = remoteProfile.robvn_account
        }
        return profile
    }
}


