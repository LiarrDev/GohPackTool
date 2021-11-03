package goh.ad

import goh.utils.*
import java.io.File
import java.time.LocalDateTime

/**
 * 广告包打包脚本
 * 所有市场投放包和运营包都需使用该脚本注入参数才可提供给玩家
 *
 * Usage: java -jar $脚本jar $渠道母包Apk $临时操作路径 $广告包文件 $Apktool $ZipAlign $ApkSigner $keystore $storePass $keyPass $keyAlias $appId $pcId $channelId $adId $注册回传倍率 $付费回传倍率 $重新修改AppName $一键登录包名
 */
fun main(vararg args: String) {
    println("广告包打包任务开始...")
    println("打包时间：${LocalDateTime.now()}")

    val apk = args[0]                       // 渠道母包 Apk
    val cachePath = args[1]                 // 临时操作路径
    val releaseApk = args[2]                // 最终释出的 Apk 文件
    val apktool = args[3]                   // ApkTool 路径

    val zipAlign = args[4]                  // ZipAlign 对齐工具
    val apkSigner = args[5]                 // ApkSigner 签名工具

    val keyStorePath = args[6]              // 签名路径
    val storePassword = args[7]
    val keyPassword = args[8]
    val keyAlias = args[9]

    val appId = args[10]
    val pcId = args[11]
    val channelId = args[12]
    val adId = args[13]

    val registerRatio = args[14]            // 注册回传倍率，默认是 1
    val purchaseRatio = args[15]            // 付费回传倍率，默认是 1

    val appName = args[16]                  // 部分渠道（如百度搜索）可能会重新修改 AppName，注：Windows PS 运行时该参数为空会报错，改用其他终端（如：CMD）运行即可

    println(
        """
            ═════════════════════════════════════════════════════════════════╗
            
            apk = $apk
            cachePath = $cachePath
            releaseApk = $releaseApk
            apktool = $keyStorePath$apktool
            
            zipAlign = $zipAlign
            apkSigner = $apkSigner
            
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

    val unzipTemp = File(cachePath, "temp").absolutePath
    val unsignedApk = File(cachePath, apk.substring(apk.lastIndexOf(File.separator) + 1))
    println(
        """
        unzipTemp = $unzipTemp
        unsignedApk = $unsignedApk
    """.trimIndent()
    )

    // NOTE: 改用 ApkSigner 签名后，不能再使用解压缩的方式处理，只能使用 ApkTool 反编译处理
    CommandUtil.decompile(apk, unzipTemp, apktool)
    if (appName.isNotBlank()) {
        AndroidXmlHandler.setAppName(unzipTemp, appName)
    }
    AndroidXmlHandler.updateGameConfig(unzipTemp, params)
    CommandUtil.exec("java -jar $apktool b $unzipTemp -o ${unsignedApk.absolutePath}")

    File(releaseApk).apply {
        if (!File(parent).exists()) {
            File(parent).mkdirs()
        }
    }

    try {
        if (!System.getProperty("os.name").contains("Windows")) {       // Linux 可能需要文件操作权限
            CommandUtil.exec("chmod 777 $apk")
        }
        val alignApkPath = File(cachePath, "align.apk").absolutePath
        val alignCommand = "$zipAlign 4 ${unsignedApk.absolutePath} $alignApkPath"
        val apkSignCommand =
            "java -jar $apkSigner sign --ks $keyStorePath --ks-key-alias $keyAlias --ks-pass pass:$storePassword --key-pass pass:$keyPassword --out $releaseApk $alignApkPath"

        if (CommandUtil.exec(alignCommand)) {
            println("对齐完成")
        } else {
            println("对齐失败")
            return
        }

        if (CommandUtil.exec(apkSignCommand)) {
            // 删除临时目录
            FileUtil.delete(File(cachePath))
            // 删除 ApkSigner 签名所生成的 IDSIG 文件
            FileUtil.delete(
                File(
                    File(releaseApk).parentFile,
                    releaseApk.substring(releaseApk.lastIndexOf(File.separator) + 1) + ".idsig"
                )
            )
            println("ApkSigner 签名完成")
        } else {
            println("ApkSigner 签名失败")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}