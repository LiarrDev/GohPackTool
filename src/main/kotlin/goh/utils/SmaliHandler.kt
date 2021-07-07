package goh.utils

import org.json.JSONObject
import java.io.File

/**
 * 对 Smali 文件的修改操作
 */
object SmaliHandler {

    /**
     * 获取默认的主体
     */
    fun getDefaultCoType(decompileDir: String): Int {
        var file = File(
            decompileDir
                    + File.separator + "smali"
                    + File.separator + "com"
                    + File.separator + "mayisdk"
                    + File.separator + "means"
                    + File.separator + "OutilString.smali"
        )
        if (!file.exists()) {
            file = File(
                decompileDir
                        + File.separator + "smali_classes2"
                        + File.separator + "com"
                        + File.separator + "mayisdk"
                        + File.separator + "means"
                        + File.separator + "OutilString.smali"
            )
        }
        val smali = file.readText()
        return when {
            smali.contains("iskywan.com") -> 2
            smali.contains("rongyao666.com") -> 1
            else -> -1
        }
    }

    /**
     * ApiSendMessage 接口的前缀
     */
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
        var file = File(
            decompileDir
                    + File.separator + "smali"
                    + File.separator + "com"
                    + File.separator + "mayisdk"
                    + File.separator + "means"
                    + File.separator + "OutilString.smali"
        )
        if (!file.exists()) {
            file = File(
                decompileDir
                        + File.separator + "smali_classes2"
                        + File.separator + "com"
                        + File.separator + "mayisdk"
                        + File.separator + "means"
                        + File.separator + "OutilString.smali"
            )
        }
        var smali = file.readText()
        smali = smali.replace(defaultUrl, "message/${prefix}api/ApiSendMessage.php")
        file.writeText(smali)
        println("--> ApiSendMessage 添加前缀完成")
    }

    /**
     * 随机账号前缀
     */
    fun setRegisterAccountPrefix(decompileDir: String, coPrefix: String, defaultCoType: Int) {
        val defaultPrefix = when (defaultCoType) {
            1 -> "\"ry\""
            2 -> "\"cq\""
            else -> return
        }
        var file = File(
            decompileDir
                    + File.separator + "smali"
                    + File.separator + "com"
                    + File.separator + "mayisdk"
                    + File.separator + "means"
                    + File.separator + "OutilTool.smali"
        )
        if (!file.exists()) {
            file = File(
                decompileDir
                        + File.separator + "smali_classes2"
                        + File.separator + "com"
                        + File.separator + "mayisdk"
                        + File.separator + "means"
                        + File.separator + "OutilTool.smali"
            )
        }
        val prefix = coPrefix.ifBlank { "cq" }
        var smali = file.readText()
        smali = smali.replace(defaultPrefix, "\"" + prefix + "\"")
        file.writeText(smali)
        println("--> 修改随机账号前缀完成")
    }

    /**
     * 分享内容的主体文字
     */
    fun setCoShareText(decompileDir: String, coText: String, defaultCoType: Int) {
        val defaultCoText = when (defaultCoType) {
            1 -> "\\u8363\\u8000\\u6e38\\u620f\\uff1a"      // “荣耀游戏：”
            2 -> "\\u82cd\\u7a79\\u6e38\\u620f\\uff1a"      // “苍穹游戏：”
            else -> return
        }
        var file = File(
            decompileDir
                    + File.separator + "smali"
                    + File.separator + "com"
                    + File.separator + "tgsdkUi"
                    + File.separator + "view"
                    + File.separator + "RyShareDialog.smali"
        )
        if (!file.exists()) {
            file = File(
                decompileDir
                        + File.separator + "smali_classes2"
                        + File.separator + "com"
                        + File.separator + "tgsdkUi"
                        + File.separator + "view"
                        + File.separator + "RyShareDialog.smali"
            )
        }
        var smali = file.readText()
        smali = smali.replace(defaultCoText, coText)
        file.writeText(smali)
        println("--> 分享下载链接公司文字修改完成")
    }

    /**
     * 全局替换请求 URL 中主体域名
     *
     * @param coDomainJson 主体域名的 JSON，格式如下：
     *          {
     *              "base_domain":"xxxx.com"
     *              "manage":"manage.cn.sy.xxxx.com",
     *              "user":"user.cn.sy.xxxx.com",
     *              "pay":"pay.cn.sy.xxxx.com",
     *              "page":"page.xxxx.com",
     *              "basedata":"api.basedata.sy.xxxx.com",
     *              "weixin":"weixin.xxxx.com",
     *              "pay_union":"pay.cn.union.xxxx.com",
     *              "user_union":"user.cn.union.xxxx.com",
     *              "message":"message.cn.sy.iskywan.com"
     *          }
     */
    fun setCoDomain(decompileDir: String, coDomainJson: String, defaultCoType: Int) {
        val defaultCoDomain = when (defaultCoType) {
            1 -> "rongyao666.com"
            2 -> "iskywan.com"
            else -> return
        }
        println("默认域名：$defaultCoDomain")
        println("修改后域名 JSON：$coDomainJson")

        val defaultBaseData = "api.basedata.sy.$defaultCoDomain"
        val defaultManage = "manage.cn.sy.$defaultCoDomain"
        val defaultMessage = "message.cn.sy.$defaultCoDomain"
        val defaultPage = "page.$defaultCoDomain"
        val defaultPay = "pay.cn.sy.$defaultCoDomain"
        val defaultPayUnion = "pay.cn.union.$defaultCoDomain"
        val defaultUser = "user.cn.sy.$defaultCoDomain"
        val defaultUserUnion = "user.cn.union.$defaultCoDomain"
        val defaultWechat = "weixin.$defaultCoDomain"

        val json = JSONObject(coDomainJson)
        val baseDomain = json.getString("base_domain")
        val baseData = json.getString("basedata")
        val manage = json.getString("manage")
        val message = json.getString("message")
        val page = json.getString("page")
        val pay = json.getString("pay")
        val payUnion = json.getString("pay_union")
        val user = json.getString("user")
        val userUnion = json.getString("user_union")
        val wechat = json.getString("weixin")

        var file = File(
            decompileDir
                    + File.separator + "smali"
                    + File.separator + "com"
                    + File.separator + "mayisdk"
                    + File.separator + "means"
                    + File.separator + "OutilString.smali"
        )
        if (!file.exists()) {
            file = File(
                decompileDir
                        + File.separator + "smali_classes2"
                        + File.separator + "com"
                        + File.separator + "mayisdk"
                        + File.separator + "means"
                        + File.separator + "OutilString.smali"
            )
        }
        val smali = file.readText().replace(defaultBaseData, baseData)
            .replace(defaultManage, manage)
            .replace(defaultMessage, message)
            .replace(defaultPage, page)
            .replace(defaultPay, pay)
            .replace(defaultPayUnion, payUnion)
            .replace(defaultUser, user)
            .replace(defaultUserUnion, userUnion)
            .replace(defaultWechat, wechat)
            .replace("\"" + defaultCoDomain + "\"", "\"" + baseDomain + "\"")   // BaseDomain 的修改必须放到最后且加上双引号，避免污染其他域名
        file.writeText(smali)
        println("--> 请求域名修改完成")
    }

    /**
     * 替换协议链接
     */
    fun setCoContract(decompileDir: String, defaultCoType: Int, coSuffix: String) {
        val defaultSuffix = when (defaultCoType) {
            1 -> "ry"
            2 -> "cq"
            else -> return
        }
        var file = File(
            decompileDir
                    + File.separator + "smali"
                    + File.separator + "com"
                    + File.separator + "mayisdk"
                    + File.separator + "means"
                    + File.separator + "OutilString.smali"
        )
        if (!file.exists()) {
            file = File(
                decompileDir
                        + File.separator + "smali_classes2"
                        + File.separator + "com"
                        + File.separator + "mayisdk"
                        + File.separator + "means"
                        + File.separator + "OutilString.smali"
            )
        }
        var smali = file.readText()
        smali = smali.replace("user_agreement_$defaultSuffix.html", "user_agreement_$coSuffix.html")
        smali = smali.replace("reivacy_agreement_$defaultSuffix.html", "reivacy_agreement_$coSuffix.html")
        file.writeText(smali)
        println("--> 修改协议链接完成")
    }
}