package goh.channels

import goh.games.*
import java.io.File
import java.time.LocalDateTime

/**
 * 普通买量渠道通用打包脚本
 * 目前可用渠道包括：原包、头条、UC、快手、爱奇艺、搜狗搜索
 */
fun main(vararg args: String) {
    println("通用买量渠道打包任务开始...")
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

    val channelAppId = args[16]             // 渠道 AppId
    val channelAppName = args[17]           // 渠道 AppName
    val channelFile = args[18]              // 渠道注入文件路径
    val channelTag = args[19]               // 渠道标记，0：无，1：头条，2：UC，3：快手，4：爱奇艺；即 0 为无 SDK，非 0 递增为有 SDK
    val channelAbbr = args[20]              // 渠道简称，其实可以根据母包类型判断，但是如果配置 ID 修改就要更新脚本，所以单独传
    val packType = args[21]                 // 母包类型，和后台打包配置 ID 一致

    val loginType = args[22]                // 三方登录类型，0：无，1：微信，2：QQ，3：微信和 QQ
    val qqAppId = args[23]                  // QQ AppId
    val wxAppId = args[24]                  // 微信 AppId
    val thirdPartyBasePatch = args[25]      // 三方登录基础库
    val wxApiPath = args[26]                // WxApi 路径
    val qqLoginPatch = args[27]             // QQ 登录注入文件
    val wxLoginPatch = args[28]             // 微信登录注入文件

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
            
            channelAppId = $channelAppId
            channelAppName = $channelAppName
            channelFile = $channelFile
            channelTag = $channelTag
            channelAbbr = $channelAbbr
            packType = $packType
                    
            loginType = $loginType
            qqAppId = $qqAppId
            wxAppId = $wxAppId
            thirdPartyBasePatch = $thirdPartyBasePatch
            wxApiPath = $wxApiPath
            qqLoginPatch = $qqLoginPatch
            wxLoginPatch = $wxLoginPatch
            
            ═════════════════════════════════════════════════════════════════╝
    """.trimIndent()
    )

    GameFactory(apk).getGame(gid)?.apply {
        decompile(generatePath + File.separator + "temp", apktool)
        replaceResource(loginImg, loadingImg, logoImg, splashImg)
        replaceIcon(icon)
        setAppName(
            appName.ifBlank {
                pkName
            }
        )
        setPackageName(packageName)
        gameConfig(sdkVersion, pkId)
        patchChannelFile(channelFile)
        thirdPartyLogin(
            loginType,
            thirdPartyBasePatch,
            qqLoginPatch,
            qqAppId,
            wxApiPath,
            wxLoginPatch,
            wxAppId,
            packageName
        )
        channelConfig(channelTag, channelAppId, channelAppName)
        setPackType(packType)
        if (generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr)) {
            deleteDecompileDir()
        }
    }
}