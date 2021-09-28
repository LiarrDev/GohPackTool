package goh.utils

import kotlin.math.max

/**
 * 中文转 Unicode
 */
fun String.toUnicode(): String {
    val builder = StringBuilder()
    for (i in this.iterator()) {
        val s = i.code
        if (s in 19968..171941) {
            builder.append("\\u").append(Integer.toHexString(s))
        } else {
            builder.append(i)
        }
    }
    return builder.toString()
}

/**
 * 比较版本号大小
 *
 * @param version 要比较的版本号
 * @return 大于 version 返回 1，等于 version 返回 0，小于 version 返回 -1
 */
fun String.compareVersionWith(version: String): Int {
    val bVer = this.split(".")
    val cVer = version.split(".")
    for (index in 0 until max(bVer.size, cVer.size)) {
        val i = if (index < bVer.size) (bVer[index]).toInt() else 0
        val j = if (index < cVer.size) (cVer[index]).toInt() else 0
        if (i < j) {
            return -1
        } else if (i > j) {
            return 1
        }
    }
    return 0
}

/**
 * 判断版本号是否比指定版本号更新
 *
 * @param version 要比较的版本号
 * @return 当版本号大于或等于 version 时为 true，否则为 false
 */
fun String.versionLaterThan(version: String): Boolean {
    return this.compareVersionWith(version) >= 0
}

/**
 * 判断版本号是否比指定版本号更老
 *
 * @param version 要比较的版本号
 * @return 当版本号小于 version 时为 true，否则为 false
 */
fun String.versionOlderThan(version: String): Boolean {
    return this.versionLaterThan(version).not()
}