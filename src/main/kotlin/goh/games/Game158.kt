package goh.games

import goh.utils.FileUtil
import goh.utils.copyDirTo
import goh.utils.getDirectoryList
import java.io.File

/**
 * GID: 158
 * 逍遥三国
 */
class Game158(apk: String) : Game(apk) {
    override fun replaceResource(
        loginImage: String?,
        loadingImage: String?,
        logoImage: String?,
        splashImage: String?
    ) {
    }

    /**
     * CP 采用工程出包，导致 SDK 内的文件分在两个不同的 Dex 中。
     * 这里将 smali 文件夹中的 SDK 文件移动到 smali_classes2，
     * 然后把渠道注入到 smali_classes2
     */
    override fun patchChannelFile(patchFile: String) {
        if (patchFile.isBlank()) {
            println("Channel File path is empty")
            return
        }
        val sourceSdkDir = File(decompileDir, "smali/com/mayisdk")
        val targetSdkDir = File(decompileDir, "smali_classes2/com/mayisdk")
        val sourceRyDir = File(decompileDir, "smali/com/rongyao")
        val targetRyDir = File(decompileDir, "smali_classes2/com/rongyao")
        sourceSdkDir.copyDirTo(targetSdkDir)
        sourceRyDir.copyDirTo(targetRyDir)
        FileUtil.delete(sourceSdkDir)
        FileUtil.delete(sourceRyDir)
        File(patchFile).getDirectoryList().forEach {
            when (it.name) {
                "assets", "res" -> File(patchFile, it.name).copyDirTo(File(decompileDir, it.name))
                "smali", "smali_classes2" -> File(patchFile, it.name).copyDirTo(File(decompileDir, "smali_classes2"))
                "so", "lib", "jni" -> FileUtil.copySoLib(
                    patchFile + File.separator + it.name,
                    decompileDir + File.separator + "lib"
                )
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
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "xysg")
    }
}