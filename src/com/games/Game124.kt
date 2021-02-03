package com.games

import com.utils.FileUtil
import java.io.File

/**
 * GID: 124
 * 代号黎明
 */
open class Game124(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "res" + File.separator + "ui" + File.separator + "single" + File.separator + "background.jpg"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "res" + File.separator + "ui" + File.separator + "single" + File.separator + "loading.jpg"
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "res" + File.separator + "ui" + File.separator + "single" + File.separator + "logo.png"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelName: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName, "dhlm")
    }
}