package com.utils

import org.w3c.dom.Document
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import com.sun.org.glassfish.external.amx.AMXUtil.prop
import java.io.FileOutputStream


object FileUtil {

    fun delete(file: File) {
        if (file.exists()) {
            if (file.isFile) {
                file.delete()
            } else if (file.isDirectory) {
                val files = file.listFiles()
                if (files != null) {
                    for (i in files.indices) {
                        delete(files[i])
                    }
                }
            }
            file.delete()
        } else {
            println("${file.absoluteFile} is not exist")
        }
    }

    @Deprecated("已用扩展方法重写")
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
            """.trimIndent())
        }
        println("""
            文件长度：${files?.size}
            SourcePath: $sourceDirPath
            TargetPath: $targetDirPath
        """.trimIndent())

        if (files != null) {
            for (i in files.indices) {
                if (files[i].isFile) {
                    val sourceFile = files[i]
                    val targetFile = File(targetDirPath + File.separator + files[i].name)
                    sourceFile.replace(targetFile)
                } else if (files[i].isDirectory) {
                    val dir = targetDirPath + File.separator + files[i].name
                    copyDir(files[i].absolutePath, dir)
                }
            }
        }
    }

    /**
     * 获取目录中的文件夹列表
     */
    fun getDirectoryList(dir: File): List<String> {
        val files = dir.listFiles()
        val folders = mutableListOf<String>()
        files?.let {
            for (i in it.indices) {
                if (it[i].isDirectory) {
                    folders.add(it[i].name)
                }
            }
        }
        return folders
    }

    /**
     * so 库的复制，仅复制到已有的文件夹
     */
    fun copySoLib(soFileDirPath: String, targetDirPath: String) {
        val soFileDir = File(soFileDirPath)
        val targetDir = File(targetDirPath)
        if (soFileDir.exists()) {
            val existList = targetDir.listFiles()
            existList?.let {
                for (i in it.indices) {
                    if (it[i].isDirectory) {
                        val existDir = File(soFileDirPath + File.separator + it[i].name)
                        if (existDir.exists()) {
                            existDir.copyDir(it[i])
                        }
                    }
                }
            }
        }
    }

    fun copyWeChatLoginFile(decompileDir: String, wxApiFile: File, packageName: String) {
        val tencentPackage = packageName.replace(Regex("\\pP"), "/")
        val targetPath = decompileDir + File.separator + "smali" + File.separator + tencentPackage + File.separator + "wxapi" + File.separator
        wxApiFile.copyDir(File(targetPath))

        val activity = File(targetPath + "WXEntryActivity.smali")
        val activityContent = activity.readText().replace("com/tencent/tmgp/njxx2/wxapi", "$tencentPackage/wxapi")
        activity.writeText(activityContent)

        val handler = File(targetPath + "WXEntryActivity\$MyHandler.smali")
        val handlerContent = handler.readText().replace("com/tencent/tmgp/njxx2/wxapi", "$tencentPackage/wxapi")
        handler.writeText(handlerContent)
    }

    fun writePlatformProperties(propertiesFile: File, map: Map<String, String>): Boolean {
        return try {
            val properties = Properties()
            val fis = FileInputStream(propertiesFile)
            properties.load(fis)
            for ((key, value) in map) {
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
 * 替换文件，Usage: File.replace(FileNeedToBeReplaced)
 */
fun File.replace(target: File) {
    if (isFile) {
        if (target.exists()) {
            FileUtil.delete(target)
        }
        try {
            Files.copy(this.toPath(), target.toPath())
        } catch (e: Exception) {    // 当 target 目录不存在时会抛异常，无需理会，因为不存在时不复制，需要复制时使用 File.copyDir() 扩展方法即可
            e.printStackTrace()
            println("如果 ${target.absolutePath} 不存在，该错误无需理会")
        }
//        this.copyTo(target)
    }
}

fun File.copyDir(destDir: File) {
    val files = listFiles()
    if (files != null) {
        for (f in files) {
            val file = File(destDir.absolutePath, f.name)
            if (f.isFile) {
                println("${f.absolutePath}  -->  ${file.absolutePath}")
                f.replace(file)
            } else {
                file.mkdirs()
                f.copyDir(file)
            }
        }
    }
    return
}