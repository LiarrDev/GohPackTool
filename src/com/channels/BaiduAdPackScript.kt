package com.channels

import com.games.*
import com.utils.PropertiesUtil
import java.io.File

/**
 * 百度 SDK 渠道打包脚本
 * TODO: 可与广点通联盟脚本合并
 */
fun main(vararg  args:String) {
    println("百度 SDK 渠道打包任务开始...")

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

    val channelUserActionSetID = args[16]   // 渠道行为数据源 ID
    val channelAppSecretKey = args[17]      // 渠道数据接入密钥
    val channelFile = args[18]              // 渠道注入文件路径
    val channelTag = args[19]               // 渠道标记，6：百度
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
            
            channelUserActionSetID = $channelUserActionSetID
            channelAppSecretKey = $channelAppSecretKey
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
        gameConfig(sdkVersion, pkid)
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
        channelConfig(channelTag, "", "")
        extra {
            // TODO: 后面可以整合到 ZSmultil 文件内
            PropertiesUtil(File(decompileDir + File.separator + "assets" + File.separator + "BaiduConf.ini"))
                    .setProperties(mapOf(
                            "userActionSetID" to channelUserActionSetID,
                            "appSecretKey" to channelAppSecretKey
                    ))
        }
        setPackType(packType)
        if (generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr)) {
            deleteDecompileDir()
        }
    }
}