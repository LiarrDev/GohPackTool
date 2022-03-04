package goh.games

/**
 * GID: 154
 * 幻灵修仙传（小程序），非研发，是荣耀制作的 H5 游戏壳
 */
class Game154(apk: String) : Game(apk) {
    override fun replaceResource(
        loginImage: String?,
        loadingImage: String?,
        logoImage: String?,
        splashImage: String?
    ) {
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "hlxxzxcx")
    }
}