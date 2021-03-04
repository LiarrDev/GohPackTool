package com.utils

import java.nio.CharBuffer

/**
 * 中文转 Unicode
 */
fun String.toUnicode(): String {
    val builder = StringBuilder()
    for (i in this.iterator()) {
        val s = i.toInt()
        if (s in 19968..171941) {
            builder.append("\\u").append(Integer.toHexString(s))
        } else {
            builder.append(i)
        }
    }
    return builder.toString()
}