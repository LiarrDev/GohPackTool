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
 * @param benchmark 比较基准
 * @param compareWith 比较对象
 * @return (benchmark > compareWith) == 1；(benchmark == compareWith) == 0；(benchmark < compareWith) == -1
 */
fun compareVersion(benchmark: String, compareWith: String): Int {
    val bVer = benchmark.split(".")
    val cVer = compareWith.split(".")
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