package com.channels

import com.games.*
import java.io.File

/**
 * 普通买量渠道通用打包脚本
 * 目前可用渠道包括：原包、头条、UC、快手、爱奇艺、搜狗搜索
 */
fun main(args: Array<String>) {
    println("买量渠道打包任务开始...")
    val apk = args[0]
    val generatePath = args[1]
    val icon = args[2]
    val loginImg = args[3]
    val logoImg = args[4]
    val loadingImg = args[5]
    val packageName = args[6]
    val pkName = args[7]            // 副包名称
    val pkid = args[8]
    val channelAppId = args[9]
    val channelTag = args[10]       // 后面去掉
    val channelCode = args[11]      // 0：无，1：头条，2：UC，3：快手       // TODO: 其他渠道应该也要增加，但未增加
    val appVersion = args[12]
    val keyStorePath = args[13]
    val apktool = args[14]
    val sdkVersion = args[15]
    val loginType = args[16]        // 0：无，1：微信，2：QQ，3：微信和 QQ
    val qqAppId = args[17]
    val wxAppId = args[18]
    val thirdPartyBasePatch = args[19]
    val wxApiPath = args[20]
    val qqLoginPatch = args[21]
    val wxLoginPatch = args[22]
    val channelAppName = args[23]
    val gid = args[24]
    val splashImg = args[25]
    val channelFile = args[26]
    val appInfo = args[27]          // 是否获取手机应用列表，1：获取，0：不获取
    val appName = args[28]
    val channelName = args[29]

    println("""
        apk = $apk
        generatePath = $generatePath
        icon = $icon
        loginImg = $loginImg
        logoImg = $logoImg
        loadingImg = $loadingImg
        packageName = $packageName
        pkName = $pkName
        pkid = $pkid
        channelAppId = $channelAppId
        channelTag = $channelTag
        channelCode = $channelCode
        appVersion = $appVersion
        keyStorePath = $keyStorePath
        apktool = $apktool
        sdkVersion = $sdkVersion
        loginType = $loginType
        qqAppId = $qqAppId
        wxAppId = $wxAppId
        thirdPartyBasePatch = $thirdPartyBasePatch
        wxApiPath = $wxApiPath
        qqLoginPatch = $qqLoginPatch
        wxLoginPatch = $wxLoginPatch
        channelAppName = $channelAppName
        gid = $gid
        splashImg = $splashImg
        channelFile = $channelFile
        appInfo = $appInfo
        appName = $appName
        channelName = $channelName
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
    game?.let {
        it.decompile(generatePath + File.separator + "temp", apktool)
        it.replaceResource(loginImg, loadingImg, logoImg, splashImg)
        it.replaceIcon(icon)
        it.setAppName(if (appName.isBlank()) {
            pkName
        } else {
            appName
        })
        it.setPackageName(packageName)
        it.gameConfig(sdkVersion, pkid)
        it.patchChannelFile(channelFile)
        it.thirdPartyLogin(loginType, thirdPartyBasePatch, qqLoginPatch, qqAppId, wxApiPath, wxLoginPatch, wxAppId, packageName)
        it.channelConfig(channelCode, channelAppId, channelAppName, channelName, appInfo)
        it.setPackType(1)               // FIXME：增加参数
        if (it.generateSignedApk(keyStorePath, generatePath, gid, appVersion, channelName)) {
            it.deleteDecompileDir()
        }
    }


}