package com.utils

import java.io.File

/**
 * 对 Smali 文件的修改操作
 */
object SmaliHandler {

    fun getDefaultCoType(decompileDir: String): Int {
        val file = File(decompileDir + File.separator + "smali" + File.separator + "com" + File.separator + "mayisdk" + File.separator + "means" + File.separator + "OutilString.smali")
        val smali = file.readText()
        return when {
            smali.contains("rongyao666.com") -> 1
            smali.contains("iskywan.com") -> 2
            else -> -1
        }
    }

    fun setUrlCoPrefix(decompileDir: String, coPrefix: String, defaultCoType: Int) {
        val defaultUrl = when (defaultCoType) {
            1 -> "message/api/ApiSendMessage.php"
            2 -> "message/cqapi/ApiSendMessage.php"
            else -> return
        }
        val prefix = if (coPrefix == "ry") {
            ""          // 荣耀的 URL 里不需要加
        } else {
            coPrefix
        }
        val file = File(decompileDir + File.separator + "smali" + File.separator + "com" + File.separator + "mayisdk" + File.separator + "means" + File.separator + "OutilString.smali")
        var smali = file.readText()
        smali = smali.replace(defaultUrl, "message/${prefix}api/ApiSendMessage.php")
        file.writeText(smali)
        println("--> ApiSendMessage 添加前缀完成")
    }

    fun setRegisterAccountPrefix(decompileDir: String, coPrefix: String, defaultCoType: Int) {
        val defaultPrefix = when (defaultCoType) {
            1 -> "\"ry\""
            2 -> "\"cq\""
            else -> "\"def\""
        }
        val file = File(decompileDir + File.separator + "smali" + File.separator + "com" + File.separator + "mayisdk" + File.separator + "means" + File.separator + "OutilTool.smali")
        val prefix = if (coPrefix.isBlank()) {
            "cq"
        } else {
            coPrefix
        }
        var smali = file.readText()
        smali = smali.replace(defaultPrefix, "\"" + prefix + "\"")
        file.writeText(smali)
        println("--> 修改随机账号前缀完成")
    }

    fun setCoShareText(decompileDir: String, coText: String, defaultCoType: Int) {
        val defaultCoText = when (defaultCoType) {
            1 -> "\\u8363\\u8000\\u6e38\\u620f\\uff1a"      // “荣耀游戏：”
            2 -> "\\u82cd\\u7a79\\u6e38\\u620f\\uff1a"      // “苍穹游戏：”
            else -> return
        }
        val file = File(decompileDir + File.separator + "smali" + File.separator + "com" + File.separator + "tgsdkUi" + File.separator + "view" + File.separator + "RyShareDialog.smali")
        var smali = file.readText()
        smali = smali.replace(defaultCoText, coText)
        file.writeText(smali)
        println("--> 分享下载链接公司文字修改完成")
    }

    fun setCoDomain(decompileDir: String, coDomain: String, defaultCoType: Int) {
        val defaultCoDomain = when (defaultCoType) {
            1 -> "rongyao666.com"
            2 -> "iskywan.com"
            else -> return
        }
        println("默认域名：$defaultCoDomain")
        println("修改后域名：$coDomain")
        val file = File(decompileDir + File.separator + "smali" + File.separator + "com" + File.separator + "mayisdk" + File.separator + "means" + File.separator + "OutilString.smali")
        var smali = file.readText()
        smali = smali.replace(defaultCoDomain, coDomain)
        file.writeText(smali)
        println("--> 请求域名修改完成")
    }
}