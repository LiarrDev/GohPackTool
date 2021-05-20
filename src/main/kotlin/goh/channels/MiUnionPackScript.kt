package goh.channels

import goh.games.*
import goh.utils.AndroidXmlHandler
import goh.utils.PropertiesUtil
import java.io.File
import java.time.LocalDateTime

/**
 * 小米联运打包脚本
 */
fun main(vararg args: String) {
    println("小米 联运打包任务开始...")
    println("打包时间：${LocalDateTime.now()}")

    val apk = args[0]                       // 母包 Apk 路径
    val generatePath = args[1]              // 生成的 Apk 路径
    val apktool = args[2]                   // ApkTool 路径
    val keyStorePath = args[3]              // 签名路径

    val gid = args[4]                       // 游戏 GID
    val pkId = args[5]                      // 游戏 PKID
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
    val channelAppId = args[17]             // 小米 AppId
    val channelAppKey = args[18]            // 小米 AppKey
    val channelFile = args[19]              // 渠道注入文件路径
    val channelTag = "8"                    // 渠道标记，8：小米
    val channelAbbr = "Mi"                  // 渠道简称，其实可以根据母包类型判断，但是如果配置 ID 修改就要更新脚本，所以单独传

    println(
        """
            ═════════════════════════════════════════════════════════════════╗
            
            apk = $apk
            generatePath = $generatePath
            apktool = $apktool
            keyStorePath = $keyStorePath
            
            gid = $gid
            pkId = $pkId
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
            channelAppKey = $channelAppKey
            channelFile = $packType$channelFile
            channelTag = $packType$channelTag
            channelAbbr = $packType$channelAbbr
            
            ═════════════════════════════════════════════════════════════════╝
    """.trimIndent()
    )

    val decompileDir = generatePath + File.separator + "temp"
    GameFactory(apk).getGame(gid)?.apply {
        decompile(decompileDir, apktool)
        replaceResource(loginImg, loadingImg, logoImg, splashImg)
        replaceIcon(icon)
        setAppName(
            appName.ifBlank {
                pkName
            }
        )
        setPackageName(packageName)
        gameConfig(sdkVersion, pkId, "8")
        patchChannelFile(channelFile)
        channelConfig(channelTag, "", "")
        setPackType(packType)
        extra {
            AndroidXmlHandler.setMiManifest(decompileDir, packageName)
            PropertiesUtil(File(decompileDir + File.separator + "assets" + File.separator + "mi_config.ini"))
                .setProperties(
                    mapOf(
                        "mi_app_id" to channelAppId,
                        "mi_app_key" to channelAppKey
                    )
                )
        }
        if (generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr)) {
            deleteDecompileDir()
        }
    }
}