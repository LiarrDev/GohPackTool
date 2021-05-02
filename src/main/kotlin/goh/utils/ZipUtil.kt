package goh.utils

import net.lingala.zip4j.ZipFile
import java.io.File

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