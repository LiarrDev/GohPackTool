package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 147
 * 九州横板测试
 */
class Game153(apk: String) : Game(apk) {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "agent" + File.separator +
                "login_bg_base.jpg"
        val gameLoadingImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "agent" + File.separator +
                "loading_bg_base.jpg"
        val gameLogoImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "agent" + File.separator +
                "logo_base.png"
        val gameSplashImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "agent" + File.separator +
                "loading_bg_1.jpg"      // 内加载页
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
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "jzhbcs")
    }
}