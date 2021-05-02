package goh.games

/**
 * GID: 133
 * 我在江湖HB
 */
class Game133(apk: String) : Game126(apk) {
    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "wzjhHB")
    }
}