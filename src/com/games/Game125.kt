package com.games

import com.utils.FileUtil
import java.io.File

/**
 * GID: 125
 * 逆火苍穹
 */
class Game125(apk: String) : Game(apk) {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "agent" + File.separator + "login_bg.jpg"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "agent" + File.separator + "update_bg.jpg"
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "agent" + File.separator + "logo.png"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "nhqc")
    }
}