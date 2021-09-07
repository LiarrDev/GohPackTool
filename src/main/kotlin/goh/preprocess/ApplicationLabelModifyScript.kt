package goh.preprocess

import goh.utils.AndroidXmlHandler
import java.io.File

/**
 * 用于修改 AndroidManifest.xml 中 android:label 属性不为 app_name 的工具
 */
fun main(vararg args: String) {
    AndroidXmlHandler.setApplicationLabel(File(args[0]))
}