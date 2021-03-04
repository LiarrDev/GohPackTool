package com.utils

import java.io.File
import java.util.regex.Pattern

object AndroidManifestHandler {

    fun getIconName(decompileDir: String): String {
        val manifest = File(decompileDir + File.separator + "AndroidManifest.xml").readText()
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
        val file = File(decompileDir + File.separator + "AndroidManifest.xml")
        var manifest = file.readText()
        manifest = manifest.replace("</application>", "$content</application>")
        file.writeText(manifest)
    }
}