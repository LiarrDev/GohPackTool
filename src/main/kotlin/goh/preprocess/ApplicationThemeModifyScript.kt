package goh.preprocess

import goh.utils.AndroidXmlHandler
import java.io.File

/**
 * 用于修改 AndroidManifest.xml 中 android:theme
 * SDK 改版为 H5 后该脚本已废弃
 */
fun main(vararg args: String) {
    AndroidXmlHandler.setApplicationTheme(File(args[0]))
}