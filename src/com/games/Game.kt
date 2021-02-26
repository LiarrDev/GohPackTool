package com.games

import com.utils.*
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * 所有游戏的基类，调用时应当按照顺序调用，某些方法可能不需要调用
 */
abstract class Game(private val apk: String) {

    protected lateinit var decompileDir: String
    private lateinit var apktool: String
    private lateinit var gameName: String
    private lateinit var pkgName: String
    private lateinit var pkid: String
    private var loginType = "0"

    /**
     * 反编译
     */
    fun decompile(decompileDir: String, apktool: String): Boolean {

        this.decompileDir = decompileDir
        this.apktool = apktool

        val apkFile = File(apk)
        return if (apkFile.exists() && apkFile.isFile) {
            val decompileFile = File(decompileDir)
            if (decompileFile.exists()) {
                FileUtil.delete(decompileFile)
            }
            val command = "java -jar $apktool d -f $apk -o $decompileDir --only-main-classes"
            CommandUtil.exec(command)
        } else {
            print("APK is not exist.")
            false
        }
    }

    /**
     * 替换素材资源
     */
    abstract fun replaceResource(loginImage: String?, loadingImage: String?, logoImage: String?, splashImage: String?)

    /**
     * 替换应用图标
     */
    fun replaceIcon(icon: String): Boolean {
        if (icon.isBlank()) {
            return true
        }
        if (!icon.endsWith(".png")) {
            println("ICON 格式不正确")
            throw Exception("ICON 格式不正确")
        }
        val iconName = AndroidManifestHandler.getIconName(decompileDir)
        val file = File(icon)
        return if (file.exists() && file.isFile) {

            // xxxhdpi
            DrawableUtil.replaceIcon(decompileDir, file, "xxxhdpi", iconName)

            // xxhdpi 和 drawable
            val xxhdpiImage: File? = DrawableUtil.resizeImage(
                icon,
                144,
                144,
                decompileDir + File.separator + "temp_icon" + File.separator + "xx"
            )
            xxhdpiImage?.let {
                DrawableUtil.replaceIcon(decompileDir, it, "xxhdpi", iconName)
                it.replace(File(decompileDir + File.separator + "res" + File.separator + "drawable" + File.separator + iconName))
            }

            // xhdpi
            val xhdpiImage: File? = DrawableUtil.resizeImage(
                icon,
                96,
                96,
                decompileDir + File.separator + "temp_icon" + File.separator + "xx"
            )
            xhdpiImage?.let {
                DrawableUtil.replaceIcon(decompileDir, it, "xhdpi", iconName)
            }

            val lowDpiImage: File? = DrawableUtil.resizeImage(
                icon,
                72,
                72,
                decompileDir + File.separator + "temp_icon" + File.separator + "xx"
            )
            lowDpiImage?.let {
                DrawableUtil.replaceIcon(decompileDir, it, "hdpi", iconName)
                DrawableUtil.replaceIcon(decompileDir, it, "mdpi", iconName)
                DrawableUtil.replaceIcon(decompileDir, it, "ldpi", iconName)
            }

            true
        } else {
            print("ICON 路径无效")
            false
        }
    }

    /**
     * 修改 AppName
     */
    fun setAppName(appName: String) {
        gameName = appName
        val xml = decompileDir + File.separator + "res" + File.separator + "values" + File.separator + "strings.xml"
        val document = File(xml).loadDocument()
        val element = document?.documentElement
        element?.apply {
            if (hasChildNodes()) {
                val nodes = document.getElementsByTagName("string")
                for (i in 0 until nodes.length) {
                    val node = nodes.item(i)
                    if (node.attributes.getNamedItem("name").nodeValue == "app_name") {
                        node.firstChild.nodeValue = appName
                        break
                    }
                }
            }
        }
        document?.toFile(File(xml))
    }

