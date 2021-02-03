package com.games

/**
 * GID: 129
 * 太古封魔录OL
 */
class Game129(apk: String) : Game111(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelName: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName, "tgfmlol")
    }
}