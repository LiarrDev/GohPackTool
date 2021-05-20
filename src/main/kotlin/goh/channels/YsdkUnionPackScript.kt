package goh.channels

import goh.games.*
import goh.utils.AndroidXmlHandler
import goh.utils.FileUtil
import goh.utils.PropertiesUtil
import java.io.File
import java.time.LocalDateTime

/**
 * 应用宝 YSDK 联运打包脚本
 */
fun main(vararg args: String) {
    println("应用宝 YSDK 联运打包任务开始...")
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

    val gdtUserActionSetID = args[16]       // 广点通行为数据源 ID
    val gdtAppSecretKey = args[17]          // 广点通数据接入密钥
    val qqAppId = args[18]                  // QQ AppId
    val wxAppId = args[19]                  // 微信 AppId
    val wxApiPath = args[20]                // WxApi 路径
    val isTest = args[21]                   // 是否为测试包，默认为 0。正式：0，测试：1

    val packType = args[22]                 // 母包类型，和后台打包配置 ID 一致
    val channelFile = args[23]              // 渠道注入文件路径
    val channelTag = "7"                    // 渠道标记，7：应用宝 YSDK，必须非 0，为了切支付登录后依然能通过广点通上报
    val channelAbbr = "YSDK"                // 渠道简称

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
            
            gdtUserActionSetID = $gdtUserActionSetID
            gdtAppSecretKey = $gdtAppSecretKey
            qqAppId = $gdtAppSecretKey$qqAppId
            wxAppId = $gdtAppSecretKey$wxAppId
            wxApiPath = $wxAppId$wxApiPath
            isTest = $isTest
            
            packType = $packType
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
        gameConfig(sdkVersion, pkId, "9")
        patchChannelFile(channelFile)
        channelConfig(channelTag, "", "")
        setPackType(packType)
        extra {
            FileUtil.copyYsdkWxEntry(decompileDir, File(wxApiPath), packageName)
            PropertiesUtil(File(decompileDir + File.separator + "assets" + File.separator + "ysdkconf.ini"))
                .setProperties(
                    mapOf(
                        "userActionSetID" to gdtUserActionSetID,
                        "appSecretKey" to gdtAppSecretKey,
                        "QQ_APP_ID" to qqAppId,
                        "OFFER_ID" to qqAppId,
                        "WX_APP_ID" to wxAppId,
                        "YSDK_URL" to if (isTest == "1") {
                            "https://ysdktest.qq.com"
                        } else {
                            "https://ysdk.qq.com"
                        }
                    )
                )
            AndroidXmlHandler.setYsdkManifest(decompileDir, packageName, qqAppId, wxAppId)
        }
        if (generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelAbbr)) {
            deleteDecompileDir()
        }
    }
}