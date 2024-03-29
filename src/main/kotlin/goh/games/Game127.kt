package goh.games

/**
 * GID: 127
 * 帝国文明测试1
 */
class Game127(apk: String) : Game(apk) {

    /**
     * 不需要替换素材
     */
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {}

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "dgwmcs1")
    }
}