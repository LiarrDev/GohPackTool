package goh.games

import goh.utils.FileUtil
import java.io.File

/**
 * GID: 148
 * 疾风忍者
 *
 * NOTE: 该游戏 targetSdkVersion 为 30，当前脚本只用 V1 签名会导致在 Android 11 上安装不了。
 * 目前两种解决方案：
 * 1. 手动给游戏母包降级为 29；
 * 2. 用 V2 重新签名，广告服务器已支持 V2 签名
 */
class Game148(apk: String) : Game(apk) {
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "l_logo.png"
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "jfrz")
    }
}