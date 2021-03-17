package com.ad

import com.utils.*
import java.io.File

/**
 * 广告包打包脚本
 * 所有市场投放包和运营包都需使用该脚本注入参数才可提供给玩家
 */
fun main(vararg args: String) {
    println("广告包打包任务开始...")

    val apk = args[0]                       // 渠道母包 Apk
    val unzipPath = args[1]                 // 解压操作路径
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

    println(
            """
            ═════════════════════════════════════════════════════════════════╗
            
            apk = $apk
            unzipPath = $unzipPath
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

    if (appName.isBlank()) {                // 不需要修改 AppName，可以用解压的方式，提高打包效率
        File(apk).unzipTo(File(unzipPath))
        val metaInf = File(unzipPath, "META-INF")
        if (metaInf.exists()) {
            FileUtil.delete(metaInf)    // 删除签名信息
        }
        AndroidXmlHandler.updateGameConfig(unzipPath, params)
        File(unzipPath).zipTo(File(apk))
    } else {
        CommandUtil.decompile(apk, unzipPath, apktool)
        AndroidXmlHandler.setAppName(unzipPath, appName)
        AndroidXmlHandler.updateGameConfig(unzipPath, params)
        CommandUtil.exec("java -jar $apktool b $unzipPath -o $apk")
    }

    try {
        if (!System.getProperty("os.name").contains("Windows")) {       // Linux 需要文件操作权限
            CommandUtil.exec("chmod 777 $apk")
        }
        val signCommand = "jarsigner -keystore $keyStorePath -storepass $storePassword -keypass $keyPassword -signedjar $releaseApk $apk $keyAlias"
        if (CommandUtil.exec(signCommand)) {
            println("签名完成")
            FileUtil.delete(File(unzipPath))
        } else {
            println("签名失败")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}