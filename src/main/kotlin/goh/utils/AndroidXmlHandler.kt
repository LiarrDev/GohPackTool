package goh.utils

import org.dom4j.Element
import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
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
        if (attrsFile.exists()) {   // SDK 本身不包含 attrs.xml 文件，因此需要判断是否存在
            attrsFile.writeText(attrsFile.readText().replace("</resources>", attrs))
        } else {
            attrsFile.createNewFile()
            attrsFile.writeText("""<?xml version="1.0" encoding="utf-8"?><resources>$attrs""")
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