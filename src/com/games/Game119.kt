package com.games

import com.utils.FileUtil
import java.io.File

/**
 * GID: 119
 * 女神盟约
 */
class Game119(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "icon_loginbg.jpg"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "icon_loading.jpg"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "nsmy")
    }
}