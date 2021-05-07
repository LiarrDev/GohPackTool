package goh.games

import goh.utils.FileUtil
import goh.utils.copyDirTo
import goh.utils.getDirectoryList
import java.io.File

/**
 * GID: 137
 * 仙侠神域
 */
class Game137(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir +
                File.separator + "assets" +
                File.separator + "AgentAssets" +
                File.separator + "login_bg.png"
        val gameLoadingImgPath = decompileDir +
                File.separator + "assets" +
                File.separator + "AgentAssets" +
                File.separator + "loading_bg.png"
        val gameLogoImgPath = decompileDir +
                File.separator + "assets" +
                File.separator + "AgentAssets" +
                File.separator + "logo.png"
        val gameSplashImgPath = decompileDir + File.separator + "assets" + File.separator + "splash_image_0.png"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(loadingImage, gameLoadingImgPath)
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
        FileUtil.replaceResource(splashImage, gameSplashImgPath)
    }

    /**
     * 该游戏做了分 Dex 处理，所以要放到 smali_classes2 中
     */
    override fun patchChannelFile(patchFile: String) {
        if (patchFile.isBlank()) {
            println("$patchFile File path is empty")
            return
        }
        File(patchFile).getDirectoryList().forEach {
            when (it.name) {
                "assets", "res" -> File(patchFile, it.name).copyDirTo(File(decompileDir, it.name))
                "smali", "smali_classes2" -> File(patchFile, it.name).copyDirTo(File(decompileDir, "smali_classes2"))
                "so" -> FileUtil.copySoLib(patchFile + File.separator + "so", decompileDir + File.separator + "lib")
            }
        }
    }

    override fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String
    ): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "xxsy")
    }
}