    /**
     * 修改包名
     */
    fun setPackageName(packageName: String) {
        pkgName = packageName
        val file = File(decompileDir + File.separator + "AndroidManifest.xml")
        var manifest = file.readText()
        val packageMatcher = Pattern.compile("package=\"(.*?)\"").matcher(manifest)
        packageMatcher.find()
        val oldPackageName = packageMatcher.group(1)
        manifest = manifest.replace(oldPackageName, packageName)

        val authoritiesMatcher = Pattern.compile("android:authorities=\"(.*?)\"").matcher(manifest)
        if (authoritiesMatcher.find()) {
            val s = authoritiesMatcher.group(1)
            manifest = manifest.replaceFirst(s, s.replace(packageMatcher.group(1), packageName))
        }

        manifest = manifest.replace("android:name=\"\\.", "android:name=\"$oldPackageName\\.")

        file.writeText(manifest)
    }

    /**
     * 修改游戏配置 ZSinfo.xml
     */
    fun gameConfig(sdkVersion: String, pkid: String) {
        this.pkid = pkid
        val xml = decompileDir + File.separator + "assets" + File.separator + "ZSinfo.xml"
        val document = File(xml).loadDocument()
        val element = document?.documentElement
        element?.apply {
            if (hasChildNodes()) {
                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i)
                    when (node.nodeName) {
                        "kpid", "pkid" -> node.firstChild.nodeValue = pkid
                        "version" -> node.firstChild.nodeValue = sdkVersion
                    }
                }
            }
        }
        document?.toFile(File(xml))
    }

    /**
     * 注入渠道文件
     */
    open fun patchChannelFile(patchFile: String) {
        if (patchFile.isBlank()) {
            println("$patchFile File path is empty")
            return
        }
        val list = FileUtil.getDirectoryList(File(patchFile))
        list.forEach { dirName ->
            when (dirName) {
                "assets" -> File(patchFile, "assets").copyDir(File(decompileDir, "assets"))
                "smali" -> File(patchFile, "smali").copyDir(File(decompileDir, "smali"))
                "res" -> File(patchFile, "res").copyDir(File(decompileDir, "res"))
                "so" -> FileUtil.copySoLib(
                    patchFile + File.separator + "so",
                    decompileDir + File.separator + "lib"
                )
            }
        }
    }

    /**
     * 第三方登录
     */
    fun thirdPartyLogin(
        loginType: String?,
        thirdPartyBasePatch: String,
        qqLoginPatch: String,
        qqAppId: String,
        wxApiPath: String,
        wxLoginPatch: String,
        weChatAppId: String,
        packageName: String
    ) {
        val open = (loginType != null && loginType.isNotBlank() && "0" != loginType)
        if (open) {
            this.loginType = loginType!!
            val sourceSdk = File(thirdPartyBasePatch + File.separator + "sdk")
            val sourceMSDK = File(thirdPartyBasePatch + File.separator + "msdk")
            val targetSmali = decompileDir + File.separator + "smali"
            val targetMSDK = targetSmali + File.separator + "com" + File.separator + "mayisdk" + File.separator + "msdk"
            val targetSdk = targetMSDK + File.separator + "api" + File.separator + "sdk"
            sourceSdk.copyDir(File(targetSdk))
            sourceMSDK.copyDir(File(targetMSDK))
            val manifest =
                AndroidManifestHandler.getThirdPartyLoginManifest(loginType, qqAppId, weChatAppId, packageName)
            AndroidManifestHandler.addApplicationConfig(decompileDir, manifest)

            when (loginType) {
                "1" -> {
                    FileUtil.copyWeChatLoginFile(decompileDir, File(wxApiPath), packageName)
                    File(wxLoginPatch).copyDir(File(targetSmali))
                }
                "2" -> {
                    File(qqLoginPatch).copyDir(File(targetSmali))
                }
                "3" -> {
                    FileUtil.copyWeChatLoginFile(decompileDir, File(wxApiPath), packageName)
                    File(wxLoginPatch).copyDir(File(targetSmali))
                    File(qqLoginPatch).copyDir(File(targetSmali))
                }
            }
        }
    }

    /**
     * 渠道配置
     */
    open fun channelConfig(
        channelTag: String,
        channelAppId: String,
        channelAppName: String,
        appInfo: String = "0"
    ) {
        val map = HashMap<String, String>()
        map["isReport"] = if ("0" == channelTag) "0" else "1"
        if ("1" == channelTag) {                                    // 头条的 AppId 做加密处理
            map["appId"] = EncryptUtil.encryptAppId(channelAppId)
            map["tt_appId"] = EncryptUtil.getFakeAppId()            // 这个字段已废弃，生成一个假的 AppId 用来迷惑
        } else {
            map["appId"] = channelAppId
        }
        map["appName"] = if (channelAppName.isEmpty()) gameName else channelAppName
        map["channel"] = "0"                                        // 这个字段暂时没有渠道需要使用（某些渠道可选，但我们暂未使用，头条渠道不能为空）
        map["appinfo"] = if (appInfo == "1") "1" else "0"           // 获取应用列表。现在都不获取了所以默认置 0，但留了方法，需要再在脚本郑家
        map["issplash"] = "0"                                       // 是否开启闪屏。代号黎明必须关闪屏，否则会有按键冲突，现在所有游戏都关

        FileUtil.writePlatformProperties(
            File(decompileDir + File.separator + "assets" + File.separator + "ZSmultil"),
            map
        )
    }

    /**
     * 设置 Pack Type，跟后台打包配置的母包类型 ID 对应
     */
    open fun setPackType(packType: String): Boolean {
        return try {
            val file = File(decompileDir + File.separator + "assets" + File.separator + "ZSinfo.xml")
            val document = SAXReader().read(file)
            val element = document.rootElement.element("packtype")
            element.text = packType
            val writer = XMLWriter(FileWriter(file))
            writer.write(document)
            writer.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 额外操作
     */
    fun extra(block: () -> Unit) {
        block()
    }

    /**
     * 生成最终渠道包，子类重写调用同名方法传入 GameName
     */
    abstract fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String = "Undefine"
    ): Boolean

    /**
     * 生成最终渠道包，该方法必须在子类传入 GameName
     */
    protected fun generateSignedApk(
        keyStorePath: String,
        generatePath: String,
        gid: String,
        appVersion: String,
        channelAbbr: String = "Undefine",
        gameName: String = "UNKNOWN"
    ): Boolean {
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HHmmss"))
        val fileName = "${gameName}_${
            pkgName.substring(pkgName.lastIndexOf(".") + 1)     // 截取包名最后一段拼接，由于现参数混用，这种方法已无法标记到包，只能靠 pkid，后面可以考虑将 pkName 转拼音拼接
        }_${gid}_${pkid}_${appVersion}_rongyao_${time}_${channelAbbr}${loginType}.apk"
        val filePath = generatePath + File.separator + fileName
        println("文件路径：$filePath")

        val buildCommand = "java -jar $apktool b $decompileDir"
        return if (CommandUtil.exec(buildCommand)) {
            println("回编译成功！！！！！！！！！！！！！！！")
            val unsignedApk = decompileDir + File.separator + "dist" + File.separator + File(apk).name
            println("待签名文件：$unsignedApk")
            val keyStoreName = File(keyStorePath).name
            if (keyStoreName == "rongyao.jks") {
                val storePassword = "ry201901"
                val keyPassword = "ry201901"
                val keyAlias = "rongyao"
                val signCommand =
                    "jarsigner -keystore $keyStorePath -storepass $storePassword -keypass $keyPassword -signedjar $filePath $unsignedApk $keyAlias"
                if (CommandUtil.exec(signCommand)) {
                    println("##-------------------打包结果---------------------##")
                    println("##------------打包成功：------------------------##")
                    println("##---文件名：$fileName-------------##")
                    println("##------------------打包结果end--------------------##")
                    true
                } else {
                    println("##---签名出错----##")
                    println("##------------------打包结果end--------------------##")
                    false
                }
            } else {
                println("签名不存在！！！！！！！！！！！！！！！")
                false
            }
        } else {
            println("回编译失败！！！！！！！！！！！！！！！")
            false
        }
    }

    /**
     * 删除反编译生成的临时目录
     */
    fun deleteDecompileDir() {
        FileUtil.delete(File(decompileDir))
    }
}