package com.example.test.base.data

import com.github.shadowsocks.database.Profile

data class ListProfile(var profileList: MutableList<RemoteProfile>? = null)


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
        }
        return profile
    }
}


