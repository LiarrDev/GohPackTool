package com.games

import com.utils.FileUtil
import java.io.File

/**
 * GID: 116
 * 青云传
 */
open class Game116(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "resources" + File.separator + "android" + File.separator + "rawres" + File.separator + "login" + File.separator + "qyj_login_sp.jpg"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "resources" + File.separator + "android" + File.separator + "rawres" + File.separator + "login" + File.separator + "qyj_loading_sp.jpg"
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "resources" + File.separator + "android" + File.separator + "rawres" + File.separator + "login" + File.separator + "qyj_logo_sp.png"
        val gameSplashImgPath = decompileDir + File.separator + "assets" + File.separator + "resources" + File.separator + "android" + File.separator + "rawres" + File.separator + "loading" + File.separator + "splash.jpg"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
        FileUtil.replaceResource(splashImage, gameSplashImgPath)
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelName: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName, "qyj2")
    }
}