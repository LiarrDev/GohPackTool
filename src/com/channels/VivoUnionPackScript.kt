package com.channels

import com.games.*
import com.utils.FileUtil
import com.utils.PropertiesUtil
import java.io.File

/**
 * VIVO 联运打包脚本
 */
fun main(vararg args: String) {
    println("ViVO 联运打包任务开始...")

    val apk = args[0]                       // 母包 Apk 路径
    val generatePath = args[1]              // 生成的 Apk 路径
    val apktool = args[2]                   // ApkTool 路径
    val keyStorePath = args[3]              // 签名路径

    val gid = args[4]                       // 游戏 GID
    val pkid = args[5]                      // 游戏 PKID
    val pkName = args[6]                    // 副包名称
    val packageName = args[7]               // 包名
    val appName = args[8]                   // 应用名称，如果为空则使用副包名称
    val appVersion = args[9]                // 应用版本号
    val sdkVersion = args[10]               // SDK 版本号

    val icon = args[11]                     // ICON 路径
    val loginImg = args[12]                 // 登录背景图路径
    val logoImg = args[13]                  // LOGO 路径
    val loadingImg = args[14]               // 加载背景图路径
    val splashImg = args[15]                // 闪屏路径

    val packType = args[16]                 // 母包类型，和后台打包配置 ID 一致
    val channelAppId = args[17]             // ViVO AppId
    val channelFile = args[18]              // 渠道注入文件路径
    val channelTag = "10"                   // 渠道标记，10：ViVO
    val channelAbbr = "ViVO"                // 渠道简称

    println(
            """
            ═════════════════════════════════════════════════════════════════╗
            
            apk = $apk
            generatePath = $generatePath
            apktool = $apktool
            keyStorePath = $keyStorePath
            
            gid = $gid
            pkid = $pkid
            pkName = $pkName
            packageName = $packageName
            appName = $appName
            appVersion = $appVersion
            sdkVersion = $sdkVersion
            
            icon = $icon
            loginImg = $loginImg
            logoImg = $logoImg
            loadingImg = $loadingImg
            splashImg = $splashImg
            
            packType = $packType
            channelAppId = $channelAppId
            channelFile = $packType$channelFile
            channelTag = $packType$channelTag
            channelAbbr = $packType$channelAbbr
            
            ═════════════════════════════════════════════════════════════════╝
    """.trimIndent())

    val game = when (gid) {
        "111" -> Game111(apk)
        "116" -> Game116(apk)
        "119" -> Game119(apk)
        "120" -> Game120(apk)
        "123" -> Game123(apk)
        "124" -> Game124(apk)
        "125" -> Game125(apk)
        "126" -> Game126(apk)
        "127" -> Game127(apk)
        "128" -> Game128(apk)
        "129" -> Game129(apk)
        "131" -> Game131(apk)
        "132" -> Game132(apk)
        "133" -> Game133(apk)
        "135" -> Game135(apk)
        "136" -> Game136(apk)
        "137" -> Game137(apk)
        "139" -> Game139(apk)
        "141" -> Game141(apk)
        else -> null
    }
    game?.apply {
        val decompileDir = generatePath + File.separator + "temp"
        decompile(decompileDir, apktool)
        replaceResource(loginImg, loadingImg, logoImg, splashImg)
        replaceIcon(icon)
        setAppName(
                if (appName.isBlank()) {
                    pkName
                } else {
                    appName
                }
        )
        setPackageName(packageName)
        gameConfig(sdkVersion, pkid, "4")
        patchChannelFile(channelFile)
        channelConfig(channelTag, "", "")
        setPackType(packType)
        extra {
            FileUtil.deleteOriginPayMethod(decompileDir)
            PropertiesUtil(File(decompileDir + File.separator + "assets" + File.separator + "ZSmultil"))
                    .setProperties(mapOf("open_delay" to "1"))      // 根据广告来源回传到不同的 AID，但广告来源需要到登录后才能拿到，所以此处用于对初始化延迟上报

        }
        if (generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr)) {
            deleteDecompileDir()
        }
    }
}