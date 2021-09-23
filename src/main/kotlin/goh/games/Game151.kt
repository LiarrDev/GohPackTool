package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 151
 * 幻灵修仙传
 */
class Game151(apk: String) : Game(apk) {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameSplashImgPath = decompileDir + File.separator +
                "res" + File.separator +
                "drawable-xxxhdpi" + File.separator +
                "loading.jpg"
        val gameLoginImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "h5" + File.separator +
                "assets" + File.separator +
                "game_bg" + File.separator +
                "8.jpg"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(splashImage, gameSplashImgPath)
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "hlxxz")
    }
}