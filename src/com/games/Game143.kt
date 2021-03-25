package com.games

/**
 * GID: 143
 * 天命山海测试1
 */
class Game143(apk: String) : Game(apk) {

    /**
     * 不需要替换素材
     */
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "tmshcs1")
    }
}