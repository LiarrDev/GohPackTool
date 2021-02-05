package com.games

/**
 * GID: 132
 * 彩虹物语KS
 */
class Game132(apk: String) : Game123(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "chwyKS")
    }
}