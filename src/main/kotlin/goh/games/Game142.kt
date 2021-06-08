package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 142
 * 时空测试1
 */
class Game142(apk: String) : Game(apk) {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "apk_res" + File.separator + "LaucherLoadingBg.jpg"
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "skzxcs1")
    }
}