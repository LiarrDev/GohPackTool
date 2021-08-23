package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 150
 * 火源战纪
 */
class Game150(apk: String) : Game(apk) {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoadingImgPath = decompileDir + File.separator +
                "res" + File.separator +
                "drawable-nodpi-v4" + File.separator +
                "game_background.jpg"
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "hyzj")
    }
}