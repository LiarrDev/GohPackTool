package com.games

/**
 * GID: 136
 * 我在江湖YF
 */
class Game136(apk: String) : Game126(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "wzjhYF")
    }
}