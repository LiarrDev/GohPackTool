package com.utils

import org.w3c.dom.Document
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object FileUtil {

    /**
     * 删除文件或文件夹
     */
    fun delete(file: File) {
        if (file.exists()) {
            if (file.isFile) {
                file.delete()
            } else if (file.isDirectory) {
                val files = file.listFiles()
                files?.forEach { f ->
                    delete(f)
                }
            }
            file.delete()
        } else {
            println("${file.absoluteFile} is not exist")
        }
    }

    /**
     * 复制目录
     */
    @Deprecated("已用扩展方法重写", ReplaceWith("File(sourceDirPath).copyDir(File(targetDirPath))"))
    fun copyDir(sourceDirPath: String, targetDirPath: String) {
        File(targetDirPath).mkdirs()        // 假如目标文件夹不存在则新建
        val file = File(sourceDirPath)
        val files = file.listFiles()
        if (file.exists()) {
            println("文件夹存在：" + file.name)
        } else {
            println("""
                文件夹不存在：${file.name}
                SourcePath: $sourceDirPath
                TargetPath: $targetDirPath
            """.trimIndent()
            )
        }
        println("""
            文件长度：${files?.size}
            SourcePath: $sourceDirPath
            TargetPath: $targetDirPath
        """.trimIndent()
        )

        files?.forEach { f ->
            if (f.isFile) {
                val targetFile = File(targetDirPath + File.separator + f.name)
                f.replace(targetFile)
            } else if (f.isDirectory) {
                val dir = targetDirPath + File.separator + f
                copyDir(f.absolutePath, dir)
            }
        }
    }

    /**
     * so 库的复制，仅复制到已有的文件夹
     */
    fun copySoLib(soFileDirPath: String, targetDirPath: String) {
        val soFileDir = File(soFileDirPath)
        val targetDir = File(targetDirPath)
        if (soFileDir.exists()) {
            targetDir.getDirectoryList().forEach { dir ->
                File(soFileDirPath, dir.name).apply {
                    if (exists()) {
                        copyDirTo(dir)
                    }
                }
            }
        }
    }

    /**
     * 复制 wxapi 相关文件
     */
    fun copyWeChatLoginFile(decompileDir: String, wxApiFile: File, packageName: String) {
        val tencentPackage = packageName.replace(Regex("\\pP"), "/")
        val targetPath = decompileDir + File.separator +
                "smali" + File.separator +
                tencentPackage + File.separator +
                "wxapi" + File.separator
        wxApiFile.copyDirTo(File(targetPath))

        val activity = File(targetPath + "WXEntryActivity.smali")
        val activityContent = activity.readText().replace("com/tencent/tmgp/njxx2/wxapi", "$tencentPackage/wxapi")
        activity.writeText(activityContent)

        val handler = File(targetPath + "WXEntryActivity\$MyHandler.smali")
        val handlerContent = handler.readText().replace("com/tencent/tmgp/njxx2/wxapi", "$tencentPackage/wxapi")
        handler.writeText(handlerContent)
    }

    /**
     * YSDK 专用的复制 wxapi/WXEntryActivity 的方法，其他渠道勿用
     * 由于 YSDK 注入可能会使方法超限，所以这里也做分 Dex 处理
     */
    fun copyYsdkWxEntry(decompileDir: String, wxApiFile: File, packageName: String) {
        val tencentPackage = packageName.replace(Regex("\\pP"), "/")
        val targetPath = decompileDir + File.separator + "smali_classes2" + File.separator + tencentPackage + File.separator + "wxapi"
        wxApiFile.copyDirTo(File(targetPath))
        val activity = File(targetPath, "WXEntryActivity.smali")
        val content = activity.readText().replace("com/tencent/tmgp/qyj2/qyz/wxapi", "$tencentPackage/wxapi")
        activity.writeText(content)
    }

    /**
     * 写入平台参数
     */
    @Deprecated("由 PropertiesUtil 提供支持", ReplaceWith("PropertiesUtil(propertiesFile).setProperties(map)"))
    fun writePlatformProperties(propertiesFile: File, map: Map<String, String>): Boolean {
        return try {
            val properties = Properties()
            val fis = FileInputStream(propertiesFile)
            properties.load(fis)
            map.forEach { (key, value) ->
                println("Key = $key, Value = $value")
                properties.setProperty(key, value)
            }
            val fos = FileOutputStream(propertiesFile)
            properties.store(fos, "#--#")
            fos.close()
            fis.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 替换图片资源
     */
    fun replaceResource(newImagePath: String?, oldImagePath: String): Boolean {
        return if (newImagePath.isNullOrBlank()) {
            println("文件不存在：$newImagePath")
            true
        } else {
            val file = File(newImagePath)
            if (file.exists() && file.isFile) {
                file.replace(File(oldImagePath))
                true
            } else {
                println("文件出错或不存在！！！！！！")
                false
            }
        }
    }
}

fun File.loadDocument(): Document? {
    var document: Document? = null
    try {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        document = builder.parse(this)
        document.normalize()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return document
}

fun Document.toFile(file: File): Boolean {
    return try {
        val source = DOMSource(this)
        val result = StreamResult(file)
        TransformerFactory.newInstance().newTransformer().transform(source, result)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * 替换文件
 * Usage: File.replace(FileNeedToBeReplaced)
 */
fun File.replace(target: File) {
    if (isFile) {
        if (target.exists()) {
            FileUtil.delete(target)
            println("${target.absolutePath} 已存在，先删除")
        }
        try {
            Files.copy(this.toPath(), target.toPath())
            println("${this.absolutePath}  -->  ${target.absolutePath}")
        } catch (e: Exception) {    // 当 target 父目录不存在时会抛异常，无需理会，因为不存在时不复制，需要复制时使用 File.copyDir() 扩展方法即可
            e.printStackTrace()
            println("如果 ${target.absolutePath} 不存在，该错误无需理会")
        }
//        this.copyTo(target)
    }
}

/**
 * 复制文件到指定目录
 */
fun File.copyDirTo(destDir: File) {
    val files = listFiles()
    files?.forEach { f ->
        val file = File(destDir.absolutePath, f.name)
        if (f.isFile) {
            println("${f.absolutePath}  -->  ${file.absolutePath}")
            f.replace(file)
        } else {
            file.mkdirs()
            f.copyDirTo(file)
        }
    }
}

/**
 * 获取目录中的文件夹列表
 */
fun File.getDirectoryList(): List<File> {
    val folders = mutableListOf<File>()
    listFiles()?.forEach { f ->
        if (f.isDirectory) {
            folders.add(f)
        }
    }
    return folders
}