package com.games

/**
 * GID: 133
 * 我在江湖HB
 */
class Game133(apk: String) : Game126(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelName: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName, "wzjhHB")
    }
}