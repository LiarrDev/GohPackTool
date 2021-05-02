package goh.utils

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
        var s = xml.readText()
        s = s.replace(endTag, replaceWith)
        xml.writeText(s)
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
     * 获取主 Activity 设置的屏幕方向
     */
    private fun getMainActivityScreenOrientation(androidManifest: File): String? {
        SAXReader().read(androidManifest).rootElement.element("application").elements("activity").forEach { activityNode ->
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
     * 三方登录需要设置的 AndroidManifest
     */
    fun setThirdPartyLoginManifest(decompileDir: String, loginType: String, qqAppId: String, weChatAppId: String, packageName: String) {
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

        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", when (loginType) {
            "1" -> weChat
            "2" -> qq
            "3" -> qq + weChat
            else -> ""
        } + "</application>")
    }

    /**
     * 应用宝 YSDK 的 AndroidManifest 设置
     */
    fun setYsdkManifest(decompileDir: String, packageName: String, qqAppId: String, wxAppId: String) {
        val file = File(decompileDir, "AndroidManifest.xml")
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
            <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
        """.trimIndent()
        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
    }

    /**
     * ViVO 联运的 AndroidManifest 设置
     */
    fun setVivoManifest(decompileDir: String, appId: String) {
        val content = """
                <meta-data
                    android:name="vivo_app_id"
                    android:value="$appId" />
                <meta-data
                    android:name="vivo_union_sdk"
                    android:value="4.6.0.1" />
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
            <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
            <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
        """.trimIndent()
        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
    }

    /**
     * OPPO 联运的 AndroidManifest 设置
     */
    fun setOppoManifest(decompileDir: String, packageName: String, appKey: String, appSecret: String) {
        val content = """
                <activity
                    android:name="com.nearme.game.sdk.component.proxy.JumpToProxyActivity"
                    android:configChanges="keyboardHidden|orientation|screenSize"
                    android:exported="true"
                    android:process=":gcsdk"
                    android:theme="@style/Theme_Dialog_Custom" />
                <provider
                    android:name="com.nearme.platform.opensdk.pay.NearMeFileProvider"
                    android:authorities="$packageName.fileProvider"
                    android:exported="false"
                    android:grantUriPermissions="true" >
                    <meta-data
                        android:name="android.support.FILE_PROVIDER_PATHS"
                        android:resource="@xml/oppo_file_paths" />
                </provider>
                <activity
                    android:name="com.nearme.game.sdk.component.proxy.ProxyActivity"
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
            <uses-permission android:name="android.permission.USE_CREDENTIALS" />
            <uses-permission android:name="android.permission.GET_ACCOUNTS" />
            <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
        """.trimIndent()
        replaceXmlEndTag(File(decompileDir, "AndroidManifest.xml"), "</application>", content)
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
        val file = File(decompileDir + File.separator + "res" + File.separator + "values" + File.separator + "styles.xml")
        replaceXmlEndTag(file, "</resources>", content)
    }

    fun updateGameConfig(decompileDir: String, params: Map<String, String>) {
        val file = File(decompileDir + File.separator + "assets" + File.separator + "ZSinfo.xml")
        val document = SAXReader().read(file)
        params.forEach { (key, value) ->
            when (key) {
                "pkid" -> {
                    document.rootElement.element("pkid").text = value
                    document.rootElement.element("kpid").text = value
                }
                "version", "platform", "appid", "channel", "adid", "pcid", "packtype", "register_ratio", "purchase_ratio" -> {
                    val element = document.rootElement.element(key)
                    if (element == null) {
                        document.rootElement.addElement(key).text = value
                    } else {
                        element.text = value
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
}