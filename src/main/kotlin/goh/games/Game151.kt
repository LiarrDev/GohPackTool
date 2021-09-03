package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 151
 * 幻灵修仙传
 */
class Game151(apk: String) : Game(apk) {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator +
                "res" + File.separator +
                "drawable-xxxhdpi" + File.separator +
                "loading.jpg"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
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