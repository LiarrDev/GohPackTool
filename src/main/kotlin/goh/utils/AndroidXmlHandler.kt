package goh.utils

import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.*
import java.util.regex.Pattern

object AndroidXmlHandler {

    /**
     * 替换尾标签
     * @param xml 需要修改的 XML 文件
     * @param endTag 尾标签
     * @param replaceWith 替换的内容
     */
    private fun replaceXmlEndTag(xml: File, endTag: String, replaceWith: String) {
        if (!endTag.startsWith("</") && !endTag.endsWith(">")) {
            throw InputMismatchException("$endTag is not an End-Tag")
        }
        xml.writeText(xml.readText().replace(endTag, replaceWith))
    }

    /**
     * 获取 ICON 的文件名
     */
    fun getIconName(decompileDir: String): String {
        val manifest = File(decompileDir, "AndroidManifest.xml").readText()
        val p = Pattern.compile("android:icon=\"(.*?)\"")
        val m = p.matcher(manifest)
        m.find()
        var iconName = m.group(1)
        if (iconName.contains("@drawable/")) {
            iconName = iconName.replace("@drawable/", "")
        } else if (iconName.contains("@mipmap/")) {
            iconName = iconName.replace("@mipmap/", "")
        }
        iconName += ".png"
        println("Icon Name: $iconName")
        return iconName
    }

    /**
     * 移除 android:roundIcon 属性
     */
    fun removeRoundIcon(androidManifest: File) {
        val document = SAXReader().read(androidManifest)
        val application = document.rootElement.element("application")
        val attr = application.attribute("roundIcon") ?: return
        application.remove(attr)
        val writer = XMLWriter(FileWriter(androidManifest))
        writer.write(document)
        writer.close()
        println("移除 android:roundIcon 属性")
    }

    /**
     * 获取 SDK 版本号
     */
    fun getSdkVersion(decompileDir: String): String {
        val file = File(decompileDir + File.separator + "assets" + File.separator + "ZSinfo.xml")
        return SAXReader().read(file).rootElement.element("version").text
    }

    /**
     * 获取主 Activity 设置的屏幕方向
     */
    private fun getMainActivityScreenOrientation(androidManifest: File): String? {
        SAXReader().read(androidManifest)
            .rootElement
            .element("application")
            .elements("activity")
            .forEach { activityNode ->
                activityNode.element("intent-filter")?.apply {
                    elements("action").forEach { actionNode ->
                        if (actionNode.attributeValue("name") == "android.intent.action.MAIN") {    // 存在该 Action 即可认为是主 Activity
                            return activityNode.attributeValue("screenOrientation")
                        }
                    }
                }
            }
        return null
    }

    /**
     * 获取主 Activity 的 android:name
     */
    private fun getMainActivityName(androidManifest: File): String? {
        SAXReader().read(androidManifest)
            .rootElement
            .element("application")
            .elements("activity")
            .forEach { activityNode ->
                activityNode.element("intent-filter")?.apply {
                    elements("action").forEach { actionNode ->
                        if (actionNode.attributeValue("name") == "android.intent.action.MAIN") {    // 存在该 Action 即可认为是主 Activity
                            return activityNode.attributeValue("name")
                        }
                    }
                }
            }
        return null
    }

    /**
     * 为 AndroidManifest 文件增加命名空间
     */
    fun setManifestNameSpace(androidManifest: File) {
        var manifest = androidManifest.readText()
        val toolsNameSpace = "xmlns:tools=\"http://schemas.android.com/tools\""
        if (!manifest.contains(toolsNameSpace)) {
            manifest = manifest.replace("<manifest", "<manifest $toolsNameSpace")
        }
        androidManifest.writeText(manifest)
    }

    /**
     * 移除 SDK 的闪屏页
     * @since V3.2.1.8
     */
    private fun removeSdkSplashActivity(androidManifest: File) {
        println("移除 Splash Activity")
        val actionMain = "<action android:name=\"android.intent.action.MAIN\"/>"
        val categoryLauncher = "<category android:name=\"android.intent.category.LAUNCHER\"/>"
        var manifest = androidManifest.readText()
        manifest = manifest.replace(actionMain, "").replace(categoryLauncher, "")
        androidManifest.writeText(manifest)

        val document = SAXReader().read(androidManifest)
        val applicationElement = document.rootElement.element("application")
        var gameActivity = ""
        applicationElement.elements("meta-data").forEach {
            if (it.attributeValue("name") == "ry.game.activity") {
                gameActivity = it.attributeValue("value")
                println("游戏 Activity：$gameActivity")
            }
        }
        if (gameActivity.isNotBlank()) {
            applicationElement.elements("activity").forEach {
                if (it.attributeValue("name") == gameActivity) {
                    val intentFilter = it.element("intent-filter")
                    if (intentFilter == null) {
                        it.addElement("intent-filter")
                    }
                    it.element("intent-filter")
                        .addElement("action")
                        .addAttribute("android:name", "android.intent.action.MAIN")
                    it.element("intent-filter")
                        .addElement("category")
                        .addAttribute("android:name", "android.intent.category.LAUNCHER")
                }
            }
        }
        val writer = XMLWriter(FileWriter(androidManifest))
        writer.write(document)
        writer.close()
    }

    /**
     * 设置大蓝 VIP SDK 的 App ID
     * @since V3.2.1.8
     */
    fun setVipAppId(androidManifest: File, pkId: String) {
        println("设置 VIP AppId")
        var manifest = androidManifest.readText()
        if (manifest.contains("DL_APPID")) {    // 避免 CP 没有移除
            SAXReader().read(androidManifest).apply {
                rootElement.element("application")
                    .elements("meta-data")
                    .forEach {
                        if (it.attributeValue("name") == "DL_APPID") {
                            it.attribute("value").value = pkId
                        }
                    }
                val writer = XMLWriter(FileWriter(androidManifest))
                writer.write(this)
                writer.close()
            }
        } else {
            val content = """
                    <meta-data
                        android:name="DL_APPID"
                        android:value="$pkId" />
                    <meta-data
                        android:name="DL_PLAT_ID"
                        android:value="4" />
                </application>
            """.trimIndent()
            manifest = manifest.replace("</application>", content)
            androidManifest.writeText(manifest)
        }
    }

    /**
     * 配置大蓝劲飞 VIP SDK 所需的 Res Value 资源
     */
    fun setVipResValue(decompileDir: String) {
        val attrs = """
                <declare-styleable name="RoundedWebView">
                    <attr format="dimension" name="corner_radius"/>
                </declare-styleable>
            </resources>
        """.trimIndent()
        val colors = """
                <color name="dlhm_main_color">#21AAEE</color>
                <color name="dlhm_permission_bg_color">#9926A9ED</color>
                <color name="dlhm_permission_button_color">#FF21AAEE</color>
                <color name="dlhm_progress_bar_center_color">#9925ABEE</color>
                <color name="dlhm_progress_bar_end_color">#FF25ABEE</color>
            </resources>
        """.trimIndent()
        val strings = """
                <string name="dlhm_cancel">取消</string>
                <string name="dlhm_change_icon">更换图片</string>
                <string name="dlhm_choose_photo">图库选取</string>
                <string name="dlhm_device_no_relation_app">该设备没有相关应用</string>
                <string name="dlhm_float_close_hint_text">拖到此处关闭悬浮窗\n摇动手机可显示悬浮窗</string>
                <string name="dlhm_float_open_hint_text">松开关闭悬浮窗\n摇动手机可显示悬浮窗</string>
                <string name="dlhm_no_apk_parser">未能在该设备找到安装包解析器</string>
                <string name="dlhm_no_call_app">未能在该设备找到拨号应用</string>
                <string name="dlhm_take_photo">拍照</string>
                <string name="dlhm_vip_save_album">已截图保存至相册</string>
                <string name="dlhm_webview_content_load_fail">内容加载失败，点击重试</string>
                <string name="dlhm_webview_content_loading">内容加载中...</string>
            </resources>
        """.trimIndent()
        val styles = """
                <style name="sdk_simple_dialog" parent="@android:style/Theme.Holo.Light.Dialog">
                    <item name="android:windowFrame">@android:color/transparent</item>
                    <item name="android:windowIsFloating">true</item>
                    <item name="android:windowIsTranslucent">true</item>
                    <item name="android:windowNoTitle">true</item>
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:backgroundDimEnabled">false</item>
                </style>
            </resources>
        """.trimIndent()
        val valuesDir = File(decompileDir + File.separator + "res" + File.separator + "values")
        val attrsFile = File(valuesDir, "attrs.xml")
        val colorsFile = File(valuesDir, "colors.xml")
        val stringsFile = File(valuesDir, "strings.xml")
        val stylesFile = File(valuesDir, "styles.xml")
        if (attrsFile.exists()) {   // SDK 本身不包含 attrs.xml 文件，因此需要判断是否存在
            attrsFile.writeText(attrsFile.readText().replace("</resources>", attrs))
        } else {
            attrsFile.createNewFile()
            attrsFile.writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$attrs""")
        }
        colorsFile.writeText(colorsFile.readText().replace("</resources>", colors))
        stringsFile.writeText(stringsFile.readText().replace("</resources>", strings))
        stylesFile.writeText(stylesFile.readText().replace("</resources>", styles))
    }

    /**
     * 三方登录需要设置的 AndroidManifest
     */
    fun setThirdPartyLoginManifest(
        decompileDir: String,
        loginType: String,
        qqAppId: String,
        weChatAppId: String,
        packageName: String
    ) {
        val qq = """
            <activity 
                android:name="com.tencent.tauth.AuthActivity"
                android:noHistory="true"
                android:launchMode="singleTask">
				<intent-filter>
				    <action android:name="android.intent.action.VIEW" />
				    <category android:name="android.intent.category.DEFAULT" />
				    <category android:name="android.intent.category.BROWSABLE" />
				    <data android:scheme="tencent$qqAppId" />
                </intent-filter>
			</activity>
			<activity
			    android:name="com.tencent.connect.common.AssistActivity"
			    android:screenOrientation="behind"
			    android:theme="@android:style/Theme.Translucent.NoTitleBar"
			    android:configChanges="orientation|keyboardHidden" />
        """.trimIndent()

        val weChat = """
            <activity
			    android:name="$packageName.wxapi.WXEntryActivity"
				android:theme="@android:style/Theme.Translucent.NoTitleBar"
				android:exported="true"
				android:taskAffinity="com.tencent.tmgp"
				android:launchMode="singleTask" />
        """.trimIndent()

        replaceXmlEndTag(
            File(decompileDir, "AndroidManifest.xml"), "</application>", when (loginType) {
                "1" -> weChat
                "2" -> qq
                "3" -> qq + weChat
                else -> ""
            } + "</application>"
        )
    }

    /**
     * 头条的 AndroidManifest 设置
     */
    fun setBytedanceManifest(decompileDir: String) {
        val content = """
                <activity android:name="com.bytedance.applog.util.SimulateLaunchActivity">
                    <intent-filter>
                        <action android:name="android.intent.action.VIEW" />
                        <category android:name="android.intent.category.BROWSABLE" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <data
                            android:host="rangersapplog"
                            android:path="/picker"
                            android:scheme="rangersapplog.byax6uyt" />
                    </intent-filter>
                </activity>
                <receiver
                    android:name="com.bytedance.applog.collector.Collector"
                    android:enabled="true"
                    android:exported="false" />
            </application>
        """.trimIndent()
        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
    }

    /**
     * 百度 OCPC 的 AndroidManifest 设置
     */
    fun setBaiduOCPCManifest(decompileDir: String, packageName: String) {
        val content = """
                <activity
                    android:name="com.baidu.xenv.XenvActivity"
                    android:configChanges="mcc|mnc|locale|touchscreen|keyboard|keyboardHidden|navigation|orientation|screenLayout|uiMode|screenSize|smallestScreenSize|fontScale"
                    android:excludeFromRecents="true"
                    android:exported="false"
                    android:launchMode="standard"
                    android:theme="@android:style/Theme.Translucent" >
                    <intent-filter>
                        <action android:name="com.baidu.action.Xenv.VIEW" />
                        <category android:name="com.baidu.category.xenv" />
                        <category android:name="android.intent.category.DEFAULT" />
                    </intent-filter>
                </activity>
                <service
                    android:name="com.baidu.xenv.XenvService"
                    android:exported="false" >
                    <intent-filter>
                        <action android:name="com.baidu.action.Xenv.VIEW" />
                        <category android:name="com.baidu.category.xenv" />
                        <category android:name="android.intent.category.DEFAULT" />
                    </intent-filter>
                </service>
                <provider
                    android:name="com.baidu.xenv.XenvProvider"
                    android:authorities="$packageName.xenv.ac.provider"
                    android:exported="false"
                    tools:replace="android:authorities" />
                <meta-data
                    android:name="seckey_avscan"
                    android:value="660346260f8a841a04ec2a56815b421b" />
                <meta-data
                    android:name="appkey_avscan"
                    android:value="100034" />
            </application>
            <permission
                android:name="$packageName.permission.xenv.RECEIVE"
                android:protectionLevel="signatureOrSystem" />
            <uses-permission android:name="$packageName.permission.xenv.RECEIVE" />
        """.trimIndent()
        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
    }

    /**
     * 广点通的 AndroidManifest 设置
     */
    fun setGdtManifest(decompileDir: String, packageName: String) {
        val content = """
                <provider
                    android:name="com.qq.gdt.action.GDTInitProvider"
                    android:authorities="$packageName.GDTInitProvider"
                    android:exported="false">
                </provider>
            </application>
        """.trimIndent()
        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
    }

    /**
     * 应用宝 YSDK 的 AndroidManifest 设置
     */
    fun setYsdkManifest(decompileDir: String, packageName: String, qqAppId: String, wxAppId: String) {
        val file = File(decompileDir, "AndroidManifest.xml")
        removeSdkSplashActivity(file)
        val content = """
                <activity
                    android:name="com.tencent.midas.proxyactivity.APMidasPayProxyActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="${getMainActivityScreenOrientation(file)}"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.tencent.midas.wx.APMidasWXPayActivity"
                    android:exported="true"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.tencent.midas.qq.APMidasQQWalletActivity"
                    android:configChanges="orientation|screenSize|keyboardHidden"
                    android:exported="true"
                    android:launchMode="singleTop"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar">
                    <intent-filter>
                        <action android:name="android.intent.action.VIEW" />
                        <category android:name="android.intent.category.BROWSABLE" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <data android:scheme="qwallet100703379" />
                    </intent-filter>
                </activity>
                <activity
                    android:name="com.tencent.midas.jsbridge.APWebJSBridgeActivity"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar"
                    android:windowSoftInputMode="stateAlwaysHidden" />
                <activity
                    android:name="com.tencent.tauth.AuthActivity"
                    android:launchMode="singleTask"
                    android:noHistory="true">
                    <intent-filter>
                        <action android:name="android.intent.action.VIEW" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <category android:name="android.intent.category.BROWSABLE" />
                        <data android:scheme="tencent$qqAppId" />
                    </intent-filter>
                </activity>
                <activity
                    android:name="com.tencent.connect.common.AssistActivity"
                    android:configChanges="orientation|screenSize|keyboardHidden"
                    android:screenOrientation="portrait"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="$packageName.wxapi.WXEntryActivity"
                    android:excludeFromRecents="true"
                    android:exported="true"
                    android:label="WXEntryActivity"
                    android:launchMode="singleTop"
                    android:taskAffinity="$packageName.diff">
                    <intent-filter>
                        <action android:name="android.intent.action.VIEW" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <data android:scheme="$wxAppId" />
                    </intent-filter>
                </activity>
                <activity
                    android:name="com.tencent.ysdk.module.realName.impl.RegisterRealNameActivity"
                    android:configChanges="orientation|screenSize|keyboardHidden"
                    android:screenOrientation="sensor"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
            </application>
            <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
            <uses-permission android:name="android.permission.RESTART_PACKAGES" />
            <uses-permission android:name="android.permission.READ_SMS" />
            <uses-permission android:name="android.permission.SEND_SMS" />
            <supports-screens
                android:anyDensity="true"
                android:largeScreens="true"
                android:normalScreens="true" />
        """.trimIndent()
        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
        val document = SAXReader().read(file)
        document.rootElement.element("application").elements("activity").forEach { activityNode ->
            activityNode.element("intent-filter")?.apply {
                elements("action").forEach { actionNode ->
                    if (actionNode.attributeValue("name") == "android.intent.action.MAIN") {    // 存在该 Action 即可认为是主 Activity
                        val attribute = activityNode.attribute("launchMode")
                        if (attribute == null) {
                            activityNode.addAttribute("android:launchMode", "singleTop")
                        } else {
                            attribute.text = "singleTop"
                        }
                    }
                }
            }
        }
        val writer = XMLWriter(FileWriter(file))
        writer.write(document)
        writer.close()
    }

    /**
     * 华为联运的 AndroidManifest 设置
     * @param appId agconnect-services.json 文件里的 AppId
     */
    fun setHuaweiManifest(decompileDir: String, packageName: String, appId: String) {
        val content = """
                <provider
                    android:name="com.huawei.agconnect.core.provider.AGConnectInitializeProvider"
                    android:authorities="$packageName.AGCInitializeProvider"
                    android:exported="false" />
                <service
                    android:name="com.huawei.agconnect.core.ServiceDiscovery"
                    android:exported="false" />
                <meta-data
                    android:name="availableLoaded"
                    android:value="yes" />
                <provider
                    android:name="com.huawei.hms.update.provider.UpdateProvider"
                    android:authorities="$packageName.hms.update.provider"
                    android:exported="false"
                    android:grantUriPermissions="true" />
                <provider
                    android:name="com.huawei.hms.device.provider.CheckHmsProvider"
                    android:authorities="$packageName.hms.device.validate.spoofprovider"
                    android:exported="false"
                    android:grantUriPermissions="false" />
                <activity
                    android:name="com.huawei.hms.hwid.internal.ui.activity.HwIdSignInHubActivity"
                    android:configChanges="fontScale|uiMode"
                    android:excludeFromRecents="true"
                    android:exported="false"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.huawei.hms.account.internal.ui.activity.AccountSignInHubActivity"
                    android:excludeFromRecents="true"
                    android:exported="false"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.huawei.hms.activity.BridgeActivity"
                    android:configChanges="orientation|locale|layoutDirection|fontScale|screenSize|smallestScreenSize|screenLayout|uiMode"
                    android:excludeFromRecents="true"
                    android:exported="false"
                    android:hardwareAccelerated="true"
                    android:screenOrientation="behind"
                    android:theme="@style/Base_Translucent" >
                    <meta-data
                        android:name="hwc-theme"
                        android:value="androidhwext:style/Theme.Emui.Translucent" />
                </activity>
                <activity
                    android:name="com.huawei.hms.activity.EnableServiceActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize|smallestScreenSize|screenLayout"
                    android:exported="false" />
                <activity
                    android:name="com.huawei.updatesdk.service.otaupdate.AppUpdateActivity"
                    android:configChanges="orientation|screenSize"
                    android:exported="false"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" >
                    <meta-data
                        android:name="hwc-theme"
                        android:value="androidhwext:style/Theme.Emui.Translucent.NoTitleBar" />
                    <meta-data
                        android:name="hnc-theme"
                        android:value="androidhnext:style/Theme.Magic.Translucent.NoTitleBar" />
                </activity>
                <activity
                    android:name="com.huawei.updatesdk.support.pm.PackageInstallerActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:exported="false"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" >
                    <meta-data
                        android:name="hwc-theme"
                        android:value="androidhwext:style/Theme.Emui.Translucent" />
                    <meta-data
                        android:name="hnc-theme"
                        android:value="androidhnext:style/Theme.Magic.Translucent" />
                </activity>
                <provider
                    android:name="com.huawei.updatesdk.fileprovider.UpdateSdkFileProvider"
                    android:authorities="$packageName.updateSdk.fileProvider"
                    android:exported="false"
                    android:grantUriPermissions="true" />
                <service
                    android:name="com.huawei.agconnect.core.ServiceDiscovery"
                    android:exported="false" >
                    <meta-data
                        android:name="com.huawei.agconnect.credential.CredentialServiceRegistrar:100"
                        android:value="com.huawei.agconnect.core.ServiceRegistrar" />
                </service>
                <service
                    android:name="com.huawei.hms.jos.games.service.GameService"
                    android:exported="true" >
                    <intent-filter>
                        <action android:name="com.huawei.hms.games.service" />
                    </intent-filter>
                </service>
                <provider
                    android:name="com.huawei.hms.jos.games.archive.ArchiveRemoteAccessProvider"
                    android:authorities="$packageName.hmssdk.jos.archive"
                    android:exported="true" />
                <activity
                    android:name="com.huawei.appmarket.component.buoycircle.impl.delegete.BuoyBridgeActivity"
                    android:configChanges="orientation|locale|screenSize|layoutDirection|fontScale"
                    android:excludeFromRecents="true"
                    android:exported="false"
                    android:hardwareAccelerated="true"
                    android:theme="@android:style/Theme.Translucent" >
                    <meta-data
                        android:name="hwc-theme"
                        android:value="androidhwext:style/Theme.Emui.Translucent" />
                </activity>
                <receiver
                    android:name="com.huawei.hms.analytics.receiver.HiAnalyticsSvcEvtReceiver"
                    android:exported="false" >
                    <intent-filter>
                        <action android:name="com.huawei.hms.analytics.pps.event" />
                    </intent-filter>
                </receiver>
                <provider
                    android:name="com.huawei.hms.analytics.provider.AnalyticsInitializeProvider"
                    android:authorities="$packageName.AnalyticsKitInitializeProvider"
                    android:exported="false" />
                <provider
                    android:name="com.huawei.hms.aaid.InitProvider"
                    android:authorities="$packageName.aaidinitprovider"
                    android:exported="false"
                    android:initOrder="500" />
                <meta-data
                    android:name="com.huawei.hms.client.channel.androidMarket"
                    android:value="false" />
                <meta-data
                    android:name="com.huawei.hms.client.service.name:hwid"
                    android:value="hwid:6.3.0.300" />
                <meta-data
                    android:name="com.huawei.hms.client.service.name:base"
                    android:value="base:6.3.0.300" />
                <meta-data
                    android:name="com.huawei.hms.client.service.name:game"
                    android:value="game:6.1.0.301" />
                <meta-data
                    android:name="com.huawei.hms.client.service.name:iap"
                    android:value="iap:6.2.0.300" />
                <meta-data
                    android:name="com.huawei.hms.client.service.name:opendevice"
                    android:value="opendevice:5.1.1.306" />
                <meta-data
                    android:name="com.huawei.hms.client.service.name:hianalytics"
                    android:value="hianalytics:6.3.2.300" />
                <meta-data
                    android:name="com.huawei.hms.min_api_level:hianalytics:hianalytics"
                    android:value="1" />
                <meta-data
                    android:name="com.huawei.hms.min_api_level:opendevice:push"
                    android:value="1" />
                <meta-data
                    android:name="com.huawei.hms.min_api_level:hwid:hwid"
                    android:value="1" />
                <meta-data
                    android:name="com.huawei.hms.min_api_level:hwid:account"
                    android:value="13" />
                <meta-data
                    android:name="com.huawei.hms.min_api_level:base:hmscore"
                    android:value="1" />
                <meta-data
                    android:name="componentverify_ag_cbg_root"
                    android:value="@string/ag_sdk_cbg_root" />
                <meta-data
                    android:name="com.huawei.hms.jos.versioncode"
                    android:value="60100301" />
                <meta-data
                    android:name="com.huawei.hms.client.appid"
                    android:value="appid=$appId" />
            </application>
            <queries>
                <intent>
                    <action android:name="com.apptouch.intent.action.update_hms" />
                </intent>
                <intent>
                    <action android:name="com.huawei.appmarket.intent.action.AppDetail" />
                </intent>
                <intent>
                    <action android:name="com.huawei.hms.core.aidlservice" />
                </intent>
                <intent>
                    <action android:name="com.huawei.hms.core" />
                </intent>
                <package android:name="com.hisilicon.android.hiRMService" />
            </queries>
            <meta-data
                android:name="com.huawei.hms.min_api_level:apptouch:apptouch"
                android:value="1" />
            <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
            <uses-permission android:name="com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE"/>
            <uses-permission android:name="com.huawei.hwid.permission.gameservice.archive.access.provider" />
            <uses-permission android:name="com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA" />
        """.trimIndent()
        val file = File(decompileDir, "AndroidManifest.xml")
        replaceXmlEndTag(file, "</application>", content)
        removeSdkSplashActivity(file)
    }

    /**
     * 华为联运资源配置
     */
    fun setHuaweiResValue(decompileDir: String) {
        val strings = """
                <string name="ag_sdk_cbg_root">MIIFZDCCA0ygAwIBAgIIYsLLTehAXpYwDQYJKoZIhvcNAQELBQAwUDELMAkGA1UE
                BhMCQ04xDzANBgNVBAoMBkh1YXdlaTETMBEGA1UECwwKSHVhd2VpIENCRzEbMBkG
                A1UEAwwSSHVhd2VpIENCRyBSb290IENBMB4XDTE3MDgyMTEwNTYyN1oXDTQyMDgx
                NTEwNTYyN1owUDELMAkGA1UEBhMCQ04xDzANBgNVBAoMBkh1YXdlaTETMBEGA1UE
                CwwKSHVhd2VpIENCRzEbMBkGA1UEAwwSSHVhd2VpIENCRyBSb290IENBMIICIjAN
                BgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA1OyKm3Ig/6eibB7Uz2o93UqGk2M7
                84WdfF8mvffvu218d61G5M3Px54E3kefUTk5Ky1ywHvw7Rp9KDuYv7ktaHkk+yr5
                9Ihseu3a7iM/C6SnMSGt+LfB/Bcob9Abw95EigXQ4yQddX9hbNrin3AwZw8wMjEI
                SYYDo5GuYDL0NbAiYg2Y5GpfYIqRzoi6GqDz+evLrsl20kJeCEPgJZN4Jg00Iq9k
                ++EKOZ5Jc/Zx22ZUgKpdwKABkvzshEgG6WWUPB+gosOiLv++inu/9blDpEzQZhjZ
                9WVHpURHDK1YlCvubVAMhDpnbqNHZ0AxlPletdoyugrH/OLKl5inhMXNj3Re7Hl8
                WsBWLUKp6sXFf0dvSFzqnr2jkhicS+K2IYZnjghC9cOBRO8fnkonh0EBt0evjUIK
                r5ClbCKioBX8JU+d4ldtWOpp2FlxeFTLreDJ5ZBU4//bQpTwYMt7gwMK+MO5Wtok
                Ux3UF98Z6GdUgbl6nBjBe82c7oIQXhHGHPnURQO7DDPgyVnNOnTPIkmiHJh/e3vk
                VhiZNHFCCLTip6GoJVrLxwb9i4q+d0thw4doxVJ5NB9OfDMV64/ybJgpf7m3Ld2y
                E0gsf1prrRlDFDXjlYyqqpf1l9Y0u3ctXo7UpXMgbyDEpUQhq3a7txZQO/17luTD
                oA6Tz1ADavvBwHkCAwEAAaNCMEAwDgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQF
                MAMBAf8wHQYDVR0OBBYEFKrE03lH6G4ja+/wqWwicz16GWmhMA0GCSqGSIb3DQEB
                CwUAA4ICAQC1d3TMB+VHZdGrWJbfaBShFNiCTN/MceSHOpzBn6JumQP4N7mxCOwd
                RSsGKQxV2NPH7LTXWNhUvUw5Sek96FWx/+Oa7jsj3WNAVtmS3zKpCQ5iGb08WIRO
                cFnx3oUQ5rcO8r/lUk7Q2cN0E+rF4xsdQrH9k2cd3kAXZXBjfxfKPJTdPy1XnZR/
                h8H5EwEK5DWjSzK1wKd3G/Fxdm3E23pcr4FZgdYdOlFSiqW2TJ3Qe6lF4GOKOOyd
                WHkpu54ieTsqoYcuMKnKMjT2SLNNgv9Gu5ipaG8Olz6g9C7Htp943lmK/1Vtnhgg
                pL3rDTsFX/+ehk7OtxuNzRMD9lXUtEfok7f8XB0dcL4ZjnEhDmp5QZqC1kMubHQt
                QnTauEiv0YkSGOwJAUZpK1PIff5GgxXYfaHfBC6Op4q02ppl5Q3URl7XIjYLjvs9
                t4S9xPe8tb6416V2fe1dZ62vOXMMKHkZjVihh+IceYpJYHuyfKoYJyahLOQXZykG
                K5iPAEEtq3HPfMVF43RKHOwfhrAH5KwelUA/0EkcR4Gzth1MKEqojdnYNemkkSy7
                aNPPT4LEm5R7sV6vG1CjwbgvQrWCgc4nMb8ngdfnVF7Ydqjqi9SAqUzIk4+Uf0ZY
                +6RY5IcHdCaiPaWIE1xURQ8B0DRUURsQwXdjZhgLN/DKJpCl5aCCxg==</string>
                <string name="c_buoycircle_auto_hide_notice" priority="translator">"拖到此处隐藏"</string>
                <string name="c_buoycircle_cancel" priority="translator">"取消"</string>
                <string name="c_buoycircle_confirm" priority="translator">"知道了"</string>
                <string name="c_buoycircle_floatwindow_click_fail_toast" priority="translator">"请尝试打开“手机管家”或“设置”，开启华为应用市场的 “关联启动” 权限，并重试。"</string>
                <string name="c_buoycircle_hide_guide_btn_cancel" priority="translator">"取消"</string>
                <string name="c_buoycircle_hide_guide_btn_confirm" priority="translator">"隐藏"</string>
                <string name="c_buoycircle_hide_guide_content_nosensor" priority="translator">"浮标隐藏后，重启应用可重新显示浮标。是否隐藏？"</string>
                <string name="c_buoycircle_hide_guide_content_sensor" priority="translator">"浮标隐藏后，翻转设备可重新显示浮标。是否隐藏？"</string>
                <string name="c_buoycircle_hide_guide_noremind" priority="translator">"不再提示"</string>
                <string name="c_buoycircle_hide_guide_title" priority="translator">"隐藏浮标"</string>
                <string name="c_buoycircle_install" priority="translator">"安装"</string>
                <string name="hms_game_achievement_finish_notice" priority="translator">"%1${'$'}s 已解锁"</string>
                <string name="hms_game_check_update_failed_content" priority="translator">"访问失败。该服务需安装应用助手最新版本才可使用。"</string>
                <string name="hms_game_check_update_success_content" priority="translator">"要使用该服务，需安装以下应用的最新版本：\n\n·应用助手(%1${'$'}s MB)"</string>
                <string name="hms_game_login_notice" priority="LT">"欢迎，华为用户 %1${'$'}s"</string>
                <string name="hwid_huawei_login_button_text" priority="translator">华为帐号登录</string>
                <string name="push_cat_body" translatable="false">99A9343CEC0A64112FD2496EF752F719</string>
                <string name="push_cat_head" translatable="false">767499AE5B2DFC9D873AF46032E13B00</string>
                <string name="hms_abort" priority="translator">"终止"</string>
                <string name="hms_abort_message" priority="translator">"是否终止下载？"</string>
                <string name="hms_bindfaildlg_message" priority="LT">"%1${'$'}s无法正常使用 HMS Core。请尝试打开“手机管家”或“设置”，开启%2${'$'}s的所有权限 (“自启动”、“关联启动”等)，并重试。"</string>
                <string name="hms_cancel" priority="translator">"取消"</string>
                <string name="hms_cancel_after_cancel" priority="translator">"取消安装"</string>
                <string name="hms_cancel_install_message" priority="translator">"取消安装可能无法正常使用该应用。"</string>
                <string name="hms_check_failure" priority="translator">"检查 HMS Core 更新失败。"</string>
                <string name="hms_checking" priority="LT">"正在检测新版本..."</string>
                <string name="hms_confirm" priority="translator">"知道了"</string>
                <string name="hms_download_failure" priority="translator">"下载 HMS Core 安装包失败。"</string>
                <string name="hms_download_no_space" priority="translator">"剩余空间不足，无法下载。"</string>
                <string name="hms_download_retry" priority="translator">"下载 HMS Core 失败。是否重试？"</string>
                <string name="hms_downloading_loading" priority="LT">"正在加载"</string>
                <string name="hms_install" priority="translator">"安装"</string>
                <string name="hms_install_after_cancel" priority="translator">"立即安装"</string>
                <string name="hms_install_message" priority="translator">"您尚未安装 HMS Core，安装之后才可用此功能。是否安装？"</string>
                <string name="hms_is_spoof" priority="translator">"本设备安装的 (%1${'$'}s) 为非法 HMS Core 版本。请先进入系统设置的应用管理中卸载该版本，然后回到本应用安装合法的 HMS Core 版本。"</string>
                <string name="hms_retry" priority="translator">"重试"</string>
                <string name="hms_spoof_hints" priority="translator">"提示"</string>
                <string name="hms_update" priority="translator">"更新"</string>
                <string name="hms_update_continue" priority="translator">"继续下载"</string>
                <string name="hms_update_message" priority="translator">"HMS Core 版本太旧，更新到最新版本才可用此功能。是否更新？"</string>
                <string name="hms_update_message_new" priority="LT">"该服务需安装以下应用的最新版本才能使用：\n\n·%1${'$'}s"</string>
                <string name="hms_update_nettype" priority="translator">"当前为非 Wi-Fi 网络。是否继续下载？"</string>
                <string name="hms_update_title" priority="translator">"HMS Core"</string>
                <string name="upsdk_app_download_info_new" priority="LT">"安装"</string>
                <string name="upsdk_app_download_installing" priority="translator">"正在下载 %s"</string>
                <string name="upsdk_app_size" priority="LT">"大小"</string>
                <string name="upsdk_app_version" priority="LT">"版本"</string>
                <string name="upsdk_appstore_install" priority="translator">"需要使用 %s 才能升级。是否现在安装？"</string>
                <string name="upsdk_cancel" priority="LT">"取消"</string>
                <string name="upsdk_checking_update_prompt" priority="LT">"正在检查更新..."</string>
                <string name="upsdk_choice_update" priority="LT">"有可用更新，快去升级吧。"</string>
                <string name="upsdk_detail" priority="LT">"详情"</string>
                <string name="upsdk_getting_message_fail_prompt_toast" priority="translator">"无法获取信息，请稍后重试"</string>
                <string name="upsdk_mobile_dld_warn" priority="translator">"当前使用移动数据网络，立即安装将消耗 %1${'$'}s 数据流量。"</string>
                <string name="upsdk_no_available_network_prompt_toast" priority="translator">"网络未连接，请检查网络设置"</string>
                <string name="upsdk_ota_app_name" priority="translator">"应用"</string>
                <string name="upsdk_ota_cancel" priority="LT">"以后再说"</string>
                <string name="upsdk_ota_force_cancel_new" priority="translator">"退出应用"</string>
                <string name="upsdk_ota_notify_updatebtn" priority="LT">"立即更新"</string>
                <string name="upsdk_ota_title" priority="LT">"发现新版本"</string>
                <string name="upsdk_storage_utils" priority="translator">"%1${'$'}s MB"</string>
                <string name="upsdk_third_app_dl_cancel_download_prompt_ex" priority="translator">"是否取消安装？"</string>
                <string name="upsdk_third_app_dl_install_failed" priority="translator">"安装失败"</string>
                <string name="upsdk_third_app_dl_sure_cancel_download" priority="translator">"确认"</string>
                <string name="upsdk_update_check_no_new_version" priority="LT">"已是最新版本"</string>
            </resources>
        """.trimIndent()
        val colors = """
                <color name="hwid_auth_button_color_black">#000000</color>
                <color name="hwid_auth_button_color_border">#CCCCCC</color>
                <color name="hwid_auth_button_color_gray">#F2F2F2</color>
                <color name="hwid_auth_button_color_red">#EF484B</color>
                <color name="hwid_auth_button_color_text_black">#000000</color>
                <color name="hwid_auth_button_color_text_white">#FFFFFF</color>
                <color name="hwid_auth_button_color_white">#FFFFFF</color>
                <color name="upsdk_color_gray_1">#F2F2F2</color>
                <color name="upsdk_color_gray_10">#191919</color>
                <color name="upsdk_color_gray_7">#808080</color>
            </resources>
        """.trimIndent()
        val attrs = """
                <declare-styleable name="HuaweiIdAuthButton">
                    <attr format="enum" name="hwid_color_policy">
                        <enum name="hwid_color_policy_red" value="0"/>
                        <enum name="hwid_color_policy_white" value="1"/>
                        <enum name="hwid_color_policy_white_with_border" value="2"/>
                        <enum name="hwid_color_policy_black" value="3"/>
                        <enum name="hwid_color_policy_gray" value="4"/>
                    </attr>
                    <attr format="enum" name="hwid_button_theme">
                        <enum name="hwid_button_theme_no_title" value="0"/>
                        <enum name="hwid_button_theme_full_title" value="1"/>
                    </attr>
                    <attr format="dimension" name="hwid_corner_radius">
                        <enum name="hwid_corner_radius_large" value="-1"/>
                        <enum name="hwid_corner_radius_medium" value="-2"/>
                        <enum name="hwid_corner_radius_small" value="-3"/>
                    </attr>
                </declare-styleable>
            </resources>
        """.trimIndent()
        val styles = """
                <style name="Base_Translucent" parent="@android:style/Theme.Translucent">
                    <item name="android:windowNoTitle">true</item>
                </style>
            </resources>
        """.trimIndent()
        val dimens = """
                <dimen name="upsdk_margin_l">16dp</dimen>
                <dimen name="upsdk_margin_m">8dp</dimen>
                <dimen name="upsdk_margin_xs">2dp</dimen>
                <dimen name="upsdk_master_body_2">13sp</dimen>
                <dimen name="upsdk_master_subtitle">15sp</dimen>
            </resources>
        """.trimIndent()
        val resValuesFolder = File(decompileDir + File.separator + "res" + File.separator + "values")
        val stringsFile = File(resValuesFolder, "strings.xml")
        val stylesFile = File(resValuesFolder, "styles.xml")
        val colorsFile = File(resValuesFolder, "colors.xml")
        val attrsFile = File(resValuesFolder, "attrs.xml")
        val dimensFile = File(resValuesFolder, "dimens.xml")
        attrsFile.apply {       // SDK 本身不包含 attrs.xml 文件，因此需要判断是否存在
            if (exists()) {
                writeText(readText().replace("</resources>", attrs))
            } else {
                createNewFile()
                writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$attrs""")
            }
        }
        colorsFile.writeText(colorsFile.readText().replace("</resources>", colors))
        stringsFile.writeText(stringsFile.readText().replace("</resources>", strings))
        stylesFile.writeText(stylesFile.readText().replace("</resources>", styles))
        dimensFile.writeText(dimensFile.readText().replace("</resources>", dimens))
    }

    /**
     * 小米联运的 AndroidManifest 设置
     */
    fun setMiManifest(decompileDir: String, packageName: String) {
        val content = """
                <meta-data
                    android:name="MiLinkGroupAppID"
                    android:value="@integer/MiLinkGroupAppID" />
                <activity
                    android:name="com.xiaomi.gamecenter.sdk.ui.MiActivity"
                    android:configChanges="orientation|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.xiaomi.gamecenter.sdk.ui.PayListActivity"
                    android:configChanges="orientation|screenSize"
                    android:exported="true"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.xiaomi.hy.dj.HyDjActivity"
                    android:configChanges="orientation|screenSize"
                    android:exported="true"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.alipay.sdk.app.H5PayActivity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false"
                    android:screenOrientation="behind"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <service
                    android:name="com.xiaomi.gamecenter.push.GamePushService"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="$packageName.MI_GAME_PUSH" />
                    </intent-filter>
                </service>
                <receiver
                    android:name="com.xiaomi.gamecenter.push.OnClickReceiver"
                    android:exported="true">
                    <intent-filter>
                        <action android:name="com.xiaomi.hy.push.client.ONCLICK" />
                    </intent-filter>
                </receiver>
                <provider
                    android:name="com.xiaomi.gamecenter.sdk.utils.MiFileProvider"
                    android:authorities="$packageName.mi_fileprovider"
                    android:exported="false"
                    android:grantUriPermissions="true">
                    <meta-data
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/mio_file_paths" />
                </provider>
                <activity
                    android:name="com.xiaomi.gamecenter.sdk.ui.fault.ViewFaultNoticeActivity"
                    android:configChanges="orientation|screenSize"
                    android:excludeFromRecents="true"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.xiaomi.gamecenter.sdk.ui.notice.NoticeActivity"
                    android:configChanges="orientation|screenSize"
                    android:excludeFromRecents="true"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.xiaomi.gamecenter.sdk.anti.ui.MiAntiAlertActivity"
                    android:configChanges="orientation|screenSize"
                    android:excludeFromRecents="true"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar">
                    <intent-filter>
                        <data
                            android:host="open_anti_alert"
                            android:scheme="mioauthsdk" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <action android:name="android.intent.action.VIEW" />
                    </intent-filter>
                </activity>
                <activity
                    android:name="com.xiaomi.gamecenter.sdk.ui.MiPayAntiActivity"
                    android:configChanges="orientation|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity
                    android:name="com.xiaomi.gamecenter.sdk.ui.MiVerifyActivity"
                    android:configChanges="orientation|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
            </application>
            <uses-permission android:name="android.permission.WAKE_LOCK" />
            <uses-permission android:name="com.xiaomi.sdk.permission.PAYMENT" />
            <uses-permission android:name="com.xiaomi.permission.AUTH_SERVICE" />
            <uses-permission android:name="android.permission.GET_ACCOUNTS" />
            <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
        """.trimIndent()
        val file = File(decompileDir, "AndroidManifest.xml")
        replaceXmlEndTag(file, "</application>", content)
//        removeSdkSplashActivity(file)
    }

    /**
     * ViVO 联运的 AndroidManifest 设置
     */
    fun setVivoManifest(decompileDir: String, appId: String) {
        // SDK 内已内置了 REQUEST_INSTALL_PACKAGES 权限，但是有些 CP 没有处理，以防万一这里加上
        val content = """
                <meta-data
                    android:name="vivo_app_id"
                    android:value="$appId" />
                <meta-data
                    android:name="vivo_union_sdk"
                    android:value="4.7.2.0" />
                <activity
                    android:name="com.vivo.unionsdk.ui.UnionActivity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false"
                    android:theme="@android:style/Theme.Dialog">
                    <intent-filter>
                        <action android:name="android.intent.action.VIEW" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <category android:name="android.intent.category.BROWSABLE" />
                        <data
                            android:host="union.vivo.com"
                            android:path="/openjump"
                            android:scheme="vivounion" />
                    </intent-filter>
                </activity>
            </application>
            <uses-permission android:name="vivo.game.permission.OPEN_JUMP_INTENTS" />
            <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
        """.trimIndent()
        val file = File(decompileDir, "AndroidManifest.xml")
        replaceXmlEndTag(file, "</application>", content)
//        removeSdkSplashActivity(file)
    }

    /**
     * OPPO 联运的 AndroidManifest 设置
     */
    fun setOppoManifest(decompileDir: String, packageName: String, appKey: String, appSecret: String) {
        val content = """
                <service
                    android:name="com.nearme.game.sdk.common.serice.OutPutFileService"
                    android:enabled="true"
                    android:exported="true" >
                    <intent-filter>
                        <action android:name="gusdk.children.discern.data" />
                    </intent-filter>
                </service>
                <activity
                    android:name="com.nearme.game.sdk.component.proxy.JumpToProxyActivity"
                    android:configChanges="keyboardHidden|orientation|screenSize"
                    android:exported="true"
                    android:process=":gcsdk"
                    android:theme="@style/Theme_Dialog_Custom" />
                <provider
                    android:name="com.heytap.msp.sdk.MspFileProvider"
                    android:authorities="$packageName.MspFileProvider"
                    android:exported="false"
                    android:grantUriPermissions="true"
                    android:process=":gcsdk">
                    <meta-data
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/provider_paths" />
                </provider>
                <provider
                    android:name="com.nearme.platform.opensdk.pay.NearMeFileProvider"
                    android:authorities="$packageName.fileProvider"
                    android:exported="false"
                    android:grantUriPermissions="true"
                    android:process=":gcsdk">
                    <meta-data
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/file_paths" />
                </provider>
                <activity
                    android:name="com.nearme.game.sdk.component.proxy.ProxyActivity"
                    android:configChanges="keyboardHidden|orientation|screenSize"
                    android:exported="false"
                    android:process=":gcsdk"
                    android:theme="@style/Theme_Dialog_Custom" />
                <activity
                    android:name="com.oppo.usercenter.opensdk.dialog.register.UserCenterOperateActivity"
                    android:configChanges="keyboardHidden|orientation|screenSize"
                    android:exported="false"
                    android:process=":gcsdk"
                    android:theme="@style/Theme_Dialog_Custom" />
                <activity
                    android:name="com.nearme.game.sdk.component.proxy.ExitActivity"
                    android:configChanges="keyboardHidden|orientation|screenSize"
                    android:exported="false"
                    android:launchMode="singleTask"
                    android:process=":gcsdk"
                    android:theme="@style/Theme_Dialog_Custom" />
                <service
                    android:name="com.nearme.game.sdk.component.proxy.ProxyApiService"
                    android:priority="1000"
                    android:process=":gcsdk" />
                <receiver
                    android:name="com.nearme.game.sdk.component.proxy.ProxyUserCenterOperateReceiver"
                    android:exported="true"
                    android:process=":gcsdk" >
                    <intent-filter>
                        <action android:name="com.oppo.usercenter.account_login" />
                        <action android:name="com.oppo.usercenter.account_logout" />
                        <action android:name="com.oppo.usercenter.modify_name" />
                        <action android:name="com.usercenter.action.receiver.account_login" />
                        <action android:name="com.heytap.usercenter.account_logout" />
                    </intent-filter>
                </receiver>
                <meta-data
                    android:name="debug_mode"
                    android:value="false" />
                <meta-data
                    android:name="is_offline_game"
                    android:value="false" />
                <meta-data
                    android:name="app_key"
                    android:value="$appKey" />
                <meta-data
                    android:name="app_secret"
                    android:value="$appSecret" />
            </application>
            <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
            <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
            <uses-permission android:name="android.permission.USE_CREDENTIALS" />
            <uses-permission android:name="android.permission.GET_ACCOUNTS" />
            <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
        """.trimIndent()
        val file = File(decompileDir, "AndroidManifest.xml")
        replaceXmlEndTag(file, "</application>", content)
        removeSdkSplashActivity(file)
    }

    /**
     * OPPO 联运的 Style 设置
     */
    fun setOppoStyle(decompileDir: String) {
        val content = """
                <style name="Theme_Dialog_Custom">
                    <item name="android:windowFrame">@null</item>
                    <item name="android:windowFullscreen">true</item>
                    <item name="android:windowNoTitle">true</item>
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:windowIsFloating">true</item>
                    <item name="android:windowContentOverlay">@null</item>
                </style>
            </resources>
        """.trimIndent()
        val file = File(
            decompileDir + File.separator +
                    "res" + File.separator +
                    "values" + File.separator +
                    "styles.xml"
        )
        replaceXmlEndTag(file, "</resources>", content)
    }

    /**
     * 大蓝联运的 AndroidManifest 设置
     */
    fun setDalanManifest(
        decompileDir: String,
        channelAppId: String,
        channelAppKey: String,
        channelGameId: String,
        channelChannelId: String,
        channelGameChannelId: String
    ) {
        val file = File(decompileDir, "AndroidManifest.xml")
        removeSdkSplashActivity(file)
        val content = """
                <activity
                    android:name="com.dalan.dl_assembly.SplashScreenActivity"
                    android:configChanges="fontScale|orientation|keyboardHidden|locale|navigation|screenSize|uiMode"
                    android:launchMode="standard"
                    android:screenOrientation="${getMainActivityScreenOrientation(file)}"
                    android:stateNotNeeded="true"
                    android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
                    <meta-data
                        android:name="dalan_union_main_activity"
                        android:value="${getMainActivityName(file)}" />
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity>
                <activity
                    android:name="com.alipay.sdk.app.H5PayActivity"
                    android:configChanges="orientation|keyboardHidden|navigation"
                    android:exported="false"
                    android:screenOrientation="behind" />
                <activity
                    android:name="com.alipay.sdk.auth.AuthActivity"
                    android:configChanges="orientation|keyboardHidden|navigation"
                    android:exported="false"
                    android:screenOrientation="behind" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.LoginActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:launchMode="singleTask"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.ProtocolActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.BindTelActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.SetPwdActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.SmsLoginActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.ExitActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.ExitPushActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.AutoActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.LoginSucActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.pay.PayActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.GameNoticeActivity01"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.activity.AccountScreenActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.dalan_template.usercenter.ui.UserCenterActivity"
                    android:configChanges="keyboardHidden|orientation|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen"
                    android:windowSoftInputMode="adjustResize" />
                <activity
                    android:name="com.dalan_template.darkhorse_ui.pay.NativePayActivity"
                    android:configChanges="orientation|keyboardHidden|screenSize"
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" />
                <service android:name="com.dalan_template.darkhorse_ui.service.AnalysisService" />
                <service android:name="com.dalan_template.usercenter.widget.FloatWindowService" />
                <meta-data
                    android:name="DL_CHANNEL_ID"
                    android:value="$channelChannelId" />
                <meta-data
                    android:name="DL_GAME_CHANNEL_ID"
                    android:value="$channelGameChannelId" />
                <meta-data
                    android:name="DL_UNION_DEBUG"
                    android:value="true" />
                <meta-data
                    android:name="DL_GAME_ID"
                    android:value="$channelGameId" />
                <meta-data
                    android:name="DL_APPID"
                    android:value="$channelAppId" />
                <meta-data
                    android:name="DL_APPKEY"
                    android:value="$channelAppKey" />
                <meta-data
                    android:name="DALAN Channel"
                    android:value="111" />
            </application>
            <uses-permission android:name="android.permission.CAMERA" />
            <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />
            <uses-permission android:name="android.permission.RECORD_AUDIO" />
            <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
            <uses-permission android:name="org.simalliance.openmobileapi.SMARTCARD" />
        """.trimIndent()

        // 删除原主 Activity 的 LAUNCHER 以及 android:requestLegacyExternalStorage
        file.writeText(
            file.readText()
                .replace("android:requestLegacyExternalStorage=\"true\"", "")
                .replace("<category android:name=\"android.intent.category.LAUNCHER\"/>", "")
        )

        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
    }

    /**
     * 字节联运的 AndroidManifest 设置
     */
    fun setGbSdkManifest(decompileDir: String, packageName: String) {
        val content = """
                <provider
                    android:name="com.just.agentweb.AgentWebFileProvider"
                    android:authorities="$packageName.AgentWebFileProvider"
                    android:exported="false"
                    android:grantUriPermissions="true" >
                    <meta-data
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/web_files_public" />
                </provider>
                <activity
                    android:name="com.just.agentweb.ActionActivity"
                    android:configChanges="keyboardHidden|orientation|screenSize"
                    android:exported="false"
                    android:launchMode="standard"
                    android:theme="@style/actionActivity"
                    android:windowSoftInputMode="stateHidden|stateAlwaysHidden" >
                </activity>
                <service 
                    android:name="com.volcengine.onekit.component.ComponentDiscoveryService"
                    android:exported="false">
                    <meta-data 
                        android:name="com.bytedance.applog.onekit.AnalyticsComponentRegistrar"
                        android:value="com.volcengine.onekit.component.ComponentRegistrar" />
                    <meta-data 
                        android:name="com.bytedance.applog.onekit.DeviceComponentRegistrar"
                        android:value="com.volcengine.onekit.component.ComponentRegistrar" />
                </service>
                <activity android:name="com.bytedance.applog.migrate.MigrateDetectorActivity" />
                <receiver 
                    android:name="com.bytedance.applog.collector.Collector" 
                    android:enabled="true"
                    android:exported="false" />
                <provider 
                    android:name="com.bytedance.applog.readapk.ApkChannelFileProvider"
                    android:authorities="$packageName.ApkChannelFileProvider" 
                    android:exported="false"
                    android:initOrder="999999" />
                <meta-data 
                    android:name="com.bytedance.ttgame.tob.artifact_version.gbsdk_optional_applog"
                    android:value="2.1.0.2" />
                <activity 
                    android:name="com.bytedance.sdk.open.douyin.ui.DouYinWebAuthorizeActivity"
                    android:configChanges="orientation|screenSize" 
                    android:screenOrientation="portrait"
                    android:theme="@android:style/Theme.NoTitleBar.Fullscreen" />
                <activity
                    android:name="com.bytedance.ttgame.tob.optional.aweme.impl.activity.DouYinEntryActivity"
                    android:configChanges="orientation|screenSize|screenLayout" 
                    android:exported="true" />
                <meta-data 
                    android:name="com.bytedance.ttgame.tob.artifact_version.gbsdk_optional_aweme"
                    android:value="2.1.0.2" />
                <provider 
                    android:name="com.bytedance.ttgame.library.luffy.provider.LuffyProvider"
                    android:authorities="$packageName.library.luffy.provider" 
                    android:enabled="true"
                    android:exported="true" />
                <provider 
                    android:name="com.volcengine.zeus.servermanager.MainServerManager"
                    android:authorities="$packageName.zeus.servermanager.main"
                    android:exported="false" />
                <provider 
                    android:name="com.volcengine.zeus.provider.MainProcessProviderProxy"
                    android:authorities="$packageName.zeus.provider.proxy.main"
                    android:exported="false" />
                <activity android:name="com.apm.applog.migrate.MigrateDetectorActivity" />
                <activity
                    android:name="com.ss.android.downloadlib.addownload.compliance.AppPrivacyPolicyActivity" />
                <activity
                    android:name="com.ss.android.downloadlib.addownload.compliance.AppDetailInfoActivity" />
                <activity 
                    android:name="com.ss.android.downloadlib.activity.TTDelegateActivity"
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <activity 
                    android:name="com.ss.android.downloadlib.activity.JumpKllkActivity"
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent.NoTitleBar" />
                <receiver android:name="com.ss.android.downloadlib.core.download.DownloadReceiver" />
                <service android:name="com.ss.android.socialbase.appdownloader.DownloadHandlerService" />
                <activity
                    android:name="com.ss.android.socialbase.appdownloader.view.DownloadTaskDeleteActivity"
                    android:launchMode="singleTask" android:theme="@android:style/Theme.Dialog" />
                <activity
                    android:name="com.ss.android.socialbase.appdownloader.view.JumpUnknownSourceActivity"
                    android:launchMode="singleTask" android:theme="@android:style/Theme.Dialog" />
                <service 
                    android:name="com.ss.android.socialbase.appdownloader.RetryJobSchedulerService"
                    android:enabled="true" android:exported="false"
                    android:permission="android.permission.BIND_JOB_SERVICE" />
                <service
                    android:name="com.ss.android.socialbase.downloader.downloader.IndependentProcessDownloadService"
                    android:exported="false" android:process=":downloader">
                    <intent-filter>
                        <action android:name="com.ss.android.socialbase.downloader.remote" />
                    </intent-filter>
                </service>
                <service
                    android:name="com.ss.android.socialbase.downloader.notification.DownloadNotificationService" />
                <service android:name="com.ss.android.socialbase.downloader.downloader.DownloadService" />
                <service android:name="com.ss.android.socialbase.downloader.impls.DownloadHandleService" />
                <service
                    android:name="com.ss.android.socialbase.downloader.downloader.SqlDownloadCacheService" />
                <provider android:name="com.bytedance.frankie.provider.FrankieProvider"
                    android:authorities="$packageName.frankie" android:exported="false" />
                <receiver android:name="com.bytedance.ttnet.hostmonitor.ConnectivityReceiver">
                    <intent-filter>
                        <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
                    </intent-filter>
                </receiver>
                <service android:name="com.bytedance.ttnet.hostmonitor.HostMonitor" />
                <activity 
                    android:name="com.bytedance.ttnet.debug.TTnetDebugActivity"
                    android:configChanges="keyboardHidden|orientation">
                    <intent-filter>
                        <action android:name="android.intent.action.VIEW" />
                        <category android:name="android.intent.category.DEFAULT" />
                        <category android:name="android.intent.category.BROWSABLE" />
                        <data android:host="page" android:scheme="ttnet_debug" />
                    </intent-filter>
                </activity>
                <activity
                    android:name="com.bytedance.ttgame.tob.common.host.base.impl.debug.DebugInfoActivity"
                    android:configChanges="orientation|screenSize" 
                    android:exported="true" />
                <provider
                    android:name="com.bytedance.ttgame.tob.common.host.base.impl.applog.ApkChannelFileProvider"
                    android:authorities="$packageName.ApkChannelFileProvider" 
                    android:exported="false"
                    android:initOrder="999999" />
                <service
                    android:name="com.bytedance.ttgame.tob.common.host.base.impl.applog.ApkChannelService"
                    android:exported="false" android:process=":apkchannel" />
                <activity android:name="com.bytedance.ttgame.tob.common.host.api.multidex.LoadDexActivity"
                    android:alwaysRetainTaskState="false" 
                    android:excludeFromRecents="true"
                    android:launchMode="singleTask" 
                    android:process=":nodex"
                    android:theme="@style/PreLoadStyle" />
                <activity
                    android:name="com.bytedance.ttgame.tob.common.host.main.plugin.update.UpdateNoticeActivity"
                    android:configChanges="orientation|screenSize" 
                    android:launchMode="singleTop"
                    android:theme="@style/Theme.ActivityDialogStyle"
                    android:windowSoftInputMode="adjustPan" />
                <activity
                    android:name="com.bytedance.ttgame.tob.common.host.main.debug.DebugModeActivity" />
                <activity android:name="com.bytedance.ttgame.tob.common.host.main.debug.InterceptorActivity"
                    android:theme="@style/Theme.AppCompat.Dialog" />
                <provider 
                    android:name="com.bytedance.ttgame.tob.common.host.main.GBFileProvider"
                    android:authorities="$packageName.fileprovider" 
                    android:exported="false"
                    android:grantUriPermissions="true">
                    <meta-data 
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/gbsdk_file_paths" />
                </provider>
                <meta-data 
                    android:name="com.bytedance.ttgame.tob.artifact_version.gbsdk_common_host"
                    android:value="2.1.0.2" />
                <provider
                    android:name="com.bytedance.ttgame.tob.packer.common.StubServerManager${'$'}PushServerManager"
                    android:authorities="$packageName.zeus.servermanager.push.com.bytedance.ttgame.tob.common"
                    android:exported="false" android:process=":push" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.common.StubActivity${'$'}AppCompat"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.common.StubActivity${'$'}AppCompat_T"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.common.StubActivity${'$'}Activity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.common.StubActivity${'$'}Activity_T"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.debug.StubActivity${'$'}AppCompat"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.debug.StubActivity${'$'}AppCompat_T"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.debug.StubActivity${'$'}Activity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.debug.StubActivity${'$'}Activity_T"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.debug.StubActivity${'$'}AppCompat_Portrait"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:screenOrientation="portrait"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <meta-data 
                    android:name="com.bytedance.ttgame.tob.artifact_version.gbsdk_common_plugin"
                    android:value="2.1.0.2" />
                <meta-data 
                    android:name="ZEUS_PLUGIN_GBSDK_COMMON"
                    android:value="{apiVersionCode:21002,packageName:gbsdk.plugin.common,signature:'MIIDYTCCAkmgAwIBAgIESHTxpjANBgkqhkiG9w0BAQsFADBgMQ4wDAYDVQQGEwV1bmlvbjEOMAwGA1UECBMFdW5pb24xDjAMBgNVBAcTBXVuaW9uMQ4wDAYDVQQKEwV1bmlvbjEOMAwGA1UECxMFdW5pb24xDjAMBgNVBAMTBXVuaW9uMCAXDTIwMDUxMTEwNDcxM1oYDzIwNTgwOTA5MTA0NzEzWjBgMQ4wDAYDVQQGEwV1bmlvbjEOMAwGA1UECBMFdW5pb24xDjAMBgNVBAcTBXVuaW9uMQ4wDAYDVQQKEwV1bmlvbjEOMAwGA1UECxMFdW5pb24xDjAMBgNVBAMTBXVuaW9uMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2Sf6mz43R1wUe/dbzyrDglKyYPkgfk9b0rvxKoxu5n4P4t8bisJ3j39XLhIaboacwMMbJ/MS91FjZUCpElJuaBioCStZGvXs8MdJAuIP3TfbJ94XW8cw8eH6LcmEHQXSHYRvQ70y4QXZktHg1YRwRKlQ1hTZ7neUsbeJRONHqYWLW1GddDXw1KcvbPyvUZTrYWhNVE56m/vis+f5ZgqkeacmoPdrn4xH6sdw4o31jF9ch9f+SfScr17rkRzfGkC5QkrGyApsi2nxWOikOY89NZN/N82RWEKGuw8UKUdyyANKKSwhUS9e1NHwdKjAdAhE/h3hqpjAAZujtxKc2Ys0yQIDAQABoyEwHzAdBgNVHQ4EFgQUNVmaHEewLM/uA8DFQZXj/LWIrNswDQYJKoZIhvcNAQELBQADggEBADsWyf2UUPUYPWdGPqFgzwVCvtgm/EfCtMIIuX4yXYljqa0lqlfl9NQ5QeO9vltbOYO5wgcKrJLLz7fOwvLy0ThwRFSzXXhONqdtya6xNkV5iB+UzLUKl7bAvWSd/l/oST0+uCXu52YIGWcUQhIp2kkJFtezSpBZPhoDCObzcwycQz16dLwMpH1eEugH5QpVQNPvJjHBnleJzmsieJroi2KP2Qt5bLQLZFf5QNKvNKKq5ZlJRSCxxx6SM7ots4yCSizoPEYBXifZ7HUG+bigiT9nb0NJnAP/E9JGayWjf8ntRXVrVsfY80Gai+R1c0CwGJRsXKFAsXBtqWgkIYgM1d4=',appKey:7296581568dbd58b41bf9aba0fc25850,appSecretKey:4d3f2fe71336e07e1ef1a366c60d6e3b,internalPath:gbsdk.plugin.common.apk,internalVersionCode:21002,minPluginVersion:21002}" />
                <meta-data 
                    android:name="ZEUS_PLUGIN_GBSDK_DEBUG"
                    android:value="{apiVersionCode:21002,packageName:gbsdk.plugin.debug,signature:'MIIDYTCCAkmgAwIBAgIESHTxpjANBgkqhkiG9w0BAQsFADBgMQ4wDAYDVQQGEwV1bmlvbjEOMAwGA1UECBMFdW5pb24xDjAMBgNVBAcTBXVuaW9uMQ4wDAYDVQQKEwV1bmlvbjEOMAwGA1UECxMFdW5pb24xDjAMBgNVBAMTBXVuaW9uMCAXDTIwMDUxMTEwNDcxM1oYDzIwNTgwOTA5MTA0NzEzWjBgMQ4wDAYDVQQGEwV1bmlvbjEOMAwGA1UECBMFdW5pb24xDjAMBgNVBAcTBXVuaW9uMQ4wDAYDVQQKEwV1bmlvbjEOMAwGA1UECxMFdW5pb24xDjAMBgNVBAMTBXVuaW9uMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2Sf6mz43R1wUe/dbzyrDglKyYPkgfk9b0rvxKoxu5n4P4t8bisJ3j39XLhIaboacwMMbJ/MS91FjZUCpElJuaBioCStZGvXs8MdJAuIP3TfbJ94XW8cw8eH6LcmEHQXSHYRvQ70y4QXZktHg1YRwRKlQ1hTZ7neUsbeJRONHqYWLW1GddDXw1KcvbPyvUZTrYWhNVE56m/vis+f5ZgqkeacmoPdrn4xH6sdw4o31jF9ch9f+SfScr17rkRzfGkC5QkrGyApsi2nxWOikOY89NZN/N82RWEKGuw8UKUdyyANKKSwhUS9e1NHwdKjAdAhE/h3hqpjAAZujtxKc2Ys0yQIDAQABoyEwHzAdBgNVHQ4EFgQUNVmaHEewLM/uA8DFQZXj/LWIrNswDQYJKoZIhvcNAQELBQADggEBADsWyf2UUPUYPWdGPqFgzwVCvtgm/EfCtMIIuX4yXYljqa0lqlfl9NQ5QeO9vltbOYO5wgcKrJLLz7fOwvLy0ThwRFSzXXhONqdtya6xNkV5iB+UzLUKl7bAvWSd/l/oST0+uCXu52YIGWcUQhIp2kkJFtezSpBZPhoDCObzcwycQz16dLwMpH1eEugH5QpVQNPvJjHBnleJzmsieJroi2KP2Qt5bLQLZFf5QNKvNKKq5ZlJRSCxxx6SM7ots4yCSizoPEYBXifZ7HUG+bigiT9nb0NJnAP/E9JGayWjf8ntRXVrVsfY80Gai+R1c0CwGJRsXKFAsXBtqWgkIYgM1d4=',appKey:c45e51250ab7c3171b8e5682060b849b,appSecretKey:6af27e8b1fd5e83cc4da31866ec5b2e8,internalPath:gbsdk.plugin.debug.apk,internalVersionCode:21002}" />
                <provider
                    android:name="android.arch.lifecycle.ProcessLifecycleOwnerInitializer"
                    android:authorities="$packageName.lifecycle-trojan"
                    android:exported="false"
                    android:multiprocess="true" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.optional.union.privacy.ui.PrivacyActivity"
                    android:configChanges="orientation|screenSize"
                    android:theme="@style/Theme.ActivityDialogStyle" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTop1"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTop"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTop2"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTop"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTop3"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTop"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_Portrait"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:screenOrientation="portrait"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_Behind"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:screenOrientation="behind"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_Behind_SingleTop1"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTop"
                    android:screenOrientation="behind" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_T_SingleTop1"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTop"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTop4"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTop"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTask1"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTask2"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTask3"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleTask4"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_Portrait"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:screenOrientation="portrait"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_Behind"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:screenOrientation="behind"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_T"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.alipay.sdk.app.PayResultActivity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="true" 
                    android:launchMode="singleInstance"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_T_SingleInstance1"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleInstance"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity 
                    android:name="com.alipay.sdk.app.AlipayResultActivity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="true" 
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_T_SingleTask"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize"
                    android:exported="false" 
                    android:launchMode="singleTask"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.optional.union.account.impl.login.ui.DouyinEntryActivity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize|screenLayout"
                    android:exported="true" 
                    android:launchMode="singleInstance"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_T_SingleInstance2"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize|screenLayout"
                    android:exported="false" 
                    android:launchMode="singleInstance"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.optional.union.account.impl.login.ui.TTEntryActivity"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize|screenLayout"
                    android:exported="true" 
                    android:launchMode="singleInstance"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompat_T_SingleInstance"
                    android:configChanges="orientation|keyboardHidden|navigation|screenSize|screenLayout"
                    android:exported="false" 
                    android:launchMode="singleInstance"
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden" />
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}Activity_T_NotchSupport"
                    android:configChanges="fontScale|keyboard|keyboardHidden|locale|mnc|mcc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|touchscreen"
                    android:exported="false" 
                    android:theme="@android:style/Theme.Translucent"
                    android:windowSoftInputMode="adjustResize|stateHidden">
                    <meta-data 
                        android:name="android.notch_support" 
                        android:value="true" />
                </activity>
                <activity
                    android:name="com.bytedance.ttgame.tob.packer.union.StubActivity${'$'}AppCompatActivity_NotchSupport"
                    android:configChanges="fontScale|keyboard|keyboardHidden|locale|mnc|mcc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|touchscreen"
                    android:exported="false" 
                    android:windowSoftInputMode="adjustResize|stateHidden">
                    <meta-data 
                        android:name="android.notch_support" 
                        android:value="true" />
                </activity>
                <provider
                    android:name="com.bytedance.ttgame.tob.packer.union.StubServerManager${'$'}PushServerManager"
                    android:authorities="$packageName.zeus.servermanager.push.com.bytedance.ttgame.tob.union"
                    android:exported="false" 
                    android:process=":push" />
                <provider 
                    android:name="com.bytedance.ttgame.tob.packer.union.CJPayFileProvider"
                    android:authorities="$packageName.ttcjpay.fileprovider" 
                    android:exported="false"
                    android:grantUriPermissions="true">
                    <meta-data 
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/cj_pay_filepaths" />
                </provider>
                <provider 
                    android:name="com.bytedance.ttgame.tob.packer.union.AgentWebFileProvider"
                    android:authorities="$packageName.AgentWebFileProvider" 
                    android:exported="false"
                    android:grantUriPermissions="true">
                    <meta-data 
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/web_files_public" />
                </provider>
                <meta-data
                    android:name="com.bytedance.ttgame.tob.artifact_version.gbsdk_optional_union_plugin"
                    android:value="2.1.0.2" />
                <meta-data 
                    android:name="ZEUS_PLUGIN_GBSDK_UNION"
                    android:value="{apiVersionCode:21002,packageName:gbsdk.plugin.union,signature:'MIIDYTCCAkmgAwIBAgIESHTxpjANBgkqhkiG9w0BAQsFADBgMQ4wDAYDVQQGEwV1bmlvbjEOMAwGA1UECBMFdW5pb24xDjAMBgNVBAcTBXVuaW9uMQ4wDAYDVQQKEwV1bmlvbjEOMAwGA1UECxMFdW5pb24xDjAMBgNVBAMTBXVuaW9uMCAXDTIwMDUxMTEwNDcxM1oYDzIwNTgwOTA5MTA0NzEzWjBgMQ4wDAYDVQQGEwV1bmlvbjEOMAwGA1UECBMFdW5pb24xDjAMBgNVBAcTBXVuaW9uMQ4wDAYDVQQKEwV1bmlvbjEOMAwGA1UECxMFdW5pb24xDjAMBgNVBAMTBXVuaW9uMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2Sf6mz43R1wUe/dbzyrDglKyYPkgfk9b0rvxKoxu5n4P4t8bisJ3j39XLhIaboacwMMbJ/MS91FjZUCpElJuaBioCStZGvXs8MdJAuIP3TfbJ94XW8cw8eH6LcmEHQXSHYRvQ70y4QXZktHg1YRwRKlQ1hTZ7neUsbeJRONHqYWLW1GddDXw1KcvbPyvUZTrYWhNVE56m/vis+f5ZgqkeacmoPdrn4xH6sdw4o31jF9ch9f+SfScr17rkRzfGkC5QkrGyApsi2nxWOikOY89NZN/N82RWEKGuw8UKUdyyANKKSwhUS9e1NHwdKjAdAhE/h3hqpjAAZujtxKc2Ys0yQIDAQABoyEwHzAdBgNVHQ4EFgQUNVmaHEewLM/uA8DFQZXj/LWIrNswDQYJKoZIhvcNAQELBQADggEBADsWyf2UUPUYPWdGPqFgzwVCvtgm/EfCtMIIuX4yXYljqa0lqlfl9NQ5QeO9vltbOYO5wgcKrJLLz7fOwvLy0ThwRFSzXXhONqdtya6xNkV5iB+UzLUKl7bAvWSd/l/oST0+uCXu52YIGWcUQhIp2kkJFtezSpBZPhoDCObzcwycQz16dLwMpH1eEugH5QpVQNPvJjHBnleJzmsieJroi2KP2Qt5bLQLZFf5QNKvNKKq5ZlJRSCxxx6SM7ots4yCSizoPEYBXifZ7HUG+bigiT9nb0NJnAP/E9JGayWjf8ntRXVrVsfY80Gai+R1c0CwGJRsXKFAsXBtqWgkIYgM1d4=',appKey:50810ebe66b49f705e55bd2d2aeda2b5,appSecretKey:fe88089774fd6301e453fcb4118b3825,internalPath:gbsdk.plugin.union.apk,internalVersionCode:21002,minPluginVersion:21002}" />
            </application>
            <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
            <uses-permission android:name="android.permission.VIBRATE"/>
            <uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION"/>
            <uses-permission android:name="android.permission.CAMERA"/>
            <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
            <uses-permission android:name="android.permission.ACCESS_LOCATION_EXTRA_COMMANDS"/>
            <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
            <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION"/>
            <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
            <uses-permission android:name="com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA"/>
            <uses-permission android:name="android.permission.CHANGE_CONFIGURATION"/>
            <uses-permission android:name="android.permission.FLASHLIGHT"/>
            <uses-permission android:name="android.permission.USE_FINGERPRINT"/>
        """.trimIndent()
        val file = File(decompileDir, "AndroidManifest.xml")
        replaceXmlEndTag(file, "</application>", content)
        removeSdkSplashActivity(file)
    }

    /**
     * 字节联运的 config.json 设置
     */
    fun setGbSdkConfigJson(decompileDir: String, appId: String) {
        val manifest = File(decompileDir, "AndroidManifest.xml")
        val screenOrientation = getMainActivityScreenOrientation(manifest)
        val orientation = if (screenOrientation == null) {
            "sensorPortrait"
        } else if (screenOrientation.lowercase().contains("portrait")) {
            "sensorPortrait"
        } else if (screenOrientation.lowercase().contains("landscape")) {
            "sensorLandscape"
        } else {
            "sensorPortrait"
        }
        val json = JSONObject()
        json.put("app_id", appId)
        json.put("screen_orientation", orientation)
        json.put("union_mode", 1)   // 这个是可配置的，目前只跑直播联运，所以写死，如要修改可参考文档
        json.put("login_fail_style", 0)
        json.put("debug_mode", false)
        File(decompileDir + File.separator + "assets" + File.separator + "config.json").apply {
            if (exists().not()) {
                createNewFile()
            }
            writeText(json.toString())
        }
    }

    /**
     * 字节联运的资源设置
     */
    fun setGbSdkResValue(decompileDir: String) {
        val strings = """
                <string name="abc_action_bar_home_description">Navigate home</string>
                <string name="abc_action_bar_up_description">Navigate up</string>
                <string name="abc_action_menu_overflow_description">More options</string>
                <string name="abc_action_mode_done">Done</string>
                <string name="abc_activity_chooser_view_see_all">See all</string>
                <string name="abc_activitychooserview_choose_application">Choose an app</string>
                <string name="abc_capital_off">OFF</string>
                <string name="abc_capital_on">ON</string>
                <string name="abc_font_family_body_1_material">sans-serif</string>
                <string name="abc_font_family_body_2_material">sans-serif-medium</string>
                <string name="abc_font_family_button_material">sans-serif-medium</string>
                <string name="abc_font_family_caption_material">sans-serif</string>
                <string name="abc_font_family_display_1_material">sans-serif</string>
                <string name="abc_font_family_display_2_material">sans-serif</string>
                <string name="abc_font_family_display_3_material">sans-serif</string>
                <string name="abc_font_family_display_4_material">sans-serif-light</string>
                <string name="abc_font_family_headline_material">sans-serif</string>
                <string name="abc_font_family_menu_material">sans-serif</string>
                <string name="abc_font_family_subhead_material">sans-serif</string>
                <string name="abc_font_family_title_material">sans-serif-medium</string>
                <string name="abc_menu_alt_shortcut_label">Alt+</string>
                <string name="abc_menu_ctrl_shortcut_label">Ctrl+</string>
                <string name="abc_menu_delete_shortcut_label">delete</string>
                <string name="abc_menu_enter_shortcut_label">enter</string>
                <string name="abc_menu_function_shortcut_label">Function+</string>
                <string name="abc_menu_meta_shortcut_label">Meta+</string>
                <string name="abc_menu_shift_shortcut_label">Shift+</string>
                <string name="abc_menu_space_shortcut_label">space</string>
                <string name="abc_menu_sym_shortcut_label">Sym+</string>
                <string name="abc_prepend_shortcut_label">Menu+</string>
                <string name="abc_search_hint">Search…</string>
                <string name="abc_searchview_description_clear">Clear query</string>
                <string name="abc_searchview_description_query">Search query</string>
                <string name="abc_searchview_description_search">Search</string>
                <string name="abc_searchview_description_submit">Submit query</string>
                <string name="abc_searchview_description_voice">Voice search</string>
                <string name="abc_shareactionprovider_share_with">Share with</string>
                <string name="abc_shareactionprovider_share_with_application">Share with %s</string>
                <string name="abc_toolbar_collapse_description">Collapse</string>
                <string name="agentweb_camera">相机</string>
                <string name="agentweb_cancel">取消</string>
                <string name="agentweb_click_open">点击打开</string>
                <string name="agentweb_coming_soon_download">即将开始下载文件</string>
                <string name="agentweb_current_downloaded_length">已下载:%s</string>
                <string name="agentweb_current_downloading_progress">当前进度:%s</string>
                <string name="agentweb_default_page_error">出错啦! 点击空白处刷新 ~</string>
                <string name="agentweb_download">下载</string>
                <string name="agentweb_download_fail">下载失败!</string>
                <string name="agentweb_download_task_has_been_exist">该任务已经存在 ， 请勿重复点击下载!</string>
                <string name="agentweb_file_chooser">文件</string>
                <string name="agentweb_file_download">文件下载</string>
                <string name="agentweb_honeycomblow">您正在使用手机流量 ， 继续下载该文件吗?</string>
                <string name="agentweb_leave">离开</string>
                <string name="agentweb_leave_app_and_go_other_page">您需要离开%s前往其他应用吗？</string>
                <string name="agentweb_loading">加载中 ...</string>
                <string name="agentweb_max_file_length_limit">选择的文件不能大于%sMB</string>
                <string name="agentweb_tips">提示</string>
                <string name="agentweb_trickter">您有一条新通知</string>
                <string name="appbar_scrolling_view_behavior">android.support.design.widget.AppBarLayout${'$'}ScrollingViewBehavior</string>
                <string name="aweme_loading">加载中...</string>
                <string name="aweme_open_error_tips_cancel">取消</string>
                <string name="aweme_open_network_error_confirm">确定</string>
                <string name="aweme_open_network_error_tips">当前网络不可用，请稍后重试</string>
                <string name="aweme_open_network_error_title">授权失败</string>
                <string name="aweme_open_ssl_cancel">取消</string>
                <string name="aweme_open_ssl_continue">是否继续？</string>
                <string name="aweme_open_ssl_error">证书错误</string>
                <string name="aweme_open_ssl_expired">证书过期</string>
                <string name="aweme_open_ssl_mismatched">证书不匹配</string>
                <string name="aweme_open_ssl_notyetvalid">证书尚未生效</string>
                <string name="aweme_open_ssl_ok">确认</string>
                <string name="aweme_open_ssl_untrusted">证书不可信</string>
                <string name="aweme_open_ssl_warning">警告</string>
                <string name="bottom_sheet_behavior">android.support.design.widget.BottomSheetBehavior</string>
                <string name="character_counter_content_description">Character limit exceeded %1${'$'}d of %2${'$'}d</string>
                <string name="character_counter_pattern">%1${'$'}d / %2${'$'}d</string>
                <string name="debug_mdoe_showing_recent_crash_message">上次发生了一次crash，正在展示最近发生的崩溃栈</string>
                <string name="debug_mode_record_crash_information">SDK正在为您记录crash信息</string>
                <string name="debug_mode_remind_message">现在是Debug模式，仅能用于测试，必须关闭该模式后才能提审</string>
                <string name="fab_transformation_scrim_behavior">android.support.design.transformation.FabTransformationScrimBehavior</string>
                <string name="fab_transformation_sheet_behavior">android.support.design.transformation.FabTransformationSheetBehavior</string>
                <string name="gsdk_privacy_exit">不同意并退出游戏</string>
                <string name="gsdk_privacy_policy">《隐私政策》</string>
                <string name="gsdk_privacy_policy_title">隐私政策</string>
                <string name="gsdk_privacy_protection">个人信息保护指引</string>
                <string name="gsdk_privacy_protection_content">"在您使用游戏服务的过程中，我们可能会申请以下权限为您实现相关功能：
            -系统设备权限电话权限用于安全风控、存储（相册）权限用于下载缓存相关文件；
            -如您希望通过语音、视频与其他游戏玩家互动、我们可能申请您的麦克风、摄像头、地理位置权限；
            上述权限均不会默认或强制开启收集信息。"</string>
                <string name="gsdk_privacy_protocol">如您同意本游戏《用户协议》《隐私政策》以及以上条款，请点击下面的按钮以接受我们的服务</string>
                <string name="gsdk_privacy_result_agreement_deny">用户拒绝隐私政策</string>
                <string name="gsdk_privacy_result_permissions_deny">用户拒绝权限授权</string>
                <string name="gsdk_privacy_result_unknown">系统异常</string>
                <string name="gsdk_privacy_use">同意</string>
                <string name="gsdk_privacy_user_agree">《用户协议》</string>
                <string name="gsdk_privacy_user_agree_title">用户协议</string>
                <string name="gsdk_webview_upload_permission_file">请到设置中开启存储权限</string>
                <string name="gsdk_webview_upload_permission_video">请到设置中开启摄像头权限</string>
                <string name="hide_bottom_view_on_scroll_behavior">android.support.design.behavior.HideBottomViewOnScrollBehavior</string>
                <string name="hours_ago">%d小时前</string>
                <string name="just_now">刚刚</string>
                <string name="load_dex_waiting">Loading...</string>
                <string name="minutes_ago">%d分钟前</string>
                <string name="mtrl_chip_close_icon_content_description">Remove %1${'$'}s</string>
                <string name="password_toggle_content_description">Show password</string>
                <string name="path_password_eye">M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5c-1.73,-4.39 -6,-7.5 -11,-7.5zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5 5,2.24 5,5 -2.24,5 -5,5zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3 -1.34,-3 -3,-3z</string>
                <string name="path_password_eye_mask_strike_through">M2,4.27 L19.73,22 L22.27,19.46 L4.54,1.73 L4.54,1 L23,1 L23,23 L1,23 L1,4.27 Z</string>
                <string name="path_password_eye_mask_visible">M2,4.27 L2,4.27 L4.54,1.73 L4.54,1.73 L4.54,1 L23,1 L23,23 L1,23 L1,4.27 Z</string>
                <string name="path_password_strike_through">M3.27,4.27 L19.74,20.74</string>
                <string name="search_menu_title">Search</string>
                <string name="status_bar_notification_info_overflow">999+</string>
                <string name="tt_appdownloader_button_cancel_download">取消</string>
                <string name="tt_appdownloader_button_queue_for_wifi">排队</string>
                <string name="tt_appdownloader_button_start_now">立即开始</string>
                <string name="tt_appdownloader_download_percent">%1${'$'}d%%</string>
                <string name="tt_appdownloader_download_remaining">剩余时间：%1${'$'}s</string>
                <string name="tt_appdownloader_download_unknown_title">&lt;未命名></string>
                <string name="tt_appdownloader_duration_hours">%1${'$'}d 小时</string>
                <string name="tt_appdownloader_duration_minutes">%1${'$'}d 分钟</string>
                <string name="tt_appdownloader_duration_seconds">%1${'$'}d 秒</string>
                <string name="tt_appdownloader_jump_unknown_source">请打开应用安装权限，以便顺利安装！</string>
                <string name="tt_appdownloader_label_cancel">取消</string>
                <string name="tt_appdownloader_label_cancel_directly">直接取消</string>
                <string name="tt_appdownloader_label_ok">确定</string>
                <string name="tt_appdownloader_label_reserve_wifi">Wi-Fi下恢复</string>
                <string name="tt_appdownloader_notification_download">下载</string>
                <string name="tt_appdownloader_notification_download_complete_open">下载完成，点击打开。</string>
                <string name="tt_appdownloader_notification_download_complete_with_install">下载完成，点击安装。</string>
                <string name="tt_appdownloader_notification_download_complete_without_install">下载完成</string>
                <string name="tt_appdownloader_notification_download_continue">下载暂停中，点击继续。</string>
                <string name="tt_appdownloader_notification_download_delete">确认要删除%1${'$'}s的下载任务吗？</string>
                <string name="tt_appdownloader_notification_download_failed">下载失败。</string>
                <string name="tt_appdownloader_notification_download_install">安装</string>
                <string name="tt_appdownloader_notification_download_open">打开</string>
                <string name="tt_appdownloader_notification_download_pause">暂停</string>
                <string name="tt_appdownloader_notification_download_restart">重新下载</string>
                <string name="tt_appdownloader_notification_download_resume">继续</string>
                <string name="tt_appdownloader_notification_download_space_failed">SdCard空间不足, 下载失败。</string>
                <string name="tt_appdownloader_notification_download_waiting_net">等待网络继续下载</string>
                <string name="tt_appdownloader_notification_download_waiting_wifi">等待wifi开始下载</string>
                <string name="tt_appdownloader_notification_downloading">正在下载</string>
                <string name="tt_appdownloader_notification_install_finished_open">安装完成，点击打开。</string>
                <string name="tt_appdownloader_notification_insufficient_space_error">空间不足 还需%1${'$'}s</string>
                <string name="tt_appdownloader_notification_need_wifi_for_size">文件过大，需要通过 WLAN 下载。</string>
                <string name="tt_appdownloader_notification_no_internet_error">下载异常，请检查网络</string>
                <string name="tt_appdownloader_notification_no_wifi_and_in_net">无Wi-Fi 已暂停</string>
                <string name="tt_appdownloader_notification_paused_in_background">已在后台暂停。</string>
                <string name="tt_appdownloader_notification_pausing">暂停中</string>
                <string name="tt_appdownloader_notification_prepare">准备中</string>
                <string name="tt_appdownloader_notification_request_btn_no">立即下载</string>
                <string name="tt_appdownloader_notification_request_btn_yes">去设置</string>
                <string name="tt_appdownloader_notification_request_message">为了您能在方便地在通知栏控制下载任务，请到设置中开启通知，如果不需要可直接下载</string>
                <string name="tt_appdownloader_notification_request_title">设置</string>
                <string name="tt_appdownloader_notification_waiting_download_complete_handler">处理中</string>
                <string name="tt_appdownloader_resume_in_wifi">您当前处于移动网络，是否需要在Wi-Fi环境下恢复下载？</string>
                <string name="tt_appdownloader_tip">提示</string>
                <string name="tt_appdownloader_wifi_recommended_body">"立即开始下载此内容 (%1${'$'}s ) 可能会缩短电池的使用时间并/或导致过量使用移动数据连接流量（这可能导致移动运营商向您收费，具体取决于您的流量套餐）。

            触摸“%2${'$'}s”可在下次连接到 WLAN 网络时开始下载此内容。"</string>
                <string name="tt_appdownloader_wifi_recommended_title">稍后再加入下载队列吗？</string>
                <string name="tt_appdownloader_wifi_required_body">"您必须使用 WLAN 完成此内容 (%1${'$'}s ) 的下载。

            触摸“%2${'$'}s ”可在下次连接到 WLAN 网络时开始下载此内容。"</string>
                <string name="tt_appdownloader_wifi_required_title">文件太大，不适于通过运营商网络下载</string>
                <string name="union_daemon_hook_invalid_hint">Hook is invalid, error code:%1${'$'}d</string>
                <string name="union_privacy_policy_url">https://sf3-cdn-tos.douyinstatic.com/obj/ies-hotsoon-draft/ttgame/privacy_policy_1_2.html</string>
                <string name="union_privacy_policy_url_live">https://sf1-cdn-tos.douyinstatic.com/obj/ies-hotsoon-draft/ttgame/b93a991b-5cd9-46f9-bf33-b142f7226d02.html</string>
                <string name="union_update_resource_failed_line_one">资源加载失败</string>
                <string name="union_update_resource_failed_line_two">请检查网络并重启后再次尝试</string>
                <string name="union_update_resource_loading">正在加载资源</string>
                <string name="union_update_resource_success_line_one">资源加载完毕</string>
                <string name="union_update_resource_success_line_two">请重启后进行游戏</string>
                <string name="union_update_update_exit">关闭游戏</string>
                <string name="union_update_update_text_one">当前有版本更新</string>
                <string name="union_update_update_text_two">请退出游戏后重新启动</string>
                <string name="union_update_update_title">更新提醒</string>
                <string name="union_user_agreement_url">https://sf3-cdn-tos.douyinstatic.com/obj/ies-hotsoon-draft/ttgame/user_agreement_1_2.html</string>
                <string name="union_user_agreement_url_live">https://sf1-cdn-tos.douyinstatic.com/obj/ies-hotsoon-draft/ttgame/b31c858e-9d79-4906-805b-80901edf5c9e.html</string>
            </resources>
        """.trimIndent()
        val colors = """
                <color name="abc_input_method_navigation_guard">@android:color/black</color>
                <color name="abc_search_url_text_normal">#ff7fa87f</color>
                <color name="abc_search_url_text_pressed">@android:color/black</color>
                <color name="abc_search_url_text_selected">@android:color/black</color>
                <color name="accent_material_dark">@color/material_deep_teal_200</color>
                <color name="accent_material_light">@color/material_deep_teal_500</color>
                <color name="aweme_loading_view_background">#7f161823</color>
                <color name="aweme_loading_view_text_color">#e6ffffff</color>
                <color name="aweme_network_error_button_color">#ff2a90d7</color>
                <color name="aweme_network_error_content_color">#ff999999</color>
                <color name="aweme_network_error_dialog_bg">#ffffffff</color>
                <color name="aweme_network_error_title_color">#ff222222</color>
                <color name="background_floating_material_dark">@color/material_grey_800</color>
                <color name="background_floating_material_light">@android:color/white</color>
                <color name="background_light_dark">#fff0f0f0</color>
                <color name="background_material_dark">@color/material_grey_850</color>
                <color name="background_material_light">@color/material_grey_50</color>
                <color name="black">#ff000000</color>
                <color name="bright_foreground_disabled_material_dark">#80ffffff</color>
                <color name="bright_foreground_disabled_material_light">#80000000</color>
                <color name="bright_foreground_inverse_material_dark">@color/bright_foreground_material_light</color>
                <color name="bright_foreground_inverse_material_light">@color/bright_foreground_material_dark</color>
                <color name="bright_foreground_material_dark">@android:color/white</color>
                <color name="bright_foreground_material_light">@android:color/black</color>
                <color name="button_material_dark">#ff5a595b</color>
                <color name="button_material_light">#ffd6d7d7</color>
                <color name="cardview_dark_background">#ff424242</color>
                <color name="cardview_light_background">#ffffffff</color>
                <color name="cardview_shadow_end_color">#03000000</color>
                <color name="cardview_shadow_start_color">#37000000</color>
                <color name="colorAccent">#ff79a7d3</color>
                <color name="colorControlActivated">#ffb8b8b8</color>
                <color name="colorPrimary">#ff212121</color>
                <color name="colorPrimaryDark">@color/black</color>
                <color name="colorSplashBackground">@color/white</color>
                <color name="colorToolbarText">@color/white</color>
                <color name="colorTransparent">#00000000</color>
                <color name="defaultDivisionLine">@color/w3</color>
                <color name="defaultHintText">@color/w3</color>
                <color name="defaultLinkText">#ff79a7d3</color>
                <color name="defaultMainText">@color/black</color>
                <color name="design_bottom_navigation_shadow_color">#14000000</color>
                <color name="design_default_color_primary">#ff3f51b5</color>
                <color name="design_default_color_primary_dark">#ff303f9f</color>
                <color name="design_fab_shadow_end_color">@android:color/transparent</color>
                <color name="design_fab_shadow_mid_color">#14000000</color>
                <color name="design_fab_shadow_start_color">#44000000</color>
                <color name="design_fab_stroke_end_inner_color">#0a000000</color>
                <color name="design_fab_stroke_end_outer_color">#0f000000</color>
                <color name="design_fab_stroke_top_inner_color">#1affffff</color>
                <color name="design_fab_stroke_top_outer_color">#2effffff</color>
                <color name="design_snackbar_background_color">#ff323232</color>
                <color name="dim_foreground_disabled_material_dark">#80bebebe</color>
                <color name="dim_foreground_disabled_material_light">#80323232</color>
                <color name="dim_foreground_material_dark">#ffbebebe</color>
                <color name="dim_foreground_material_light">#ff323232</color>
                <color name="error_color_material_dark">#ffff7043</color>
                <color name="error_color_material_light">#ffff5722</color>
                <color name="foreground_material_dark">@android:color/white</color>
                <color name="foreground_material_light">@android:color/black</color>
                <color name="highlighted_text_material_dark">#6680cbc4</color>
                <color name="highlighted_text_material_light">#66009688</color>
                <color name="material_blue_grey_800">#ff37474f</color>
                <color name="material_blue_grey_900">#ff263238</color>
                <color name="material_blue_grey_950">#ff21272b</color>
                <color name="material_deep_teal_200">#ff80cbc4</color>
                <color name="material_deep_teal_500">#ff009688</color>
                <color name="material_grey_100">#fff5f5f5</color>
                <color name="material_grey_300">#ffe0e0e0</color>
                <color name="material_grey_50">#fffafafa</color>
                <color name="material_grey_600">#ff757575</color>
                <color name="material_grey_800">#ff424242</color>
                <color name="material_grey_850">#ff303030</color>
                <color name="material_grey_900">#ff212121</color>
                <color name="mtrl_btn_bg_color_disabled">#1f000000</color>
                <color name="mtrl_btn_text_color_disabled">#61000000</color>
                <color name="mtrl_btn_transparent_bg_color">#00ffffff</color>
                <color name="mtrl_scrim_color">#52000000</color>
                <color name="mtrl_textinput_default_box_stroke_color">#6b000000</color>
                <color name="mtrl_textinput_disabled_color">#1f000000</color>
                <color name="mtrl_textinput_filled_box_default_background_color">#0a000000</color>
                <color name="mtrl_textinput_hovered_box_stroke_color">#de000000</color>
                <color name="notification_action_color_filter">@color/secondary_text_default_material_light</color>
                <color name="notification_icon_bg_color">#ff9e9e9e</color>
                <color name="notification_material_background_media_default_color">#ff424242</color>
                <color name="primary_dark_material_dark">@android:color/black</color>
                <color name="primary_dark_material_light">@color/material_grey_600</color>
                <color name="primary_material_dark">@color/material_grey_900</color>
                <color name="primary_material_light">@color/material_grey_100</color>
                <color name="primary_text_default_material_dark">#ffffffff</color>
                <color name="primary_text_default_material_light">#de000000</color>
                <color name="primary_text_disabled_material_dark">#4dffffff</color>
                <color name="primary_text_disabled_material_light">#39000000</color>
                <color name="purple_200">#ffbb86fc</color>
                <color name="purple_500">#ff6200ee</color>
                <color name="purple_700">#ff3700b3</color>
                <color name="ripple_material_dark">#33ffffff</color>
                <color name="ripple_material_light">#1f000000</color>
                <color name="secondary_text_default_material_dark">#b3ffffff</color>
                <color name="secondary_text_default_material_light">#8a000000</color>
                <color name="secondary_text_disabled_material_dark">#36ffffff</color>
                <color name="secondary_text_disabled_material_light">#24000000</color>
                <color name="select_color">#ff2e2e32</color>
                <color name="switch_blue">#ff3370ff</color>
                <color name="switch_thumb_disabled_material_dark">#ff616161</color>
                <color name="switch_thumb_disabled_material_light">#ffbdbdbd</color>
                <color name="switch_thumb_normal_material_dark">#ffbdbdbd</color>
                <color name="switch_thumb_normal_material_light">#fff1f1f1</color>
                <color name="teal_200">#ff03dac5</color>
                <color name="teal_700">#ff018786</color>
                <color name="tooltip_background_dark">#e6616161</color>
                <color name="tooltip_background_light">#e6ffffff</color>
                <color name="transparent">#00000000</color>
                <color name="tt_appdownloader_notification_material_background_color">#fffafafa</color>
                <color name="tt_appdownloader_notification_title_color">#7f0b0198</color>
                <color name="tt_appdownloader_s1">#ff333333</color>
                <color name="tt_appdownloader_s13">#ffcccccc</color>
                <color name="tt_appdownloader_s18">#ffeeeeee</color>
                <color name="tt_appdownloader_s4">#ffff819f</color>
                <color name="tt_appdownloader_s8">#ffffffff</color>
                <color name="ttdownloader_transparent">#00000000</color>
                <color name="union_privacy_color_222">#ff222222</color>
                <color name="union_privacy_color_333">#ff333333</color>
                <color name="union_privacy_color_666">#ff666666</color>
                <color name="union_privacy_color_999">#ff999999</color>
                <color name="union_privacy_color_agreement_light">#ff999999</color>
                <color name="union_privacy_color_coral">#fff85959</color>
                <color name="union_privacy_color_next_enable">#fff85959</color>
                <color name="union_privacy_color_next_pressed">#ffde5050</color>
                <color name="union_privacy_color_press_gray">#ffe5e5e5</color>
                <color name="union_privacy_disabled">#80f85959</color>
                <color name="union_privacy_refused_border">#4d000000</color>
                <color name="union_privacy_sdk_light_grey">#ffe8e8e8</color>
                <color name="union_privacy_sdk_pale_grey">#fffafafc</color>
                <color name="union_privacy_warm_grey_two">#ff707070</color>
                <color name="w1">#ff1f2022</color>
                <color name="w2">#ff8f8f90</color>
                <color name="w3">#ffcbcbcf</color>
                <color name="w4">#ffcd7c08</color>
                <color name="w5">#ffffffff</color>
                <color name="white">#ffffffff</color>
            </resources>
        """.trimIndent()
        val styles = """
                <style name="AlertDialog.AppCompat" parent="@style/Base.AlertDialog.AppCompat" />
                <style name="AlertDialog.AppCompat.Light" parent="@style/Base.AlertDialog.AppCompat.Light" />
                <style name="Animation.AppCompat.Dialog" parent="@style/Base.Animation.AppCompat.Dialog" />
                <style name="Animation.AppCompat.DropDownUp" parent="@style/Base.Animation.AppCompat.DropDownUp" />
                <style name="Animation.AppCompat.Tooltip" parent="@style/Base.Animation.AppCompat.Tooltip" />
                <style name="Animation.Design.BottomSheetDialog" parent="@style/Animation.AppCompat.Dialog">
                    <item name="android:windowEnterAnimation">@anim/design_bottom_sheet_slide_in</item>
                    <item name="android:windowExitAnimation">@anim/design_bottom_sheet_slide_out</item>
                </style>
                <style name="Base.AlertDialog.AppCompat" parent="@android:style/Widget">
                    <item name="android:layout">@layout/abc_alert_dialog_material</item>
                    <item name="buttonIconDimen">@dimen/abc_alert_dialog_button_dimen</item>
                    <item name="listItemLayout">@layout/select_dialog_item_material</item>
                    <item name="listLayout">@layout/abc_select_dialog_material</item>
                    <item name="multiChoiceItemLayout">@layout/select_dialog_multichoice_material</item>
                    <item name="singleChoiceItemLayout">@layout/select_dialog_singlechoice_material</item>
                </style>
                <style name="Base.AlertDialog.AppCompat.Light" parent="@style/Base.AlertDialog.AppCompat" />
                <style name="Base.Animation.AppCompat.Dialog" parent="@android:style/Animation">
                    <item name="android:windowEnterAnimation">@anim/abc_popup_enter</item>
                    <item name="android:windowExitAnimation">@anim/abc_popup_exit</item>
                </style>
                <style name="Base.Animation.AppCompat.DropDownUp" parent="@android:style/Animation">
                    <item name="android:windowEnterAnimation">@anim/abc_grow_fade_in_from_bottom</item>
                    <item name="android:windowExitAnimation">@anim/abc_shrink_fade_out_from_bottom</item>
                </style>
                <style name="Base.Animation.AppCompat.Tooltip" parent="@android:style/Animation">
                    <item name="android:windowEnterAnimation">@anim/abc_tooltip_enter</item>
                    <item name="android:windowExitAnimation">@anim/abc_tooltip_exit</item>
                </style>
                <style name="Base.CardView" parent="@android:style/Widget">
                    <item name="cardCornerRadius">@dimen/cardview_default_radius</item>
                    <item name="cardElevation">@dimen/cardview_default_elevation</item>
                    <item name="cardMaxElevation">@dimen/cardview_default_elevation</item>
                    <item name="cardPreventCornerOverlap">true</item>
                    <item name="cardUseCompatPadding">false</item>
                </style>
                <style name="Base.DialogWindowTitle.AppCompat" parent="@android:style/Widget">
                    <item name="android:textAppearance">@style/TextAppearance.AppCompat.Title</item>
                    <item name="android:maxLines">1</item>
                    <item name="android:scrollHorizontally">true</item>
                </style>
                <style name="Base.DialogWindowTitleBackground.AppCompat" parent="@android:style/Widget">
                    <item name="android:background">@null</item>
                    <item name="android:paddingLeft">?dialogPreferredPadding</item>
                    <item name="android:paddingTop">@dimen/abc_dialog_padding_top_material</item>
                    <item name="android:paddingRight">?dialogPreferredPadding</item>
                </style>
                <style name="Base.TextAppearance.AppCompat" parent="@android:style/TextAppearance.Material" />
                <style name="Base.TextAppearance.AppCompat.Body1" parent="@android:style/TextAppearance.Material.Body1" />
                <style name="Base.TextAppearance.AppCompat.Body2" parent="@android:style/TextAppearance.Material.Body2" />
                <style name="Base.TextAppearance.AppCompat.Button" parent="@android:style/TextAppearance.Material.Button" />
                <style name="Base.TextAppearance.AppCompat.Caption" parent="@android:style/TextAppearance.Material.Caption" />
                <style name="Base.TextAppearance.AppCompat.Display1" parent="@android:style/TextAppearance.Material.Display1" />
                <style name="Base.TextAppearance.AppCompat.Display2" parent="@android:style/TextAppearance.Material.Display2" />
                <style name="Base.TextAppearance.AppCompat.Display3" parent="@android:style/TextAppearance.Material.Display3" />
                <style name="Base.TextAppearance.AppCompat.Display4" parent="@android:style/TextAppearance.Material.Display4" />
                <style name="Base.TextAppearance.AppCompat.Headline" parent="@android:style/TextAppearance.Material.Headline" />
                <style name="Base.TextAppearance.AppCompat.Inverse" parent="@android:style/TextAppearance.Material.Inverse" />
                <style name="Base.TextAppearance.AppCompat.Large" parent="@android:style/TextAppearance.Material.Large" />
                <style name="Base.TextAppearance.AppCompat.Large.Inverse" parent="@android:style/TextAppearance.Material.Large.Inverse" />
                <style name="Base.TextAppearance.AppCompat.Light.Widget.PopupMenu.Large" parent="@android:style/TextAppearance.Material.Widget.PopupMenu.Large" />
                <style name="Base.TextAppearance.AppCompat.Light.Widget.PopupMenu.Small" parent="@android:style/TextAppearance.Material.Widget.PopupMenu.Small" />
                <style name="Base.TextAppearance.AppCompat.Medium" parent="@android:style/TextAppearance.Material.Medium" />
                <style name="Base.TextAppearance.AppCompat.Medium.Inverse" parent="@android:style/TextAppearance.Material.Medium.Inverse" />
                <style name="Base.TextAppearance.AppCompat.Menu" parent="@android:style/TextAppearance.Material.Menu" />
                <style name="Base.TextAppearance.AppCompat.SearchResult" parent="">
                    <item name="android:textStyle">normal</item>
                    <item name="android:textColor">?android:textColorPrimary</item>
                    <item name="android:textColorHint">?android:textColorHint</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.SearchResult.Subtitle" parent="@android:style/TextAppearance.Material.SearchResult.Subtitle" />
                <style name="Base.TextAppearance.AppCompat.SearchResult.Title" parent="@android:style/TextAppearance.Material.SearchResult.Title" />
                <style name="Base.TextAppearance.AppCompat.Small" parent="@android:style/TextAppearance.Material.Small" />
                <style name="Base.TextAppearance.AppCompat.Small.Inverse" parent="@android:style/TextAppearance.Material.Small.Inverse" />
                <style name="Base.TextAppearance.AppCompat.Subhead" parent="@android:style/TextAppearance.Material.Subhead" />
                <style name="Base.TextAppearance.AppCompat.Subhead.Inverse" parent="@style/Base.TextAppearance.AppCompat.Subhead">
                    <item name="android:textColor">?android:textColorPrimaryInverse</item>
                    <item name="android:textColorHighlight">?android:textColorHighlightInverse</item>
                    <item name="android:textColorHint">?android:textColorHintInverse</item>
                    <item name="android:textColorLink">?android:textColorLinkInverse</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Title" parent="@android:style/TextAppearance.Material.Title" />
                <style name="Base.TextAppearance.AppCompat.Title.Inverse" parent="@style/Base.TextAppearance.AppCompat.Title">
                    <item name="android:textColor">?android:textColorPrimaryInverse</item>
                    <item name="android:textColorHighlight">?android:textColorHighlightInverse</item>
                    <item name="android:textColorHint">?android:textColorHintInverse</item>
                    <item name="android:textColorLink">?android:textColorLinkInverse</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Tooltip" parent="@style/Base.TextAppearance.AppCompat">
                    <item name="android:textSize">14.0sp</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Widget.ActionBar.Menu" parent="@style/TextAppearance.AppCompat.Button">
                    <item name="android:textColor">?actionMenuTextColor</item>
                    <item name="textAllCaps">@bool/abc_config_actionMenuItemAllCaps</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Widget.ActionBar.Subtitle" parent="@android:style/TextAppearance.Material.Widget.ActionBar.Subtitle" />
                <style name="Base.TextAppearance.AppCompat.Widget.ActionBar.Subtitle.Inverse" parent="@android:style/TextAppearance.Material.Widget.ActionBar.Subtitle.Inverse" />
                <style name="Base.TextAppearance.AppCompat.Widget.ActionBar.Title" parent="@android:style/TextAppearance.Material.Widget.ActionBar.Title" />
                <style name="Base.TextAppearance.AppCompat.Widget.ActionBar.Title.Inverse" parent="@android:style/TextAppearance.Material.Widget.ActionBar.Title.Inverse" />
                <style name="Base.TextAppearance.AppCompat.Widget.ActionMode.Subtitle" parent="@android:style/TextAppearance.Material.Widget.ActionMode.Subtitle" />
                <style name="Base.TextAppearance.AppCompat.Widget.ActionMode.Title" parent="@android:style/TextAppearance.Material.Widget.ActionMode.Title" />
                <style name="Base.TextAppearance.AppCompat.Widget.Button" parent="@android:style/TextAppearance.Material.Widget.Button" />
                <style name="Base.TextAppearance.AppCompat.Widget.Button.Borderless.Colored" parent="@style/Base.TextAppearance.AppCompat.Widget.Button">
                    <item name="android:textColor">@color/abc_btn_colored_borderless_text_material</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Widget.Button.Colored" parent="@style/Base.TextAppearance.AppCompat.Widget.Button">
                    <item name="android:textColor">@color/abc_btn_colored_text_material</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Widget.Button.Inverse" parent="@style/TextAppearance.AppCompat.Button">
                    <item name="android:textColor">?android:textColorPrimaryInverse</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Widget.DropDownItem" parent="@android:style/TextAppearance.Small">
                    <item name="android:textColor">?android:textColorPrimaryDisableOnly</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Widget.PopupMenu.Header" parent="@style/TextAppearance.AppCompat">
                    <item name="android:textSize">@dimen/abc_text_size_menu_header_material</item>
                    <item name="android:textColor">?android:textColorSecondary</item>
                    <item name="android:fontFamily">@string/abc_font_family_title_material</item>
                </style>
                <style name="Base.TextAppearance.AppCompat.Widget.PopupMenu.Large" parent="@android:style/TextAppearance.Material.Widget.PopupMenu.Large" />
                <style name="Base.TextAppearance.AppCompat.Widget.PopupMenu.Small" parent="@android:style/TextAppearance.Material.Widget.PopupMenu.Small" />
                <style name="Base.TextAppearance.AppCompat.Widget.Switch" parent="@android:style/TextAppearance.Material.Button" />
                <style name="Base.TextAppearance.AppCompat.Widget.TextView.SpinnerItem" parent="@android:style/TextAppearance.Material.Widget.TextView.SpinnerItem" />
                <style name="Base.TextAppearance.Widget.AppCompat.ExpandedMenu.Item" parent="@android:style/TextAppearance.Medium">
                    <item name="android:textColor">?android:textColorPrimaryDisableOnly</item>
                </style>
                <style name="Base.TextAppearance.Widget.AppCompat.Toolbar.Subtitle" parent="@android:style/TextAppearance.Material.Widget.ActionBar.Subtitle" />
                <style name="Base.TextAppearance.Widget.AppCompat.Toolbar.Title" parent="@android:style/TextAppearance.Material.Widget.ActionBar.Title" />
                <style name="Base.Theme.AppCompat" parent="@style/Base.V21.Theme.AppCompat" />
                <style name="Base.Theme.AppCompat.CompactMenu" parent="">
                    <item name="android:listViewStyle">@style/Widget.AppCompat.ListView.Menu</item>
                    <item name="android:windowAnimationStyle">@style/Animation.AppCompat.DropDownUp</item>
                    <item name="android:itemTextAppearance">?android:textAppearanceMedium</item>
                </style>
                <style name="Base.Theme.AppCompat.Dialog" parent="@style/Base.V21.Theme.AppCompat.Dialog" />
                <style name="Base.Theme.AppCompat.Dialog.Alert" parent="@style/Base.Theme.AppCompat.Dialog">
                    <item name="android:windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="android:windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.AppCompat.Dialog.FixedSize" parent="@style/Base.Theme.AppCompat.Dialog">
                    <item name="windowFixedHeightMajor">@dimen/abc_dialog_fixed_height_major</item>
                    <item name="windowFixedHeightMinor">@dimen/abc_dialog_fixed_height_minor</item>
                    <item name="windowFixedWidthMajor">@dimen/abc_dialog_fixed_width_major</item>
                    <item name="windowFixedWidthMinor">@dimen/abc_dialog_fixed_width_minor</item>
                </style>
                <style name="Base.Theme.AppCompat.Dialog.MinWidth" parent="@style/Base.Theme.AppCompat.Dialog">
                    <item name="android:windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="android:windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.AppCompat.DialogWhenLarge" parent="@style/Theme.AppCompat" />
                <style name="Base.Theme.AppCompat.Light" parent="@style/Base.V21.Theme.AppCompat.Light" />
                <style name="Base.Theme.AppCompat.Light.DarkActionBar" parent="@style/Base.Theme.AppCompat.Light">
                    <item name="actionBarPopupTheme">@style/ThemeOverlay.AppCompat.Light</item>
                    <item name="actionBarTheme">@style/ThemeOverlay.AppCompat.Dark.ActionBar</item>
                    <item name="actionBarWidgetTheme">@null</item>
                    <item name="colorPrimary">@color/primary_material_dark</item>
                    <item name="colorPrimaryDark">@color/primary_dark_material_dark</item>
                    <item name="listChoiceBackgroundIndicator">@drawable/abc_list_selector_holo_dark</item>
                </style>
                <style name="Base.Theme.AppCompat.Light.Dialog" parent="@style/Base.V21.Theme.AppCompat.Light.Dialog" />
                <style name="Base.Theme.AppCompat.Light.Dialog.Alert" parent="@style/Base.Theme.AppCompat.Light.Dialog">
                    <item name="android:windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="android:windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.AppCompat.Light.Dialog.FixedSize" parent="@style/Base.Theme.AppCompat.Light.Dialog">
                    <item name="windowFixedHeightMajor">@dimen/abc_dialog_fixed_height_major</item>
                    <item name="windowFixedHeightMinor">@dimen/abc_dialog_fixed_height_minor</item>
                    <item name="windowFixedWidthMajor">@dimen/abc_dialog_fixed_width_major</item>
                    <item name="windowFixedWidthMinor">@dimen/abc_dialog_fixed_width_minor</item>
                </style>
                <style name="Base.Theme.AppCompat.Light.Dialog.MinWidth" parent="@style/Base.Theme.AppCompat.Light.Dialog">
                    <item name="android:windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="android:windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.AppCompat.Light.DialogWhenLarge" parent="@style/Theme.AppCompat.Light" />
                <style name="Base.Theme.MaterialComponents" parent="@style/Base.V14.Theme.MaterialComponents" />
                <style name="Base.Theme.MaterialComponents.Bridge" parent="@style/Base.V14.Theme.MaterialComponents.Bridge" />
                <style name="Base.Theme.MaterialComponents.CompactMenu" parent="">
                    <item name="android:listViewStyle">@style/Widget.AppCompat.ListView.Menu</item>
                    <item name="android:windowAnimationStyle">@style/Animation.AppCompat.DropDownUp</item>
                    <item name="android:itemTextAppearance">?android:textAppearanceMedium</item>
                </style>
                <style name="Base.Theme.MaterialComponents.Dialog" parent="@style/Base.V14.Theme.MaterialComponents.Dialog" />
                <style name="Base.Theme.MaterialComponents.Dialog.Alert" parent="@style/Base.Theme.MaterialComponents.Dialog">
                    <item name="windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.MaterialComponents.Dialog.FixedSize" parent="@style/Base.Theme.MaterialComponents.Dialog">
                    <item name="windowFixedHeightMajor">@dimen/abc_dialog_fixed_height_major</item>
                    <item name="windowFixedHeightMinor">@dimen/abc_dialog_fixed_height_minor</item>
                    <item name="windowFixedWidthMajor">@dimen/abc_dialog_fixed_width_major</item>
                    <item name="windowFixedWidthMinor">@dimen/abc_dialog_fixed_width_minor</item>
                </style>
                <style name="Base.Theme.MaterialComponents.Dialog.MinWidth" parent="@style/Base.Theme.MaterialComponents.Dialog">
                    <item name="windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.MaterialComponents.DialogWhenLarge" parent="@style/Theme.MaterialComponents" />
                <style name="Base.Theme.MaterialComponents.Light" parent="@style/Base.V14.Theme.MaterialComponents.Light" />
                <style name="Base.Theme.MaterialComponents.Light.Bridge" parent="@style/Base.V14.Theme.MaterialComponents.Light.Bridge" />
                <style name="Base.Theme.MaterialComponents.Light.DarkActionBar" parent="@style/Base.Theme.MaterialComponents.Light">
                    <item name="actionBarPopupTheme">@style/ThemeOverlay.MaterialComponents.Light</item>
                    <item name="actionBarTheme">@style/ThemeOverlay.MaterialComponents.Dark.ActionBar</item>
                    <item name="actionBarWidgetTheme">@null</item>
                    <item name="colorPrimary">@color/primary_material_dark</item>
                    <item name="colorPrimaryDark">@color/primary_dark_material_dark</item>
                    <item name="listChoiceBackgroundIndicator">@drawable/abc_list_selector_holo_dark</item>
                </style>
                <style name="Base.Theme.MaterialComponents.Light.DarkActionBar.Bridge" parent="@style/Base.V14.Theme.MaterialComponents.Light.DarkActionBar.Bridge" />
                <style name="Base.Theme.MaterialComponents.Light.Dialog" parent="@style/Base.V14.Theme.MaterialComponents.Light.Dialog" />
                <style name="Base.Theme.MaterialComponents.Light.Dialog.Alert" parent="@style/Base.Theme.MaterialComponents.Light.Dialog">
                    <item name="windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.MaterialComponents.Light.Dialog.FixedSize" parent="@style/Base.Theme.MaterialComponents.Light.Dialog">
                    <item name="windowFixedHeightMajor">@dimen/abc_dialog_fixed_height_major</item>
                    <item name="windowFixedHeightMinor">@dimen/abc_dialog_fixed_height_minor</item>
                    <item name="windowFixedWidthMajor">@dimen/abc_dialog_fixed_width_major</item>
                    <item name="windowFixedWidthMinor">@dimen/abc_dialog_fixed_width_minor</item>
                </style>
                <style name="Base.Theme.MaterialComponents.Light.Dialog.MinWidth" parent="@style/Base.Theme.MaterialComponents.Light.Dialog">
                    <item name="windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.Theme.MaterialComponents.Light.DialogWhenLarge" parent="@style/Theme.MaterialComponents.Light" />
                <style name="Base.ThemeOverlay.AppCompat" parent="@style/Platform.ThemeOverlay.AppCompat" />
                <style name="Base.ThemeOverlay.AppCompat.ActionBar" parent="@style/Base.ThemeOverlay.AppCompat">
                    <item name="colorControlNormal">?android:textColorPrimary</item>
                    <item name="searchViewStyle">@style/Widget.AppCompat.SearchView.ActionBar</item>
                </style>
                <style name="Base.ThemeOverlay.AppCompat.Dark" parent="@style/Platform.ThemeOverlay.AppCompat.Dark">
                    <item name="android:colorForeground">@color/foreground_material_dark</item>
                    <item name="android:colorBackground">@color/background_material_dark</item>
                    <item name="android:textColorPrimary">@color/abc_primary_text_material_dark</item>
                    <item name="android:textColorPrimaryDisableOnly">@color/abc_primary_text_disable_only_material_dark</item>
                    <item name="android:textColorSecondary">@color/abc_secondary_text_material_dark</item>
                    <item name="android:textColorPrimaryInverse">@color/abc_primary_text_material_light</item>
                    <item name="android:textColorSecondaryInverse">@color/abc_secondary_text_material_light</item>
                    <item name="android:textColorHintInverse">@color/abc_hint_foreground_material_light</item>
                    <item name="android:windowBackground">@color/background_material_dark</item>
                    <item name="android:textColorHighlight">@color/highlighted_text_material_dark</item>
                    <item name="android:textColorHint">@color/abc_hint_foreground_material_dark</item>
                    <item name="android:colorForegroundInverse">@color/foreground_material_light</item>
                    <item name="android:textColorTertiary">@color/abc_secondary_text_material_dark</item>
                    <item name="android:textColorTertiaryInverse">@color/abc_secondary_text_material_light</item>
                    <item name="android:colorBackgroundCacheHint">@color/abc_background_cache_hint_selector_material_dark</item>
                    <item name="colorBackgroundFloating">@color/background_floating_material_dark</item>
                    <item name="colorButtonNormal">@color/button_material_dark</item>
                    <item name="colorControlHighlight">@color/ripple_material_dark</item>
                    <item name="colorControlNormal">?android:textColorSecondary</item>
                    <item name="colorSwitchThumbNormal">@color/switch_thumb_material_dark</item>
                    <item name="isLightTheme">false</item>
                </style>
                <style name="Base.ThemeOverlay.AppCompat.Dark.ActionBar" parent="@style/Base.ThemeOverlay.AppCompat.Dark">
                    <item name="colorControlNormal">?android:textColorPrimary</item>
                    <item name="searchViewStyle">@style/Widget.AppCompat.SearchView.ActionBar</item>
                </style>
                <style name="Base.ThemeOverlay.AppCompat.Dialog" parent="@style/Base.V21.ThemeOverlay.AppCompat.Dialog" />
                <style name="Base.ThemeOverlay.AppCompat.Dialog.Alert" parent="@style/Base.ThemeOverlay.AppCompat.Dialog">
                    <item name="android:windowMinWidthMajor">@dimen/abc_dialog_min_width_major</item>
                    <item name="android:windowMinWidthMinor">@dimen/abc_dialog_min_width_minor</item>
                </style>
                <style name="Base.ThemeOverlay.AppCompat.Light" parent="@style/Platform.ThemeOverlay.AppCompat.Light">
                    <item name="android:colorForeground">@color/foreground_material_light</item>
                    <item name="android:colorBackground">@color/background_material_light</item>
                    <item name="android:textColorPrimary">@color/abc_primary_text_material_light</item>
                    <item name="android:textColorPrimaryDisableOnly">@color/abc_primary_text_disable_only_material_light</item>
                    <item name="android:textColorSecondary">@color/abc_secondary_text_material_light</item>
                    <item name="android:textColorPrimaryInverse">@color/abc_primary_text_material_dark</item>
                    <item name="android:textColorSecondaryInverse">@color/abc_secondary_text_material_dark</item>
                    <item name="android:textColorHintInverse">@color/abc_hint_foreground_material_dark</item>
                    <item name="android:windowBackground">@color/background_material_light</item>
                    <item name="android:textColorHighlight">@color/highlighted_text_material_light</item>
                    <item name="android:textColorHint">@color/abc_hint_foreground_material_light</item>
                    <item name="android:colorForegroundInverse">@color/foreground_material_dark</item>
                    <item name="android:textColorTertiary">@color/abc_secondary_text_material_light</item>
                    <item name="android:textColorTertiaryInverse">@color/abc_secondary_text_material_dark</item>
                    <item name="android:textColorPrimaryInverseDisableOnly">@color/abc_primary_text_disable_only_material_dark</item>
                    <item name="android:colorBackgroundCacheHint">@color/abc_background_cache_hint_selector_material_light</item>
                    <item name="colorBackgroundFloating">@color/background_floating_material_light</item>
                    <item name="colorButtonNormal">@color/button_material_light</item>
                    <item name="colorControlHighlight">@color/ripple_material_light</item>
                    <item name="colorControlNormal">?android:textColorSecondary</item>
                    <item name="colorSwitchThumbNormal">@color/switch_thumb_material_light</item>
                    <item name="isLightTheme">true</item>
                </style>
                <style name="Base.ThemeOverlay.MaterialComponents.Dialog" parent="@style/Base.V14.ThemeOverlay.MaterialComponents.Dialog" />
                <style name="Base.ThemeOverlay.MaterialComponents.Dialog.Alert" parent="@style/Base.V14.ThemeOverlay.MaterialComponents.Dialog.Alert" />
                <style name="Base.V14.Theme.MaterialComponents" parent="@style/Base.V14.Theme.MaterialComponents.Bridge">
                    <item name="android:timePickerDialogTheme">@style/ThemeOverlay.MaterialComponents.Dialog</item>
                    <item name="android:datePickerDialogTheme">@style/ThemeOverlay.MaterialComponents.Dialog</item>
                    <item name="alertDialogTheme">@style/ThemeOverlay.MaterialComponents.Dialog.Alert</item>
                    <item name="borderlessButtonStyle">@style/Widget.MaterialComponents.Button.TextButton</item>
                    <item name="bottomAppBarStyle">@style/Widget.MaterialComponents.BottomAppBar</item>
                    <item name="bottomNavigationStyle">@style/Widget.MaterialComponents.BottomNavigationView.Colored</item>
                    <item name="chipGroupStyle">@style/Widget.MaterialComponents.ChipGroup</item>
                    <item name="chipStandaloneStyle">@style/Widget.MaterialComponents.Chip.Entry</item>
                    <item name="chipStyle">@style/Widget.MaterialComponents.Chip.Action</item>
                    <item name="colorAccent">?colorSecondary</item>
                    <item name="colorPrimary">@color/design_default_color_primary</item>
                    <item name="colorPrimaryDark">@color/design_default_color_primary_dark</item>
                    <item name="floatingActionButtonStyle">@style/Widget.MaterialComponents.FloatingActionButton</item>
                    <item name="materialButtonStyle">@style/Widget.MaterialComponents.Button</item>
                    <item name="materialCardViewStyle">@style/Widget.MaterialComponents.CardView</item>
                    <item name="navigationViewStyle">@style/Widget.MaterialComponents.NavigationView</item>
                    <item name="snackbarButtonStyle">?borderlessButtonStyle</item>
                    <item name="snackbarStyle">@style/Widget.MaterialComponents.Snackbar</item>
                    <item name="tabStyle">@style/Widget.MaterialComponents.TabLayout.Colored</item>
                    <item name="textInputStyle">@style/Widget.Design.TextInputLayout</item>
                    <item name="toolbarStyle">@style/Widget.MaterialComponents.Toolbar</item>
                    <item name="viewInflaterClass">android.support.design.theme.MaterialComponentsViewInflater</item>
                </style>
                <style name="Base.V14.Theme.MaterialComponents.Bridge" parent="@style/Platform.MaterialComponents">
                    <item name="colorSecondary">?colorPrimary</item>
                    <item name="scrimBackground">@color/mtrl_scrim_color</item>
                    <item name="textAppearanceBody1">@style/TextAppearance.MaterialComponents.Body1</item>
                    <item name="textAppearanceBody2">@style/TextAppearance.MaterialComponents.Body2</item>
                    <item name="textAppearanceButton">@style/TextAppearance.MaterialComponents.Button</item>
                    <item name="textAppearanceCaption">@style/TextAppearance.MaterialComponents.Caption</item>
                    <item name="textAppearanceHeadline1">@style/TextAppearance.MaterialComponents.Headline1</item>
                    <item name="textAppearanceHeadline2">@style/TextAppearance.MaterialComponents.Headline2</item>
                    <item name="textAppearanceHeadline3">@style/TextAppearance.MaterialComponents.Headline3</item>
                    <item name="textAppearanceHeadline4">@style/TextAppearance.MaterialComponents.Headline4</item>
                    <item name="textAppearanceHeadline5">@style/TextAppearance.MaterialComponents.Headline5</item>
                    <item name="textAppearanceHeadline6">@style/TextAppearance.MaterialComponents.Headline6</item>
                    <item name="textAppearanceOverline">@style/TextAppearance.MaterialComponents.Overline</item>
                    <item name="textAppearanceSubtitle1">@style/TextAppearance.MaterialComponents.Subtitle1</item>
                    <item name="textAppearanceSubtitle2">@style/TextAppearance.MaterialComponents.Subtitle2</item>
                </style>
                <style name="Base.V14.Theme.MaterialComponents.Dialog" parent="@style/Platform.MaterialComponents.Dialog">
                    <item name="borderlessButtonStyle">@style/Widget.MaterialComponents.Button.TextButton</item>
                    <item name="bottomAppBarStyle">@style/Widget.MaterialComponents.BottomAppBar</item>
                    <item name="bottomNavigationStyle">@style/Widget.MaterialComponents.BottomNavigationView.Colored</item>
                    <item name="buttonBarButtonStyle">@style/Widget.MaterialComponents.Button.TextButton.Dialog</item>
                    <item name="chipGroupStyle">@style/Widget.MaterialComponents.ChipGroup</item>
                    <item name="chipStandaloneStyle">@style/Widget.MaterialComponents.Chip.Entry</item>
                    <item name="chipStyle">@style/Widget.MaterialComponents.Chip.Action</item>
                    <item name="colorAccent">?colorSecondary</item>
                    <item name="colorPrimary">@color/design_default_color_primary</item>
                    <item name="colorPrimaryDark">@color/design_default_color_primary_dark</item>
                    <item name="colorSecondary">?colorPrimary</item>
                    <item name="floatingActionButtonStyle">@style/Widget.MaterialComponents.FloatingActionButton</item>
                    <item name="materialButtonStyle">@style/Widget.MaterialComponents.Button</item>
                    <item name="materialCardViewStyle">@style/Widget.MaterialComponents.CardView</item>
                    <item name="navigationViewStyle">@style/Widget.MaterialComponents.NavigationView</item>
                    <item name="scrimBackground">@color/mtrl_scrim_color</item>
                    <item name="snackbarButtonStyle">?borderlessButtonStyle</item>
                    <item name="snackbarStyle">@style/Widget.MaterialComponents.Snackbar</item>
                    <item name="tabStyle">@style/Widget.MaterialComponents.TabLayout.Colored</item>
                    <item name="textAppearanceBody1">@style/TextAppearance.MaterialComponents.Body1</item>
                    <item name="textAppearanceBody2">@style/TextAppearance.MaterialComponents.Body2</item>
                    <item name="textAppearanceButton">@style/TextAppearance.MaterialComponents.Button</item>
                    <item name="textAppearanceCaption">@style/TextAppearance.MaterialComponents.Caption</item>
                    <item name="textAppearanceHeadline1">@style/TextAppearance.MaterialComponents.Headline1</item>
                    <item name="textAppearanceHeadline2">@style/TextAppearance.MaterialComponents.Headline2</item>
                    <item name="textAppearanceHeadline3">@style/TextAppearance.MaterialComponents.Headline3</item>
                    <item name="textAppearanceHeadline4">@style/TextAppearance.MaterialComponents.Headline4</item>
                    <item name="textAppearanceHeadline5">@style/TextAppearance.MaterialComponents.Headline5</item>
                    <item name="textAppearanceHeadline6">@style/TextAppearance.MaterialComponents.Headline6</item>
                    <item name="textAppearanceOverline">@style/TextAppearance.MaterialComponents.Overline</item>
                    <item name="textAppearanceSubtitle1">@style/TextAppearance.MaterialComponents.Subtitle1</item>
                    <item name="textAppearanceSubtitle2">@style/TextAppearance.MaterialComponents.Subtitle2</item>
                    <item name="textInputStyle">@style/Widget.Design.TextInputLayout</item>
                    <item name="toolbarStyle">@style/Widget.MaterialComponents.Toolbar</item>
                    <item name="viewInflaterClass">android.support.design.theme.MaterialComponentsViewInflater</item>
                </style>
                <style name="Base.V14.Theme.MaterialComponents.Light" parent="@style/Base.V14.Theme.MaterialComponents.Light.Bridge">
                    <item name="android:timePickerDialogTheme">@style/ThemeOverlay.MaterialComponents.Dialog</item>
                    <item name="android:datePickerDialogTheme">@style/ThemeOverlay.MaterialComponents.Dialog</item>
                    <item name="alertDialogTheme">@style/ThemeOverlay.MaterialComponents.Dialog.Alert</item>
                    <item name="borderlessButtonStyle">@style/Widget.MaterialComponents.Button.TextButton</item>
                    <item name="bottomAppBarStyle">@style/Widget.MaterialComponents.BottomAppBar</item>
                    <item name="bottomNavigationStyle">@style/Widget.MaterialComponents.BottomNavigationView</item>
                    <item name="chipGroupStyle">@style/Widget.MaterialComponents.ChipGroup</item>
                    <item name="chipStandaloneStyle">@style/Widget.MaterialComponents.Chip.Entry</item>
                    <item name="chipStyle">@style/Widget.MaterialComponents.Chip.Action</item>
                    <item name="colorAccent">?colorSecondary</item>
                    <item name="colorPrimary">@color/design_default_color_primary</item>
                    <item name="colorPrimaryDark">@color/design_default_color_primary_dark</item>
                    <item name="floatingActionButtonStyle">@style/Widget.MaterialComponents.FloatingActionButton</item>
                    <item name="materialButtonStyle">@style/Widget.MaterialComponents.Button</item>
                    <item name="materialCardViewStyle">@style/Widget.MaterialComponents.CardView</item>
                    <item name="navigationViewStyle">@style/Widget.MaterialComponents.NavigationView</item>
                    <item name="snackbarButtonStyle">?borderlessButtonStyle</item>
                    <item name="snackbarStyle">@style/Widget.MaterialComponents.Snackbar</item>
                    <item name="tabStyle">@style/Widget.MaterialComponents.TabLayout</item>
                    <item name="textInputStyle">@style/Widget.Design.TextInputLayout</item>
                    <item name="toolbarStyle">@style/Widget.MaterialComponents.Toolbar</item>
                    <item name="viewInflaterClass">android.support.design.theme.MaterialComponentsViewInflater</item>
                </style>
                <style name="Base.V14.Theme.MaterialComponents.Light.Bridge" parent="@style/Platform.MaterialComponents.Light">
                    <item name="colorSecondary">?colorPrimary</item>
                    <item name="scrimBackground">@color/mtrl_scrim_color</item>
                    <item name="textAppearanceBody1">@style/TextAppearance.MaterialComponents.Body1</item>
                    <item name="textAppearanceBody2">@style/TextAppearance.MaterialComponents.Body2</item>
                    <item name="textAppearanceButton">@style/TextAppearance.MaterialComponents.Button</item>
                    <item name="textAppearanceCaption">@style/TextAppearance.MaterialComponents.Caption</item>
                    <item name="textAppearanceHeadline1">@style/TextAppearance.MaterialComponents.Headline1</item>
                    <item name="textAppearanceHeadline2">@style/TextAppearance.MaterialComponents.Headline2</item>
                    <item name="textAppearanceHeadline3">@style/TextAppearance.MaterialComponents.Headline3</item>
                    <item name="textAppearanceHeadline4">@style/TextAppearance.MaterialComponents.Headline4</item>
                    <item name="textAppearanceHeadline5">@style/TextAppearance.MaterialComponents.Headline5</item>
                    <item name="textAppearanceHeadline6">@style/TextAppearance.MaterialComponents.Headline6</item>
                    <item name="textAppearanceOverline">@style/TextAppearance.MaterialComponents.Overline</item>
                    <item name="textAppearanceSubtitle1">@style/TextAppearance.MaterialComponents.Subtitle1</item>
                    <item name="textAppearanceSubtitle2">@style/TextAppearance.MaterialComponents.Subtitle2</item>
                </style>
                <style name="Base.V14.Theme.MaterialComponents.Light.DarkActionBar.Bridge" parent="@style/Theme.AppCompat.Light.DarkActionBar">
                    <item name="colorSecondary">?colorPrimary</item>
                    <item name="scrimBackground">@color/mtrl_scrim_color</item>
                    <item name="textAppearanceBody1">@style/TextAppearance.MaterialComponents.Body1</item>
                    <item name="textAppearanceBody2">@style/TextAppearance.MaterialComponents.Body2</item>
                    <item name="textAppearanceButton">@style/TextAppearance.MaterialComponents.Button</item>
                    <item name="textAppearanceCaption">@style/TextAppearance.MaterialComponents.Caption</item>
                    <item name="textAppearanceHeadline1">@style/TextAppearance.MaterialComponents.Headline1</item>
                    <item name="textAppearanceHeadline2">@style/TextAppearance.MaterialComponents.Headline2</item>
                    <item name="textAppearanceHeadline3">@style/TextAppearance.MaterialComponents.Headline3</item>
                    <item name="textAppearanceHeadline4">@style/TextAppearance.MaterialComponents.Headline4</item>
                    <item name="textAppearanceHeadline5">@style/TextAppearance.MaterialComponents.Headline5</item>
                    <item name="textAppearanceHeadline6">@style/TextAppearance.MaterialComponents.Headline6</item>
                    <item name="textAppearanceOverline">@style/TextAppearance.MaterialComponents.Overline</item>
                    <item name="textAppearanceSubtitle1">@style/TextAppearance.MaterialComponents.Subtitle1</item>
                    <item name="textAppearanceSubtitle2">@style/TextAppearance.MaterialComponents.Subtitle2</item>
                </style>
                <style name="Base.V14.Theme.MaterialComponents.Light.Dialog" parent="@style/Platform.MaterialComponents.Light.Dialog">
                    <item name="borderlessButtonStyle">@style/Widget.MaterialComponents.Button.TextButton</item>
                    <item name="bottomAppBarStyle">@style/Widget.MaterialComponents.BottomAppBar</item>
                    <item name="bottomNavigationStyle">@style/Widget.MaterialComponents.BottomNavigationView</item>
                    <item name="buttonBarButtonStyle">@style/Widget.MaterialComponents.Button.TextButton.Dialog</item>
                    <item name="chipGroupStyle">@style/Widget.MaterialComponents.ChipGroup</item>
                    <item name="chipStandaloneStyle">@style/Widget.MaterialComponents.Chip.Entry</item>
                    <item name="chipStyle">@style/Widget.MaterialComponents.Chip.Action</item>
                    <item name="colorAccent">?colorSecondary</item>
                    <item name="colorPrimary">@color/design_default_color_primary</item>
                    <item name="colorPrimaryDark">@color/design_default_color_primary_dark</item>
                    <item name="colorSecondary">?colorPrimary</item>
                    <item name="floatingActionButtonStyle">@style/Widget.MaterialComponents.FloatingActionButton</item>
                    <item name="materialButtonStyle">@style/Widget.MaterialComponents.Button</item>
                    <item name="materialCardViewStyle">@style/Widget.MaterialComponents.CardView</item>
                    <item name="navigationViewStyle">@style/Widget.MaterialComponents.NavigationView</item>
                    <item name="scrimBackground">@color/mtrl_scrim_color</item>
                    <item name="snackbarButtonStyle">?borderlessButtonStyle</item>
                    <item name="snackbarStyle">@style/Widget.MaterialComponents.Snackbar</item>
                    <item name="tabStyle">@style/Widget.MaterialComponents.TabLayout</item>
                    <item name="textAppearanceBody1">@style/TextAppearance.MaterialComponents.Body1</item>
                    <item name="textAppearanceBody2">@style/TextAppearance.MaterialComponents.Body2</item>
                    <item name="textAppearanceButton">@style/TextAppearance.MaterialComponents.Button</item>
                    <item name="textAppearanceCaption">@style/TextAppearance.MaterialComponents.Caption</item>
                    <item name="textAppearanceHeadline1">@style/TextAppearance.MaterialComponents.Headline1</item>
                    <item name="textAppearanceHeadline2">@style/TextAppearance.MaterialComponents.Headline2</item>
                    <item name="textAppearanceHeadline3">@style/TextAppearance.MaterialComponents.Headline3</item>
                    <item name="textAppearanceHeadline4">@style/TextAppearance.MaterialComponents.Headline4</item>
                    <item name="textAppearanceHeadline5">@style/TextAppearance.MaterialComponents.Headline5</item>
                    <item name="textAppearanceHeadline6">@style/TextAppearance.MaterialComponents.Headline6</item>
                    <item name="textAppearanceOverline">@style/TextAppearance.MaterialComponents.Overline</item>
                    <item name="textAppearanceSubtitle1">@style/TextAppearance.MaterialComponents.Subtitle1</item>
                    <item name="textAppearanceSubtitle2">@style/TextAppearance.MaterialComponents.Subtitle2</item>
                    <item name="textInputStyle">@style/Widget.Design.TextInputLayout</item>
                    <item name="toolbarStyle">@style/Widget.MaterialComponents.Toolbar</item>
                    <item name="viewInflaterClass">android.support.design.theme.MaterialComponentsViewInflater</item>
                </style>
                <style name="Base.V14.ThemeOverlay.MaterialComponents.Dialog" parent="@style/ThemeOverlay.AppCompat.Dialog">
                    <item name="materialButtonStyle">@style/Widget.MaterialComponents.Button.TextButton.Dialog</item>
                </style>
                <style name="Base.V14.ThemeOverlay.MaterialComponents.Dialog.Alert" parent="@style/ThemeOverlay.AppCompat.Dialog.Alert">
                    <item name="buttonBarButtonStyle">@style/Widget.MaterialComponents.Button.TextButton.Dialog</item>
                </style>
                <style name="Base.V21.Theme.AppCompat" parent="@style/Base.V7.Theme.AppCompat">
                    <item name="android:colorControlNormal">?colorControlNormal</item>
                    <item name="android:colorControlActivated">?colorControlActivated</item>
                    <item name="android:colorButtonNormal">?colorButtonNormal</item>
                    <item name="android:colorControlHighlight">?colorControlHighlight</item>
                    <item name="android:colorPrimary">?colorPrimary</item>
                    <item name="android:colorPrimaryDark">?colorPrimaryDark</item>
                    <item name="android:colorAccent">?colorAccent</item>
                    <item name="actionBarDivider">?android:actionBarDivider</item>
                    <item name="actionBarItemBackground">@drawable/abc_action_bar_item_background_material</item>
                    <item name="actionBarSize">?android:actionBarSize</item>
                    <item name="actionButtonStyle">?android:actionButtonStyle</item>
                    <item name="actionModeBackground">?android:actionModeBackground</item>
                    <item name="actionModeCloseDrawable">?android:actionModeCloseDrawable</item>
                    <item name="actionOverflowButtonStyle">?android:actionOverflowButtonStyle</item>
                    <item name="borderlessButtonStyle">?android:borderlessButtonStyle</item>
                    <item name="buttonStyle">?android:buttonStyle</item>
                    <item name="buttonStyleSmall">?android:buttonStyleSmall</item>
                    <item name="checkboxStyle">?android:checkboxStyle</item>
                    <item name="checkedTextViewStyle">?android:checkedTextViewStyle</item>
                    <item name="dividerHorizontal">?android:dividerHorizontal</item>
                    <item name="dividerVertical">?android:dividerVertical</item>
                    <item name="editTextBackground">@drawable/abc_edit_text_material</item>
                    <item name="editTextColor">?android:editTextColor</item>
                    <item name="homeAsUpIndicator">?android:homeAsUpIndicator</item>
                    <item name="listChoiceBackgroundIndicator">?android:listChoiceBackgroundIndicator</item>
                    <item name="listPreferredItemHeightSmall">?android:listPreferredItemHeightSmall</item>
                    <item name="radioButtonStyle">?android:radioButtonStyle</item>
                    <item name="ratingBarStyle">?android:ratingBarStyle</item>
                    <item name="selectableItemBackground">?android:selectableItemBackground</item>
                    <item name="selectableItemBackgroundBorderless">?android:selectableItemBackgroundBorderless</item>
                    <item name="spinnerStyle">?android:spinnerStyle</item>
                    <item name="textAppearanceLargePopupMenu">?android:textAppearanceLargePopupMenu</item>
                    <item name="textAppearanceSmallPopupMenu">?android:textAppearanceSmallPopupMenu</item>
                </style>
                <style name="Base.V21.Theme.AppCompat.Dialog" parent="@style/Base.V7.Theme.AppCompat.Dialog">
                    <item name="android:windowElevation">@dimen/abc_floating_window_z</item>
                </style>
                <style name="Base.V21.Theme.AppCompat.Light" parent="@style/Base.V7.Theme.AppCompat.Light">
                    <item name="android:colorControlNormal">?colorControlNormal</item>
                    <item name="android:colorControlActivated">?colorControlActivated</item>
                    <item name="android:colorButtonNormal">?colorButtonNormal</item>
                    <item name="android:colorControlHighlight">?colorControlHighlight</item>
                    <item name="android:colorPrimary">?colorPrimary</item>
                    <item name="android:colorPrimaryDark">?colorPrimaryDark</item>
                    <item name="android:colorAccent">?colorAccent</item>
                    <item name="actionBarDivider">?android:actionBarDivider</item>
                    <item name="actionBarItemBackground">@drawable/abc_action_bar_item_background_material</item>
                    <item name="actionBarSize">?android:actionBarSize</item>
                    <item name="actionButtonStyle">?android:actionButtonStyle</item>
                    <item name="actionModeBackground">?android:actionModeBackground</item>
                    <item name="actionModeCloseDrawable">?android:actionModeCloseDrawable</item>
                    <item name="actionOverflowButtonStyle">?android:actionOverflowButtonStyle</item>
                    <item name="borderlessButtonStyle">?android:borderlessButtonStyle</item>
                    <item name="buttonStyle">?android:buttonStyle</item>
                    <item name="buttonStyleSmall">?android:buttonStyleSmall</item>
                    <item name="checkboxStyle">?android:checkboxStyle</item>
                    <item name="checkedTextViewStyle">?android:checkedTextViewStyle</item>
                    <item name="dividerHorizontal">?android:dividerHorizontal</item>
                    <item name="dividerVertical">?android:dividerVertical</item>
                    <item name="editTextBackground">@drawable/abc_edit_text_material</item>
                    <item name="editTextColor">?android:editTextColor</item>
                    <item name="homeAsUpIndicator">?android:homeAsUpIndicator</item>
                    <item name="listChoiceBackgroundIndicator">?android:listChoiceBackgroundIndicator</item>
                    <item name="listPreferredItemHeightSmall">?android:listPreferredItemHeightSmall</item>
                    <item name="radioButtonStyle">?android:radioButtonStyle</item>
                    <item name="ratingBarStyle">?android:ratingBarStyle</item>
                    <item name="selectableItemBackground">?android:selectableItemBackground</item>
                    <item name="selectableItemBackgroundBorderless">?android:selectableItemBackgroundBorderless</item>
                    <item name="spinnerStyle">?android:spinnerStyle</item>
                    <item name="textAppearanceLargePopupMenu">?android:textAppearanceLargePopupMenu</item>
                    <item name="textAppearanceSmallPopupMenu">?android:textAppearanceSmallPopupMenu</item>
                </style>
                <style name="Base.V21.Theme.AppCompat.Light.Dialog" parent="@style/Base.V7.Theme.AppCompat.Light.Dialog">
                    <item name="android:windowElevation">@dimen/abc_floating_window_z</item>
                </style>
                <style name="Base.V21.ThemeOverlay.AppCompat.Dialog" parent="@style/Base.V7.ThemeOverlay.AppCompat.Dialog">
                    <item name="android:windowElevation">@dimen/abc_floating_window_z</item>
                </style>
                <style name="Base.V7.Theme.AppCompat" parent="@style/Platform.AppCompat">
                    <item name="android:panelBackground">@android:color/transparent</item>
                    <item name="android:dropDownListViewStyle">@style/Widget.AppCompat.ListView.DropDown</item>
                    <item name="android:dropDownItemStyle">@style/Widget.AppCompat.DropDownItem.Spinner</item>
                    <item name="android:spinnerItemStyle">@style/Widget.AppCompat.TextView.SpinnerItem</item>
                    <item name="android:textAppearanceButton">@style/TextAppearance.AppCompat.Widget.Button</item>
                    <item name="actionBarDivider">?dividerVertical</item>
                    <item name="actionBarItemBackground">?selectableItemBackgroundBorderless</item>
                    <item name="actionBarPopupTheme">@null</item>
                    <item name="actionBarSize">@dimen/abc_action_bar_default_height_material</item>
                    <item name="actionBarSplitStyle">?actionBarStyle</item>
                    <item name="actionBarStyle">@style/Widget.AppCompat.ActionBar.Solid</item>
                    <item name="actionBarTabBarStyle">@style/Widget.AppCompat.ActionBar.TabBar</item>
                    <item name="actionBarTabStyle">@style/Widget.AppCompat.ActionBar.TabView</item>
                    <item name="actionBarTabTextStyle">@style/Widget.AppCompat.ActionBar.TabText</item>
                    <item name="actionBarTheme">@style/ThemeOverlay.AppCompat.ActionBar</item>
                    <item name="actionBarWidgetTheme">@null</item>
                    <item name="actionButtonStyle">@style/Widget.AppCompat.ActionButton</item>
                    <item name="actionDropDownStyle">@style/Widget.AppCompat.Spinner.DropDown.ActionBar</item>
                    <item name="actionMenuTextAppearance">@style/TextAppearance.AppCompat.Widget.ActionBar.Menu</item>
                    <item name="actionMenuTextColor">?android:textColorPrimaryDisableOnly</item>
                    <item name="actionModeBackground">@drawable/abc_cab_background_top_material</item>
                    <item name="actionModeCloseButtonStyle">@style/Widget.AppCompat.ActionButton.CloseMode</item>
                    <item name="actionModeCloseDrawable">@drawable/abc_ic_ab_back_material</item>
                    <item name="actionModeCopyDrawable">@drawable/abc_ic_menu_copy_mtrl_am_alpha</item>
                    <item name="actionModeCutDrawable">@drawable/abc_ic_menu_cut_mtrl_alpha</item>
                    <item name="actionModePasteDrawable">@drawable/abc_ic_menu_paste_mtrl_am_alpha</item>
                    <item name="actionModeSelectAllDrawable">@drawable/abc_ic_menu_selectall_mtrl_alpha</item>
                    <item name="actionModeShareDrawable">@drawable/abc_ic_menu_share_mtrl_alpha</item>
                    <item name="actionModeSplitBackground">?colorPrimaryDark</item>
                    <item name="actionModeStyle">@style/Widget.AppCompat.ActionMode</item>
                    <item name="actionOverflowButtonStyle">@style/Widget.AppCompat.ActionButton.Overflow</item>
                    <item name="actionOverflowMenuStyle">@style/Widget.AppCompat.PopupMenu.Overflow</item>
                    <item name="activityChooserViewStyle">@style/Widget.AppCompat.ActivityChooserView</item>
                    <item name="alertDialogCenterButtons">false</item>
                    <item name="alertDialogStyle">@style/AlertDialog.AppCompat</item>
                    <item name="alertDialogTheme">@style/ThemeOverlay.AppCompat.Dialog.Alert</item>
                    <item name="autoCompleteTextViewStyle">@style/Widget.AppCompat.AutoCompleteTextView</item>
                    <item name="borderlessButtonStyle">@style/Widget.AppCompat.Button.Borderless</item>
                    <item name="buttonBarButtonStyle">@style/Widget.AppCompat.Button.ButtonBar.AlertDialog</item>
                    <item name="buttonBarNegativeButtonStyle">?buttonBarButtonStyle</item>
                    <item name="buttonBarNeutralButtonStyle">?buttonBarButtonStyle</item>
                    <item name="buttonBarPositiveButtonStyle">?buttonBarButtonStyle</item>
                    <item name="buttonBarStyle">@style/Widget.AppCompat.ButtonBar</item>
                    <item name="buttonStyle">@style/Widget.AppCompat.Button</item>
                    <item name="buttonStyleSmall">@style/Widget.AppCompat.Button.Small</item>
                    <item name="checkboxStyle">@style/Widget.AppCompat.CompoundButton.CheckBox</item>
                    <item name="colorAccent">@color/accent_material_dark</item>
                    <item name="colorBackgroundFloating">@color/background_floating_material_dark</item>
                    <item name="colorButtonNormal">@color/button_material_dark</item>
                    <item name="colorControlActivated">?colorAccent</item>
                    <item name="colorControlHighlight">@color/ripple_material_dark</item>
                    <item name="colorControlNormal">?android:textColorSecondary</item>
                    <item name="colorError">@color/error_color_material_dark</item>
                    <item name="colorPrimary">@color/primary_material_dark</item>
                    <item name="colorPrimaryDark">@color/primary_dark_material_dark</item>
                    <item name="colorSwitchThumbNormal">@color/switch_thumb_material_dark</item>
                    <item name="controlBackground">?selectableItemBackgroundBorderless</item>
                    <item name="dialogCornerRadius">@dimen/abc_dialog_corner_radius_material</item>
                    <item name="dialogPreferredPadding">@dimen/abc_dialog_padding_material</item>
                    <item name="dialogTheme">@style/ThemeOverlay.AppCompat.Dialog</item>
                    <item name="dividerHorizontal">@drawable/abc_list_divider_mtrl_alpha</item>
                    <item name="dividerVertical">@drawable/abc_list_divider_mtrl_alpha</item>
                    <item name="drawerArrowStyle">@style/Widget.AppCompat.DrawerArrowToggle</item>
                    <item name="dropDownListViewStyle">?android:dropDownListViewStyle</item>
                    <item name="dropdownListPreferredItemHeight">?listPreferredItemHeightSmall</item>
                    <item name="editTextBackground">@drawable/abc_edit_text_material</item>
                    <item name="editTextColor">?android:textColorPrimary</item>
                    <item name="editTextStyle">@style/Widget.AppCompat.EditText</item>
                    <item name="homeAsUpIndicator">@drawable/abc_ic_ab_back_material</item>
                    <item name="imageButtonStyle">@style/Widget.AppCompat.ImageButton</item>
                    <item name="isLightTheme">false</item>
                    <item name="listChoiceBackgroundIndicator">@drawable/abc_list_selector_holo_dark</item>
                    <item name="listDividerAlertDialog">@null</item>
                    <item name="listMenuViewStyle">@style/Widget.AppCompat.ListMenuView</item>
                    <item name="listPopupWindowStyle">@style/Widget.AppCompat.ListPopupWindow</item>
                    <item name="listPreferredItemHeight">64.0dip</item>
                    <item name="listPreferredItemHeightLarge">80.0dip</item>
                    <item name="listPreferredItemHeightSmall">48.0dip</item>
                    <item name="listPreferredItemPaddingLeft">@dimen/abc_list_item_padding_horizontal_material</item>
                    <item name="listPreferredItemPaddingRight">@dimen/abc_list_item_padding_horizontal_material</item>
                    <item name="panelBackground">@drawable/abc_menu_hardkey_panel_mtrl_mult</item>
                    <item name="panelMenuListTheme">@style/Theme.AppCompat.CompactMenu</item>
                    <item name="panelMenuListWidth">@dimen/abc_panel_menu_list_width</item>
                    <item name="popupMenuStyle">@style/Widget.AppCompat.PopupMenu</item>
                    <item name="radioButtonStyle">@style/Widget.AppCompat.CompoundButton.RadioButton</item>
                    <item name="ratingBarStyle">@style/Widget.AppCompat.RatingBar</item>
                    <item name="ratingBarStyleIndicator">@style/Widget.AppCompat.RatingBar.Indicator</item>
                    <item name="ratingBarStyleSmall">@style/Widget.AppCompat.RatingBar.Small</item>
                    <item name="searchViewStyle">@style/Widget.AppCompat.SearchView</item>
                    <item name="seekBarStyle">@style/Widget.AppCompat.SeekBar</item>
                    <item name="selectableItemBackground">@drawable/abc_item_background_holo_dark</item>
                    <item name="selectableItemBackgroundBorderless">?selectableItemBackground</item>
                    <item name="spinnerDropDownItemStyle">@style/Widget.AppCompat.DropDownItem.Spinner</item>
                    <item name="spinnerStyle">@style/Widget.AppCompat.Spinner</item>
                    <item name="switchStyle">@style/Widget.AppCompat.CompoundButton.Switch</item>
                    <item name="textAppearanceLargePopupMenu">@style/TextAppearance.AppCompat.Widget.PopupMenu.Large</item>
                    <item name="textAppearanceListItem">@style/TextAppearance.AppCompat.Subhead</item>
                    <item name="textAppearanceListItemSecondary">@style/TextAppearance.AppCompat.Body1</item>
                    <item name="textAppearanceListItemSmall">@style/TextAppearance.AppCompat.Subhead</item>
                    <item name="textAppearancePopupMenuHeader">@style/TextAppearance.AppCompat.Widget.PopupMenu.Header</item>
                    <item name="textAppearanceSearchResultSubtitle">@style/TextAppearance.AppCompat.SearchResult.Subtitle</item>
                    <item name="textAppearanceSearchResultTitle">@style/TextAppearance.AppCompat.SearchResult.Title</item>
                    <item name="textAppearanceSmallPopupMenu">@style/TextAppearance.AppCompat.Widget.PopupMenu.Small</item>
                    <item name="textColorAlertDialogListItem">@color/abc_primary_text_material_dark</item>
                    <item name="textColorSearchUrl">@color/abc_search_url_text</item>
                    <item name="toolbarNavigationButtonStyle">@style/Widget.AppCompat.Toolbar.Button.Navigation</item>
                    <item name="toolbarStyle">@style/Widget.AppCompat.Toolbar</item>
                    <item name="tooltipForegroundColor">@color/foreground_material_light</item>
                    <item name="tooltipFrameBackground">@drawable/tooltip_frame_light</item>
                    <item name="viewInflaterClass">android.support.v7.app.AppCompatViewInflater</item>
                    <item name="windowActionBar">true</item>
                    <item name="windowActionBarOverlay">false</item>
                    <item name="windowActionModeOverlay">false</item>
                    <item name="windowFixedHeightMajor">@null</item>
                    <item name="windowFixedHeightMinor">@null</item>
                    <item name="windowFixedWidthMajor">@null</item>
                    <item name="windowFixedWidthMinor">@null</item>
                    <item name="windowNoTitle">false</item>
                </style>
                <style name="Base.V7.Theme.AppCompat.Dialog" parent="@style/Base.Theme.AppCompat">
                    <item name="android:colorBackground">?colorBackgroundFloating</item>
                    <item name="android:windowBackground">@drawable/abc_dialog_material_background</item>
                    <item name="android:windowFrame">@null</item>
                    <item name="android:windowIsFloating">true</item>
                    <item name="android:windowContentOverlay">@null</item>
                    <item name="android:windowTitleStyle">@style/RtlOverlay.DialogWindowTitle.AppCompat</item>
                    <item name="android:windowTitleBackgroundStyle">@style/Base.DialogWindowTitleBackground.AppCompat</item>
                    <item name="android:windowAnimationStyle">@style/Animation.AppCompat.Dialog</item>
                    <item name="android:listDivider">@null</item>
                    <item name="android:backgroundDimEnabled">true</item>
                    <item name="android:windowSoftInputMode">adjustPan</item>
                    <item name="android:colorBackgroundCacheHint">@null</item>
                    <item name="android:borderlessButtonStyle">@style/Widget.AppCompat.Button.Borderless</item>
                    <item name="android:buttonBarStyle">@style/Widget.AppCompat.ButtonBar.AlertDialog</item>
                    <item name="android:windowCloseOnTouchOutside">true</item>
                    <item name="listPreferredItemPaddingLeft">24.0dip</item>
                    <item name="listPreferredItemPaddingRight">24.0dip</item>
                    <item name="windowActionBar">false</item>
                    <item name="windowActionModeOverlay">true</item>
                </style>
                <style name="Base.V7.Theme.AppCompat.Light" parent="@style/Platform.AppCompat.Light">
                    <item name="android:panelBackground">@android:color/transparent</item>
                    <item name="android:dropDownListViewStyle">@style/Widget.AppCompat.ListView.DropDown</item>
                    <item name="android:dropDownItemStyle">@style/Widget.AppCompat.DropDownItem.Spinner</item>
                    <item name="android:spinnerItemStyle">@style/Widget.AppCompat.TextView.SpinnerItem</item>
                    <item name="android:textAppearanceButton">@style/TextAppearance.AppCompat.Widget.Button</item>
                    <item name="actionBarDivider">?dividerVertical</item>
                    <item name="actionBarItemBackground">?selectableItemBackgroundBorderless</item>
                    <item name="actionBarPopupTheme">@null</item>
                    <item name="actionBarSize">@dimen/abc_action_bar_default_height_material</item>
                    <item name="actionBarSplitStyle">?actionBarStyle</item>
                    <item name="actionBarStyle">@style/Widget.AppCompat.Light.ActionBar.Solid</item>
                    <item name="actionBarTabBarStyle">@style/Widget.AppCompat.Light.ActionBar.TabBar</item>
                    <item name="actionBarTabStyle">@style/Widget.AppCompat.Light.ActionBar.TabView</item>
                    <item name="actionBarTabTextStyle">@style/Widget.AppCompat.Light.ActionBar.TabText</item>
                    <item name="actionBarTheme">@style/ThemeOverlay.AppCompat.ActionBar</item>
                    <item name="actionBarWidgetTheme">@null</item>
                    <item name="actionButtonStyle">@style/Widget.AppCompat.Light.ActionButton</item>
                    <item name="actionDropDownStyle">@style/Widget.AppCompat.Light.Spinner.DropDown.ActionBar</item>
                    <item name="actionMenuTextAppearance">@style/TextAppearance.AppCompat.Widget.ActionBar.Menu</item>
                    <item name="actionMenuTextColor">?android:textColorPrimaryDisableOnly</item>
                    <item name="actionModeBackground">@drawable/abc_cab_background_top_material</item>
                    <item name="actionModeCloseButtonStyle">@style/Widget.AppCompat.ActionButton.CloseMode</item>
                    <item name="actionModeCloseDrawable">@drawable/abc_ic_ab_back_material</item>
                    <item name="actionModeCopyDrawable">@drawable/abc_ic_menu_copy_mtrl_am_alpha</item>
                    <item name="actionModeCutDrawable">@drawable/abc_ic_menu_cut_mtrl_alpha</item>
                    <item name="actionModePasteDrawable">@drawable/abc_ic_menu_paste_mtrl_am_alpha</item>
                    <item name="actionModeSelectAllDrawable">@drawable/abc_ic_menu_selectall_mtrl_alpha</item>
                    <item name="actionModeShareDrawable">@drawable/abc_ic_menu_share_mtrl_alpha</item>
                    <item name="actionModeSplitBackground">?colorPrimaryDark</item>
                    <item name="actionModeStyle">@style/Widget.AppCompat.ActionMode</item>
                    <item name="actionOverflowButtonStyle">@style/Widget.AppCompat.Light.ActionButton.Overflow</item>
                    <item name="actionOverflowMenuStyle">@style/Widget.AppCompat.Light.PopupMenu.Overflow</item>
                    <item name="activityChooserViewStyle">@style/Widget.AppCompat.ActivityChooserView</item>
                    <item name="alertDialogCenterButtons">false</item>
                    <item name="alertDialogStyle">@style/AlertDialog.AppCompat.Light</item>
                    <item name="alertDialogTheme">@style/ThemeOverlay.AppCompat.Dialog.Alert</item>
                    <item name="autoCompleteTextViewStyle">@style/Widget.AppCompat.AutoCompleteTextView</item>
                    <item name="borderlessButtonStyle">@style/Widget.AppCompat.Button.Borderless</item>
                    <item name="buttonBarButtonStyle">@style/Widget.AppCompat.Button.ButtonBar.AlertDialog</item>
                    <item name="buttonBarNegativeButtonStyle">?buttonBarButtonStyle</item>
                    <item name="buttonBarNeutralButtonStyle">?buttonBarButtonStyle</item>
                    <item name="buttonBarPositiveButtonStyle">?buttonBarButtonStyle</item>
                    <item name="buttonBarStyle">@style/Widget.AppCompat.ButtonBar</item>
                    <item name="buttonStyle">@style/Widget.AppCompat.Button</item>
                    <item name="buttonStyleSmall">@style/Widget.AppCompat.Button.Small</item>
                    <item name="checkboxStyle">@style/Widget.AppCompat.CompoundButton.CheckBox</item>
                    <item name="colorAccent">@color/accent_material_light</item>
                    <item name="colorBackgroundFloating">@color/background_floating_material_light</item>
                    <item name="colorButtonNormal">@color/button_material_light</item>
                    <item name="colorControlActivated">?colorAccent</item>
                    <item name="colorControlHighlight">@color/ripple_material_light</item>
                    <item name="colorControlNormal">?android:textColorSecondary</item>
                    <item name="colorError">@color/error_color_material_light</item>
                    <item name="colorPrimary">@color/primary_material_light</item>
                    <item name="colorPrimaryDark">@color/primary_dark_material_light</item>
                    <item name="colorSwitchThumbNormal">@color/switch_thumb_material_light</item>
                    <item name="controlBackground">?selectableItemBackgroundBorderless</item>
                    <item name="dialogCornerRadius">@dimen/abc_dialog_corner_radius_material</item>
                    <item name="dialogPreferredPadding">@dimen/abc_dialog_padding_material</item>
                    <item name="dialogTheme">@style/ThemeOverlay.AppCompat.Dialog</item>
                    <item name="dividerHorizontal">@drawable/abc_list_divider_mtrl_alpha</item>
                    <item name="dividerVertical">@drawable/abc_list_divider_mtrl_alpha</item>
                    <item name="drawerArrowStyle">@style/Widget.AppCompat.DrawerArrowToggle</item>
                    <item name="dropDownListViewStyle">?android:dropDownListViewStyle</item>
                    <item name="dropdownListPreferredItemHeight">?listPreferredItemHeightSmall</item>
                    <item name="editTextBackground">@drawable/abc_edit_text_material</item>
                    <item name="editTextColor">?android:textColorPrimary</item>
                    <item name="editTextStyle">@style/Widget.AppCompat.EditText</item>
                    <item name="homeAsUpIndicator">@drawable/abc_ic_ab_back_material</item>
                    <item name="imageButtonStyle">@style/Widget.AppCompat.ImageButton</item>
                    <item name="isLightTheme">true</item>
                    <item name="listChoiceBackgroundIndicator">@drawable/abc_list_selector_holo_light</item>
                    <item name="listDividerAlertDialog">@null</item>
                    <item name="listMenuViewStyle">@style/Widget.AppCompat.ListMenuView</item>
                    <item name="listPopupWindowStyle">@style/Widget.AppCompat.ListPopupWindow</item>
                    <item name="listPreferredItemHeight">64.0dip</item>
                    <item name="listPreferredItemHeightLarge">80.0dip</item>
                    <item name="listPreferredItemHeightSmall">48.0dip</item>
                    <item name="listPreferredItemPaddingLeft">@dimen/abc_list_item_padding_horizontal_material</item>
                    <item name="listPreferredItemPaddingRight">@dimen/abc_list_item_padding_horizontal_material</item>
                    <item name="panelBackground">@drawable/abc_menu_hardkey_panel_mtrl_mult</item>
                    <item name="panelMenuListTheme">@style/Theme.AppCompat.CompactMenu</item>
                    <item name="panelMenuListWidth">@dimen/abc_panel_menu_list_width</item>
                    <item name="popupMenuStyle">@style/Widget.AppCompat.Light.PopupMenu</item>
                    <item name="radioButtonStyle">@style/Widget.AppCompat.CompoundButton.RadioButton</item>
                    <item name="ratingBarStyle">@style/Widget.AppCompat.RatingBar</item>
                    <item name="ratingBarStyleIndicator">@style/Widget.AppCompat.RatingBar.Indicator</item>
                    <item name="ratingBarStyleSmall">@style/Widget.AppCompat.RatingBar.Small</item>
                    <item name="searchViewStyle">@style/Widget.AppCompat.Light.SearchView</item>
                    <item name="seekBarStyle">@style/Widget.AppCompat.SeekBar</item>
                    <item name="selectableItemBackground">@drawable/abc_item_background_holo_light</item>
                    <item name="selectableItemBackgroundBorderless">?selectableItemBackground</item>
                    <item name="spinnerDropDownItemStyle">@style/Widget.AppCompat.DropDownItem.Spinner</item>
                    <item name="spinnerStyle">@style/Widget.AppCompat.Spinner</item>
                    <item name="switchStyle">@style/Widget.AppCompat.CompoundButton.Switch</item>
                    <item name="textAppearanceLargePopupMenu">@style/TextAppearance.AppCompat.Light.Widget.PopupMenu.Large</item>
                    <item name="textAppearanceListItem">@style/TextAppearance.AppCompat.Subhead</item>
                    <item name="textAppearanceListItemSecondary">@style/TextAppearance.AppCompat.Body1</item>
                    <item name="textAppearanceListItemSmall">@style/TextAppearance.AppCompat.Subhead</item>
                    <item name="textAppearancePopupMenuHeader">@style/TextAppearance.AppCompat.Widget.PopupMenu.Header</item>
                    <item name="textAppearanceSearchResultSubtitle">@style/TextAppearance.AppCompat.SearchResult.Subtitle</item>
                    <item name="textAppearanceSearchResultTitle">@style/TextAppearance.AppCompat.SearchResult.Title</item>
                    <item name="textAppearanceSmallPopupMenu">@style/TextAppearance.AppCompat.Light.Widget.PopupMenu.Small</item>
                    <item name="textColorAlertDialogListItem">@color/abc_primary_text_material_light</item>
                    <item name="textColorSearchUrl">@color/abc_search_url_text</item>
                    <item name="toolbarNavigationButtonStyle">@style/Widget.AppCompat.Toolbar.Button.Navigation</item>
                    <item name="toolbarStyle">@style/Widget.AppCompat.Toolbar</item>
                    <item name="tooltipForegroundColor">@color/foreground_material_dark</item>
                    <item name="tooltipFrameBackground">@drawable/tooltip_frame_dark</item>
                    <item name="viewInflaterClass">android.support.v7.app.AppCompatViewInflater</item>
                    <item name="windowActionBar">true</item>
                    <item name="windowActionBarOverlay">false</item>
                    <item name="windowActionModeOverlay">false</item>
                    <item name="windowFixedHeightMajor">@null</item>
                    <item name="windowFixedHeightMinor">@null</item>
                    <item name="windowFixedWidthMajor">@null</item>
                    <item name="windowFixedWidthMinor">@null</item>
                    <item name="windowNoTitle">false</item>
                </style>
                <style name="Base.V7.Theme.AppCompat.Light.Dialog" parent="@style/Base.Theme.AppCompat.Light">
                    <item name="android:colorBackground">?colorBackgroundFloating</item>
                    <item name="android:windowBackground">@drawable/abc_dialog_material_background</item>
                    <item name="android:windowFrame">@null</item>
                    <item name="android:windowIsFloating">true</item>
                    <item name="android:windowContentOverlay">@null</item>
                    <item name="android:windowTitleStyle">@style/RtlOverlay.DialogWindowTitle.AppCompat</item>
                    <item name="android:windowTitleBackgroundStyle">@style/Base.DialogWindowTitleBackground.AppCompat</item>
                    <item name="android:windowAnimationStyle">@style/Animation.AppCompat.Dialog</item>
                    <item name="android:listDivider">@null</item>
                    <item name="android:backgroundDimEnabled">true</item>
                    <item name="android:windowSoftInputMode">adjustPan</item>
                    <item name="android:colorBackgroundCacheHint">@null</item>
                    <item name="android:borderlessButtonStyle">@style/Widget.AppCompat.Button.Borderless</item>
                    <item name="android:buttonBarStyle">@style/Widget.AppCompat.ButtonBar.AlertDialog</item>
                    <item name="android:windowCloseOnTouchOutside">true</item>
                    <item name="listPreferredItemPaddingLeft">24.0dip</item>
                    <item name="listPreferredItemPaddingRight">24.0dip</item>
                    <item name="windowActionBar">false</item>
                    <item name="windowActionModeOverlay">true</item>
                </style>
                <style name="Base.V7.ThemeOverlay.AppCompat.Dialog" parent="@style/Base.ThemeOverlay.AppCompat">
                    <item name="android:colorBackground">?colorBackgroundFloating</item>
                    <item name="android:windowBackground">@drawable/abc_dialog_material_background</item>
                    <item name="android:windowFrame">@null</item>
                    <item name="android:windowIsFloating">true</item>
                    <item name="android:windowContentOverlay">@null</item>
                    <item name="android:windowTitleStyle">@style/RtlOverlay.DialogWindowTitle.AppCompat</item>
                    <item name="android:windowTitleBackgroundStyle">@style/Base.DialogWindowTitleBackground.AppCompat</item>
                    <item name="android:windowAnimationStyle">@style/Animation.AppCompat.Dialog</item>
                    <item name="android:listDivider">@null</item>
                    <item name="android:backgroundDimEnabled">true</item>
                    <item name="android:windowSoftInputMode">adjustPan</item>
                    <item name="android:colorBackgroundCacheHint">@null</item>
                    <item name="android:borderlessButtonStyle">@style/Widget.AppCompat.Button.Borderless</item>
                    <item name="android:buttonBarStyle">@style/Widget.AppCompat.ButtonBar.AlertDialog</item>
                    <item name="android:windowCloseOnTouchOutside">true</item>
                    <item name="listPreferredItemPaddingLeft">24.0dip</item>
                    <item name="listPreferredItemPaddingRight">24.0dip</item>
                    <item name="windowActionBar">false</item>
                    <item name="windowActionModeOverlay">true</item>
                    <item name="windowFixedHeightMajor">@null</item>
                    <item name="windowFixedHeightMinor">@null</item>
                    <item name="windowFixedWidthMajor">@null</item>
                    <item name="windowFixedWidthMinor">@null</item>
                </style>
                <style name="Base.V7.Widget.AppCompat.AutoCompleteTextView" parent="@android:style/Widget.AutoCompleteTextView">
                    <item name="android:textAppearance">?android:textAppearanceMediumInverse</item>
                    <item name="android:textColor">?editTextColor</item>
                    <item name="android:background">?editTextBackground</item>
                    <item name="android:dropDownSelector">?listChoiceBackgroundIndicator</item>
                    <item name="android:popupBackground">@drawable/abc_popup_background_mtrl_mult</item>
                    <item name="android:textCursorDrawable">@drawable/abc_text_cursor_material</item>
                </style>
                <style name="Base.V7.Widget.AppCompat.EditText" parent="@android:style/Widget.EditText">
                    <item name="android:textAppearance">?android:textAppearanceMediumInverse</item>
                    <item name="android:textColor">?editTextColor</item>
                    <item name="android:background">?editTextBackground</item>
                    <item name="android:textCursorDrawable">@drawable/abc_text_cursor_material</item>
                </style>
                <style name="Base.V7.Widget.AppCompat.Toolbar" parent="@android:style/Widget">
                    <item name="android:paddingLeft">@dimen/abc_action_bar_default_padding_start_material</item>
                    <item name="android:paddingRight">@dimen/abc_action_bar_default_padding_end_material</item>
                    <item name="android:minHeight">?actionBarSize</item>
                    <item name="buttonGravity">top</item>
                    <item name="collapseContentDescription">@string/abc_toolbar_collapse_description</item>
                    <item name="collapseIcon">?homeAsUpIndicator</item>
                    <item name="contentInsetStart">16.0dip</item>
                    <item name="contentInsetStartWithNavigation">@dimen/abc_action_bar_content_inset_with_nav</item>
                    <item name="maxButtonHeight">@dimen/abc_action_bar_default_height_material</item>
                    <item name="subtitleTextAppearance">@style/TextAppearance.Widget.AppCompat.Toolbar.Subtitle</item>
                    <item name="titleMargin">4.0dip</item>
                    <item name="titleTextAppearance">@style/TextAppearance.Widget.AppCompat.Toolbar.Title</item>
                </style>
                <style name="Base.Widget.AppCompat.ActionBar" parent="">
                    <item name="android:gravity">center_vertical</item>
                    <item name="actionButtonStyle">@style/Widget.AppCompat.ActionButton</item>
                    <item name="actionOverflowButtonStyle">@style/Widget.AppCompat.ActionButton.Overflow</item>
                    <item name="background">@null</item>
                    <item name="backgroundSplit">@null</item>
                    <item name="backgroundStacked">@null</item>
                    <item name="contentInsetEnd">@dimen/abc_action_bar_content_inset_material</item>
                    <item name="contentInsetStart">@dimen/abc_action_bar_content_inset_material</item>
                    <item name="contentInsetStartWithNavigation">@dimen/abc_action_bar_content_inset_with_nav</item>
                    <item name="displayOptions">showTitle</item>
                    <item name="divider">?dividerVertical</item>
                    <item name="elevation">@dimen/abc_action_bar_elevation_material</item>
                    <item name="height">?actionBarSize</item>
                    <item name="popupTheme">?actionBarPopupTheme</item>
                    <item name="subtitleTextStyle">@style/TextAppearance.AppCompat.Widget.ActionBar.Subtitle</item>
                    <item name="titleTextStyle">@style/TextAppearance.AppCompat.Widget.ActionBar.Title</item>
                </style>
                <style name="Base.Widget.AppCompat.ActionBar.Solid" parent="@style/Base.Widget.AppCompat.ActionBar">
                    <item name="background">?colorPrimary</item>
                    <item name="backgroundSplit">?colorPrimary</item>
                    <item name="backgroundStacked">?colorPrimary</item>
                </style>
                <style name="Base.Widget.AppCompat.ActionBar.TabBar" parent="">
                    <item name="divider">?actionBarDivider</item>
                    <item name="dividerPadding">8.0dip</item>
                    <item name="showDividers">middle</item>
                </style>
                <style name="Base.Widget.AppCompat.ActionBar.TabText" parent="@android:style/Widget.Material.ActionBar.TabText" />
                <style name="Base.Widget.AppCompat.ActionBar.TabView" parent="@android:style/Widget.Material.ActionBar.TabView" />
                <style name="Base.Widget.AppCompat.ActionButton" parent="@android:style/Widget.Material.ActionButton" />
                <style name="Base.Widget.AppCompat.ActionButton.CloseMode" parent="@android:style/Widget.Material.ActionButton.CloseMode">
                    <item name="android:minWidth">56.0dip</item>
                </style>
                <style name="Base.Widget.AppCompat.ActionButton.Overflow" parent="@android:style/Widget.Material.ActionButton.Overflow" />
                <style name="Base.Widget.AppCompat.ActionMode" parent="">
                    <item name="background">?actionModeBackground</item>
                    <item name="backgroundSplit">?actionModeSplitBackground</item>
                    <item name="closeItemLayout">@layout/abc_action_mode_close_item_material</item>
                    <item name="height">?actionBarSize</item>
                    <item name="subtitleTextStyle">@style/TextAppearance.AppCompat.Widget.ActionMode.Subtitle</item>
                    <item name="titleTextStyle">@style/TextAppearance.AppCompat.Widget.ActionMode.Title</item>
                </style>
                <style name="Base.Widget.AppCompat.ActivityChooserView" parent="">
                    <item name="android:gravity">center</item>
                    <item name="android:background">@drawable/abc_ab_share_pack_mtrl_alpha</item>
                    <item name="divider">?dividerVertical</item>
                    <item name="dividerPadding">6.0dip</item>
                    <item name="showDividers">middle</item>
                </style>
                <style name="Base.Widget.AppCompat.AutoCompleteTextView" parent="@android:style/Widget.Material.AutoCompleteTextView">
                    <item name="android:background">?editTextBackground</item>
                </style>
                <style name="Base.Widget.AppCompat.Button" parent="@android:style/Widget.Material.Button" />
                <style name="Base.Widget.AppCompat.Button.Borderless" parent="@android:style/Widget.Material.Button.Borderless" />
                <style name="Base.Widget.AppCompat.Button.Borderless.Colored" parent="@android:style/Widget.Material.Button.Borderless.Colored">
                    <item name="android:textColor">@color/abc_btn_colored_borderless_text_material</item>
                </style>
                <style name="Base.Widget.AppCompat.Button.ButtonBar.AlertDialog" parent="@style/Widget.AppCompat.Button.Borderless.Colored">
                    <item name="android:minWidth">64.0dip</item>
                    <item name="android:minHeight">@dimen/abc_alert_dialog_button_bar_height</item>
                </style>
                <style name="Base.Widget.AppCompat.Button.Colored" parent="@style/Base.Widget.AppCompat.Button">
                    <item name="android:textAppearance">@style/TextAppearance.AppCompat.Widget.Button.Colored</item>
                    <item name="android:background">@drawable/abc_btn_colored_material</item>
                </style>
                <style name="Base.Widget.AppCompat.Button.Small" parent="@android:style/Widget.Material.Button.Small" />
                <style name="Base.Widget.AppCompat.ButtonBar" parent="@android:style/Widget.Material.ButtonBar" />
                <style name="Base.Widget.AppCompat.ButtonBar.AlertDialog" parent="@style/Base.Widget.AppCompat.ButtonBar" />
                <style name="Base.Widget.AppCompat.CompoundButton.CheckBox" parent="@android:style/Widget.Material.CompoundButton.CheckBox" />
                <style name="Base.Widget.AppCompat.CompoundButton.RadioButton" parent="@android:style/Widget.Material.CompoundButton.RadioButton" />
                <style name="Base.Widget.AppCompat.CompoundButton.Switch" parent="@android:style/Widget.CompoundButton">
                    <item name="android:background">?controlBackground</item>
                    <item name="android:textOn">@string/abc_capital_on</item>
                    <item name="android:textOff">@string/abc_capital_off</item>
                    <item name="android:thumb">@drawable/abc_switch_thumb_material</item>
                    <item name="showText">false</item>
                    <item name="switchPadding">@dimen/abc_switch_padding</item>
                    <item name="switchTextAppearance">@style/TextAppearance.AppCompat.Widget.Switch</item>
                    <item name="track">@drawable/abc_switch_track_mtrl_alpha</item>
                </style>
                <style name="Base.Widget.AppCompat.DrawerArrowToggle" parent="@style/Base.Widget.AppCompat.DrawerArrowToggle.Common">
                    <item name="barLength">18.0dip</item>
                    <item name="drawableSize">24.0dip</item>
                    <item name="gapBetweenBars">3.0dip</item>
                </style>
                <style name="Base.Widget.AppCompat.DrawerArrowToggle.Common" parent="">
                    <item name="arrowHeadLength">8.0dip</item>
                    <item name="arrowShaftLength">16.0dip</item>
                    <item name="color">?android:textColorSecondary</item>
                    <item name="spinBars">true</item>
                    <item name="thickness">2.0dip</item>
                </style>
                <style name="Base.Widget.AppCompat.DropDownItem.Spinner" parent="@android:style/Widget.Material.DropDownItem.Spinner" />
                <style name="Base.Widget.AppCompat.EditText" parent="@android:style/Widget.Material.EditText">
                    <item name="android:background">?editTextBackground</item>
                </style>
                <style name="Base.Widget.AppCompat.ImageButton" parent="@android:style/Widget.Material.ImageButton" />
                <style name="Base.Widget.AppCompat.Light.ActionBar" parent="@style/Base.Widget.AppCompat.ActionBar">
                    <item name="actionButtonStyle">@style/Widget.AppCompat.Light.ActionButton</item>
                    <item name="actionOverflowButtonStyle">@style/Widget.AppCompat.Light.ActionButton.Overflow</item>
                </style>
                <style name="Base.Widget.AppCompat.Light.ActionBar.Solid" parent="@style/Base.Widget.AppCompat.Light.ActionBar">
                    <item name="background">?colorPrimary</item>
                    <item name="backgroundSplit">?colorPrimary</item>
                    <item name="backgroundStacked">?colorPrimary</item>
                </style>
                <style name="Base.Widget.AppCompat.Light.ActionBar.TabBar" parent="@style/Base.Widget.AppCompat.ActionBar.TabBar" />
                <style name="Base.Widget.AppCompat.Light.ActionBar.TabText" parent="@android:style/Widget.Material.Light.ActionBar.TabText" />
                <style name="Base.Widget.AppCompat.Light.ActionBar.TabText.Inverse" parent="@android:style/Widget.Material.Light.ActionBar.TabText" />
                <style name="Base.Widget.AppCompat.Light.ActionBar.TabView" parent="@android:style/Widget.Material.Light.ActionBar.TabView" />
                <style name="Base.Widget.AppCompat.Light.PopupMenu" parent="@android:style/Widget.Material.Light.PopupMenu" />
                <style name="Base.Widget.AppCompat.Light.PopupMenu.Overflow" parent="@style/Base.Widget.AppCompat.Light.PopupMenu">
                    <item name="android:dropDownHorizontalOffset">-4.0dip</item>
                    <item name="android:overlapAnchor">true</item>
                </style>
                <style name="Base.Widget.AppCompat.ListMenuView" parent="@android:style/Widget">
                    <item name="subMenuArrow">@drawable/abc_ic_arrow_drop_right_black_24dp</item>
                </style>
                <style name="Base.Widget.AppCompat.ListPopupWindow" parent="@android:style/Widget.Material.ListPopupWindow" />
                <style name="Base.Widget.AppCompat.ListView" parent="@android:style/Widget.Material.ListView" />
                <style name="Base.Widget.AppCompat.ListView.DropDown" parent="@android:style/Widget.Material.ListView.DropDown" />
                <style name="Base.Widget.AppCompat.ListView.Menu" parent="@style/Base.Widget.AppCompat.ListView" />
                <style name="Base.Widget.AppCompat.PopupMenu" parent="@android:style/Widget.Material.PopupMenu" />
                <style name="Base.Widget.AppCompat.PopupMenu.Overflow" parent="@style/Base.Widget.AppCompat.PopupMenu">
                    <item name="android:dropDownHorizontalOffset">-4.0dip</item>
                    <item name="android:overlapAnchor">true</item>
                </style>
                <style name="Base.Widget.AppCompat.PopupWindow" parent="@android:style/Widget.PopupWindow" />
                <style name="Base.Widget.AppCompat.ProgressBar" parent="@android:style/Widget.Material.ProgressBar" />
                <style name="Base.Widget.AppCompat.ProgressBar.Horizontal" parent="@android:style/Widget.Material.ProgressBar.Horizontal" />
                <style name="Base.Widget.AppCompat.RatingBar" parent="@android:style/Widget.Material.RatingBar" />
                <style name="Base.Widget.AppCompat.RatingBar.Indicator" parent="@android:style/Widget.RatingBar">
                    <item name="android:maxHeight">36.0dip</item>
                    <item name="android:indeterminateDrawable">@drawable/abc_ratingbar_indicator_material</item>
                    <item name="android:progressDrawable">@drawable/abc_ratingbar_indicator_material</item>
                    <item name="android:minHeight">36.0dip</item>
                    <item name="android:thumb">@null</item>
                    <item name="android:isIndicator">true</item>
                </style>
                <style name="Base.Widget.AppCompat.RatingBar.Small" parent="@android:style/Widget.RatingBar">
                    <item name="android:maxHeight">16.0dip</item>
                    <item name="android:indeterminateDrawable">@drawable/abc_ratingbar_small_material</item>
                    <item name="android:progressDrawable">@drawable/abc_ratingbar_small_material</item>
                    <item name="android:minHeight">16.0dip</item>
                    <item name="android:thumb">@null</item>
                    <item name="android:isIndicator">true</item>
                </style>
                <style name="Base.Widget.AppCompat.SearchView" parent="@android:style/Widget">
                    <item name="closeIcon">@drawable/abc_ic_clear_material</item>
                    <item name="commitIcon">@drawable/abc_ic_commit_search_api_mtrl_alpha</item>
                    <item name="goIcon">@drawable/abc_ic_go_search_api_material</item>
                    <item name="layout">@layout/abc_search_view</item>
                    <item name="queryBackground">@drawable/abc_textfield_search_material</item>
                    <item name="searchHintIcon">@drawable/abc_ic_search_api_material</item>
                    <item name="searchIcon">@drawable/abc_ic_search_api_material</item>
                    <item name="submitBackground">@drawable/abc_textfield_search_material</item>
                    <item name="suggestionRowLayout">@layout/abc_search_dropdown_item_icons_2line</item>
                    <item name="voiceIcon">@drawable/abc_ic_voice_search_api_material</item>
                </style>
                <style name="Base.Widget.AppCompat.SearchView.ActionBar" parent="@style/Base.Widget.AppCompat.SearchView">
                    <item name="defaultQueryHint">@string/abc_search_hint</item>
                    <item name="queryBackground">@null</item>
                    <item name="searchHintIcon">@null</item>
                    <item name="submitBackground">@null</item>
                </style>
                <style name="Base.Widget.AppCompat.SeekBar" parent="@android:style/Widget.Material.SeekBar" />
                <style name="Base.Widget.AppCompat.SeekBar.Discrete" parent="@style/Base.Widget.AppCompat.SeekBar">
                    <item name="tickMark">@drawable/abc_seekbar_tick_mark_material</item>
                </style>
                <style name="Base.Widget.AppCompat.Spinner" parent="@android:style/Widget.Material.Spinner" />
                <style name="Base.Widget.AppCompat.Spinner.Underlined" parent="@style/Base.Widget.AppCompat.Spinner">
                    <item name="android:background">@drawable/abc_spinner_textfield_background_material</item>
                </style>
                <style name="Base.Widget.AppCompat.TextView.SpinnerItem" parent="@android:style/Widget.Material.TextView.SpinnerItem" />
                <style name="Base.Widget.AppCompat.Toolbar" parent="@style/Base.V7.Widget.AppCompat.Toolbar" />
                <style name="Base.Widget.AppCompat.Toolbar.Button.Navigation" parent="@android:style/Widget.Material.Toolbar.Button.Navigation" />
                <style name="Base.Widget.Design.TabLayout" parent="@android:style/Widget">
                    <item name="android:background">@null</item>
                    <item name="tabIconTint">@null</item>
                    <item name="tabIndicator">@drawable/mtrl_tabs_default_indicator</item>
                    <item name="tabIndicatorAnimationDuration">@integer/design_tab_indicator_anim_duration_ms</item>
                    <item name="tabIndicatorColor">?colorAccent</item>
                    <item name="tabIndicatorGravity">bottom</item>
                    <item name="tabMaxWidth">@dimen/design_tab_max_width</item>
                    <item name="tabPaddingEnd">12.0dip</item>
                    <item name="tabPaddingStart">12.0dip</item>
                    <item name="tabRippleColor">?colorControlHighlight</item>
                    <item name="tabTextAppearance">@style/TextAppearance.Design.Tab</item>
                    <item name="tabUnboundedRipple">false</item>
                </style>
                <style name="Base.Widget.MaterialComponents.Chip" parent="@android:style/Widget">
                    <item name="android:textAppearance">?textAppearanceBody2</item>
                    <item name="android:textColor">@color/mtrl_chip_text_color</item>
                    <item name="android:focusable">true</item>
                    <item name="android:clickable">true</item>
                    <item name="android:text">@null</item>
                    <item name="android:checkable">false</item>
                    <item name="android:stateListAnimator">@animator/mtrl_chip_state_list_anim</item>
                    <item name="checkedIcon">@drawable/ic_mtrl_chip_checked_circle</item>
                    <item name="checkedIconVisible">true</item>
                    <item name="chipBackgroundColor">@color/mtrl_chip_background_color</item>
                    <item name="chipCornerRadius">16.0dip</item>
                    <item name="chipEndPadding">6.0dip</item>
                    <item name="chipIcon">@null</item>
                    <item name="chipIconSize">24.0dip</item>
                    <item name="chipIconVisible">true</item>
                    <item name="chipMinHeight">32.0dip</item>
                    <item name="chipStartPadding">4.0dip</item>
                    <item name="chipStrokeColor">#00000000</item>
                    <item name="chipStrokeWidth">0.0dip</item>
                    <item name="closeIcon">@drawable/ic_mtrl_chip_close_circle</item>
                    <item name="closeIconEndPadding">2.0dip</item>
                    <item name="closeIconSize">18.0dip</item>
                    <item name="closeIconStartPadding">2.0dip</item>
                    <item name="closeIconTint">@color/mtrl_chip_close_icon_tint</item>
                    <item name="closeIconVisible">true</item>
                    <item name="enforceTextAppearance">true</item>
                    <item name="iconEndPadding">0.0dip</item>
                    <item name="iconStartPadding">0.0dip</item>
                    <item name="rippleColor">@color/mtrl_chip_ripple_color</item>
                    <item name="textEndPadding">6.0dip</item>
                    <item name="textStartPadding">8.0dip</item>
                </style>
                <style name="Base.Widget.MaterialComponents.TextInputEditText" parent="@style/Widget.AppCompat.EditText">
                    <item name="android:paddingLeft">12.0dip</item>
                    <item name="android:paddingTop">16.0dip</item>
                    <item name="android:paddingRight">12.0dip</item>
                    <item name="android:paddingBottom">16.0dip</item>
                    <item name="android:paddingStart">12.0dip</item>
                    <item name="android:paddingEnd">12.0dip</item>
                </style>
                <style name="Base.Widget.MaterialComponents.TextInputLayout" parent="@style/Widget.Design.TextInputLayout">
                    <item name="boxBackgroundColor">@null</item>
                    <item name="boxBackgroundMode">outline</item>
                    <item name="boxCollapsedPaddingTop">0.0dip</item>
                    <item name="boxCornerRadiusBottomEnd">@dimen/mtrl_textinput_box_corner_radius_medium</item>
                    <item name="boxCornerRadiusBottomStart">@dimen/mtrl_textinput_box_corner_radius_medium</item>
                    <item name="boxCornerRadiusTopEnd">@dimen/mtrl_textinput_box_corner_radius_medium</item>
                    <item name="boxCornerRadiusTopStart">@dimen/mtrl_textinput_box_corner_radius_medium</item>
                    <item name="boxStrokeColor">?colorControlActivated</item>
                </style>
                <style name="CardView" parent="@style/Base.CardView" />
                <style name="CardView.Dark" parent="@style/CardView">
                    <item name="cardBackgroundColor">@color/cardview_dark_background</item>
                </style>
                <style name="CardView.Light" parent="@style/CardView">
                    <item name="cardBackgroundColor">@color/cardview_light_background</item>
                </style>
                <style name="LoadingProgressBarStyle" parent="@android:style/Widget.ProgressBar.Small">
                    <item name="android:maxWidth">60.0dip</item>
                    <item name="android:maxHeight">60.0dip</item>
                    <item name="android:indeterminateDrawable">@drawable/icon_loading_loop_anim</item>
                    <item name="android:indeterminateDuration">500</item>
                    <item name="android:minWidth">25.0dip</item>
                    <item name="android:minHeight">25.0dip</item>
                </style>
                <style name="Platform.AppCompat" parent="@style/Platform.V21.AppCompat" />
                <style name="Platform.AppCompat.Light" parent="@style/Platform.V21.AppCompat.Light" />
                <style name="Platform.MaterialComponents" parent="@style/Theme.AppCompat" />
                <style name="Platform.MaterialComponents.Dialog" parent="@style/Theme.AppCompat.Dialog" />
                <style name="Platform.MaterialComponents.Light" parent="@style/Theme.AppCompat.Light" />
                <style name="Platform.MaterialComponents.Light.Dialog" parent="@style/Theme.AppCompat.Light.Dialog" />
                <style name="Platform.ThemeOverlay.AppCompat" parent="">
                    <item name="android:colorControlNormal">?colorControlNormal</item>
                    <item name="android:colorControlActivated">?colorControlActivated</item>
                    <item name="android:colorButtonNormal">?colorButtonNormal</item>
                    <item name="android:colorControlHighlight">?colorControlHighlight</item>
                    <item name="android:colorPrimary">?colorPrimary</item>
                    <item name="android:colorPrimaryDark">?colorPrimaryDark</item>
                    <item name="android:colorAccent">?colorAccent</item>
                </style>
                <style name="Platform.ThemeOverlay.AppCompat.Dark" parent="@style/Platform.ThemeOverlay.AppCompat" />
                <style name="Platform.ThemeOverlay.AppCompat.Light" parent="@style/Platform.ThemeOverlay.AppCompat" />
                <style name="Platform.V21.AppCompat" parent="@android:style/Theme.Material.NoActionBar">
                    <item name="android:textColorHintInverse">@color/abc_hint_foreground_material_light</item>
                    <item name="android:textColorHint">@color/abc_hint_foreground_material_dark</item>
                    <item name="android:textColorLink">?android:colorAccent</item>
                    <item name="android:buttonBarStyle">?buttonBarStyle</item>
                    <item name="android:buttonBarButtonStyle">?buttonBarButtonStyle</item>
                    <item name="android:textColorLinkInverse">?android:colorAccent</item>
                </style>
                <style name="Platform.V21.AppCompat.Light" parent="@android:style/Theme.Material.Light.NoActionBar">
                    <item name="android:textColorHintInverse">@color/abc_hint_foreground_material_dark</item>
                    <item name="android:textColorHint">@color/abc_hint_foreground_material_light</item>
                    <item name="android:textColorLink">?android:colorAccent</item>
                    <item name="android:buttonBarStyle">?buttonBarStyle</item>
                    <item name="android:buttonBarButtonStyle">?buttonBarButtonStyle</item>
                    <item name="android:textColorLinkInverse">?android:colorAccent</item>
                </style>
                <style name="Platform.Widget.AppCompat.Spinner" parent="@android:style/Widget.Holo.Spinner" />
                <style name="PreLoadStyle">
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:windowNoTitle">true</item>
                    <item name="android:windowIsFloating">true</item>
                    <item name="android:windowIsTranslucent">true</item>
                    <item name="android:windowContentOverlay">@null</item>
                    <item name="android:backgroundDimEnabled">true</item>
                </style>
                <style name="RtlOverlay.DialogWindowTitle.AppCompat" parent="@style/Base.DialogWindowTitle.AppCompat">
                    <item name="android:textAlignment">viewStart</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.ActionBar.TitleItem" parent="@android:style/Widget">
                    <item name="android:layout_gravity">start|center</item>
                    <item name="android:paddingEnd">8.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.DialogTitle.Icon" parent="@android:style/Widget">
                    <item name="android:layout_marginEnd">8.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.PopupMenuItem" parent="@android:style/Widget">
                    <item name="android:paddingEnd">16.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.PopupMenuItem.InternalGroup" parent="@android:style/Widget">
                    <item name="android:layout_marginStart">16.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.PopupMenuItem.Shortcut" parent="@android:style/Widget">
                    <item name="android:textAlignment">viewEnd</item>
                    <item name="android:layout_marginStart">16.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.PopupMenuItem.SubmenuArrow" parent="@android:style/Widget">
                    <item name="android:layout_marginStart">8.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.PopupMenuItem.Text" parent="@android:style/Widget">
                    <item name="android:textAlignment">viewStart</item>
                    <item name="android:layout_alignParentStart">true</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.PopupMenuItem.Title" parent="@android:style/Widget">
                    <item name="android:textAlignment">viewStart</item>
                    <item name="android:layout_marginStart">16.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.Search.DropDown" parent="@android:style/Widget">
                    <item name="android:paddingStart">@dimen/abc_dropdownitem_text_padding_left</item>
                    <item name="android:paddingEnd">4.0dip</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.Search.DropDown.Icon1" parent="@android:style/Widget">
                    <item name="android:layout_alignParentStart">true</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.Search.DropDown.Icon2" parent="@android:style/Widget">
                    <item name="android:layout_toStartOf">@id/edit_query</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.Search.DropDown.Query" parent="@android:style/Widget">
                    <item name="android:layout_alignParentEnd">true</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.Search.DropDown.Text" parent="@style/Base.Widget.AppCompat.DropDownItem.Spinner">
                    <item name="android:layout_toStartOf">@android:id/icon2</item>
                    <item name="android:layout_toEndOf">@android:id/icon1</item>
                </style>
                <style name="RtlOverlay.Widget.AppCompat.SearchView.MagIcon" parent="@android:style/Widget">
                    <item name="android:layout_marginStart">@dimen/abc_dropdownitem_text_padding_left</item>
                </style>
                <style name="RtlUnderlay.Widget.AppCompat.ActionButton" parent="@android:style/Widget">
                    <item name="android:paddingStart">12.0dip</item>
                    <item name="android:paddingEnd">12.0dip</item>
                </style>
                <style name="RtlUnderlay.Widget.AppCompat.ActionButton.Overflow" parent="@style/Base.Widget.AppCompat.ActionButton">
                    <item name="android:paddingStart">@dimen/abc_action_bar_overflow_padding_start_material</item>
                    <item name="android:paddingEnd">@dimen/abc_action_bar_overflow_padding_end_material</item>
                </style>
                <style name="TextAppearance.AppCompat" parent="@style/Base.TextAppearance.AppCompat" />
                <style name="TextAppearance.AppCompat.Body1" parent="@style/Base.TextAppearance.AppCompat.Body1" />
                <style name="TextAppearance.AppCompat.Body2" parent="@style/Base.TextAppearance.AppCompat.Body2" />
                <style name="TextAppearance.AppCompat.Button" parent="@style/Base.TextAppearance.AppCompat.Button" />
                <style name="TextAppearance.AppCompat.Caption" parent="@style/Base.TextAppearance.AppCompat.Caption" />
                <style name="TextAppearance.AppCompat.Display1" parent="@style/Base.TextAppearance.AppCompat.Display1" />
                <style name="TextAppearance.AppCompat.Display2" parent="@style/Base.TextAppearance.AppCompat.Display2" />
                <style name="TextAppearance.AppCompat.Display3" parent="@style/Base.TextAppearance.AppCompat.Display3" />
                <style name="TextAppearance.AppCompat.Display4" parent="@style/Base.TextAppearance.AppCompat.Display4" />
                <style name="TextAppearance.AppCompat.Headline" parent="@style/Base.TextAppearance.AppCompat.Headline" />
                <style name="TextAppearance.AppCompat.Inverse" parent="@style/Base.TextAppearance.AppCompat.Inverse" />
                <style name="TextAppearance.AppCompat.Large" parent="@style/Base.TextAppearance.AppCompat.Large" />
                <style name="TextAppearance.AppCompat.Large.Inverse" parent="@style/Base.TextAppearance.AppCompat.Large.Inverse" />
                <style name="TextAppearance.AppCompat.Light.SearchResult.Subtitle" parent="@style/TextAppearance.AppCompat.SearchResult.Subtitle" />
                <style name="TextAppearance.AppCompat.Light.SearchResult.Title" parent="@style/TextAppearance.AppCompat.SearchResult.Title" />
                <style name="TextAppearance.AppCompat.Light.Widget.PopupMenu.Large" parent="@style/TextAppearance.AppCompat.Widget.PopupMenu.Large" />
                <style name="TextAppearance.AppCompat.Light.Widget.PopupMenu.Small" parent="@style/TextAppearance.AppCompat.Widget.PopupMenu.Small" />
                <style name="TextAppearance.AppCompat.Medium" parent="@style/Base.TextAppearance.AppCompat.Medium" />
                <style name="TextAppearance.AppCompat.Medium.Inverse" parent="@style/Base.TextAppearance.AppCompat.Medium.Inverse" />
                <style name="TextAppearance.AppCompat.Menu" parent="@style/Base.TextAppearance.AppCompat.Menu" />
                <style name="TextAppearance.AppCompat.SearchResult.Subtitle" parent="@style/Base.TextAppearance.AppCompat.SearchResult.Subtitle" />
                <style name="TextAppearance.AppCompat.SearchResult.Title" parent="@style/Base.TextAppearance.AppCompat.SearchResult.Title" />
                <style name="TextAppearance.AppCompat.Small" parent="@style/Base.TextAppearance.AppCompat.Small" />
                <style name="TextAppearance.AppCompat.Small.Inverse" parent="@style/Base.TextAppearance.AppCompat.Small.Inverse" />
                <style name="TextAppearance.AppCompat.Subhead" parent="@style/Base.TextAppearance.AppCompat.Subhead" />
                <style name="TextAppearance.AppCompat.Subhead.Inverse" parent="@style/Base.TextAppearance.AppCompat.Subhead.Inverse" />
                <style name="TextAppearance.AppCompat.Title" parent="@style/Base.TextAppearance.AppCompat.Title" />
                <style name="TextAppearance.AppCompat.Title.Inverse" parent="@style/Base.TextAppearance.AppCompat.Title.Inverse" />
                <style name="TextAppearance.AppCompat.Tooltip" parent="@style/TextAppearance.AppCompat">
                    <item name="android:textSize">14.0sp</item>
                    <item name="android:fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.AppCompat.Widget.ActionBar.Menu" parent="@style/Base.TextAppearance.AppCompat.Widget.ActionBar.Menu" />
                <style name="TextAppearance.AppCompat.Widget.ActionBar.Subtitle" parent="@style/Base.TextAppearance.AppCompat.Widget.ActionBar.Subtitle" />
                <style name="TextAppearance.AppCompat.Widget.ActionBar.Subtitle.Inverse" parent="@style/Base.TextAppearance.AppCompat.Widget.ActionBar.Subtitle.Inverse" />
                <style name="TextAppearance.AppCompat.Widget.ActionBar.Title" parent="@style/Base.TextAppearance.AppCompat.Widget.ActionBar.Title" />
                <style name="TextAppearance.AppCompat.Widget.ActionBar.Title.Inverse" parent="@style/Base.TextAppearance.AppCompat.Widget.ActionBar.Title.Inverse" />
                <style name="TextAppearance.AppCompat.Widget.ActionMode.Subtitle" parent="@style/Base.TextAppearance.AppCompat.Widget.ActionMode.Subtitle" />
                <style name="TextAppearance.AppCompat.Widget.ActionMode.Subtitle.Inverse" parent="@style/TextAppearance.AppCompat.Widget.ActionMode.Subtitle" />
                <style name="TextAppearance.AppCompat.Widget.ActionMode.Title" parent="@style/Base.TextAppearance.AppCompat.Widget.ActionMode.Title" />
                <style name="TextAppearance.AppCompat.Widget.ActionMode.Title.Inverse" parent="@style/TextAppearance.AppCompat.Widget.ActionMode.Title" />
                <style name="TextAppearance.AppCompat.Widget.Button" parent="@style/Base.TextAppearance.AppCompat.Widget.Button" />
                <style name="TextAppearance.AppCompat.Widget.Button.Borderless.Colored" parent="@style/Base.TextAppearance.AppCompat.Widget.Button.Borderless.Colored" />
                <style name="TextAppearance.AppCompat.Widget.Button.Colored" parent="@style/Base.TextAppearance.AppCompat.Widget.Button.Colored" />
                <style name="TextAppearance.AppCompat.Widget.Button.Inverse" parent="@style/Base.TextAppearance.AppCompat.Widget.Button.Inverse" />
                <style name="TextAppearance.AppCompat.Widget.DropDownItem" parent="@style/Base.TextAppearance.AppCompat.Widget.DropDownItem" />
                <style name="TextAppearance.AppCompat.Widget.PopupMenu.Header" parent="@style/Base.TextAppearance.AppCompat.Widget.PopupMenu.Header" />
                <style name="TextAppearance.AppCompat.Widget.PopupMenu.Large" parent="@style/Base.TextAppearance.AppCompat.Widget.PopupMenu.Large" />
                <style name="TextAppearance.AppCompat.Widget.PopupMenu.Small" parent="@style/Base.TextAppearance.AppCompat.Widget.PopupMenu.Small" />
                <style name="TextAppearance.AppCompat.Widget.Switch" parent="@style/Base.TextAppearance.AppCompat.Widget.Switch" />
                <style name="TextAppearance.AppCompat.Widget.TextView.SpinnerItem" parent="@style/Base.TextAppearance.AppCompat.Widget.TextView.SpinnerItem" />
                <style name="TextAppearance.Compat.Notification" parent="@android:style/TextAppearance.Material.Notification" />
                <style name="TextAppearance.Compat.Notification.Info" parent="@android:style/TextAppearance.Material.Notification.Info" />
                <style name="TextAppearance.Compat.Notification.Info.Media" parent="@style/TextAppearance.Compat.Notification.Info">
                    <item name="android:textColor">@color/secondary_text_default_material_dark</item>
                </style>
                <style name="TextAppearance.Compat.Notification.Line2" parent="@style/TextAppearance.Compat.Notification.Info" />
                <style name="TextAppearance.Compat.Notification.Line2.Media" parent="@style/TextAppearance.Compat.Notification.Info.Media" />
                <style name="TextAppearance.Compat.Notification.Media" parent="@style/TextAppearance.Compat.Notification">
                    <item name="android:textColor">@color/secondary_text_default_material_dark</item>
                </style>
                <style name="TextAppearance.Compat.Notification.Time" parent="@android:style/TextAppearance.Material.Notification.Time" />
                <style name="TextAppearance.Compat.Notification.Time.Media" parent="@style/TextAppearance.Compat.Notification.Time">
                    <item name="android:textColor">@color/secondary_text_default_material_dark</item>
                </style>
                <style name="TextAppearance.Compat.Notification.Title" parent="@android:style/TextAppearance.Material.Notification.Title" />
                <style name="TextAppearance.Compat.Notification.Title.Media" parent="@style/TextAppearance.Compat.Notification.Title">
                    <item name="android:textColor">@color/primary_text_default_material_dark</item>
                </style>
                <style name="TextAppearance.Design.CollapsingToolbar.Expanded" parent="@style/TextAppearance.AppCompat.Display1">
                    <item name="android:textColor">?android:textColorPrimary</item>
                </style>
                <style name="TextAppearance.Design.Counter" parent="@style/TextAppearance.AppCompat.Caption" />
                <style name="TextAppearance.Design.Counter.Overflow" parent="@style/TextAppearance.AppCompat.Caption">
                    <item name="android:textColor">@color/design_error</item>
                </style>
                <style name="TextAppearance.Design.Error" parent="@style/TextAppearance.AppCompat.Caption">
                    <item name="android:textColor">@color/design_error</item>
                </style>
                <style name="TextAppearance.Design.HelperText" parent="@style/TextAppearance.AppCompat.Caption" />
                <style name="TextAppearance.Design.Hint" parent="@style/TextAppearance.AppCompat.Caption">
                    <item name="android:textColor">?colorControlActivated</item>
                </style>
                <style name="TextAppearance.Design.Snackbar.Message" parent="@android:style/TextAppearance">
                    <item name="android:textSize">@dimen/design_snackbar_text_size</item>
                    <item name="android:textColor">?android:textColorPrimary</item>
                </style>
                <style name="TextAppearance.Design.Tab" parent="@style/TextAppearance.AppCompat.Button">
                    <item name="android:textSize">@dimen/design_tab_text_size</item>
                    <item name="android:textColor">@color/mtrl_tabs_legacy_text_color_selector</item>
                    <item name="textAllCaps">true</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Body1" parent="@style/TextAppearance.AppCompat.Body2">
                    <item name="android:textSize">16.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif</item>
                    <item name="android:letterSpacing">0.03125</item>
                    <item name="fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Body2" parent="@style/TextAppearance.AppCompat.Body1">
                    <item name="android:textSize">14.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif</item>
                    <item name="android:letterSpacing">0.017857144</item>
                    <item name="fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Button" parent="@style/TextAppearance.AppCompat.Button">
                    <item name="android:textSize">14.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">true</item>
                    <item name="android:fontFamily">sans-serif-medium</item>
                    <item name="android:letterSpacing">0.08928572</item>
                    <item name="fontFamily">sans-serif-medium</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Caption" parent="@style/TextAppearance.AppCompat.Caption">
                    <item name="android:textSize">12.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif</item>
                    <item name="android:letterSpacing">0.033333335</item>
                    <item name="fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Chip" parent="@style/TextAppearance.AppCompat">
                    <item name="android:textSize">@dimen/mtrl_chip_text_size</item>
                    <item name="android:textColor">@color/mtrl_chip_text_color</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Headline1" parent="@style/TextAppearance.AppCompat.Display4">
                    <item name="android:textSize">96.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif-light</item>
                    <item name="android:letterSpacing">-0.015625</item>
                    <item name="fontFamily">sans-serif-light</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Headline2" parent="@style/TextAppearance.AppCompat.Display3">
                    <item name="android:textSize">60.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif-light</item>
                    <item name="android:letterSpacing">-0.008333334</item>
                    <item name="fontFamily">sans-serif-light</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Headline3" parent="@style/TextAppearance.AppCompat.Display2">
                    <item name="android:textSize">48.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif</item>
                    <item name="android:letterSpacing">0.0</item>
                    <item name="fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Headline4" parent="@style/TextAppearance.AppCompat.Display1">
                    <item name="android:textSize">34.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif</item>
                    <item name="android:letterSpacing">0.007352941</item>
                    <item name="fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Headline5" parent="@style/TextAppearance.AppCompat.Headline">
                    <item name="android:textSize">24.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif</item>
                    <item name="android:letterSpacing">0.0</item>
                    <item name="fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Headline6" parent="@style/TextAppearance.AppCompat.Title">
                    <item name="android:textSize">20.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif-medium</item>
                    <item name="android:letterSpacing">0.0125</item>
                    <item name="fontFamily">sans-serif-medium</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Overline" parent="@style/TextAppearance.AppCompat">
                    <item name="android:textSize">12.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">true</item>
                    <item name="android:fontFamily">sans-serif-medium</item>
                    <item name="android:letterSpacing">0.16666667</item>
                    <item name="fontFamily">sans-serif-medium</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Subtitle1" parent="@style/TextAppearance.AppCompat.Subhead">
                    <item name="android:textSize">16.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif</item>
                    <item name="android:letterSpacing">0.009375</item>
                    <item name="fontFamily">sans-serif</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Subtitle2" parent="@style/TextAppearance.AppCompat.Subhead">
                    <item name="android:textSize">14.0sp</item>
                    <item name="android:textStyle">normal</item>
                    <item name="android:textAllCaps">false</item>
                    <item name="android:fontFamily">sans-serif-medium</item>
                    <item name="android:letterSpacing">0.007142857</item>
                    <item name="fontFamily">sans-serif-medium</item>
                </style>
                <style name="TextAppearance.MaterialComponents.Tab" parent="@style/TextAppearance.Design.Tab">
                    <item name="android:textColor">@color/mtrl_tabs_icon_color_selector</item>
                </style>
                <style name="TextAppearance.Widget.AppCompat.ExpandedMenu.Item" parent="@style/Base.TextAppearance.Widget.AppCompat.ExpandedMenu.Item" />
                <style name="TextAppearance.Widget.AppCompat.Toolbar.Subtitle" parent="@style/Base.TextAppearance.Widget.AppCompat.Toolbar.Subtitle" />
                <style name="TextAppearance.Widget.AppCompat.Toolbar.Title" parent="@style/Base.TextAppearance.Widget.AppCompat.Toolbar.Title" />
                <style name="Theme.ActivityDialogStyle" parent="@style/Theme.AppCompat.Light.NoActionBar">
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:windowIsFloating">false</item>
                    <item name="android:windowIsTranslucent">true</item>
                    <item name="android:windowContentOverlay">@null</item>
                    <item name="android:layout_gravity">center</item>
                    <item name="android:backgroundDimEnabled">true</item>
                    <item name="android:windowCloseOnTouchOutside">false</item>
                </style>
                <style name="Theme.AppCompat" parent="@style/Base.Theme.AppCompat" />
                <style name="Theme.AppCompat.CompactMenu" parent="@style/Base.Theme.AppCompat.CompactMenu" />
                <style name="Theme.AppCompat.DayNight" parent="@style/Theme.AppCompat.Light" />
                <style name="Theme.AppCompat.DayNight.DarkActionBar" parent="@style/Theme.AppCompat.Light.DarkActionBar" />
                <style name="Theme.AppCompat.DayNight.Dialog" parent="@style/Theme.AppCompat.Light.Dialog" />
                <style name="Theme.AppCompat.DayNight.Dialog.Alert" parent="@style/Theme.AppCompat.Light.Dialog.Alert" />
                <style name="Theme.AppCompat.DayNight.Dialog.MinWidth" parent="@style/Theme.AppCompat.Light.Dialog.MinWidth" />
                <style name="Theme.AppCompat.DayNight.DialogWhenLarge" parent="@style/Theme.AppCompat.Light.DialogWhenLarge" />
                <style name="Theme.AppCompat.DayNight.NoActionBar" parent="@style/Theme.AppCompat.Light.NoActionBar" />
                <style name="Theme.AppCompat.Dialog" parent="@style/Base.Theme.AppCompat.Dialog" />
                <style name="Theme.AppCompat.Dialog.Alert" parent="@style/Base.Theme.AppCompat.Dialog.Alert" />
                <style name="Theme.AppCompat.Dialog.MinWidth" parent="@style/Base.Theme.AppCompat.Dialog.MinWidth" />
                <style name="Theme.AppCompat.DialogWhenLarge" parent="@style/Base.Theme.AppCompat.DialogWhenLarge" />
                <style name="Theme.AppCompat.Light" parent="@style/Base.Theme.AppCompat.Light" />
                <style name="Theme.AppCompat.Light.DarkActionBar" parent="@style/Base.Theme.AppCompat.Light.DarkActionBar" />
                <style name="Theme.AppCompat.Light.Dialog" parent="@style/Base.Theme.AppCompat.Light.Dialog" />
                <style name="Theme.AppCompat.Light.Dialog.Alert" parent="@style/Base.Theme.AppCompat.Light.Dialog.Alert" />
                <style name="Theme.AppCompat.Light.Dialog.MinWidth" parent="@style/Base.Theme.AppCompat.Light.Dialog.MinWidth" />
                <style name="Theme.AppCompat.Light.DialogWhenLarge" parent="@style/Base.Theme.AppCompat.Light.DialogWhenLarge" />
                <style name="Theme.AppCompat.Light.NoActionBar" parent="@style/Theme.AppCompat.Light">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.AppCompat.NoActionBar" parent="@style/Theme.AppCompat">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.Design" parent="@style/Theme.AppCompat" />
                <style name="Theme.Design.BottomSheetDialog" parent="@style/Theme.AppCompat.Dialog">
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:windowAnimationStyle">@style/Animation.Design.BottomSheetDialog</item>
                    <item name="bottomSheetStyle">@style/Widget.Design.BottomSheet.Modal</item>
                </style>
                <style name="Theme.Design.Light" parent="@style/Theme.AppCompat.Light" />
                <style name="Theme.Design.Light.BottomSheetDialog" parent="@style/Theme.AppCompat.Light.Dialog">
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:windowAnimationStyle">@style/Animation.Design.BottomSheetDialog</item>
                    <item name="bottomSheetStyle">@style/Widget.Design.BottomSheet.Modal</item>
                </style>
                <style name="Theme.Design.Light.NoActionBar" parent="@style/Theme.Design.Light">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.Design.NoActionBar" parent="@style/Theme.Design">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.MaterialComponents" parent="@style/Base.Theme.MaterialComponents" />
                <style name="Theme.MaterialComponents.BottomSheetDialog" parent="@style/Theme.MaterialComponents.Dialog">
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:windowAnimationStyle">@style/Animation.Design.BottomSheetDialog</item>
                    <item name="bottomSheetStyle">@style/Widget.Design.BottomSheet.Modal</item>
                </style>
                <style name="Theme.MaterialComponents.Bridge" parent="@style/Base.Theme.MaterialComponents.Bridge" />
                <style name="Theme.MaterialComponents.CompactMenu" parent="@style/Base.Theme.MaterialComponents.CompactMenu" />
                <style name="Theme.MaterialComponents.Dialog" parent="@style/Base.Theme.MaterialComponents.Dialog" />
                <style name="Theme.MaterialComponents.Dialog.Alert" parent="@style/Base.Theme.MaterialComponents.Dialog.Alert" />
                <style name="Theme.MaterialComponents.Dialog.MinWidth" parent="@style/Base.Theme.MaterialComponents.Dialog.MinWidth" />
                <style name="Theme.MaterialComponents.DialogWhenLarge" parent="@style/Base.Theme.MaterialComponents.DialogWhenLarge" />
                <style name="Theme.MaterialComponents.Light" parent="@style/Base.Theme.MaterialComponents.Light" />
                <style name="Theme.MaterialComponents.Light.BottomSheetDialog" parent="@style/Theme.MaterialComponents.Light.Dialog">
                    <item name="android:windowBackground">@android:color/transparent</item>
                    <item name="android:windowAnimationStyle">@style/Animation.Design.BottomSheetDialog</item>
                    <item name="bottomSheetStyle">@style/Widget.Design.BottomSheet.Modal</item>
                </style>
                <style name="Theme.MaterialComponents.Light.Bridge" parent="@style/Base.Theme.MaterialComponents.Light.Bridge" />
                <style name="Theme.MaterialComponents.Light.DarkActionBar" parent="@style/Base.Theme.MaterialComponents.Light.DarkActionBar" />
                <style name="Theme.MaterialComponents.Light.DarkActionBar.Bridge" parent="@style/Base.Theme.MaterialComponents.Light.DarkActionBar.Bridge" />
                <style name="Theme.MaterialComponents.Light.Dialog" parent="@style/Base.Theme.MaterialComponents.Light.Dialog" />
                <style name="Theme.MaterialComponents.Light.Dialog.Alert" parent="@style/Base.Theme.MaterialComponents.Light.Dialog.Alert" />
                <style name="Theme.MaterialComponents.Light.Dialog.MinWidth" parent="@style/Base.Theme.MaterialComponents.Light.Dialog.MinWidth" />
                <style name="Theme.MaterialComponents.Light.DialogWhenLarge" parent="@style/Base.Theme.MaterialComponents.Light.DialogWhenLarge" />
                <style name="Theme.MaterialComponents.Light.NoActionBar" parent="@style/Theme.MaterialComponents.Light">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.MaterialComponents.Light.NoActionBar.Bridge" parent="@style/Theme.MaterialComponents.Light.Bridge">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.MaterialComponents.NoActionBar" parent="@style/Theme.MaterialComponents">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.MaterialComponents.NoActionBar.Bridge" parent="@style/Theme.MaterialComponents.Bridge">
                    <item name="windowActionBar">false</item>
                    <item name="windowNoTitle">true</item>
                </style>
                <style name="Theme.RySdkFlavor" parent="@style/Theme.AppCompat.Light.DarkActionBar">
                    <item name="colorAccent">@color/teal_200</item>
                    <item name="colorPrimary">@color/purple_500</item>
                    <item name="colorPrimaryDark">@color/purple_700</item>
                </style>
                <style name="ThemeOverlay.AppCompat" parent="@style/Base.ThemeOverlay.AppCompat" />
                <style name="ThemeOverlay.AppCompat.ActionBar" parent="@style/Base.ThemeOverlay.AppCompat.ActionBar" />
                <style name="ThemeOverlay.AppCompat.Dark" parent="@style/Base.ThemeOverlay.AppCompat.Dark" />
                <style name="ThemeOverlay.AppCompat.Dark.ActionBar" parent="@style/Base.ThemeOverlay.AppCompat.Dark.ActionBar" />
                <style name="ThemeOverlay.AppCompat.Dialog" parent="@style/Base.ThemeOverlay.AppCompat.Dialog" />
                <style name="ThemeOverlay.AppCompat.Dialog.Alert" parent="@style/Base.ThemeOverlay.AppCompat.Dialog.Alert" />
                <style name="ThemeOverlay.AppCompat.Light" parent="@style/Base.ThemeOverlay.AppCompat.Light" />
                <style name="ThemeOverlay.MaterialComponents" parent="@style/ThemeOverlay.AppCompat" />
                <style name="ThemeOverlay.MaterialComponents.ActionBar" parent="@style/ThemeOverlay.AppCompat.ActionBar" />
                <style name="ThemeOverlay.MaterialComponents.Dark" parent="@style/ThemeOverlay.AppCompat.Dark" />
                <style name="ThemeOverlay.MaterialComponents.Dark.ActionBar" parent="@style/ThemeOverlay.AppCompat.Dark.ActionBar" />
                <style name="ThemeOverlay.MaterialComponents.Dialog" parent="@style/Base.ThemeOverlay.MaterialComponents.Dialog" />
                <style name="ThemeOverlay.MaterialComponents.Dialog.Alert" parent="@style/Base.ThemeOverlay.MaterialComponents.Dialog.Alert" />
                <style name="ThemeOverlay.MaterialComponents.Light" parent="@style/ThemeOverlay.AppCompat.Light" />
                <style name="ThemeOverlay.MaterialComponents.TextInputEditText" parent="" />
                <style name="ThemeOverlay.MaterialComponents.TextInputEditText.FilledBox" parent="@style/ThemeOverlay.MaterialComponents.TextInputEditText">
                    <item name="editTextStyle">@style/Widget.MaterialComponents.TextInputEditText.FilledBox</item>
                </style>
                <style name="ThemeOverlay.MaterialComponents.TextInputEditText.FilledBox.Dense" parent="@style/ThemeOverlay.MaterialComponents.TextInputEditText.FilledBox">
                    <item name="editTextStyle">@style/Widget.MaterialComponents.TextInputEditText.FilledBox.Dense</item>
                </style>
                <style name="ThemeOverlay.MaterialComponents.TextInputEditText.OutlinedBox" parent="@style/ThemeOverlay.MaterialComponents.TextInputEditText">
                    <item name="editTextStyle">@style/Widget.MaterialComponents.TextInputEditText.OutlinedBox</item>
                </style>
                <style name="ThemeOverlay.MaterialComponents.TextInputEditText.OutlinedBox.Dense" parent="@style/ThemeOverlay.MaterialComponents.TextInputEditText.OutlinedBox">
                    <item name="editTextStyle">@style/Widget.MaterialComponents.TextInputEditText.OutlinedBox.Dense</item>
                </style>
                <style name="Widget.AppCompat.ActionBar" parent="@style/Base.Widget.AppCompat.ActionBar" />
                <style name="Widget.AppCompat.ActionBar.Solid" parent="@style/Base.Widget.AppCompat.ActionBar.Solid" />
                <style name="Widget.AppCompat.ActionBar.TabBar" parent="@style/Base.Widget.AppCompat.ActionBar.TabBar" />
                <style name="Widget.AppCompat.ActionBar.TabText" parent="@style/Base.Widget.AppCompat.ActionBar.TabText" />
                <style name="Widget.AppCompat.ActionBar.TabView" parent="@style/Base.Widget.AppCompat.ActionBar.TabView" />
                <style name="Widget.AppCompat.ActionButton" parent="@style/Base.Widget.AppCompat.ActionButton" />
                <style name="Widget.AppCompat.ActionButton.CloseMode" parent="@style/Base.Widget.AppCompat.ActionButton.CloseMode" />
                <style name="Widget.AppCompat.ActionButton.Overflow" parent="@style/Base.Widget.AppCompat.ActionButton.Overflow" />
                <style name="Widget.AppCompat.ActionMode" parent="@style/Base.Widget.AppCompat.ActionMode" />
                <style name="Widget.AppCompat.ActivityChooserView" parent="@style/Base.Widget.AppCompat.ActivityChooserView" />
                <style name="Widget.AppCompat.AutoCompleteTextView" parent="@style/Base.Widget.AppCompat.AutoCompleteTextView" />
                <style name="Widget.AppCompat.Button" parent="@style/Base.Widget.AppCompat.Button" />
                <style name="Widget.AppCompat.Button.Borderless" parent="@style/Base.Widget.AppCompat.Button.Borderless" />
                <style name="Widget.AppCompat.Button.Borderless.Colored" parent="@style/Base.Widget.AppCompat.Button.Borderless.Colored" />
                <style name="Widget.AppCompat.Button.ButtonBar.AlertDialog" parent="@style/Base.Widget.AppCompat.Button.ButtonBar.AlertDialog" />
                <style name="Widget.AppCompat.Button.Colored" parent="@style/Base.Widget.AppCompat.Button.Colored" />
                <style name="Widget.AppCompat.Button.Small" parent="@style/Base.Widget.AppCompat.Button.Small" />
                <style name="Widget.AppCompat.ButtonBar" parent="@style/Base.Widget.AppCompat.ButtonBar" />
                <style name="Widget.AppCompat.ButtonBar.AlertDialog" parent="@style/Base.Widget.AppCompat.ButtonBar.AlertDialog" />
                <style name="Widget.AppCompat.CompoundButton.CheckBox" parent="@style/Base.Widget.AppCompat.CompoundButton.CheckBox" />
                <style name="Widget.AppCompat.CompoundButton.RadioButton" parent="@style/Base.Widget.AppCompat.CompoundButton.RadioButton" />
                <style name="Widget.AppCompat.CompoundButton.Switch" parent="@style/Base.Widget.AppCompat.CompoundButton.Switch" />
                <style name="Widget.AppCompat.DrawerArrowToggle" parent="@style/Base.Widget.AppCompat.DrawerArrowToggle">
                    <item name="color">?colorControlNormal</item>
                </style>
                <style name="Widget.AppCompat.DropDownItem.Spinner" parent="@style/RtlOverlay.Widget.AppCompat.Search.DropDown.Text" />
                <style name="Widget.AppCompat.EditText" parent="@style/Base.Widget.AppCompat.EditText" />
                <style name="Widget.AppCompat.ImageButton" parent="@style/Base.Widget.AppCompat.ImageButton" />
                <style name="Widget.AppCompat.Light.ActionBar" parent="@style/Base.Widget.AppCompat.Light.ActionBar" />
                <style name="Widget.AppCompat.Light.ActionBar.Solid" parent="@style/Base.Widget.AppCompat.Light.ActionBar.Solid" />
                <style name="Widget.AppCompat.Light.ActionBar.Solid.Inverse" parent="@style/Widget.AppCompat.Light.ActionBar.Solid" />
                <style name="Widget.AppCompat.Light.ActionBar.TabBar" parent="@style/Base.Widget.AppCompat.Light.ActionBar.TabBar" />
                <style name="Widget.AppCompat.Light.ActionBar.TabBar.Inverse" parent="@style/Widget.AppCompat.Light.ActionBar.TabBar" />
                <style name="Widget.AppCompat.Light.ActionBar.TabText" parent="@style/Base.Widget.AppCompat.Light.ActionBar.TabText" />
                <style name="Widget.AppCompat.Light.ActionBar.TabText.Inverse" parent="@style/Base.Widget.AppCompat.Light.ActionBar.TabText.Inverse" />
                <style name="Widget.AppCompat.Light.ActionBar.TabView" parent="@style/Base.Widget.AppCompat.Light.ActionBar.TabView" />
                <style name="Widget.AppCompat.Light.ActionBar.TabView.Inverse" parent="@style/Widget.AppCompat.Light.ActionBar.TabView" />
                <style name="Widget.AppCompat.Light.ActionButton" parent="@style/Widget.AppCompat.ActionButton" />
                <style name="Widget.AppCompat.Light.ActionButton.CloseMode" parent="@style/Widget.AppCompat.ActionButton.CloseMode" />
                <style name="Widget.AppCompat.Light.ActionButton.Overflow" parent="@style/Widget.AppCompat.ActionButton.Overflow" />
                <style name="Widget.AppCompat.Light.ActionMode.Inverse" parent="@style/Widget.AppCompat.ActionMode" />
                <style name="Widget.AppCompat.Light.ActivityChooserView" parent="@style/Widget.AppCompat.ActivityChooserView" />
                <style name="Widget.AppCompat.Light.AutoCompleteTextView" parent="@style/Widget.AppCompat.AutoCompleteTextView" />
                <style name="Widget.AppCompat.Light.DropDownItem.Spinner" parent="@style/Widget.AppCompat.DropDownItem.Spinner" />
                <style name="Widget.AppCompat.Light.ListPopupWindow" parent="@style/Widget.AppCompat.ListPopupWindow" />
                <style name="Widget.AppCompat.Light.ListView.DropDown" parent="@style/Widget.AppCompat.ListView.DropDown" />
                <style name="Widget.AppCompat.Light.PopupMenu" parent="@style/Base.Widget.AppCompat.Light.PopupMenu" />
                <style name="Widget.AppCompat.Light.PopupMenu.Overflow" parent="@style/Base.Widget.AppCompat.Light.PopupMenu.Overflow" />
                <style name="Widget.AppCompat.Light.SearchView" parent="@style/Widget.AppCompat.SearchView" />
                <style name="Widget.AppCompat.Light.Spinner.DropDown.ActionBar" parent="@style/Widget.AppCompat.Spinner.DropDown.ActionBar" />
                <style name="Widget.AppCompat.ListMenuView" parent="@style/Base.Widget.AppCompat.ListMenuView" />
                <style name="Widget.AppCompat.ListPopupWindow" parent="@style/Base.Widget.AppCompat.ListPopupWindow" />
                <style name="Widget.AppCompat.ListView" parent="@style/Base.Widget.AppCompat.ListView" />
                <style name="Widget.AppCompat.ListView.DropDown" parent="@style/Base.Widget.AppCompat.ListView.DropDown" />
                <style name="Widget.AppCompat.ListView.Menu" parent="@style/Base.Widget.AppCompat.ListView.Menu" />
                <style name="Widget.AppCompat.PopupMenu" parent="@style/Base.Widget.AppCompat.PopupMenu" />
                <style name="Widget.AppCompat.PopupMenu.Overflow" parent="@style/Base.Widget.AppCompat.PopupMenu.Overflow" />
                <style name="Widget.AppCompat.PopupWindow" parent="@style/Base.Widget.AppCompat.PopupWindow" />
                <style name="Widget.AppCompat.ProgressBar" parent="@style/Base.Widget.AppCompat.ProgressBar" />
                <style name="Widget.AppCompat.ProgressBar.Horizontal" parent="@style/Base.Widget.AppCompat.ProgressBar.Horizontal" />
                <style name="Widget.AppCompat.RatingBar" parent="@style/Base.Widget.AppCompat.RatingBar" />
                <style name="Widget.AppCompat.RatingBar.Indicator" parent="@style/Base.Widget.AppCompat.RatingBar.Indicator" />
                <style name="Widget.AppCompat.RatingBar.Small" parent="@style/Base.Widget.AppCompat.RatingBar.Small" />
                <style name="Widget.AppCompat.SearchView" parent="@style/Base.Widget.AppCompat.SearchView" />
                <style name="Widget.AppCompat.SearchView.ActionBar" parent="@style/Base.Widget.AppCompat.SearchView.ActionBar" />
                <style name="Widget.AppCompat.SeekBar" parent="@style/Base.Widget.AppCompat.SeekBar" />
                <style name="Widget.AppCompat.SeekBar.Discrete" parent="@style/Base.Widget.AppCompat.SeekBar.Discrete" />
                <style name="Widget.AppCompat.Spinner" parent="@style/Base.Widget.AppCompat.Spinner" />
                <style name="Widget.AppCompat.Spinner.DropDown" parent="@style/Widget.AppCompat.Spinner" />
                <style name="Widget.AppCompat.Spinner.DropDown.ActionBar" parent="@style/Widget.AppCompat.Spinner.DropDown" />
                <style name="Widget.AppCompat.Spinner.Underlined" parent="@style/Base.Widget.AppCompat.Spinner.Underlined" />
                <style name="Widget.AppCompat.TextView.SpinnerItem" parent="@style/Base.Widget.AppCompat.TextView.SpinnerItem" />
                <style name="Widget.AppCompat.Toolbar" parent="@style/Base.Widget.AppCompat.Toolbar" />
                <style name="Widget.AppCompat.Toolbar.Button.Navigation" parent="@style/Base.Widget.AppCompat.Toolbar.Button.Navigation" />
                <style name="Widget.Compat.NotificationActionContainer" parent="">
                    <item name="android:background">@drawable/notification_action_background</item>
                </style>
                <style name="Widget.Compat.NotificationActionText" parent="">
                    <item name="android:textAppearance">?android:textAppearanceButton</item>
                    <item name="android:textSize">@dimen/notification_action_text_size</item>
                    <item name="android:textColor">@color/secondary_text_default_material_light</item>
                </style>
                <style name="Widget.Design.AppBarLayout" parent="@android:style/Widget">
                    <item name="android:background">?colorPrimary</item>
                    <item name="android:stateListAnimator">@animator/design_appbar_state_list_animator</item>
                    <item name="android:touchscreenBlocksFocus">true</item>
                </style>
                <style name="Widget.Design.BottomNavigationView" parent="">
                    <item name="elevation">@dimen/design_bottom_navigation_elevation</item>
                    <item name="itemBackground">?selectableItemBackgroundBorderless</item>
                    <item name="itemHorizontalTranslationEnabled">true</item>
                    <item name="itemIconSize">@dimen/design_bottom_navigation_icon_size</item>
                    <item name="labelVisibilityMode">auto</item>
                </style>
                <style name="Widget.Design.BottomSheet.Modal" parent="@android:style/Widget">
                    <item name="android:background">?android:colorBackground</item>
                    <item name="android:elevation">@dimen/design_bottom_sheet_modal_elevation</item>
                    <item name="behavior_hideable">true</item>
                    <item name="behavior_peekHeight">auto</item>
                    <item name="behavior_skipCollapsed">false</item>
                </style>
                <style name="Widget.Design.CollapsingToolbar" parent="@android:style/Widget">
                    <item name="expandedTitleMargin">32.0dip</item>
                    <item name="statusBarScrim">?colorPrimaryDark</item>
                </style>
                <style name="Widget.Design.FloatingActionButton" parent="@android:style/Widget">
                    <item name="android:background">@drawable/design_fab_background</item>
                    <item name="android:focusable">true</item>
                    <item name="android:clickable">true</item>
                    <item name="backgroundTint">?colorAccent</item>
                    <item name="borderWidth">@dimen/design_fab_border_width</item>
                    <item name="elevation">@dimen/design_fab_elevation</item>
                    <item name="fabSize">auto</item>
                    <item name="hideMotionSpec">@animator/design_fab_hide_motion_spec</item>
                    <item name="hoveredFocusedTranslationZ">@dimen/design_fab_translation_z_hovered_focused</item>
                    <item name="maxImageSize">@dimen/design_fab_image_size</item>
                    <item name="pressedTranslationZ">@dimen/design_fab_translation_z_pressed</item>
                    <item name="rippleColor">?colorControlHighlight</item>
                    <item name="showMotionSpec">@animator/design_fab_show_motion_spec</item>
                </style>
                <style name="Widget.Design.NavigationView" parent="">
                    <item name="android:background">?android:windowBackground</item>
                    <item name="android:fitsSystemWindows">true</item>
                    <item name="android:maxWidth">@dimen/design_navigation_max_width</item>
                    <item name="elevation">@dimen/design_navigation_elevation</item>
                    <item name="itemHorizontalPadding">@dimen/design_navigation_item_horizontal_padding</item>
                    <item name="itemIconPadding">@dimen/design_navigation_item_icon_padding</item>
                </style>
                <style name="Widget.Design.ScrimInsetsFrameLayout" parent="">
                    <item name="insetForeground">#44000000</item>
                </style>
                <style name="Widget.Design.Snackbar" parent="@android:style/Widget">
                    <item name="android:background">@drawable/design_snackbar_background</item>
                    <item name="android:paddingLeft">@dimen/design_snackbar_padding_horizontal</item>
                    <item name="android:paddingRight">@dimen/design_snackbar_padding_horizontal</item>
                    <item name="android:maxWidth">@dimen/design_snackbar_max_width</item>
                    <item name="android:minWidth">@dimen/design_snackbar_min_width</item>
                    <item name="elevation">@dimen/design_snackbar_elevation</item>
                    <item name="maxActionInlineWidth">@dimen/design_snackbar_action_inline_max_width</item>
                </style>
                <style name="Widget.Design.TabLayout" parent="@style/Base.Widget.Design.TabLayout">
                    <item name="tabGravity">fill</item>
                    <item name="tabIndicatorFullWidth">true</item>
                    <item name="tabMode">fixed</item>
                </style>
                <style name="Widget.Design.TextInputLayout" parent="@android:style/Widget">
                    <item name="boxBackgroundMode">none</item>
                    <item name="counterOverflowTextAppearance">@style/TextAppearance.Design.Counter.Overflow</item>
                    <item name="counterTextAppearance">@style/TextAppearance.Design.Counter</item>
                    <item name="errorTextAppearance">@style/TextAppearance.Design.Error</item>
                    <item name="helperTextTextAppearance">@style/TextAppearance.Design.HelperText</item>
                    <item name="hintTextAppearance">@style/TextAppearance.Design.Hint</item>
                    <item name="passwordToggleContentDescription">@string/password_toggle_content_description</item>
                    <item name="passwordToggleDrawable">@drawable/design_password_eye</item>
                    <item name="passwordToggleTint">@color/design_tint_password_toggle</item>
                </style>
                <style name="Widget.MaterialComponents.BottomAppBar" parent="@style/Widget.AppCompat.Toolbar">
                    <item name="backgroundTint">@android:color/white</item>
                    <item name="fabCradleMargin">@dimen/mtrl_bottomappbar_fab_cradle_margin</item>
                    <item name="fabCradleRoundedCornerRadius">@dimen/mtrl_bottomappbar_fab_cradle_rounded_corner_radius</item>
                    <item name="fabCradleVerticalOffset">@dimen/mtrl_bottomappbar_fab_cradle_vertical_offset</item>
                </style>
                <style name="Widget.MaterialComponents.BottomAppBar.Colored" parent="@style/Widget.MaterialComponents.BottomAppBar">
                    <item name="backgroundTint">?colorPrimary</item>
                </style>
                <style name="Widget.MaterialComponents.BottomNavigationView" parent="@style/Widget.Design.BottomNavigationView">
                    <item name="android:background">@android:color/white</item>
                    <item name="enforceTextAppearance">true</item>
                    <item name="itemHorizontalTranslationEnabled">false</item>
                    <item name="itemIconTint">@color/mtrl_bottom_nav_item_tint</item>
                    <item name="itemTextAppearanceActive">?textAppearanceCaption</item>
                    <item name="itemTextAppearanceInactive">?textAppearanceCaption</item>
                    <item name="itemTextColor">@color/mtrl_bottom_nav_item_tint</item>
                </style>
                <style name="Widget.MaterialComponents.BottomNavigationView.Colored" parent="@style/Widget.MaterialComponents.BottomNavigationView">
                    <item name="android:background">?colorPrimary</item>
                    <item name="itemIconTint">@color/mtrl_bottom_nav_colored_item_tint</item>
                    <item name="itemTextAppearanceActive">?textAppearanceCaption</item>
                    <item name="itemTextAppearanceInactive">?textAppearanceCaption</item>
                    <item name="itemTextColor">@color/mtrl_bottom_nav_colored_item_tint</item>
                </style>
                <style name="Widget.MaterialComponents.BottomSheet.Modal" parent="@style/Widget.Design.BottomSheet.Modal" />
                <style name="Widget.MaterialComponents.Button" parent="@style/Widget.AppCompat.Button">
                    <item name="android:textAppearance">?textAppearanceButton</item>
                    <item name="android:textColor">@color/mtrl_btn_text_color_selector</item>
                    <item name="android:paddingLeft">@dimen/mtrl_btn_padding_left</item>
                    <item name="android:paddingTop">@dimen/mtrl_btn_padding_top</item>
                    <item name="android:paddingRight">@dimen/mtrl_btn_padding_right</item>
                    <item name="android:paddingBottom">@dimen/mtrl_btn_padding_bottom</item>
                    <item name="android:insetLeft">0.0dip</item>
                    <item name="android:insetRight">0.0dip</item>
                    <item name="android:insetTop">@dimen/mtrl_btn_inset</item>
                    <item name="android:insetBottom">@dimen/mtrl_btn_inset</item>
                    <item name="android:stateListAnimator">@animator/mtrl_btn_state_list_anim</item>
                    <item name="backgroundTint">@color/mtrl_btn_bg_color_selector</item>
                    <item name="cornerRadius">@dimen/mtrl_btn_corner_radius</item>
                    <item name="enforceTextAppearance">true</item>
                    <item name="iconPadding">@dimen/mtrl_btn_icon_padding</item>
                    <item name="iconTint">@color/mtrl_btn_text_color_selector</item>
                    <item name="rippleColor">@color/mtrl_btn_ripple_color</item>
                </style>
                <style name="Widget.MaterialComponents.Button.Icon" parent="@style/Widget.MaterialComponents.Button">
                    <item name="android:paddingLeft">@dimen/mtrl_btn_icon_btn_padding_left</item>
                </style>
                <style name="Widget.MaterialComponents.Button.OutlinedButton" parent="@style/Widget.MaterialComponents.Button.TextButton">
                    <item name="android:paddingLeft">@dimen/mtrl_btn_padding_left</item>
                    <item name="android:paddingRight">@dimen/mtrl_btn_padding_right</item>
                    <item name="strokeColor">@color/mtrl_btn_stroke_color_selector</item>
                    <item name="strokeWidth">@dimen/mtrl_btn_stroke_size</item>
                </style>
                <style name="Widget.MaterialComponents.Button.OutlinedButton.Icon" parent="@style/Widget.MaterialComponents.Button.OutlinedButton">
                    <item name="android:paddingLeft">@dimen/mtrl_btn_icon_btn_padding_left</item>
                </style>
                <style name="Widget.MaterialComponents.Button.TextButton" parent="@style/Widget.MaterialComponents.Button.UnelevatedButton">
                    <item name="android:textColor">@color/mtrl_text_btn_text_color_selector</item>
                    <item name="android:paddingLeft">@dimen/mtrl_btn_text_btn_padding_left</item>
                    <item name="android:paddingRight">@dimen/mtrl_btn_text_btn_padding_right</item>
                    <item name="backgroundTint">@color/mtrl_btn_transparent_bg_color</item>
                    <item name="iconPadding">@dimen/mtrl_btn_text_btn_icon_padding</item>
                    <item name="iconTint">@color/mtrl_text_btn_text_color_selector</item>
                    <item name="rippleColor">@color/mtrl_btn_text_btn_ripple_color</item>
                </style>
                <style name="Widget.MaterialComponents.Button.TextButton.Dialog" parent="@style/Widget.MaterialComponents.Button.TextButton">
                    <item name="android:minWidth">@dimen/mtrl_btn_dialog_btn_min_width</item>
                </style>
                <style name="Widget.MaterialComponents.Button.TextButton.Dialog.Icon" parent="@style/Widget.MaterialComponents.Button.TextButton.Dialog" />
                <style name="Widget.MaterialComponents.Button.TextButton.Icon" parent="@style/Widget.MaterialComponents.Button.TextButton" />
                <style name="Widget.MaterialComponents.Button.UnelevatedButton" parent="@style/Widget.MaterialComponents.Button">
                    <item name="android:stateListAnimator">@animator/mtrl_btn_unelevated_state_list_anim</item>
                </style>
                <style name="Widget.MaterialComponents.Button.UnelevatedButton.Icon" parent="@style/Widget.MaterialComponents.Button.UnelevatedButton">
                    <item name="android:paddingLeft">@dimen/mtrl_btn_icon_btn_padding_left</item>
                </style>
                <style name="Widget.MaterialComponents.CardView" parent="@style/CardView">
                    <item name="cardBackgroundColor">?colorBackgroundFloating</item>
                    <item name="cardElevation">@dimen/mtrl_card_elevation</item>
                </style>
                <style name="Widget.MaterialComponents.Chip.Action" parent="@style/Base.Widget.MaterialComponents.Chip">
                    <item name="closeIconVisible">false</item>
                </style>
                <style name="Widget.MaterialComponents.Chip.Choice" parent="@style/Base.Widget.MaterialComponents.Chip">
                    <item name="android:checkable">true</item>
                    <item name="checkedIcon">@drawable/ic_mtrl_chip_checked_black</item>
                    <item name="checkedIconVisible">false</item>
                    <item name="chipIconVisible">false</item>
                    <item name="closeIconVisible">false</item>
                </style>
                <style name="Widget.MaterialComponents.Chip.Entry" parent="@style/Base.Widget.MaterialComponents.Chip">
                    <item name="android:checkable">true</item>
                </style>
                <style name="Widget.MaterialComponents.Chip.Filter" parent="@style/Base.Widget.MaterialComponents.Chip">
                    <item name="android:checkable">true</item>
                    <item name="checkedIcon">@drawable/ic_mtrl_chip_checked_black</item>
                    <item name="chipIconVisible">false</item>
                    <item name="closeIconVisible">false</item>
                </style>
                <style name="Widget.MaterialComponents.ChipGroup" parent="@android:style/Widget">
                    <item name="chipSpacing">4.0dip</item>
                    <item name="singleLine">false</item>
                    <item name="singleSelection">false</item>
                </style>
                <style name="Widget.MaterialComponents.FloatingActionButton" parent="@style/Widget.Design.FloatingActionButton">
                    <item name="elevation">@dimen/mtrl_fab_elevation</item>
                    <item name="hideMotionSpec">@animator/mtrl_fab_hide_motion_spec</item>
                    <item name="hoveredFocusedTranslationZ">@dimen/mtrl_fab_translation_z_hovered_focused</item>
                    <item name="pressedTranslationZ">@dimen/mtrl_fab_translation_z_pressed</item>
                    <item name="rippleColor">@color/mtrl_fab_ripple_color</item>
                    <item name="showMotionSpec">@animator/mtrl_fab_show_motion_spec</item>
                </style>
                <style name="Widget.MaterialComponents.NavigationView" parent="@style/Widget.Design.NavigationView">
                    <item name="elevation">@dimen/mtrl_navigation_elevation</item>
                    <item name="itemHorizontalPadding">@dimen/mtrl_navigation_item_horizontal_padding</item>
                    <item name="itemIconPadding">@dimen/mtrl_navigation_item_icon_padding</item>
                </style>
                <style name="Widget.MaterialComponents.Snackbar" parent="@style/Widget.Design.Snackbar">
                    <item name="android:background">@drawable/mtrl_snackbar_background</item>
                    <item name="android:layout_margin">@dimen/mtrl_snackbar_margin</item>
                </style>
                <style name="Widget.MaterialComponents.Snackbar.FullWidth" parent="@style/Widget.Design.Snackbar" />
                <style name="Widget.MaterialComponents.TabLayout" parent="@style/Widget.Design.TabLayout">
                    <item name="android:background">@android:color/white</item>
                    <item name="enforceTextAppearance">true</item>
                    <item name="tabIconTint">@color/mtrl_tabs_icon_color_selector</item>
                    <item name="tabIndicatorAnimationDuration">@integer/mtrl_tab_indicator_anim_duration_ms</item>
                    <item name="tabIndicatorColor">?colorAccent</item>
                    <item name="tabRippleColor">@color/mtrl_tabs_ripple_color</item>
                    <item name="tabTextAppearance">?textAppearanceButton</item>
                    <item name="tabTextColor">@color/mtrl_tabs_icon_color_selector</item>
                    <item name="tabUnboundedRipple">true</item>
                </style>
                <style name="Widget.MaterialComponents.TabLayout.Colored" parent="@style/Widget.MaterialComponents.TabLayout">
                    <item name="android:background">?colorAccent</item>
                    <item name="tabIconTint">@color/mtrl_tabs_icon_color_selector_colored</item>
                    <item name="tabIndicatorColor">@android:color/white</item>
                    <item name="tabRippleColor">@color/mtrl_tabs_colored_ripple_color</item>
                    <item name="tabTextColor">@color/mtrl_tabs_icon_color_selector_colored</item>
                </style>
                <style name="Widget.MaterialComponents.TextInputEditText.FilledBox" parent="@style/Base.Widget.MaterialComponents.TextInputEditText">
                    <item name="android:paddingTop">20.0dip</item>
                    <item name="android:paddingBottom">16.0dip</item>
                </style>
                <style name="Widget.MaterialComponents.TextInputEditText.FilledBox.Dense" parent="@style/Widget.MaterialComponents.TextInputEditText.FilledBox">
                    <item name="android:paddingTop">16.0dip</item>
                    <item name="android:paddingBottom">16.0dip</item>
                </style>
                <style name="Widget.MaterialComponents.TextInputEditText.OutlinedBox" parent="@style/Base.Widget.MaterialComponents.TextInputEditText" />
                <style name="Widget.MaterialComponents.TextInputEditText.OutlinedBox.Dense" parent="@style/Widget.MaterialComponents.TextInputEditText.OutlinedBox">
                    <item name="android:paddingTop">12.0dip</item>
                    <item name="android:paddingBottom">12.0dip</item>
                </style>
                <style name="Widget.MaterialComponents.TextInputLayout.FilledBox" parent="@style/Base.Widget.MaterialComponents.TextInputLayout">
                    <item name="android:theme">@style/ThemeOverlay.MaterialComponents.TextInputEditText.FilledBox</item>
                    <item name="boxBackgroundColor">@color/mtrl_textinput_filled_box_default_background_color</item>
                    <item name="boxBackgroundMode">filled</item>
                    <item name="boxCollapsedPaddingTop">12.0dip</item>
                    <item name="boxCornerRadiusBottomEnd">@dimen/mtrl_textinput_box_corner_radius_small</item>
                    <item name="boxCornerRadiusBottomStart">@dimen/mtrl_textinput_box_corner_radius_small</item>
                </style>
                <style name="Widget.MaterialComponents.TextInputLayout.FilledBox.Dense" parent="@style/Widget.MaterialComponents.TextInputLayout.FilledBox">
                    <item name="android:theme">@style/ThemeOverlay.MaterialComponents.TextInputEditText.FilledBox.Dense</item>
                    <item name="boxCollapsedPaddingTop">8.0dip</item>
                </style>
                <style name="Widget.MaterialComponents.TextInputLayout.OutlinedBox" parent="@style/Base.Widget.MaterialComponents.TextInputLayout">
                    <item name="android:theme">@style/ThemeOverlay.MaterialComponents.TextInputEditText.OutlinedBox</item>
                    <item name="boxCollapsedPaddingTop">0.0dip</item>
                </style>
                <style name="Widget.MaterialComponents.TextInputLayout.OutlinedBox.Dense" parent="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox">
                    <item name="android:theme">@style/ThemeOverlay.MaterialComponents.TextInputEditText.OutlinedBox.Dense</item>
                </style>
                <style name="Widget.MaterialComponents.Toolbar" parent="@style/Widget.AppCompat.Toolbar">
                    <item name="android:minHeight">@dimen/mtrl_toolbar_default_height</item>
                    <item name="subtitleTextAppearance">?textAppearanceSubtitle1</item>
                    <item name="subtitleTextColor">?android:textColorSecondary</item>
                    <item name="titleTextAppearance">?textAppearanceHeadline6</item>
                    <item name="titleTextColor">?android:textColorPrimary</item>
                </style>
                <style name="Widget.Support.CoordinatorLayout" parent="@android:style/Widget">
                    <item name="statusBarBackground">#ff000000</item>
                </style>
                <style name="actionActivity" parent="@android:style/Theme.Translucent.NoTitleBar">
                    <item name="android:statusBarColor">@android:color/transparent</item>
                </style>
                <style name="tt_appdownloader_style_detail_download_progress_bar" parent="@android:style/Widget.ProgressBar.Horizontal">
                    <item name="android:maxHeight">32.0dip</item>
                    <item name="android:indeterminateOnly">false</item>
                    <item name="android:progressDrawable">@drawable/tt_appdownloader_ad_detail_download_progress</item>
                    <item name="android:minHeight">32.0dip</item>
                </style>
                <style name="tt_appdownloader_style_notification_text" parent="@android:style/TextAppearance.Material.Notification" />
                <style name="tt_appdownloader_style_notification_title" parent="@android:style/TextAppearance.Material.Notification.Title" />
                <style name="tt_appdownloader_style_progress_bar" parent="@android:style/Widget.ProgressBar.Horizontal">
                    <item name="android:maxHeight">6.0dip</item>
                    <item name="android:indeterminateOnly">false</item>
                    <item name="android:progressDrawable">@drawable/tt_appdownloader_download_progress_bar_horizontal</item>
                    <item name="android:minHeight">3.0dip</item>
                </style>
                <style name="tt_appdownloader_style_progress_bar_new" parent="@android:style/Widget.ProgressBar.Horizontal">
                    <item name="android:maxHeight">6.0dip</item>
                    <item name="android:indeterminateOnly">false</item>
                    <item name="android:progressDrawable">@drawable/tt_appdownloader_download_progress_bar_horizontal_new</item>
                    <item name="android:minHeight">3.0dip</item>
                </style>
                <style name="ttdownloader_translucent_dialog" parent="@android:style/Theme.Dialog">
                    <item name="android:windowBackground">@color/ttdownloader_transparent</item>
                    <item name="android:windowFrame">@null</item>
                    <item name="android:windowNoTitle">true</item>
                    <item name="android:windowIsFloating">true</item>
                    <item name="android:windowIsTranslucent">true</item>
                    <item name="android:background">@color/ttdownloader_transparent</item>
                    <item name="android:backgroundDimEnabled">true</item>
                </style>
            </resources>
        """.trimIndent()
        val dimens = """
                <dimen name="abc_action_bar_content_inset_material">16.0dip</dimen>
                <dimen name="abc_action_bar_content_inset_with_nav">72.0dip</dimen>
                <dimen name="abc_action_bar_default_height_material">56.0dip</dimen>
                <dimen name="abc_action_bar_default_padding_end_material">0.0dip</dimen>
                <dimen name="abc_action_bar_default_padding_start_material">0.0dip</dimen>
                <dimen name="abc_action_bar_elevation_material">4.0dip</dimen>
                <dimen name="abc_action_bar_icon_vertical_padding_material">16.0dip</dimen>
                <dimen name="abc_action_bar_overflow_padding_end_material">10.0dip</dimen>
                <dimen name="abc_action_bar_overflow_padding_start_material">6.0dip</dimen>
                <dimen name="abc_action_bar_stacked_max_height">48.0dip</dimen>
                <dimen name="abc_action_bar_stacked_tab_max_width">180.0dip</dimen>
                <dimen name="abc_action_bar_subtitle_bottom_margin_material">5.0dip</dimen>
                <dimen name="abc_action_bar_subtitle_top_margin_material">-3.0dip</dimen>
                <dimen name="abc_action_button_min_height_material">48.0dip</dimen>
                <dimen name="abc_action_button_min_width_material">48.0dip</dimen>
                <dimen name="abc_action_button_min_width_overflow_material">36.0dip</dimen>
                <dimen name="abc_alert_dialog_button_bar_height">48.0dip</dimen>
                <dimen name="abc_alert_dialog_button_dimen">48.0dip</dimen>
                <item type="dimen" name="abc_button_inset_horizontal_material">@dimen/abc_control_inset_material</item>
                <dimen name="abc_button_inset_vertical_material">6.0dip</dimen>
                <dimen name="abc_button_padding_horizontal_material">8.0dip</dimen>
                <item type="dimen" name="abc_button_padding_vertical_material">@dimen/abc_control_padding_material</item>
                <dimen name="abc_cascading_menus_min_smallest_width">720.0dip</dimen>
                <dimen name="abc_config_prefDialogWidth">320.0dip</dimen>
                <dimen name="abc_control_corner_material">2.0dip</dimen>
                <dimen name="abc_control_inset_material">4.0dip</dimen>
                <dimen name="abc_control_padding_material">4.0dip</dimen>
                <dimen name="abc_dialog_corner_radius_material">2.0dip</dimen>
                <item type="dimen" name="abc_dialog_fixed_height_major">79.99999%</item>
                <item type="dimen" name="abc_dialog_fixed_height_minor">100.0%</item>
                <dimen name="abc_dialog_fixed_width_major">320.0dip</dimen>
                <dimen name="abc_dialog_fixed_width_minor">320.0dip</dimen>
                <dimen name="abc_dialog_list_padding_bottom_no_buttons">8.0dip</dimen>
                <dimen name="abc_dialog_list_padding_top_no_title">8.0dip</dimen>
                <item type="dimen" name="abc_dialog_min_width_major">65.0%</item>
                <item type="dimen" name="abc_dialog_min_width_minor">95.00001%</item>
                <dimen name="abc_dialog_padding_material">24.0dip</dimen>
                <dimen name="abc_dialog_padding_top_material">18.0dip</dimen>
                <dimen name="abc_dialog_title_divider_material">8.0dip</dimen>
                <item type="dimen" name="abc_disabled_alpha_material_dark">0.3</item>
                <item type="dimen" name="abc_disabled_alpha_material_light">0.26</item>
                <dimen name="abc_dropdownitem_icon_width">32.0dip</dimen>
                <dimen name="abc_dropdownitem_text_padding_left">8.0dip</dimen>
                <dimen name="abc_dropdownitem_text_padding_right">8.0dip</dimen>
                <dimen name="abc_edit_text_inset_bottom_material">7.0dip</dimen>
                <dimen name="abc_edit_text_inset_horizontal_material">4.0dip</dimen>
                <dimen name="abc_edit_text_inset_top_material">10.0dip</dimen>
                <dimen name="abc_floating_window_z">16.0dip</dimen>
                <item type="dimen" name="abc_list_item_padding_horizontal_material">@dimen/abc_action_bar_content_inset_material</item>
                <dimen name="abc_panel_menu_list_width">296.0dip</dimen>
                <dimen name="abc_progress_bar_height_material">4.0dip</dimen>
                <dimen name="abc_search_view_preferred_height">48.0dip</dimen>
                <dimen name="abc_search_view_preferred_width">320.0dip</dimen>
                <dimen name="abc_seekbar_track_background_height_material">2.0dip</dimen>
                <dimen name="abc_seekbar_track_progress_height_material">2.0dip</dimen>
                <dimen name="abc_select_dialog_padding_start_material">20.0dip</dimen>
                <dimen name="abc_switch_padding">0.0px</dimen>
                <dimen name="abc_text_size_body_1_material">14.0sp</dimen>
                <dimen name="abc_text_size_body_2_material">14.0sp</dimen>
                <dimen name="abc_text_size_button_material">14.0sp</dimen>
                <dimen name="abc_text_size_caption_material">12.0sp</dimen>
                <dimen name="abc_text_size_display_1_material">34.0sp</dimen>
                <dimen name="abc_text_size_display_2_material">45.0sp</dimen>
                <dimen name="abc_text_size_display_3_material">56.0sp</dimen>
                <dimen name="abc_text_size_display_4_material">112.0sp</dimen>
                <dimen name="abc_text_size_headline_material">24.0sp</dimen>
                <dimen name="abc_text_size_large_material">22.0sp</dimen>
                <dimen name="abc_text_size_medium_material">18.0sp</dimen>
                <dimen name="abc_text_size_menu_header_material">14.0sp</dimen>
                <dimen name="abc_text_size_menu_material">16.0sp</dimen>
                <dimen name="abc_text_size_small_material">14.0sp</dimen>
                <dimen name="abc_text_size_subhead_material">16.0sp</dimen>
                <dimen name="abc_text_size_subtitle_material_toolbar">16.0dip</dimen>
                <dimen name="abc_text_size_title_material">20.0sp</dimen>
                <dimen name="abc_text_size_title_material_toolbar">20.0dip</dimen>
                <dimen name="cardview_compat_inset_shadow">1.0dip</dimen>
                <dimen name="cardview_default_elevation">2.0dip</dimen>
                <dimen name="cardview_default_radius">2.0dip</dimen>
                <dimen name="compat_button_inset_horizontal_material">4.0dip</dimen>
                <dimen name="compat_button_inset_vertical_material">6.0dip</dimen>
                <dimen name="compat_button_padding_horizontal_material">8.0dip</dimen>
                <dimen name="compat_button_padding_vertical_material">4.0dip</dimen>
                <dimen name="compat_control_corner_material">2.0dip</dimen>
                <dimen name="compat_notification_large_icon_max_height">320.0dip</dimen>
                <dimen name="compat_notification_large_icon_max_width">320.0dip</dimen>
                <dimen name="design_appbar_elevation">4.0dip</dimen>
                <dimen name="design_bottom_navigation_active_item_max_width">168.0dip</dimen>
                <dimen name="design_bottom_navigation_active_item_min_width">96.0dip</dimen>
                <dimen name="design_bottom_navigation_active_text_size">14.0sp</dimen>
                <dimen name="design_bottom_navigation_elevation">8.0dip</dimen>
                <dimen name="design_bottom_navigation_height">56.0dip</dimen>
                <dimen name="design_bottom_navigation_icon_size">24.0dip</dimen>
                <dimen name="design_bottom_navigation_item_max_width">96.0dip</dimen>
                <dimen name="design_bottom_navigation_item_min_width">56.0dip</dimen>
                <dimen name="design_bottom_navigation_margin">8.0dip</dimen>
                <dimen name="design_bottom_navigation_shadow_height">1.0dip</dimen>
                <dimen name="design_bottom_navigation_text_size">12.0sp</dimen>
                <dimen name="design_bottom_sheet_modal_elevation">16.0dip</dimen>
                <dimen name="design_bottom_sheet_peek_height_min">64.0dip</dimen>
                <dimen name="design_fab_border_width">0.5dip</dimen>
                <dimen name="design_fab_elevation">6.0dip</dimen>
                <dimen name="design_fab_image_size">24.0dip</dimen>
                <dimen name="design_fab_size_mini">40.0dip</dimen>
                <dimen name="design_fab_size_normal">56.0dip</dimen>
                <dimen name="design_fab_translation_z_hovered_focused">6.0dip</dimen>
                <dimen name="design_fab_translation_z_pressed">6.0dip</dimen>
                <dimen name="design_navigation_elevation">16.0dip</dimen>
                <dimen name="design_navigation_icon_padding">32.0dip</dimen>
                <dimen name="design_navigation_icon_size">24.0dip</dimen>
                <dimen name="design_navigation_item_horizontal_padding">16.0dip</dimen>
                <dimen name="design_navigation_item_icon_padding">32.0dip</dimen>
                <dimen name="design_navigation_max_width">280.0dip</dimen>
                <dimen name="design_navigation_padding_bottom">8.0dip</dimen>
                <dimen name="design_navigation_separator_vertical_padding">8.0dip</dimen>
                <dimen name="design_snackbar_action_inline_max_width">128.0dip</dimen>
                <dimen name="design_snackbar_background_corner_radius">0.0dip</dimen>
                <dimen name="design_snackbar_elevation">6.0dip</dimen>
                <dimen name="design_snackbar_extra_spacing_horizontal">0.0dip</dimen>
                <dimen name="design_snackbar_max_width">-1.0px</dimen>
                <dimen name="design_snackbar_min_width">-1.0px</dimen>
                <dimen name="design_snackbar_padding_horizontal">12.0dip</dimen>
                <dimen name="design_snackbar_padding_vertical">14.0dip</dimen>
                <dimen name="design_snackbar_padding_vertical_2lines">24.0dip</dimen>
                <dimen name="design_snackbar_text_size">14.0sp</dimen>
                <dimen name="design_tab_max_width">264.0dip</dimen>
                <dimen name="design_tab_scrollable_min_width">72.0dip</dimen>
                <dimen name="design_tab_text_size">14.0sp</dimen>
                <dimen name="design_tab_text_size_2line">12.0sp</dimen>
                <dimen name="design_textinput_caption_translate_y">5.0dip</dimen>
                <item type="dimen" name="disabled_alpha_material_dark">0.3</item>
                <item type="dimen" name="disabled_alpha_material_light">0.26</item>
                <dimen name="fastscroll_default_thickness">8.0dip</dimen>
                <dimen name="fastscroll_margin">0.0dip</dimen>
                <dimen name="fastscroll_minimum_range">50.0dip</dimen>
                <item type="dimen" name="highlight_alpha_material_colored">0.26</item>
                <item type="dimen" name="highlight_alpha_material_dark">0.2</item>
                <item type="dimen" name="highlight_alpha_material_light">0.12</item>
                <item type="dimen" name="hint_alpha_material_dark">0.5</item>
                <item type="dimen" name="hint_alpha_material_light">0.38</item>
                <item type="dimen" name="hint_pressed_alpha_material_dark">0.7</item>
                <item type="dimen" name="hint_pressed_alpha_material_light">0.54</item>
                <dimen name="item_touch_helper_max_drag_scroll_per_frame">20.0dip</dimen>
                <dimen name="item_touch_helper_swipe_escape_max_velocity">800.0dip</dimen>
                <dimen name="item_touch_helper_swipe_escape_velocity">120.0dip</dimen>
                <dimen name="mohist_utility_large_pad_min_height">700.0dip</dimen>
                <dimen name="mohist_utility_large_pad_min_width">540.0dip</dimen>
                <dimen name="mtrl_bottomappbar_fabOffsetEndMode">60.0dip</dimen>
                <dimen name="mtrl_bottomappbar_fab_cradle_margin">5.0dip</dimen>
                <dimen name="mtrl_bottomappbar_fab_cradle_rounded_corner_radius">8.0dip</dimen>
                <dimen name="mtrl_bottomappbar_fab_cradle_vertical_offset">0.0dip</dimen>
                <dimen name="mtrl_bottomappbar_height">56.0dip</dimen>
                <dimen name="mtrl_btn_corner_radius">4.0dip</dimen>
                <dimen name="mtrl_btn_dialog_btn_min_width">64.0dip</dimen>
                <dimen name="mtrl_btn_disabled_elevation">0.0dip</dimen>
                <dimen name="mtrl_btn_disabled_z">0.0dip</dimen>
                <dimen name="mtrl_btn_elevation">2.0dip</dimen>
                <dimen name="mtrl_btn_focused_z">2.0dip</dimen>
                <dimen name="mtrl_btn_hovered_z">2.0dip</dimen>
                <dimen name="mtrl_btn_icon_btn_padding_left">12.0dip</dimen>
                <dimen name="mtrl_btn_icon_padding">8.0dip</dimen>
                <dimen name="mtrl_btn_inset">6.0dip</dimen>
                <item type="dimen" name="mtrl_btn_letter_spacing">0.07</item>
                <dimen name="mtrl_btn_padding_bottom">4.0dip</dimen>
                <dimen name="mtrl_btn_padding_left">16.0dip</dimen>
                <dimen name="mtrl_btn_padding_right">16.0dip</dimen>
                <dimen name="mtrl_btn_padding_top">4.0dip</dimen>
                <dimen name="mtrl_btn_pressed_z">6.0dip</dimen>
                <dimen name="mtrl_btn_stroke_size">1.0dip</dimen>
                <dimen name="mtrl_btn_text_btn_icon_padding">4.0dip</dimen>
                <dimen name="mtrl_btn_text_btn_padding_left">8.0dip</dimen>
                <dimen name="mtrl_btn_text_btn_padding_right">8.0dip</dimen>
                <dimen name="mtrl_btn_text_size">14.0sp</dimen>
                <dimen name="mtrl_btn_z">0.0dip</dimen>
                <dimen name="mtrl_card_elevation">1.0dip</dimen>
                <dimen name="mtrl_card_spacing">8.0dip</dimen>
                <dimen name="mtrl_chip_pressed_translation_z">3.0dip</dimen>
                <dimen name="mtrl_chip_text_size">14.0sp</dimen>
                <dimen name="mtrl_fab_elevation">6.0dip</dimen>
                <dimen name="mtrl_fab_translation_z_hovered_focused">2.0dip</dimen>
                <dimen name="mtrl_fab_translation_z_pressed">6.0dip</dimen>
                <dimen name="mtrl_navigation_elevation">0.0dip</dimen>
                <dimen name="mtrl_navigation_item_horizontal_padding">22.0dip</dimen>
                <dimen name="mtrl_navigation_item_icon_padding">14.0dip</dimen>
                <dimen name="mtrl_snackbar_background_corner_radius">4.0dip</dimen>
                <dimen name="mtrl_snackbar_margin">8.0dip</dimen>
                <dimen name="mtrl_textinput_box_bottom_offset">3.0dip</dimen>
                <dimen name="mtrl_textinput_box_corner_radius_medium">4.0dip</dimen>
                <dimen name="mtrl_textinput_box_corner_radius_small">0.0dip</dimen>
                <dimen name="mtrl_textinput_box_label_cutout_padding">4.0dip</dimen>
                <dimen name="mtrl_textinput_box_padding_end">12.0dip</dimen>
                <dimen name="mtrl_textinput_box_stroke_width_default">1.0dip</dimen>
                <dimen name="mtrl_textinput_box_stroke_width_focused">2.0dip</dimen>
                <dimen name="mtrl_textinput_outline_box_expanded_padding">16.0dip</dimen>
                <dimen name="mtrl_toolbar_default_height">56.0dip</dimen>
                <dimen name="notification_action_icon_size">32.0dip</dimen>
                <dimen name="notification_action_text_size">13.0sp</dimen>
                <dimen name="notification_big_circle_margin">12.0dip</dimen>
                <dimen name="notification_content_margin_start">0.0dip</dimen>
                <dimen name="notification_large_icon_height">64.0dip</dimen>
                <dimen name="notification_large_icon_width">64.0dip</dimen>
                <dimen name="notification_main_column_padding_top">0.0dip</dimen>
                <dimen name="notification_media_narrow_margin">12.0dip</dimen>
                <dimen name="notification_right_icon_size">16.0dip</dimen>
                <dimen name="notification_right_side_padding_top">4.0dip</dimen>
                <dimen name="notification_small_icon_background_padding">3.0dip</dimen>
                <dimen name="notification_small_icon_size_as_large">24.0dip</dimen>
                <dimen name="notification_subtext_size">13.0sp</dimen>
                <dimen name="notification_top_pad">10.0dip</dimen>
                <dimen name="notification_top_pad_large_text">5.0dip</dimen>
                <dimen name="subtitle_corner_radius">2.0dip</dimen>
                <dimen name="subtitle_outline_width">2.0dip</dimen>
                <dimen name="subtitle_shadow_offset">2.0dip</dimen>
                <dimen name="subtitle_shadow_radius">2.0dip</dimen>
                <dimen name="tooltip_corner_radius">2.0dip</dimen>
                <dimen name="tooltip_horizontal_padding">16.0dip</dimen>
                <dimen name="tooltip_margin">8.0dip</dimen>
                <dimen name="tooltip_precise_anchor_extra_offset">8.0dip</dimen>
                <dimen name="tooltip_precise_anchor_threshold">96.0dip</dimen>
                <dimen name="tooltip_vertical_padding">6.5dip</dimen>
                <dimen name="tooltip_y_offset_non_touch">0.0dip</dimen>
                <dimen name="tooltip_y_offset_touch">16.0dip</dimen>
            </resources>
        """.trimIndent()
        val drawables = """
                <item type="drawable" name="notification_template_icon_bg">#3333b5e5</item>
                <item type="drawable" name="notification_template_icon_low_bg">#0cffffff</item>
            </resources>
        """.trimIndent()
        val attrs = """
                <attr name="actionBarDivider" format="reference" />
                <attr name="actionBarItemBackground" format="reference" />
                <attr name="actionBarPopupTheme" format="reference" />
                <attr name="actionBarSize" format="dimension">
                    <enum name="wrap_content" value="0" />
                </attr>
                <attr name="actionBarSplitStyle" format="reference" />
                <attr name="actionBarStyle" format="reference" />
                <attr name="actionBarTabBarStyle" format="reference" />
                <attr name="actionBarTabStyle" format="reference" />
                <attr name="actionBarTabTextStyle" format="reference" />
                <attr name="actionBarTheme" format="reference" />
                <attr name="actionBarWidgetTheme" format="reference" />
                <attr name="actionButtonStyle" format="reference" />
                <attr name="actionDropDownStyle" format="reference" />
                <attr name="actionLayout" format="reference" />
                <attr name="actionMenuTextAppearance" format="reference" />
                <attr name="actionMenuTextColor" format="reference|color" />
                <attr name="actionModeBackground" format="reference" />
                <attr name="actionModeCloseButtonStyle" format="reference" />
                <attr name="actionModeCloseDrawable" format="reference" />
                <attr name="actionModeCopyDrawable" format="reference" />
                <attr name="actionModeCutDrawable" format="reference" />
                <attr name="actionModeFindDrawable" format="reference" />
                <attr name="actionModePasteDrawable" format="reference" />
                <attr name="actionModePopupWindowStyle" format="reference" />
                <attr name="actionModeSelectAllDrawable" format="reference" />
                <attr name="actionModeShareDrawable" format="reference" />
                <attr name="actionModeSplitBackground" format="reference" />
                <attr name="actionModeStyle" format="reference" />
                <attr name="actionModeWebSearchDrawable" format="reference" />
                <attr name="actionOverflowButtonStyle" format="reference" />
                <attr name="actionOverflowMenuStyle" format="reference" />
                <attr name="actionProviderClass" format="string" />
                <attr name="actionViewClass" format="string" />
                <attr name="activityChooserViewStyle" format="reference" />
                <attr name="alertDialogButtonGroupStyle" format="reference" />
                <attr name="alertDialogCenterButtons" format="boolean" />
                <attr name="alertDialogStyle" format="reference" />
                <attr name="alertDialogTheme" format="reference" />
                <attr name="allowStacking" format="boolean" />
                <attr name="alpha" format="float" />
                <attr name="alphabeticModifiers">
                    <flag name="META" value="0x00010000" />
                    <flag name="CTRL" value="0x00001000" />
                    <flag name="ALT" value="0x00000002" />
                    <flag name="SHIFT" value="0x00000001" />
                    <flag name="SYM" value="0x00000004" />
                    <flag name="FUNCTION" value="0x00000008" />
                </attr>
                <attr name="arrowHeadLength" format="dimension" />
                <attr name="arrowShaftLength" format="dimension" />
                <attr name="autoCompleteTextViewStyle" format="reference" />
                <attr name="autoSizeMaxTextSize" format="dimension" />
                <attr name="autoSizeMinTextSize" format="dimension" />
                <attr name="autoSizePresetSizes" format="reference" />
                <attr name="autoSizeStepGranularity" format="dimension" />
                <attr name="autoSizeTextType">
                    <enum name="none" value="0" />
                    <enum name="uniform" value="1" />
                </attr>
                <attr name="background" format="reference" />
                <attr name="backgroundSplit" format="reference|color" />
                <attr name="backgroundStacked" format="reference|color" />
                <attr name="backgroundTint" format="color" />
                <attr name="backgroundTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="barLength" format="dimension" />
                <attr name="barrierAllowsGoneWidgets" format="boolean" />
                <attr name="barrierDirection">
                    <enum name="left" value="0" />
                    <enum name="right" value="1" />
                    <enum name="top" value="2" />
                    <enum name="bottom" value="3" />
                    <enum name="start" value="5" />
                    <enum name="end" value="6" />
                </attr>
                <attr name="behavior_autoHide" format="boolean" />
                <attr name="behavior_fitToContents" format="boolean" />
                <attr name="behavior_hideable" format="boolean" />
                <attr name="behavior_overlapTop" format="dimension" />
                <attr name="behavior_peekHeight" format="dimension">
                    <enum name="auto" value="-1" />
                </attr>
                <attr name="behavior_skipCollapsed" format="boolean" />
                <attr name="borderWidth" format="dimension" />
                <attr name="borderlessButtonStyle" format="reference" />
                <attr name="bottomAppBarStyle" format="reference" />
                <attr name="bottomNavigationStyle" format="reference" />
                <attr name="bottomSheetDialogTheme" format="reference" />
                <attr name="bottomSheetStyle" format="reference" />
                <attr name="boxBackgroundColor" format="color" />
                <attr name="boxBackgroundMode">
                    <enum name="none" value="0" />
                    <enum name="filled" value="1" />
                    <enum name="outline" value="2" />
                </attr>
                <attr name="boxCollapsedPaddingTop" format="dimension" />
                <attr name="boxCornerRadiusBottomEnd" format="dimension" />
                <attr name="boxCornerRadiusBottomStart" format="dimension" />
                <attr name="boxCornerRadiusTopEnd" format="dimension" />
                <attr name="boxCornerRadiusTopStart" format="dimension" />
                <attr name="boxStrokeColor" format="color" />
                <attr name="boxStrokeWidth" format="dimension" />
                <attr name="buttonBarButtonStyle" format="reference" />
                <attr name="buttonBarNegativeButtonStyle" format="reference" />
                <attr name="buttonBarNeutralButtonStyle" format="reference" />
                <attr name="buttonBarPositiveButtonStyle" format="reference" />
                <attr name="buttonBarStyle" format="reference" />
                <attr name="buttonGravity">
                    <flag name="top" value="0x00000030" />
                    <flag name="bottom" value="0x00000050" />
                </attr>
                <attr name="buttonIconDimen" format="dimension" />
                <attr name="buttonPanelSideLayout" format="reference" />
                <attr name="buttonStyle" format="reference" />
                <attr name="buttonStyleSmall" format="reference" />
                <attr name="buttonTint" format="color" />
                <attr name="buttonTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="cardBackgroundColor" format="color" />
                <attr name="cardCornerRadius" format="dimension" />
                <attr name="cardElevation" format="dimension" />
                <attr name="cardMaxElevation" format="dimension" />
                <attr name="cardPreventCornerOverlap" format="boolean" />
                <attr name="cardUseCompatPadding" format="boolean" />
                <attr name="cardViewStyle" format="reference" />
                <attr name="chainUseRtl" format="boolean" />
                <attr name="checkboxStyle" format="reference" />
                <attr name="checkedChip" format="reference" />
                <attr name="checkedIcon" format="reference" />
                <attr name="checkedIconEnabled" format="boolean" />
                <attr name="checkedIconVisible" format="boolean" />
                <attr name="checkedTextViewStyle" format="reference" />
                <attr name="chipBackgroundColor" format="color" />
                <attr name="chipCornerRadius" format="dimension" />
                <attr name="chipEndPadding" format="dimension" />
                <attr name="chipGroupStyle" format="reference" />
                <attr name="chipIcon" format="reference" />
                <attr name="chipIconEnabled" format="boolean" />
                <attr name="chipIconSize" format="dimension" />
                <attr name="chipIconTint" format="color" />
                <attr name="chipIconVisible" format="boolean" />
                <attr name="chipMinHeight" format="dimension" />
                <attr name="chipSpacing" format="dimension" />
                <attr name="chipSpacingHorizontal" format="dimension" />
                <attr name="chipSpacingVertical" format="dimension" />
                <attr name="chipStandaloneStyle" format="reference" />
                <attr name="chipStartPadding" format="dimension" />
                <attr name="chipStrokeColor" format="color" />
                <attr name="chipStrokeWidth" format="dimension" />
                <attr name="chipStyle" format="reference" />
                <attr name="closeIcon" format="reference" />
                <attr name="closeIconEnabled" format="boolean" />
                <attr name="closeIconEndPadding" format="dimension" />
                <attr name="closeIconSize" format="dimension" />
                <attr name="closeIconStartPadding" format="dimension" />
                <attr name="closeIconTint" format="color" />
                <attr name="closeIconVisible" format="boolean" />
                <attr name="closeItemLayout" format="reference" />
                <attr name="collapseContentDescription" format="string" />
                <attr name="collapseIcon" format="reference" />
                <attr name="collapsedTitleGravity">
                    <flag name="top" value="0x00000030" />
                    <flag name="bottom" value="0x00000050" />
                    <flag name="left" value="0x00000003" />
                    <flag name="right" value="0x00000005" />
                    <flag name="center_vertical" value="0x00000010" />
                    <flag name="fill_vertical" value="0x00000070" />
                    <flag name="center_horizontal" value="0x00000001" />
                    <flag name="center" value="0x00000011" />
                    <flag name="start" value="0x00800003" />
                    <flag name="end" value="0x00800005" />
                </attr>
                <attr name="collapsedTitleTextAppearance" format="reference" />
                <attr name="color" format="color" />
                <attr name="colorAccent" format="color" />
                <attr name="colorBackgroundFloating" format="color" />
                <attr name="colorButtonNormal" format="color" />
                <attr name="colorControlActivated" format="color" />
                <attr name="colorControlHighlight" format="color" />
                <attr name="colorControlNormal" format="color" />
                <attr name="colorError" format="reference|color" />
                <attr name="colorPrimary" format="color" />
                <attr name="colorPrimaryDark" format="color" />
                <attr name="colorSecondary" format="color" />
                <attr name="colorSwitchThumbNormal" format="color" />
                <attr name="commitIcon" format="reference" />
                <attr name="constraintSet" format="reference" />
                <attr name="constraint_referenced_ids" format="string" />
                <attr name="content" format="reference" />
                <attr name="contentDescription" format="string" />
                <attr name="contentInsetEnd" format="dimension" />
                <attr name="contentInsetEndWithActions" format="dimension" />
                <attr name="contentInsetLeft" format="dimension" />
                <attr name="contentInsetRight" format="dimension" />
                <attr name="contentInsetStart" format="dimension" />
                <attr name="contentInsetStartWithNavigation" format="dimension" />
                <attr name="contentPadding" format="dimension" />
                <attr name="contentPaddingBottom" format="dimension" />
                <attr name="contentPaddingLeft" format="dimension" />
                <attr name="contentPaddingRight" format="dimension" />
                <attr name="contentPaddingTop" format="dimension" />
                <attr name="contentScrim" format="color" />
                <attr name="controlBackground" format="reference" />
                <attr name="coordinatorLayoutStyle" format="reference" />
                <attr name="cornerRadius" format="dimension" />
                <attr name="counterEnabled" format="boolean" />
                <attr name="counterMaxLength" format="integer" />
                <attr name="counterOverflowTextAppearance" format="reference" />
                <attr name="counterTextAppearance" format="reference" />
                <attr name="customNavigationLayout" format="reference" />
                <attr name="defaultQueryHint" format="string" />
                <attr name="dialogCornerRadius" format="dimension" />
                <attr name="dialogPreferredPadding" format="dimension" />
                <attr name="dialogTheme" format="reference" />
                <attr name="displayOptions">
                    <flag name="none" value="0x00000000" />
                    <flag name="useLogo" value="0x00000001" />
                    <flag name="showHome" value="0x00000002" />
                    <flag name="homeAsUp" value="0x00000004" />
                    <flag name="showTitle" value="0x00000008" />
                    <flag name="showCustom" value="0x00000010" />
                    <flag name="disableHome" value="0x00000020" />
                </attr>
                <attr name="divider" format="reference" />
                <attr name="dividerHorizontal" format="reference" />
                <attr name="dividerPadding" format="dimension" />
                <attr name="dividerVertical" format="reference" />
                <attr name="drawableSize" format="dimension" />
                <attr name="drawerArrowStyle" format="reference" />
                <attr name="dropDownListViewStyle" format="reference" />
                <attr name="dropdownListPreferredItemHeight" format="dimension" />
                <attr name="editTextBackground" format="reference" />
                <attr name="editTextColor" format="reference|color" />
                <attr name="editTextStyle" format="reference" />
                <attr name="elevation" format="dimension" />
                <attr name="emptyVisibility">
                    <enum name="gone" value="0" />
                    <enum name="invisible" value="1" />
                </attr>
                <attr name="enforceMaterialTheme" format="boolean" />
                <attr name="enforceTextAppearance" format="boolean" />
                <attr name="errorEnabled" format="boolean" />
                <attr name="errorTextAppearance" format="reference" />
                <attr name="expandActivityOverflowButtonDrawable" format="reference" />
                <attr name="expanded" format="boolean" />
                <attr name="expandedTitleGravity">
                    <flag name="top" value="0x00000030" />
                    <flag name="bottom" value="0x00000050" />
                    <flag name="left" value="0x00000003" />
                    <flag name="right" value="0x00000005" />
                    <flag name="center_vertical" value="0x00000010" />
                    <flag name="fill_vertical" value="0x00000070" />
                    <flag name="center_horizontal" value="0x00000001" />
                    <flag name="center" value="0x00000011" />
                    <flag name="start" value="0x00800003" />
                    <flag name="end" value="0x00800005" />
                </attr>
                <attr name="expandedTitleMargin" format="dimension" />
                <attr name="expandedTitleMarginBottom" format="dimension" />
                <attr name="expandedTitleMarginEnd" format="dimension" />
                <attr name="expandedTitleMarginStart" format="dimension" />
                <attr name="expandedTitleMarginTop" format="dimension" />
                <attr name="expandedTitleTextAppearance" format="reference" />
                <attr name="fabAlignmentMode">
                    <enum name="center" value="0" />
                    <enum name="end" value="1" />
                </attr>
                <attr name="fabCradleMargin" format="dimension" />
                <attr name="fabCradleRoundedCornerRadius" format="dimension" />
                <attr name="fabCradleVerticalOffset" format="dimension" />
                <attr name="fabCustomSize" format="dimension" />
                <attr name="fabSize">
                    <enum name="auto" value="-1" />
                    <enum name="normal" value="0" />
                    <enum name="mini" value="1" />
                </attr>
                <attr name="fastScrollEnabled" format="boolean" />
                <attr name="fastScrollHorizontalThumbDrawable" format="reference" />
                <attr name="fastScrollHorizontalTrackDrawable" format="reference" />
                <attr name="fastScrollVerticalThumbDrawable" format="reference" />
                <attr name="fastScrollVerticalTrackDrawable" format="reference" />
                <attr name="firstBaselineToTopHeight" format="dimension" />
                <attr name="floatingActionButtonStyle" format="reference|string|integer|boolean|color|float|dimension|fraction" />
                <attr name="font" format="reference" />
                <attr name="fontFamily" format="string" />
                <attr name="fontProviderAuthority" format="string" />
                <attr name="fontProviderCerts" format="reference" />
                <attr name="fontProviderFetchStrategy">
                    <enum name="blocking" value="0" />
                    <enum name="async" value="1" />
                </attr>
                <attr name="fontProviderFetchTimeout" format="integer">
                    <enum name="forever" value="-1" />
                </attr>
                <attr name="fontProviderPackage" format="string" />
                <attr name="fontProviderQuery" format="string" />
                <attr name="fontStyle">
                    <enum name="normal" value="0" />
                    <enum name="italic" value="1" />
                </attr>
                <attr name="fontVariationSettings" format="string" />
                <attr name="fontWeight" format="integer" />
                <attr name="foregroundInsidePadding" format="boolean" />
                <attr name="gapBetweenBars" format="dimension" />
                <attr name="goIcon" format="reference" />
                <attr name="headerLayout" format="reference" />
                <attr name="height" format="dimension" />
                <attr name="helperText" format="string" />
                <attr name="helperTextEnabled" format="boolean" />
                <attr name="helperTextTextAppearance" format="reference" />
                <attr name="hideMotionSpec" format="reference" />
                <attr name="hideOnContentScroll" format="boolean" />
                <attr name="hideOnScroll" format="boolean" />
                <attr name="hintAnimationEnabled" format="boolean" />
                <attr name="hintEnabled" format="boolean" />
                <attr name="hintTextAppearance" format="reference" />
                <attr name="homeAsUpIndicator" format="reference" />
                <attr name="homeLayout" format="reference" />
                <attr name="hoveredFocusedTranslationZ" format="dimension" />
                <attr name="icon" format="reference" />
                <attr name="iconEndPadding" format="dimension" />
                <attr name="iconGravity">
                    <flag name="start" value="0x00000001" />
                    <flag name="textStart" value="0x00000002" />
                </attr>
                <attr name="iconPadding" format="dimension" />
                <attr name="iconSize" format="dimension" />
                <attr name="iconStartPadding" format="dimension" />
                <attr name="iconTint" format="color" />
                <attr name="iconTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="iconifiedByDefault" format="boolean" />
                <attr name="imageButtonStyle" format="reference" />
                <attr name="indeterminateProgressStyle" format="reference" />
                <attr name="initialActivityCount" format="string" />
                <attr name="insetForeground" format="reference|color" />
                <attr name="isLightTheme" format="boolean" />
                <attr name="itemBackground" format="reference" />
                <attr name="itemHorizontalPadding" format="dimension" />
                <attr name="itemHorizontalTranslationEnabled" format="boolean" />
                <attr name="itemIconPadding" format="dimension" />
                <attr name="itemIconSize" format="dimension" />
                <attr name="itemIconTint" format="color" />
                <attr name="itemPadding" format="dimension" />
                <attr name="itemSpacing" format="dimension" />
                <attr name="itemTextAppearance" format="reference" />
                <attr name="itemTextAppearanceActive" format="reference" />
                <attr name="itemTextAppearanceInactive" format="reference" />
                <attr name="itemTextColor" format="color" />
                <attr name="keylines" format="reference" />
                <attr name="labelVisibilityMode">
                    <enum name="auto" value="-1" />
                    <enum name="selected" value="0" />
                    <enum name="labeled" value="1" />
                    <enum name="unlabeled" value="2" />
                </attr>
                <attr name="lastBaselineToBottomHeight" format="dimension" />
                <attr name="layout" format="reference" />
                <attr name="layoutManager" format="string" />
                <attr name="layout_anchor" format="reference" />
                <attr name="layout_anchorGravity">
                    <flag name="top" value="0x00000030" />
                    <flag name="bottom" value="0x00000050" />
                    <flag name="left" value="0x00000003" />
                    <flag name="right" value="0x00000005" />
                    <flag name="center_vertical" value="0x00000010" />
                    <flag name="fill_vertical" value="0x00000070" />
                    <flag name="center_horizontal" value="0x00000001" />
                    <flag name="fill_horizontal" value="0x00000007" />
                    <flag name="center" value="0x00000011" />
                    <flag name="fill" value="0x00000077" />
                    <flag name="clip_vertical" value="0x00000080" />
                    <flag name="clip_horizontal" value="0x00000008" />
                    <flag name="start" value="0x00800003" />
                    <flag name="end" value="0x00800005" />
                </attr>
                <attr name="layout_behavior" format="string" />
                <attr name="layout_collapseMode">
                    <enum name="none" value="0" />
                    <enum name="pin" value="1" />
                    <enum name="parallax" value="2" />
                </attr>
                <attr name="layout_collapseParallaxMultiplier" format="float" />
                <attr name="layout_constrainedHeight" format="boolean" />
                <attr name="layout_constrainedWidth" format="boolean" />
                <attr name="layout_constraintBaseline_creator" format="integer" />
                <attr name="layout_constraintBaseline_toBaselineOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintBottom_creator" format="integer" />
                <attr name="layout_constraintBottom_toBottomOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintBottom_toTopOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintCircle" format="reference" />
                <attr name="layout_constraintCircleAngle" format="integer" />
                <attr name="layout_constraintCircleRadius" format="dimension" />
                <attr name="layout_constraintDimensionRatio" format="string" />
                <attr name="layout_constraintEnd_toEndOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintEnd_toStartOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintGuide_begin" format="dimension" />
                <attr name="layout_constraintGuide_end" format="dimension" />
                <attr name="layout_constraintGuide_percent" format="float" />
                <attr name="layout_constraintHeight_default">
                    <enum name="spread" value="0" />
                    <enum name="wrap" value="1" />
                    <enum name="percent" value="2" />
                </attr>
                <attr name="layout_constraintHeight_max" format="dimension">
                    <enum name="wrap" value="-2" />
                </attr>
                <attr name="layout_constraintHeight_min" format="dimension">
                    <enum name="wrap" value="-2" />
                </attr>
                <attr name="layout_constraintHeight_percent" format="float" />
                <attr name="layout_constraintHorizontal_bias" format="float" />
                <attr name="layout_constraintHorizontal_chainStyle">
                    <enum name="spread" value="0" />
                    <enum name="spread_inside" value="1" />
                    <enum name="packed" value="2" />
                </attr>
                <attr name="layout_constraintHorizontal_weight" format="float" />
                <attr name="layout_constraintLeft_creator" format="integer" />
                <attr name="layout_constraintLeft_toLeftOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintLeft_toRightOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintRight_creator" format="integer" />
                <attr name="layout_constraintRight_toLeftOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintRight_toRightOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintStart_toEndOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintStart_toStartOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintTop_creator" format="integer" />
                <attr name="layout_constraintTop_toBottomOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintTop_toTopOf" format="reference">
                    <enum name="parent" value="0" />
                </attr>
                <attr name="layout_constraintVertical_bias" format="float" />
                <attr name="layout_constraintVertical_chainStyle">
                    <enum name="spread" value="0" />
                    <enum name="spread_inside" value="1" />
                    <enum name="packed" value="2" />
                </attr>
                <attr name="layout_constraintVertical_weight" format="float" />
                <attr name="layout_constraintWidth_default">
                    <enum name="spread" value="0" />
                    <enum name="wrap" value="1" />
                    <enum name="percent" value="2" />
                </attr>
                <attr name="layout_constraintWidth_max" format="dimension">
                    <enum name="wrap" value="-2" />
                </attr>
                <attr name="layout_constraintWidth_min" format="dimension">
                    <enum name="wrap" value="-2" />
                </attr>
                <attr name="layout_constraintWidth_percent" format="float" />
                <attr name="layout_dodgeInsetEdges">
                    <flag name="none" value="0x00000000" />
                    <flag name="top" value="0x00000030" />
                    <flag name="bottom" value="0x00000050" />
                    <flag name="left" value="0x00000003" />
                    <flag name="right" value="0x00000005" />
                    <flag name="start" value="0x00800003" />
                    <flag name="end" value="0x00800005" />
                    <flag name="all" value="0x00000077" />
                </attr>
                <attr name="layout_editor_absoluteX" format="dimension" />
                <attr name="layout_editor_absoluteY" format="dimension" />
                <attr name="layout_goneMarginBottom" format="dimension" />
                <attr name="layout_goneMarginEnd" format="dimension" />
                <attr name="layout_goneMarginLeft" format="dimension" />
                <attr name="layout_goneMarginRight" format="dimension" />
                <attr name="layout_goneMarginStart" format="dimension" />
                <attr name="layout_goneMarginTop" format="dimension" />
                <attr name="layout_insetEdge">
                    <enum name="none" value="0" />
                    <enum name="top" value="48" />
                    <enum name="bottom" value="80" />
                    <enum name="left" value="3" />
                    <enum name="right" value="5" />
                    <enum name="start" value="8388611" />
                    <enum name="end" value="8388613" />
                </attr>
                <attr name="layout_keyline" format="integer" />
                <attr name="layout_optimizationLevel">
                    <flag name="none" value="0x00000000" />
                    <flag name="standard" value="0x00000007" />
                    <flag name="direct" value="0x00000001" />
                    <flag name="barrier" value="0x00000002" />
                    <flag name="chains" value="0x00000004" />
                    <flag name="dimensions" value="0x00000008" />
                    <flag name="groups" value="0x00000020" />
                </attr>
                <attr name="layout_scrollFlags">
                    <flag name="scroll" value="0x00000001" />
                    <flag name="exitUntilCollapsed" value="0x00000002" />
                    <flag name="enterAlways" value="0x00000004" />
                    <flag name="enterAlwaysCollapsed" value="0x00000008" />
                    <flag name="snap" value="0x00000010" />
                    <flag name="snapMargins" value="0x00000020" />
                </attr>
                <attr name="layout_scrollInterpolator" format="reference" />
                <attr name="liftOnScroll" format="boolean" />
                <attr name="lineHeight" format="dimension" />
                <attr name="lineSpacing" format="dimension" />
                <attr name="listChoiceBackgroundIndicator" format="reference" />
                <attr name="listDividerAlertDialog" format="reference" />
                <attr name="listItemLayout" format="reference" />
                <attr name="listLayout" format="reference" />
                <attr name="listMenuViewStyle" format="reference" />
                <attr name="listPopupWindowStyle" format="reference" />
                <attr name="listPreferredItemHeight" format="dimension" />
                <attr name="listPreferredItemHeightLarge" format="dimension" />
                <attr name="listPreferredItemHeightSmall" format="dimension" />
                <attr name="listPreferredItemPaddingLeft" format="dimension" />
                <attr name="listPreferredItemPaddingRight" format="dimension" />
                <attr name="logo" format="reference" />
                <attr name="logoDescription" format="string" />
                <attr name="materialButtonStyle" format="reference" />
                <attr name="materialCardViewStyle" format="reference" />
                <attr name="maxActionInlineWidth" format="dimension" />
                <attr name="maxButtonHeight" format="dimension" />
                <attr name="maxImageSize" format="dimension" />
                <attr name="measureWithLargestChild" format="boolean" />
                <attr name="menu" format="reference" />
                <attr name="multiChoiceItemLayout" format="reference" />
                <attr name="navigationContentDescription" format="string" />
                <attr name="navigationIcon" format="reference" />
                <attr name="navigationMode">
                    <enum name="normal" value="0" />
                    <enum name="listMode" value="1" />
                    <enum name="tabMode" value="2" />
                </attr>
                <attr name="navigationViewStyle" format="reference" />
                <attr name="numericModifiers">
                    <flag name="META" value="0x00010000" />
                    <flag name="CTRL" value="0x00001000" />
                    <flag name="ALT" value="0x00000002" />
                    <flag name="SHIFT" value="0x00000001" />
                    <flag name="SYM" value="0x00000004" />
                    <flag name="FUNCTION" value="0x00000008" />
                </attr>
                <attr name="overlapAnchor" format="boolean" />
                <attr name="paddingBottomNoButtons" format="dimension" />
                <attr name="paddingEnd" format="dimension" />
                <attr name="paddingStart" format="dimension" />
                <attr name="paddingTopNoTitle" format="dimension" />
                <attr name="panelBackground" format="reference" />
                <attr name="panelMenuListTheme" format="reference" />
                <attr name="panelMenuListWidth" format="dimension" />
                <attr name="passwordToggleContentDescription" format="string" />
                <attr name="passwordToggleDrawable" format="reference" />
                <attr name="passwordToggleEnabled" format="boolean" />
                <attr name="passwordToggleTint" format="color" />
                <attr name="passwordToggleTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                </attr>
                <attr name="popupMenuStyle" format="reference" />
                <attr name="popupTheme" format="reference" />
                <attr name="popupWindowStyle" format="reference" />
                <attr name="preserveIconSpacing" format="boolean" />
                <attr name="pressedTranslationZ" format="dimension" />
                <attr name="progressBarPadding" format="dimension" />
                <attr name="progressBarStyle" format="reference" />
                <attr name="queryBackground" format="reference" />
                <attr name="queryHint" format="string" />
                <attr name="radioButtonStyle" format="reference" />
                <attr name="ratingBarStyle" format="reference" />
                <attr name="ratingBarStyleIndicator" format="reference" />
                <attr name="ratingBarStyleSmall" format="reference" />
                <attr name="reverseLayout" format="boolean" />
                <attr name="rippleColor" format="color" />
                <attr name="scrimAnimationDuration" format="integer" />
                <attr name="scrimBackground" format="reference|color" />
                <attr name="scrimVisibleHeightTrigger" format="dimension" />
                <attr name="searchHintIcon" format="reference" />
                <attr name="searchIcon" format="reference" />
                <attr name="searchViewStyle" format="reference" />
                <attr name="seekBarStyle" format="reference" />
                <attr name="selectableItemBackground" format="reference" />
                <attr name="selectableItemBackgroundBorderless" format="reference" />
                <attr name="showAsAction">
                    <flag name="never" value="0x00000000" />
                    <flag name="ifRoom" value="0x00000001" />
                    <flag name="always" value="0x00000002" />
                    <flag name="withText" value="0x00000004" />
                    <flag name="collapseActionView" value="0x00000008" />
                </attr>
                <attr name="showDividers">
                    <flag name="none" value="0x00000000" />
                    <flag name="beginning" value="0x00000001" />
                    <flag name="middle" value="0x00000002" />
                    <flag name="end" value="0x00000004" />
                </attr>
                <attr name="showMotionSpec" format="reference" />
                <attr name="showText" format="boolean" />
                <attr name="showTitle" format="boolean" />
                <attr name="singleChoiceItemLayout" format="reference" />
                <attr name="singleLine" format="boolean" />
                <attr name="singleSelection" format="boolean" />
                <attr name="snackbarButtonStyle" format="reference" />
                <attr name="snackbarStyle" format="reference" />
                <attr name="spanCount" format="integer" />
                <attr name="spinBars" format="boolean" />
                <attr name="spinnerDropDownItemStyle" format="reference" />
                <attr name="spinnerStyle" format="reference" />
                <attr name="splitTrack" format="boolean" />
                <attr name="srcCompat" format="reference" />
                <attr name="stackFromEnd" format="boolean" />
                <attr name="state_above_anchor" format="boolean" />
                <attr name="state_collapsed" format="boolean" />
                <attr name="state_collapsible" format="boolean" />
                <attr name="state_liftable" format="boolean" />
                <attr name="state_lifted" format="boolean" />
                <attr name="statusBarBackground" format="reference|color" />
                <attr name="statusBarScrim" format="color" />
                <attr name="strokeColor" format="color" />
                <attr name="strokeWidth" format="dimension" />
                <attr name="subMenuArrow" format="reference" />
                <attr name="submitBackground" format="reference" />
                <attr name="subtitle" format="string" />
                <attr name="subtitleTextAppearance" format="reference" />
                <attr name="subtitleTextColor" format="color" />
                <attr name="subtitleTextStyle" format="reference" />
                <attr name="suggestionRowLayout" format="reference" />
                <attr name="switchMinWidth" format="dimension" />
                <attr name="switchPadding" format="dimension" />
                <attr name="switchStyle" format="reference" />
                <attr name="switchTextAppearance" format="reference" />
                <attr name="tabBackground" format="reference" />
                <attr name="tabContentStart" format="dimension" />
                <attr name="tabGravity">
                    <enum name="fill" value="0" />
                    <enum name="center" value="1" />
                </attr>
                <attr name="tabIconTint" format="color" />
                <attr name="tabIconTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="tabIndicator" format="reference" />
                <attr name="tabIndicatorAnimationDuration" format="integer" />
                <attr name="tabIndicatorColor" format="color" />
                <attr name="tabIndicatorFullWidth" format="boolean" />
                <attr name="tabIndicatorGravity">
                    <enum name="bottom" value="0" />
                    <enum name="center" value="1" />
                    <enum name="top" value="2" />
                    <enum name="stretch" value="3" />
                </attr>
                <attr name="tabIndicatorHeight" format="dimension" />
                <attr name="tabInlineLabel" format="boolean" />
                <attr name="tabMaxWidth" format="dimension" />
                <attr name="tabMinWidth" format="dimension" />
                <attr name="tabMode">
                    <enum name="scrollable" value="0" />
                    <enum name="fixed" value="1" />
                </attr>
                <attr name="tabPadding" format="dimension" />
                <attr name="tabPaddingBottom" format="dimension" />
                <attr name="tabPaddingEnd" format="dimension" />
                <attr name="tabPaddingStart" format="dimension" />
                <attr name="tabPaddingTop" format="dimension" />
                <attr name="tabRippleColor" format="color" />
                <attr name="tabSelectedTextColor" format="color" />
                <attr name="tabStyle" format="reference" />
                <attr name="tabTextAppearance" format="reference" />
                <attr name="tabTextColor" format="color" />
                <attr name="tabUnboundedRipple" format="boolean" />
                <attr name="textAllCaps" format="reference|boolean" />
                <attr name="textAppearanceBody1" format="reference" />
                <attr name="textAppearanceBody2" format="reference" />
                <attr name="textAppearanceButton" format="reference" />
                <attr name="textAppearanceCaption" format="reference" />
                <attr name="textAppearanceHeadline1" format="reference" />
                <attr name="textAppearanceHeadline2" format="reference" />
                <attr name="textAppearanceHeadline3" format="reference" />
                <attr name="textAppearanceHeadline4" format="reference" />
                <attr name="textAppearanceHeadline5" format="reference" />
                <attr name="textAppearanceHeadline6" format="reference" />
                <attr name="textAppearanceLargePopupMenu" format="reference" />
                <attr name="textAppearanceListItem" format="reference" />
                <attr name="textAppearanceListItemSecondary" format="reference" />
                <attr name="textAppearanceListItemSmall" format="reference" />
                <attr name="textAppearanceOverline" format="reference" />
                <attr name="textAppearancePopupMenuHeader" format="reference" />
                <attr name="textAppearanceSearchResultSubtitle" format="reference" />
                <attr name="textAppearanceSearchResultTitle" format="reference" />
                <attr name="textAppearanceSmallPopupMenu" format="reference" />
                <attr name="textAppearanceSubtitle1" format="reference" />
                <attr name="textAppearanceSubtitle2" format="reference" />
                <attr name="textColorAlertDialogListItem" format="reference|color" />
                <attr name="textColorSearchUrl" format="reference|color" />
                <attr name="textEndPadding" format="dimension" />
                <attr name="textInputStyle" format="reference" />
                <attr name="textStartPadding" format="dimension" />
                <attr name="theme" format="reference" />
                <attr name="thickness" format="dimension" />
                <attr name="thumbTextPadding" format="dimension" />
                <attr name="thumbTint" format="color" />
                <attr name="thumbTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="tickMark" format="reference" />
                <attr name="tickMarkTint" format="color" />
                <attr name="tickMarkTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="tint" format="color" />
                <attr name="tintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="title" format="string" />
                <attr name="titleEnabled" format="boolean" />
                <attr name="titleMargin" format="dimension" />
                <attr name="titleMarginBottom" format="dimension" />
                <attr name="titleMarginEnd" format="dimension" />
                <attr name="titleMarginStart" format="dimension" />
                <attr name="titleMarginTop" format="dimension" />
                <attr name="titleMargins" format="dimension" />
                <attr name="titleTextAppearance" format="reference" />
                <attr name="titleTextColor" format="color" />
                <attr name="titleTextStyle" format="reference" />
                <attr name="toolbarId" format="reference" />
                <attr name="toolbarNavigationButtonStyle" format="reference" />
                <attr name="toolbarStyle" format="reference" />
                <attr name="tooltipForegroundColor" format="reference|color" />
                <attr name="tooltipFrameBackground" format="reference" />
                <attr name="tooltipText" format="string" />
                <attr name="track" format="reference" />
                <attr name="trackTint" format="color" />
                <attr name="trackTintMode">
                    <enum name="src_over" value="3" />
                    <enum name="src_in" value="5" />
                    <enum name="src_atop" value="9" />
                    <enum name="multiply" value="14" />
                    <enum name="screen" value="15" />
                    <enum name="add" value="16" />
                </attr>
                <attr name="ttcIndex" format="integer" />
                <attr name="useCompatPadding" format="boolean" />
                <attr name="viewInflaterClass" format="string" />
                <attr name="voiceIcon" format="reference" />
                <attr name="windowActionBar" format="boolean" />
                <attr name="windowActionBarOverlay" format="boolean" />
                <attr name="windowActionModeOverlay" format="boolean" />
                <attr name="windowFixedHeightMajor" format="dimension|fraction" />
                <attr name="windowFixedHeightMinor" format="dimension|fraction" />
                <attr name="windowFixedWidthMajor" format="dimension|fraction" />
                <attr name="windowFixedWidthMinor" format="dimension|fraction" />
                <attr name="windowMinWidthMajor" format="dimension|fraction" />
                <attr name="windowMinWidthMinor" format="dimension|fraction" />
                <attr name="windowNoTitle" format="boolean" />
            </resources>
        """.trimIndent()
        val bools = """
                <bool name="abc_action_bar_embed_tabs">true</bool>
                <bool name="abc_allow_stacked_button_bar">false</bool>
                <bool name="abc_config_actionMenuItemAllCaps">true</bool>
                <bool name="mtrl_btn_textappearance_all_caps">true</bool>
            </resources>
        """.trimIndent()
        val integers = """
                    <integer name="abc_config_activityDefaultDur">220</integer>
                    <integer name="abc_config_activityShortDur">150</integer>
                    <integer name="app_bar_elevation_anim_duration">150</integer>
                    <integer name="bottom_sheet_slide_duration">150</integer>
                    <integer name="cancel_button_image_alpha">127</integer>
                    <integer name="config_tooltipAnimTime">150</integer>
                    <integer name="design_snackbar_text_max_lines">2</integer>
                    <integer name="design_tab_indicator_anim_duration_ms">300</integer>
                    <integer name="hide_password_duration">320</integer>
                    <integer name="mtrl_btn_anim_delay_ms">100</integer>
                    <integer name="mtrl_btn_anim_duration_ms">100</integer>
                    <integer name="mtrl_chip_anim_duration">100</integer>
                    <integer name="mtrl_tab_indicator_anim_duration_ms">250</integer>
                    <integer name="show_password_duration">200</integer>
                    <integer name="status_bar_notification_info_maxnum">999</integer>
                </resources>
        """.trimIndent()
        val ids = """
                <item type="id" name="action0" />
                <item type="id" name="action_bar" />
                <item type="id" name="action_bar_activity_content" />
                <item type="id" name="action_bar_container" />
                <item type="id" name="action_bar_root" />
                <item type="id" name="action_bar_spinner" />
                <item type="id" name="action_bar_subtitle" />
                <item type="id" name="action_bar_title" />
                <item type="id" name="action_container" />
                <item type="id" name="action_context_bar" />
                <item type="id" name="action_divider" />
                <item type="id" name="action_image" />
                <item type="id" name="action_menu_divider" />
                <item type="id" name="action_menu_presenter" />
                <item type="id" name="action_mode_bar" />
                <item type="id" name="action_mode_bar_stub" />
                <item type="id" name="action_mode_close_button" />
                <item type="id" name="actions" />
                <item type="id" name="activity_chooser_view_content" />
                <item type="id" name="agentweb_webview_id" />
                <item type="id" name="alertTitle" />
                <item type="id" name="before_server_btn" />
                <item type="id" name="btn_exit" />
                <item type="id" name="buttonPanel" />
                <item type="id" name="cancel" />
                <item type="id" name="cancel_action" />
                <item type="id" name="cancel_tv" />
                <item type="id" name="change_account_btn" />
                <item type="id" name="checkbox" />
                <item type="id" name="chronometer" />
                <item type="id" name="confirm_tv" />
                <item type="id" name="container" />
                <item type="id" name="content" />
                <item type="id" name="contentPanel" />
                <item type="id" name="coordinator" />
                <item type="id" name="custom" />
                <item type="id" name="customPanel" />
                <item type="id" name="dash_line" />
                <item type="id" name="debug_mode_back" />
                <item type="id" name="debug_mode_btn_copy_error" />
                <item type="id" name="debug_mode_btn_report" />
                <item type="id" name="debug_mode_btn_report_error" />
                <item type="id" name="debug_mode_copy_error" />
                <item type="id" name="debug_mode_error_code" />
                <item type="id" name="debug_mode_error_logid" />
                <item type="id" name="debug_mode_error_msg" />
                <item type="id" name="debug_mode_error_path" />
                <item type="id" name="debug_mode_sdk_version" />
                <item type="id" name="debug_mode_status_code" />
                <item type="id" name="decor_content_parent" />
                <item type="id" name="default_activity_button" />
                <item type="id" name="design_bottom_sheet" />
                <item type="id" name="design_menu_item_action_area" />
                <item type="id" name="design_menu_item_action_area_stub" />
                <item type="id" name="design_menu_item_text" />
                <item type="id" name="design_navigation_view" />
                <item type="id" name="edit_query" />
                <item type="id" name="end_padder" />
                <item type="id" name="enter_btn" />
                <item type="id" name="exit_btn" />
                <item type="id" name="expand_activities_button" />
                <item type="id" name="expanded_menu" />
                <item type="id" name="ghost_view" />
                <item type="id" name="glide_custom_view_target_tag" />
                <item type="id" name="group_divider" />
                <item type="id" name="home" />
                <item type="id" name="icon" />
                <item type="id" name="icon_group" />
                <item type="id" name="image" />
                <item type="id" name="img_back" />
                <item type="id" name="img_close" />
                <item type="id" name="info" />
                <item type="id" name="item_touch_helper_previous_elevation" />
                <item type="id" name="iv_app_icon" />
                <item type="id" name="iv_custom_toast_icon" />
                <item type="id" name="iv_detail_back" />
                <item type="id" name="iv_privacy_back" />
                <item type="id" name="largeLabel" />
                <item type="id" name="level_btn" />
                <item type="id" name="line" />
                <item type="id" name="line1" />
                <item type="id" name="line3" />
                <item type="id" name="list_item" />
                <item type="id" name="ll_download" />
                <item type="id" name="log_switcher" />
                <item type="id" name="login_btn" />
                <item type="id" name="login_panel" />
                <item type="id" name="logout_btn" />
                <item type="id" name="mainframe_error_container_id" />
                <item type="id" name="mainframe_error_viewsub_id" />
                <item type="id" name="masked" />
                <item type="id" name="media_actions" />
                <item type="id" name="message" />
                <item type="id" name="message_tv" />
                <item type="id" name="mtrl_child_content_container" />
                <item type="id" name="mtrl_internal_children_alpha_tag" />
                <item type="id" name="nav_host_fragment" />
                <item type="id" name="navigation_header_container" />
                <item type="id" name="no_record_hint" />
                <item type="id" name="notification_background" />
                <item type="id" name="notification_main_column" />
                <item type="id" name="notification_main_column_container" />
                <item type="id" name="open_header_view" />
                <item type="id" name="open_loading_group" />
                <item type="id" name="open_rl_container" />
                <item type="id" name="parentPanel" />
                <item type="id" name="parent_matrix" />
                <item type="id" name="pay_btn" />
                <item type="id" name="permission_list" />
                <item type="id" name="privacy_webview" />
                <item type="id" name="progress_circular" />
                <item type="id" name="progress_horizontal" />
                <item type="id" name="radio" />
                <item type="id" name="right_icon" />
                <item type="id" name="right_side" />
                <item type="id" name="role_btn" />
                <item type="id" name="role_info_btn" />
                <item type="id" name="rv_debug_info" />
                <item type="id" name="save_image_matrix" />
                <item type="id" name="save_non_transition_alpha" />
                <item type="id" name="save_scale_type" />
                <item type="id" name="scrollIndicatorDown" />
                <item type="id" name="scrollIndicatorUp" />
                <item type="id" name="scrollView" />
                <item type="id" name="search_badge" />
                <item type="id" name="search_bar" />
                <item type="id" name="search_button" />
                <item type="id" name="search_close_btn" />
                <item type="id" name="search_edit_frame" />
                <item type="id" name="search_go_btn" />
                <item type="id" name="search_mag_icon" />
                <item type="id" name="search_plate" />
                <item type="id" name="search_src_text" />
                <item type="id" name="search_voice_btn" />
                <item type="id" name="select_dialog_listview" />
                <item type="id" name="server_btn" />
                <item type="id" name="shortcut" />
                <item type="id" name="smallLabel" />
                <item type="id" name="snackbar_action" />
                <item type="id" name="snackbar_text" />
                <item type="id" name="spacer" />
                <item type="id" name="split_action_bar" />
                <item type="id" name="status_bar_latest_event_content" />
                <item type="id" name="submenuarrow" />
                <item type="id" name="submit_area" />
                <item type="id" name="sv_content" />
                <item type="id" name="tag_transition_group" />
                <item type="id" name="tag_unhandled_key_event_manager" />
                <item type="id" name="tag_unhandled_key_listeners" />
                <item type="id" name="text" />
                <item type="id" name="text2" />
                <item type="id" name="textSpacerNoButtons" />
                <item type="id" name="textSpacerNoTitle" />
                <item type="id" name="text_input_password_toggle" />
                <item type="id" name="text_tip" />
                <item type="id" name="textinput_counter" />
                <item type="id" name="textinput_error" />
                <item type="id" name="textinput_helper_text" />
                <item type="id" name="time" />
                <item type="id" name="title" />
                <item type="id" name="titleDividerNoCustom" />
                <item type="id" name="title_bar" />
                <item type="id" name="title_template" />
                <item type="id" name="topPanel" />
                <item type="id" name="touch_outside" />
                <item type="id" name="transition_current_scene" />
                <item type="id" name="transition_layout_save" />
                <item type="id" name="transition_position" />
                <item type="id" name="transition_scene_layoutid_cache" />
                <item type="id" name="transition_transform" />
                <item type="id" name="tt_appdownloader_action" />
                <item type="id" name="tt_appdownloader_desc" />
                <item type="id" name="tt_appdownloader_download_progress" />
                <item type="id" name="tt_appdownloader_download_progress_new" />
                <item type="id" name="tt_appdownloader_download_size" />
                <item type="id" name="tt_appdownloader_download_status" />
                <item type="id" name="tt_appdownloader_download_success" />
                <item type="id" name="tt_appdownloader_download_success_size" />
                <item type="id" name="tt_appdownloader_download_success_status" />
                <item type="id" name="tt_appdownloader_download_text" />
                <item type="id" name="tt_appdownloader_icon" />
                <item type="id" name="tt_appdownloader_root" />
                <item type="id" name="tv_app_detail" />
                <item type="id" name="tv_app_developer" />
                <item type="id" name="tv_app_name" />
                <item type="id" name="tv_app_privacy" />
                <item type="id" name="tv_app_version" />
                <item type="id" name="tv_confirm" />
                <item type="id" name="tv_content" />
                <item type="id" name="tv_content_protocol" />
                <item type="id" name="tv_custom_toast" />
                <item type="id" name="tv_custom_toast_icon" />
                <item type="id" name="tv_custom_toast_loading" />
                <item type="id" name="tv_empty" />
                <item type="id" name="tv_give_up" />
                <item type="id" name="tv_name" />
                <item type="id" name="tv_one" />
                <item type="id" name="tv_permission_description" />
                <item type="id" name="tv_permission_title" />
                <item type="id" name="tv_two" />
                <item type="id" name="tv_update_hints_one" />
                <item type="id" name="tv_update_hints_two" />
                <item type="id" name="tv_update_title" />
                <item type="id" name="tv_value" />
                <item type="id" name="up" />
                <item type="id" name="visible" />
                <item type="id" name="web_parent_layout_id" />
                <item type="id" name="webview_parent" />
                <item type="id" name="x86_support" />
            </resources>
        """.trimIndent()
        val landDimens = """
                <dimen name="abc_action_bar_default_height_material">48.0dip</dimen>
                <dimen name="abc_text_size_subtitle_material_toolbar">12.0dip</dimen>
                <dimen name="abc_text_size_title_material_toolbar">14.0dip</dimen>
            </resources>
        """.trimIndent()
        val landStyles = """
                <style name="Widget.Design.TabLayout" parent="@style/Base.Widget.Design.TabLayout">
                    <item name="tabGravity">center</item>
                    <item name="tabMode">fixed</item>
                </style>
            </resources>
        """.trimIndent()

        val resFolder = File(decompileDir, "res")
        val resValuesFolder = File(resFolder, "values")
        val resLandValuesFolder = File(resFolder, "values-land")
        val stringsFile = File(resValuesFolder, "strings.xml")
        val stylesFile = File(resValuesFolder, "styles.xml")
        val colorsFile = File(resValuesFolder, "colors.xml")
        val attrsFile = File(resValuesFolder, "attrs.xml")
        val dimensFile = File(resValuesFolder, "dimens.xml")
        val drawablesFile = File(resValuesFolder, "drawables.xml")
        val boolsFile = File(resValuesFolder, "bools.xml")
        val integersFile = File(resValuesFolder, "integers.xml")
        val idsFile = File(resValuesFolder, "ids.xml")
        val landDimensFile = File(resLandValuesFolder, "dimens.xml")
        val landStylesFile = File(resLandValuesFolder, "styles.xml")

        colorsFile.writeText(colorsFile.readText().replace("</resources>", colors))
        stringsFile.writeText(stringsFile.readText().replace("</resources>", strings))
        stylesFile.writeText(stylesFile.readText().replace("</resources>", styles))
        dimensFile.writeText(dimensFile.readText().replace("</resources>", dimens))
        landDimensFile.writeText(landDimensFile.readText().replace("</resources>", landDimens))
        attrsFile.apply {           // SDK 本身不包含 attrs.xml 文件，因此需要判断是否存在
            if (exists()) {
                writeText(readText().replace("</resources>", attrs))
            } else {
                createNewFile()
                writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$attrs""")
            }
        }
        drawablesFile.apply {       // SDK 本身不包含 drawables.xml 文件，因此需要判断是否存在
            if (exists()) {
                writeText(readText().replace("</resources>", drawables))
            } else {
                createNewFile()
                writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$drawables""")
            }
        }
        boolsFile.apply {           // SDK 本身不包含 bools.xml 文件，因此需要判断是否存在
            if (exists()) {
                writeText(readText().replace("</resources>", bools))
            } else {
                createNewFile()
                writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$bools""")
            }
        }
        integersFile.apply {        // SDK 本身不包含 bools.xml 文件，因此需要判断是否存在
            if (exists()) {
                writeText(readText().replace("</resources>", integers))
            } else {
                createNewFile()
                writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$integers""")
            }
        }
        idsFile.apply {             // SDK 本身不包含 ids.xml 文件，因此需要判断是否存在
            if (exists()) {
                writeText(readText().replace("</resources>", ids))
            } else {
                createNewFile()
                writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$ids""")
            }
        }
        landStylesFile.apply {      // SDK 本身不包含 values-land/styles.xml 文件，因此需要判断是否存在
            if (exists()) {
                writeText(readText().replace("</resources>", landStyles))
            } else {
                createNewFile()
                writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$landStyles""")
            }
        }
    }

    fun updateGameConfig(decompileDir: String, params: Map<String, String>) {
        val file = File(decompileDir + File.separator + "assets" + File.separator + "ZSinfo.xml")
        val document = SAXReader().read(file)
        params.forEach {
            when (it.key) {
                "pkid" -> {
                    document.rootElement.element("pkid").text = it.value
                    document.rootElement.element("kpid").text = it.value
                }
                "version", "platform", "appid", "channel", "adid", "pcid", "packtype", "register_ratio", "purchase_ratio" -> {
                    val element = document.rootElement.element(it.key)
                    if (element == null) {
                        document.rootElement.addElement(it.key).text = it.value
                    } else {
                        element.text = it.value
                    }
                }
            }
        }
        val writer = XMLWriter(FileWriter(file))
        writer.write(document)
        writer.close()
    }

    fun setAppName(decompileDir: String, appName: String) {
        val xml = decompileDir + File.separator + "res" + File.separator + "values" + File.separator + "strings.xml"
        val file = File(xml)
        val document = SAXReader().read(file)
        document.rootElement.elements("string").forEach {
            if (it.attributeValue("name") == "app_name") {
                it.text = appName
            }
        }
        val writer = XMLWriter(FileWriter(file))
        writer.write(document)
        writer.close()
    }

    /**
     * 由于 SDK 目前最高兼容至 API 29，所以高于 29 的需要降级
     * @see goh.games.Game148
     */
    fun downgradeTargetSdkVersion(decompileDir: String) {
        val yaml = File(decompileDir, "apktool.yml")
        yaml.writeText(yaml.readText().replace("targetSdkVersion: '30'", "targetSdkVersion: '29'"))
    }

    /**
     * 修改 AndroidManifest.xml 中 android:label 的值
     */
    fun setApplicationLabel(manifest: File) {
        val document = SAXReader().read(manifest)
        val application = document.rootElement.element("application")
        application.attribute("label").text = "@string/app_name"
        application.elements("activity").forEach {
            it.attribute("label")?.text = "@string/app_name"
        }
        val writer = XMLWriter(FileWriter(manifest))
        writer.write(document)
        writer.close()
    }

    /**
     * 修改 AndroidManifest.xml 中 android:theme 的值
     */
    fun setApplicationTheme(manifest: File) {
        val sdkTheme = "@android:style/Theme.Light.NoTitleBar.Fullscreen"
        val document = SAXReader().read(manifest)
        val application = document.rootElement.element("application")
        application.attribute("theme")?.text = sdkTheme
        application.elements("activity").forEach { activity ->
            activity.element("intent-filter")?.elements("action")?.forEach { action ->
                if ("android.intent.action.MAIN" == action.attribute("name").text) {
                    activity.attribute("theme")?.text = sdkTheme
                }
            }
        }

        val writer = XMLWriter(FileWriter(manifest))
        writer.write(document)
        writer.close()
    }

    /**
     * 遍历节点更新包名，对于简写式的 android:name=".ClassName" 还需单独处理
     */
    fun updatePackageName(node: Element, oldPackageName: String, newPackageName: String) {
        when (node.name) {
            "application", "activity", "service" -> println("跳过 ${node.name} 节点")
            else -> {
                println("遍历 ${node.name} 节点")
                for (attr in node.attributes()) {
                    if (attr.value.contains(oldPackageName)) {
                        println("修改 ${node.name} 节点： ${attr.name}=${attr.value}")
                        attr.value = attr.value.replace(oldPackageName, newPackageName)
                        println("    -> ${attr.name}=${attr.value}")
                    }
                }
            }
        }
        for (element in node.elements()) {
            updatePackageName(element, oldPackageName, newPackageName)
        }
    }
}