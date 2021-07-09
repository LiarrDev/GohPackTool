package goh.ad

import goh.utils.*
import java.io.File
import java.time.LocalDateTime

/**
 * 广告包打包脚本
 * 所有市场投放包和运营包都需使用该脚本注入参数才可提供给玩家
 */
fun main(vararg args: String) {
    println("广告包打包任务开始...")
    println("打包时间：${LocalDateTime.now()}")

    // java -jar $脚本jar $渠道母包Apk $临时操作路径 $广告包文件 $Apktool $keystore $storePass $keyPass $keyAlias $appId $pcId $channelId $adId $注册回传倍率 $付费回传倍率 $重新修改AppName

    val apk = args[0]                       // 渠道母包 Apk
    val cachePath = args[1]                 // 临时操作路径
    val releaseApk = args[2]                // 最终释出的 Apk 文件
    val apktool = args[3]                   // ApkTool 路径

    val keyStorePath = args[4]              // 签名路径
    val storePassword = args[5]
    val keyPassword = args[6]
    val keyAlias = args[7]

    val appId = args[8]
    val pcId = args[9]
    val channelId = args[10]
    val adId = args[11]

    val registerRatio = args[12]            // 注册回传倍率，默认是 1
    val purchaseRatio = args[13]            // 付费回传倍率，默认是 1

    val appName = args[14]                  // 部分渠道（如百度搜索）可能会重新修改 AppName
    val authLoginPackage = args[15]         // 阿里云手机号码一键登录包名

    println(
        """
            ═════════════════════════════════════════════════════════════════╗
            
            apk = $apk
            cachePath = $cachePath
            releaseApk = $releaseApk
            apktool = $keyStorePath$apktool
            
            keyStorePath = $keyStorePath
            storePassword = $storePassword
            keyPassword = $keyPassword
            keyAlias = $keyAlias
            
            appId = $appId
            pcId = $pcId
            channelId = $channelId
            adId = $adId
            
            registerRatio = $registerRatio
            purchaseRatio = $purchaseRatio
            appName = $appName
            authLoginPackage = $authLoginPackage
            
            ═════════════════════════════════════════════════════════════════╝
    """.trimIndent()
    )

    val params = mapOf(
        "appid" to appId,
        "pcid" to pcId,
        "channel" to channelId,
        "adid" to adId,
        "packtype" to "-1",
        "register_ratio" to registerRatio,
        "purchase_ratio" to purchaseRatio
    )

    val unzipTemp = cachePath + "temp"
    val unsignedApk = File(cachePath, apk.substring(apk.lastIndexOf("/") + 1))

    if (appName.isBlank()) {                // 不需要修改 AppName，可以用解压的方式，提高打包效率（如果将单独文件解压出来处理的话可能进一步提高打包效率）
        File(apk).unzipTo(File(unzipTemp))
        val metaInf = File(unzipTemp, "META-INF")
        if (metaInf.exists()) {
            FileUtil.delete(metaInf)    // 删除签名信息
        }
        AndroidXmlHandler.updateGameConfig(unzipTemp, params)
        val sdkVersion = AndroidXmlHandler.getSdkVersion(unzipTemp)
        if (compareVersion(sdkVersion, "3.2.0.5") >= 0) {   // 版本大于 3.2.0.5 才支持一键登录
            PropertiesUtil(File(unzipTemp + File.separator + "assets" + File.separator + "ZSmultil"))
                .setProperties(mapOf("phone_auth_package" to authLoginPackage))
        }
        File(unzipTemp).zipTo(unsignedApk)
    } else {
        CommandUtil.decompile(apk, unzipTemp, apktool)
        AndroidXmlHandler.setAppName(unzipTemp, appName)        // FIXME: Windows 下可能会由于乱码无法 Build，Linux 下正常
        AndroidXmlHandler.updateGameConfig(unzipTemp, params)
        val sdkVersion = AndroidXmlHandler.getSdkVersion(unzipTemp)
        if (compareVersion(sdkVersion, "3.2.0.5") >= 0) {   // 版本不小于 3.2.0.5 才支持一键登录
            PropertiesUtil(File(unzipTemp + File.separator + "assets" + File.separator + "ZSmultil"))
                .setProperties(mapOf("phone_auth_package" to authLoginPackage))
        }
        CommandUtil.exec("java -jar $apktool b $unzipTemp -o ${unsignedApk.absolutePath}")
    }

    File(releaseApk).apply {
        if (!File(parent).exists()) {
            File(parent).mkdirs()
        }
    }

    try {
        if (!System.getProperty("os.name").contains("Windows")) {       // Linux 可能需要文件操作权限
            CommandUtil.exec("chmod 777 $apk")
        }
        val signCommand =
            "jarsigner -keystore $keyStorePath -storepass $storePassword -keypass $keyPassword -signedjar $releaseApk ${unsignedApk.absolutePath} $keyAlias"
        if (CommandUtil.exec(signCommand)) {
            FileUtil.delete(File(cachePath))
            println("签名完成")
        } else {
            println("签名失败")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}