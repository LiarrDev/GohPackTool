package com.utils

import net.lingala.zip4j.ZipFile
import java.io.File

fun File.unzipTo(dir:File){
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

/**
 * ***************  以下方法依赖 Apache Compress 库，准备移除  ********************
 */

//fun File.unzipTo(dir: File) {
//    val buffer = ByteArray(1024)
//    val zipFile = ZipFile(this)
//    val entries = zipFile.entries()
//    while (entries.hasMoreElements()) {
//        val zipEntry = entries.nextElement()
//        val zipEntryName = zipEntry.name
//
//        val inputStream = zipFile.getInputStream(zipEntry)
//        val file = File(dir, zipEntryName).existsOrCreate()
//        val outputStream = file.outputStream()
//
//        var len: Int
//        while (inputStream.read(buffer).also { len = it } > 0) {
//            outputStream.write(buffer, 0, len)
//        }
//        inputStream.close()
//        outputStream.close()
//    }
//}

//fun File.zipFolderTo(zip: File) {
//    val aos = ZipArchiveOutputStream(zip)
//    aos.encoding = "UTF-8"
//    aos.zipArchiveOutputStreamFromDir(this)
//    IOUtils.closeQuietly(aos)
//}
//
//private fun ZipArchiveOutputStream.zipArchiveOutputStreamFromDir(dir: File, entryPath: String = "") {
//    dir.listFiles()?.forEach {
//        if (it.isDirectory) {
//            val size = it.listFiles()?.size ?: 0
//            if (size > 0) {
//                this.zipArchiveOutputStreamFromDir(it, entryPath + it.name + File.separator)
//            } else {
//                this.zipArchiveOutputStreamFromFile(it, entryPath)
//            }
//        } else {
//            this.zipArchiveOutputStreamFromFile(it, entryPath)
//        }
//    }
//    this.flush()
//}
//
//private fun ZipArchiveOutputStream.zipArchiveOutputStreamFromFile(file: File, entryPath: String) {
//    var path = entryPath + file.name
//    if (file.isDirectory) {
//        if (!path.endsWith(File.separator)) {
//            path += File.separator
//        }
//    }
//    val entry = ZipArchiveEntry(path)
//    entry.time = file.lastModified()
//    putArchiveEntry(entry)
//    if (file.isFile) {
//        val stream = file.inputStream()
//        IOUtils.copy(stream, this)
//        IOUtils.closeQuietly(stream)
//    }
//    this.closeArchiveEntry()
//}