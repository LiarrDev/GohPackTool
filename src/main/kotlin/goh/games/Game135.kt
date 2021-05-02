package goh.games

/**
 * GID: 135
 * 代号黎明测试1
 */
class Game135(apk: String) : Game124(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "dhlmcs1")
    }
}