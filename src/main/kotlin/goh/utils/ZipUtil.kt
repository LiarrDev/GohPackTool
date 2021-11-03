package goh.utils

import net.lingala.zip4j.ZipFile
import java.io.File

/**
 * 用于打广告包时解压与压缩，相比 ApkTool 反编译效率更高，但只兼容 JarSigner，升级为 ApkSigner 后已无用，后续可移除
 */

fun File.unzipTo(dir: File) {
    ZipFile(this).extractAll(dir.absolutePath)
}

fun File.zipTo(zip: File) {
    if (zip.exists()) {
        zip.delete()
    }
    this.listFiles()?.forEach {
        if (it.isDirectory) {
            ZipFile(zip).addFolder(it)
        } else {
            ZipFile(zip).addFile(it)
        }
    }
}