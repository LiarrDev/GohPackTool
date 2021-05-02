package goh.games

/**
 * GID: 129
 * 太古封魔录OL
 */
class Game129(apk: String) : Game111(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "tgfmlol")
    }
}