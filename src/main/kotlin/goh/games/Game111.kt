package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 111
 * 太古封魔录
 */
open class Game111(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "chameleon" + File.separator + "fml_login_background.png"
        val gameLoadingImgPath = decompileDir + File.separator + "assets" + File.separator + "chameleon" + File.separator + "fml_login_update.png"
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "chameleon" + File.separator + "fml_login_logo.png"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "tgfml")
    }
}