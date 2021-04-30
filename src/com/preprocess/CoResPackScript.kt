package com.preprocess

import com.utils.DrawableUtil
import com.utils.PropertiesUtil
import com.utils.SmaliHandler
import com.utils.toUnicode
import java.io.File

/**
 * 主体预处理脚本
 * 目前为了兼容所以同时兼容荣耀和苍穹，后面看情况是否去掉
 * 同时兼容的弊端：每新增一个主体，就可能要更新一次脚本
 *
 * Usage: java -jar {Jar包地址} {apk反编译后的目录} {图片资源目录} {agreement_type} {前缀cq} {分享链接前面的文字（原来是“荣耀游戏：”）} rongyao666.com {域名文件 domains.xml}
 */
fun main(vararg args: String) {
    val decompileDir = args[0]          // 反编译后的目录
    val coResDir = args[1]              // 主体 LOGO 资源目录
    val coType = args[2]                // 即 agreement_type / registerType，目前已有：区分主体类型，1：荣耀，2：苍穹
    val urlCoPrefix = args[3]           // 主体前缀
    val coText = args[4].toUnicode()    // 分享链接前面的文字，即“XX游戏：”
    val coDomain = args[5]              // 主体域名
    val switchDomainFile = args[7]      // 切换域名的 XML 文件

    // 取出默认的主体类型。注：不能根据配置文件的 agreement_type 来判断，因为母包已经乱了🙃，等后续修正后可以考虑使用该字段判断，现使用判断域名的方式
    val defaultCoType = SmaliHandler.getDefaultCoType(decompileDir)

    println("""
        decompileDir = $decompileDir
        coResDir = $coResDir
        coType = $coType
        urlCoPrefix= $urlCoPrefix
        coText = $coText
        coDomain = $coDomain
        defaultCo = $defaultCoType
        domainFile = $switchDomainFile
    """.trimIndent())

    // 替换主体资源
    DrawableUtil.replaceCoDrawable(decompileDir, coResDir)

    PropertiesUtil(File(decompileDir + File.separator + "assets" + File.separator + "ZSmultil"))
            .setProperties(mapOf(
                    "registerType" to coType,           // 设置主体类型
                    "phone_auth_package" to "com.tencent.tmgp.wzjhhb.wzjh"  // 本机号码一键登录包名设置
            ))

    SmaliHandler.copySpareDomainsFile(decompileDir, switchDomainFile)
    SmaliHandler.setUrlCoPrefix(decompileDir, urlCoPrefix, defaultCoType)
    SmaliHandler.setRegisterAccountPrefix(decompileDir, urlCoPrefix, defaultCoType)
    SmaliHandler.setCoShareText(decompileDir, coText, defaultCoType)
    SmaliHandler.setCoDomain(decompileDir, coDomain, defaultCoType)
}