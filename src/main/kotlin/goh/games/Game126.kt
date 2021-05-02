package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 126
 * 我在江湖
 */
open class Game126(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "selectserverbg.jpg"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "loadingbg.jpg"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "wzjh")
    }
}