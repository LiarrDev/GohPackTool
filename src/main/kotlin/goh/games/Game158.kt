package goh.games

/**
 * GID: 158
 * 逍遥三国
 */
class Game158(apk: String) : Game(apk) {
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
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "xysg")
    }
}