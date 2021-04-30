package com.games

/**
 * 未接入的游戏可以走这个，
 * 除了资源不替换外理论上不会有其他问题。
 * 但理论上应当为每个游戏作接入，
 * 不接入的游戏不应当参与打包。
 */
class GameDefault(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "UNKNOWN")
    }
}