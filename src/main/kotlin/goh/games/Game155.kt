package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 155
 * 九梦仙域
 */
class Game155(apk: String) : Game(apk)  {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "AgentAssets" + File.separator + "login_bg.png"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "AgentAssets" + File.separator + "loading_bg.png"
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "AgentAssets" + File.separator + "logo.png"
        val gameSplashImgPath = decompileDir + File.separator + "assets" + File.separator + "splash_image_0.png"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
        FileUtil.replaceResource(splashImage, gameSplashImgPath)
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "jmxy")
    }
}