package goh.utils

import java.io.*

/**
 * 用于处理 Runtime.getRuntime().exec() 产生的错误流及输出流
 */
class StreamGobbler(
    private val inputStream: InputStream,
    private val type: String,
    private val outputStream: OutputStream?
) : Thread() {

    constructor(inputStream: InputStream, type: String) : this(inputStream, type, null)

    override fun run() {
        try {
            var printWriter: PrintWriter? = null
            outputStream?.let {
                printWriter = PrintWriter(it)
            }
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                printWriter?.println(line)
                println("$type > $line")
            }
            printWriter?.flush()
            printWriter?.close()
            bufferedReader.close()
            inputStreamReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}