package com.utils

import java.io.BufferedReader
import java.io.InputStreamReader

object CommandUtil {
    internal fun exec(command: String): Boolean {
        val process: Process = Runtime.getRuntime().exec(command)
        val errorStream = StreamGobbler(process.errorStream, "ERROR")
        val outputStream = StreamGobbler(process.inputStream, "OUTPUT")
        errorStream.start()
        outputStream.start()

        return if (process.waitFor() == 0) {
            println("$command    Command execute succeed.")
            true
        } else {
            println("$command    Command execute failed.")
            false
        }
    }
}

fun String.execute(): Process {
    return Runtime.getRuntime().exec(this)
}

fun Process.text(): String {
    val inputStream = this.inputStream
    val isr = InputStreamReader(inputStream)
    val reader = BufferedReader(isr)
    var line: String? = ""
    var output = ""
    while (line != null) {
        line = reader.readLine()
        output += line + "\n"
    }
    return output
}