package com.games

import com.utils.FileUtil
import java.io.File

/**
 * GID: 123
 * 冒险联盟OL / 天堂奇缘OL
 */
open class Game123(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "loginBg.jpg"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "loadingBg.jpg"
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "logo.png"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelName: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName, "mxlmol")
    }
}