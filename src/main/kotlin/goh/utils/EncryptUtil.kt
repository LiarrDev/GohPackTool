package goh.utils

object EncryptUtil {

    /**
     * 加密 AppId，加密形式：
     * 1. 数学运算，AppId * 5 + 3，得到一个新的数
     * 2. 以每个数字后面插入 3 位随机块，随机块形式为 {字母}{数字}{字母}，最终得到一个数字和字母交替的字符串
     */
    fun encryptAppId(appId: String): String {
        val appIdStr = (appId.toInt() * 5 + 3).toString()
        val appIdStrArray = appIdStr.split("".toRegex())
            .filter { it.isNotEmpty() }     // Kotlin 的 split() 与 Java 稍有不同，所以需要过滤空的子项
            .toTypedArray()
        val result = StringBuilder()
        appIdStrArray.forEach {
            result.append(it)
            result.append(randomChunk())
        }
        return result.toString()
    }

    /**
     * 随机块，形式为 {字母}{数字}{字母}
     */
    private fun randomChunk(): String {
        return getRandomLetter() + (0..9).random() + getRandomLetter()
    }

    /**
     * 随机生成一个字母
     */
    private fun getRandomLetter(): String {
        val letterIndex = if ((0..1).random() % 2 == 0) 65 else 97
        return ((0 until 26).random() + letterIndex).toChar().toString()
    }

    /**
     * 随机生成一个假的 AppId
     */
    fun getFakeAppId(): String {
        val randomNum = (1000..9999).random()
        return "15$randomNum"
    }
}