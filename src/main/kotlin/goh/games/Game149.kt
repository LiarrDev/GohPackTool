package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 149
 * 深渊之怒（塔防）
 */
class Game149(apk: String) : Game(apk) {

    /**
     * 该游戏支持修改版号信息，路径：/assets/web-mobile-release3/assets/resources/native/6f/6fc4a639-8d40-492b-9196-d3efe4c09fe4.10d56.png
     * 如有需要，建议直接在母包修改
     */
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "web-mobile-release3" + File.separator +
                "assets" + File.separator +
                "resources" + File.separator +
                "native" + File.separator +
                "e1" + File.separator +
                "e15be649-3103-44c4-ade0-444808fd8c6c.5f33b.jpg"
        val gameLogoImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "web-mobile-release3" + File.separator +
                "assets" + File.separator +
                "resources" + File.separator +
                "native" + File.separator +
                "ac" + File.separator +
                "ace7a256-ed58-44af-bf58-f74a48b80d4a.58d42.png"
        val gameSplashImgPath = decompileDir + File.separator +
                "assets" + File.separator +
                "web-mobile-release3" + File.separator +
                "splash.a387e.jpg"
        val gameLoadingImgPath = decompileDir + File.separator +
                "res" + File.separator +
                "drawable" + File.separator +
                "bg_splash.jpg"
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
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "syzn")
    }
}