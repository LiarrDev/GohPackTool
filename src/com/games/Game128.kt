package com.games

/**
 * GID: 128
 * 青云传OL
 */
class Game128(apk: String) : Game116(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "qyzol")
    }
}