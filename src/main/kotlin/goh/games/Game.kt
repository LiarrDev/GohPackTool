package goh.games

import goh.utils.*
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
    private lateinit var pkId: String
    private var loginType = "0"

    /**
     * 反编译
     */
    fun decompile(decompileDir: String, apktool: String): Boolean {

        this.decompileDir = decompileDir
        this.apktool = apktool

        return CommandUtil.decompile(apk, decompileDir, apktool)
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
        val iconName = AndroidXmlHandler.getIconName(decompileDir)
        val file = File(icon)
        return if (file.exists() && file.isFile) {      // TODO: 测试在文件夹不存在时会不会报错（天命山海）

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
     * @param sdkVersion SDK 版本号
     * @param pkId 副包 ID，配置文件里 kpid 和 pkid 都要改，kpid 是老名字，后面废弃
     * @param unionPlatform 联运平台类型，默认买量是 1，其他联运根据后台设定
     */
    fun gameConfig(sdkVersion: String, pkId: String, unionPlatform: String = "1") {
        this.pkId = pkId
        val file = File(decompileDir + File.separator + "assets" + File.separator + "ZSinfo.xml")
        val document = file.loadDocument()
        document?.documentElement?.apply {
            if (hasChildNodes()) {
                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i)
                    when (node.nodeName) {
                        "kpid", "pkid" -> node.firstChild.nodeValue = pkId
                        "version" -> node.firstChild.nodeValue = sdkVersion
                        "platform" -> node.firstChild.nodeValue = unionPlatform
                    }
                }
            }
        }
        document?.toFile(file)
    }

    /**
     * 注入渠道文件
     */
    open fun patchChannelFile(patchFile: String) {
        if (patchFile.isBlank()) {
            println("$patchFile File path is empty")
            return
        }
        File(patchFile).getDirectoryList().forEach { dir ->
            when (dir.name) {
                "assets", "smali", "smali_classes2", "res" -> File(patchFile, dir.name).copyDirTo(File(decompileDir, dir.name))
                "so", "jni" -> FileUtil.copySoLib(     // FIXME: 有些渠道对该文件夹命名不一，后面要统一
                        patchFile + File.separator + dir.name,
                        decompileDir + File.separator + "lib"
                )
            }
        }
    }

    /**
     * 第三方登录
     * 普通买量渠道接入，联运渠道不可使用
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
        val open = !loginType.isNullOrBlank() && "0" != loginType
        if (open) {
            this.loginType = loginType!!
            val sourceSdk = File(thirdPartyBasePatch, "sdk")
            val sourceMSDK = File(thirdPartyBasePatch, "msdk")
            val targetSmali = decompileDir + File.separator + "smali"
            val targetMSDK = targetSmali + File.separator + "com" + File.separator + "mayisdk" + File.separator + "msdk"
            val targetSdk = targetMSDK + File.separator + "api" + File.separator + "sdk"
            sourceSdk.copyDirTo(File(targetSdk))
            sourceMSDK.copyDirTo(File(targetMSDK))
            AndroidXmlHandler.setThirdPartyLoginManifest(decompileDir, loginType, qqAppId, weChatAppId, packageName)

            when (loginType) {
                "1" -> {
                    FileUtil.copyWeChatLoginFile(decompileDir, File(wxApiPath), packageName)
                    File(wxLoginPatch).copyDirTo(File(targetSmali))
                }
                "2" -> {
                    File(qqLoginPatch).copyDirTo(File(targetSmali))
                }
                "3" -> {
                    FileUtil.copyWeChatLoginFile(decompileDir, File(wxApiPath), packageName)
                    File(wxLoginPatch).copyDirTo(File(targetSmali))
                    File(qqLoginPatch).copyDirTo(File(targetSmali))
                }
            }
        }
    }

    /**
     * 渠道配置
     */
    fun channelConfig(
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
        map["appName"] = if (channelAppName.isBlank()) gameName else channelAppName
        map["channel"] = "0"                                        // 这个字段暂时没有渠道需要使用（某些渠道可选，但我们暂未使用，头条渠道不能为空）
        map["appinfo"] = if (appInfo == "1") "1" else "0"           // 获取应用列表。现在都不获取了所以默认置 0，但留了方法，需要再在脚本增加
        map["issplash"] = "0"                                       // 是否开启闪屏。代号黎明必须关闪屏，否则会有按键冲突，现在所有游戏都关
        map["phone_auth_package"] = "com.tencent.tmgp.wzjhhb.wzjh"  // 开启本机号码一键登录，需关联包名和荣耀签名，不开的话删掉即可 TODO: All Script Rebuild

        PropertiesUtil(File(decompileDir + File.separator + "assets" + File.separator + "ZSmultil")).setProperties(map)
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
        }_${gid}_${pkId}_${appVersion}_rongyao_${time}_${channelAbbr}${loginType}.apk"
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
                val signCommand = "jarsigner -keystore $keyStorePath -storepass $storePassword -keypass $keyPassword -signedjar $filePath $unsignedApk $keyAlias"
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