package goh.games

import goh.utils.FileUtil
import goh.utils.copyDirTo
import goh.utils.getDirectoryList
import java.io.File

/**
 * GID: 146
 * 数码兽
 */
class Game146(apk: String) : Game(apk) {

    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLogoImgPath = decompileDir + File.separator + "assets" + File.separator + "logo.png"
        FileUtil.replaceResource(logoImage, gameLogoImgPath)
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
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "sms")
    }
}