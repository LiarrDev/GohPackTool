package com.games

import com.utils.FileUtil
import java.io.File

/**
 * GID: 139
 * 山海异兽H5
 */
class Game139(apk: String) : Game(apk) {

    /**
     * 该游戏不能更改 Logo 和 Loading 图
     * 但是支持修改创角图
     * 男性创角图通过 Logo 图上传
     * 女性创角图通过 Loading 图上传
     */
    override fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?) {
        val gameLoginImgPath = decompileDir + File.separator + "assets" + File.separator + "game" + File.separator + "resource" + File.separator + "loginres" + File.separator + "login_bg_f_44ae01ef.jpg"
        val gameSplashImgPath = decompileDir + File.separator + "assets" + File.separator + "chameleon" + File.separator + "chameleon_splashscreen_0.png"
        val gameMaleRoleImgPath = decompileDir + File.separator + "assets" + File.separator + "game" + File.separator + "resource" + File.separator + "shj_assets" + File.separator + "picture" + File.separator + "bigbg" + File.separator + "create_role_bg1_f1875cc3.png"
        val gameFemaleRoleImgPath = decompileDir + File.separator + "assets" + File.separator + "game" + File.separator + "resource" + File.separator + "shj_assets" + File.separator + "picture" + File.separator + "bigbg" + File.separator + "create_role_bg2_e7dbb08c.png"
        FileUtil.replaceResource(loginImage, gameLoginImgPath)
        FileUtil.replaceResource(splashImage, gameSplashImgPath)
        FileUtil.replaceResource(logoImage, gameMaleRoleImgPath)
        FileUtil.replaceResource(loadingImage, gameFemaleRoleImgPath)
    }

    override fun generateSignedApk(keyStorePath: String, generatePath: String, gid: String, appVersion: String, channelAbbr: String): Boolean {
        return generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr, "shysH5")
    }

    /**
     * 打入某些联运渠道后会提示方法超限，所以要做分 Dex 处理
     * TODO: 待测试
     */
    override fun patchChannelFile(patchFile: String) {
        super.patchChannelFile(patchFile)
        val smaliClasses2 = File(decompileDir, "smali_classes2")
        val okHttp3 = File(decompileDir + File.separator + "smali" + File.separator + "okhttp3")
        if (!smaliClasses2.exists()) {
            smaliClasses2.mkdirs()
            println("避免方法超限，创建 smali_classes2")
        }
        okHttp3.renameTo(File(smaliClasses2, "okhttp3"))
        println("移动三方库到 smali_classes2")
    }
}