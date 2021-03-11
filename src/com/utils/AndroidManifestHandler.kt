package com.utils

import org.dom4j.io.SAXReader
import org.dom4j.io.XMLWriter
import java.io.File
import java.io.FileWriter
import java.util.regex.Pattern


object AndroidManifestHandler {

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

    fun getThirdPartyLoginManifest(loginType: String, qqAppId: String, weChatAppId: String, packageName: String): String {
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

        return when (loginType) {
            "1" -> weChat
            "2" -> qq
            "3" -> qq + weChat
            else -> ""
        }
    }

    /**
     * 在 <application> 节点下添加内容
     * 方法：直接替换尾标签 </application>
     */
    fun addApplicationConfig(decompileDir: String, content: String) {
        val file = File(decompileDir, "AndroidManifest.xml")
        var manifest = file.readText()
        manifest = manifest.replace("</application>", "$content</application>")
        file.writeText(manifest)
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
        addApplicationConfig(decompileDir, content)
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
        addApplicationConfig(decompileDir, content)
    }
}