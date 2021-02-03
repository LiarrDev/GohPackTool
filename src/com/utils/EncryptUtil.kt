package com.utils

import java.lang.StringBuilder
import java.util.*

object EncryptUtil {

    /**
     * 加密 AppId，加密形式： 1. 数学运算，AppId * 5 + 3，得到一个新的数 2. 以每个数字后面插入 3 位随机块，随机块形式为
     * {字母}{数字}{字母}，最终得到一个数字和字母交替的字符串
     */
    fun encryptAppId(appId: String): String {
        val appIdStr = (appId.toInt() * 5 + 3).toString()
        val appIdStrArray = appIdStr.split("".toRegex()).toTypedArray()
        val result = StringBuilder()
        for (s in appIdStrArray) {
            result.append(s)
            result.append(randomChunk())
        }
        return result.toString()
    }

    /**
     * 随机块，形式为 {字母}{数字}{字母}
     */
    private fun randomChunk(): String {
        val random = Random()
        val firstLetter = if (random.nextInt(2) % 2 == 0) 65 else 97
        var chunk = (random.nextInt(26) + firstLetter).toChar().toString()
        chunk += java.lang.String.valueOf(random.nextInt(10))
        val secondLetter = if (random.nextInt(2) % 2 == 0) 65 else 97
        chunk += (random.nextInt(26) + secondLetter).toChar()
        return chunk
    }

    /**
     * 随机生成一个字母
     */
    fun getRandomLetter(): Char {
        val random = Random()
        val letterIndex = if (random.nextInt(2) % 2 == 0) 65 else 97
        return (random.nextInt(26) + letterIndex).toChar()
    }

    /**
     * 随机生成一个假的 AppId
     */
    fun getFakeAppId(): String {
        val randomNum = ((Math.random() * 9 + 1) * 1000).toInt()
        return "15$randomNum"
    }
}