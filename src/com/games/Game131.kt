package com.games

/**
 * GID: 131
 * 彩虹物语
 */
class Game131(apk: String) : Game123(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelName: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName, "chwy")
    }
}